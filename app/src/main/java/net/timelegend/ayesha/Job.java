package net.timelegend.ayesha;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.os.Environment;
import android.os.Handler;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

// import crl.android.pdfwriter.InvalidImageException;
import crl.android.pdfwriter.PDFWriter;

public abstract class Job {
    protected enum Status {READY, START, WAIT, COMPLETE, FAIL};
    protected enum What {
        SUCCESS(0),
        ERROR(1),
        _429(2), /* temporarily unavailable in hathitrust, wait 60 seconds */
        RETURN_BOOK(10);
        private final int value;
        private What(int value) { this.value = value; }
        public int get() { return value; }
    };

    protected final static long KEEP_ALIVE_TIME = 60L;
    protected final static int TRI_LIMIT = 3;
    protected final static int BUFFERSIZE = 8192;
    protected final static int WAIT_SECONDS = 61;

    protected Context context;
    protected MyWebView v;
    protected Object docLock;
    protected Handler handler;
    protected String origin;
    protected Status status;
    protected File downloadPath;
    protected int availableProcessors;
    protected ThreadPoolExecutor executor;

    protected String fileId;
    protected String referer;
    protected int pagecount;
    protected Map<String, String> info;
    protected String scale;
    protected String filename;

    protected String cookies;
    protected SSLSocketFactory additionalSSLSocketFactory;
    protected File file;
    protected PDFWriter doc;
    protected int tasks;
    protected ConcurrentLinkedQueue<JobInfo> jobs;
    protected AtomicInteger jobcount;
    protected int complete;
    protected int paused;
    protected int recover;
    protected Timer timer;

    protected class JobInfo {
        int pageIndex;
        int tri;

        public JobInfo(int pageIndex, int tri) {
            this.pageIndex = pageIndex;
            this.tri = tri;
        }
    }

    public Job(Context context, MyWebView v) {
        this.context = context;
        this.v = v;
        docLock = new Object();

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                costMessage(msg);
            }
        };

        downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        availableProcessors = Runtime.getRuntime().availableProcessors();
        executor = null;

        additionalSSLSocketFactory = null;
        jobs = new ConcurrentLinkedQueue<JobInfo>();
        jobcount = new AtomicInteger();
    }

    // called on every page loaded
    public void initReady() {
        readyNotify();
    }

    public abstract void setFileId(String data);
    public abstract void setData(Object data);
    public abstract void setScale(String data);
    protected abstract void dispatch();

    public void setInfo(Map<String, String> info) {
        this.info = info;
    }

    public int getAvailableProcessors() {
        return availableProcessors;
    }

    @JavascriptInterface
    public void begin() {
        Log.i("begin");
        if (status == Status.START) {
            new AlertDialog.Builder(context, R.style.AlertDialogTheme)
                    .setTitle("Warn")
                    .setMessage("To abort?")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ((MainActivity) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    abort();
                                }
                            });
                        }
                    })
                    .setCancelable(true)
                    .create().show();
        }
        else if (status == Status.COMPLETE || status == Status.FAIL) {
            readyNotify();
        }
        else if (status == Status.WAIT) {
            toContinue();
        }
        else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                begin2();
            } else {
                int PERMISSION_REQUEST_CODE = 1;
                ActivityCompat.requestPermissions(
                    (MainActivity) context
                    , new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}
                    , Coordinator.PERMISSION_REQUEST_CODE_JOB
                );
            }
        }
    }

    public void begin2() {
        cookies = CookieManager.getInstance().getCookie(origin);
    }

    public void getFile() {
        Log.i(filename + " " + pagecount + " trunks");
        file = new File(downloadPath, filename);
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file);
		    doc = new PDFWriter(fos, info);
        }
        catch (IOException e) {
            e.printStackTrace();
            doc = null;
            fos = null;
            ((MainActivity)context).alert("Error", e.getMessage());
            return;
        }

        getLeafs();
    }

    protected void initLeaf() {
        jobs.clear();
        int i = 0;

        while (i < pagecount) {
            jobs.offer(new JobInfo(i++, 0));
        }

        jobcount.set(pagecount);
        complete = 0;
        paused = 0;
        recover = 0;
        timer = null;
        endExecutor();

        executor = new ThreadPoolExecutor(
                availableProcessors,
                availableProcessors,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    protected void getLeafs() {
        startNotify();
        initLeaf();
        int i = 0;

        while (i++ < tasks) {
            dispatch();
        }
    }

    synchronized protected void pause() {
        paused++;
        Log.i(filename + " pause");
    }

    synchronized protected void resume() {
        paused--;
        Log.i(filename + " resume " + recover);

        if (paused <= 0) {
            paused = 0;
            while (recover > 0) {
                dispatch();
                recover--;
            }
        }
    }

    synchronized protected void toWait() {
        if (status == Status.START) {
            waitNotify();
            pause();
            timer = new Timer();

            timer.scheduleAtFixedRate(new TimerTask() {
                int waitlen = WAIT_SECONDS;

                @Override
                public void run() {
                    if (--waitlen <= 0) {
                        toContinue();
                    }
                    else {
                        waitProgressNotify(String.valueOf(waitlen));
                    }
                }
            }
            , /*delay*/0
            , /*period*/1000);
        }
    }

    synchronized protected void toContinue() {
        if (status == Status.WAIT) {
            endTimer();
            continueNotify();
            resume();
        }
    }

    synchronized private void endTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    synchronized protected void nextLeaf() {
        if (++complete >= jobcount.get()) {
            Log.i(filename + " complete");
            clear();
            completeNotify();
            returnBook();
        }
        else {
            updateNotify();

            if (paused > 0) {
                recover++;
            }
            else {
                dispatch();
            }
        }
    }

    protected void clear() {
        synchronized(docLock) {
            if (doc != null) {
                try {
                    doc.end();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                finally {
                    doc = null;
                }
            }
        }
    }

    public void abort() {
        abort(null);
    }

    synchronized protected void abort(String msg) {
        if (status != Status.FAIL) {
            Log.i(filename + " aborted");
            failNotify();
            endExecutor();
            endTimer();
            clear();
            file.delete();

            if (msg != null) {
                ((MainActivity)context).alert("Error", msg);
            }
        }
    }

    protected void got(int pageIndex, int tri, String uri) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.i(filename + " " + pageIndex + " " + tri);
                Message msg = handler.obtainMessage();
                msg.arg1 = pageIndex;
                msg.arg2 = tri;

                try {
                    URL url = new URL(uri);
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setRequestProperty("cookie", cookies);
                    conn.setRequestProperty("Referer", referer);

                    if (additionalSSLSocketFactory != null) {
                        conn.setDefaultSSLSocketFactory(additionalSSLSocketFactory);
                        conn.setDefaultHostnameVerifier(new HostnameVerifier() {
                            @Override
                            public boolean verify(String hostname, SSLSession session) {
                                return true;
                            }
                        });
                    }

                    conn.connect();
                    InputStream is = conn.getInputStream();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[BUFFERSIZE];
                    int n;

                    while ( (n = is.read(buf)) > 0 ) {
                        if (status == Status.FAIL) {
                            Log.w(filename + " " + pageIndex + " close");
                            baos.close();
                            is.close();
                            conn.disconnect();
                            return;
                        }

                        baos.write(buf, 0, n);
                    }

                    is.close();
                    conn.disconnect();
                    msg.what = What.SUCCESS.get();
                    Log.i(filename + " " + pageIndex + " done");
                    Bundle bundle = new Bundle();
                    bundle.putByteArray("src", baos.toByteArray());
                    msg.setData(bundle);
                    baos.close();
                }
                // temporarily unavailable in hathitrust
                catch (FileNotFoundException e) {
                    Log.e(filename + " " + pageIndex + " not found");
                    msg.what = What._429.get();
                }
                catch (IOException e) {
                    Log.e(filename + " " + pageIndex + " fail");
                    e.printStackTrace();
                    // too many request, temporarily unavailable in hathitrust
                    if (e.getMessage().indexOf("429") > -1) {
                        msg.what = What._429.get();
                    }
                    else {
                        msg.what = What.ERROR.get();
                    }
                }
                finally {
                    if (status == Status.START || status == Status.WAIT) {
                        msg.sendToTarget();
                    }
                }
            }
        });
    }

    protected void costMessage(Message msg) {
        int pageIndex = msg.arg1;
        int tri = msg.arg2;

        // return book
        if (msg.what == What.RETURN_BOOK.get()) {
            if (msg.arg1 == HttpsURLConnection.HTTP_OK) {
                v.reload();
            }
            else {
                Log.w(filename + " return fail " + msg.arg1);
            }
        }
        else if (msg.what == What.SUCCESS.get()) {
            byte[] src = msg.getData().getByteArray("src");
            try {
                synchronized(docLock) {
                    if (doc == null) {
                        throw new RuntimeException("Null document");
                    }
                    else {
                        doc.newImagePage(pageIndex, src);
                    }
                }
            }
            catch (Exception e) {
                // for test purpose
                // if (e instanceOf InvalidImageException)
                //     saveToFile(src, fileId + "_" + pageIndex + ".jpg");
                Log.e(filename + " " + pageIndex + " " + e.getMessage());
                e.printStackTrace();
                abort(e.getMessage());
                return;
            }

            nextLeaf();
        }
        else if (msg.what == What._429.get() || tri < TRI_LIMIT) {
            if (msg.what == What._429.get()) {
                toWait();
            }
            else {
                tri++;
            }

            jobs.offer(new JobInfo(pageIndex, tri));
            jobcount.incrementAndGet();
            nextLeaf();
        }
        else /* What.ERROR && tri >= TRI_LIMIT */{
            Log.e(filename + " " + pageIndex + " too many errors");
            abort("Too many errors");
        }
    }

    // for test purpose
    // private void saveToFile(byte[] src, String fn) {
    //     File f = new File(downloadPath, fn);
    //     FileOutputStream fos = null;

    //     try {
    //         fos = new FileOutputStream(f);
    //         fos.write(src);
    //     }
    //     catch (IOException e) {
    //         e.printStackTrace();
    //     }
    //     finally {
    //         fos = null;
    //     }
    // }

    protected void returnBook() {
        endExecutor();
    }

    synchronized protected void endExecutor() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    protected void readyNotify() {
        status = Status.READY;
        runJs2(0, "readynotify.js");
    }

    protected void startNotify() {
        status = Status.START;
        runJs2(0, "startnotify.js");
        v.start();
    }

    protected void updateNotify() {
        int percent = 100 * complete / jobcount.get();
        runJs2(0, "updatenotify.js", "percent", String.valueOf(percent));
        v.update(percent);
    }

    protected void waitNotify() {
        status = Status.WAIT;
        runJs2(0, "waitnotify.js");
    }

    protected void waitProgressNotify(String waitlen) {
        runJs2(0, "waitprogressnotify.js", "progresss", waitlen);
    }

    protected void continueNotify() {
        status = Status.START;
        runJs2(0, "continuenotify.js");
    }

    protected void completeNotify() {
        status = Status.COMPLETE;
        runJs2(0, "completenotify.js");
        v.stop();
    }

    protected void failNotify() {
        status = Status.FAIL;
        runJs2(0, "failnotify.js");
        v.stop();
    }

    protected void runJs2(int stage, String filepath) {
        v.runJs2(stage, filepath);
    }

    protected void runJs2(int stage, String filepath, String search, String replace) {
        v.runJs2(stage, filepath, search, replace);
    }

    protected void runJs(int stage, String cmd) {
        v.runJs(stage, cmd);
    }
}
