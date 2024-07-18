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

import net.timelegend.crl.InvalidImageException;
import net.timelegend.crl.PDFWriter;

public abstract class Job {
    protected enum Status {READY, START, COMPLETE, FAIL, WAIT};
    protected enum What {
        SUCCESS(0), ERROR(1), REJECT_429(2), RETURN_BOOK(10);
        private final int value;
        private What(int value) { this.value = value; }
        public int get() { return value; }
    };

    protected final static int CORE_POOL_SIZE = 5;
    protected final static int MAX_POOL_SIZE = 25;
    protected final static long KEEP_ALIVE_TIME = 60L;
    protected final static int TRI_LIMIT = 3;
    protected final static int BUFFERSIZE = 8192;

    protected Context context;
    protected MyWebView v;
    protected Handler handler;
    protected Object docLock;
    protected String origin;
    protected Status status;

    protected String fileId;
    protected String referer;
    protected int pagecount;
    protected Map<String, String> info;
    protected String scale;
    protected String filename;

    protected String cookies;
    protected AdditionalSSLSocketFactory additionalSSLSocketFactory;
    protected File file;
    protected PDFWriter doc;
    protected int tasks;
    protected ConcurrentLinkedQueue<JobInfo> jobs;
    protected AtomicInteger jobcount;
    protected int complete;
    protected int paused;
    protected int recover;
    protected Timer timer;

    protected final static File downloadPath = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    protected final static ThreadPoolExecutor executor = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

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

    public void setInfo(Map<String, String> data) {
        this.info = data;
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
                    , PERMISSION_REQUEST_CODE
                );
            }
        }
    }

    public void begin2() {
        cookies = CookieManager.getInstance().getCookie(origin);
    }

    public void getFile() {
        Log.i("new job: " + filename + ", " + pagecount + " trunks.");
        file = new File(downloadPath, filename);
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file);
		    doc = new PDFWriter(fos, info);
        }
        catch(IOException e) {
            e.printStackTrace();
            doc = null;
            fos = null;
            ((MainActivity)context).alert("Error", e.getMessage());
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
        Log.i("paused " + paused + ": " + filename);
    }

    synchronized protected void resume() {
        Log.i("resume " + paused + ": " + filename);

        if (--paused <= 0) {
            paused = 0;
            while (recover-- > 0) {
                dispatch();
            }
        }
    }

    synchronized protected void toWait() {
        if (status == Status.START) {
            waitNotify();
            pause();
            timer = new Timer();

            timer.scheduleAtFixedRate(new TimerTask() {
                int waitlen = 61;

                @Override
                public void run() {
                    if (--waitlen <= 0) {
                        toContinue();
                    }
                    else {
                        waitProgressNotify(String.valueOf(waitlen));
                    }
                }
            }, 0, 1000);
        }
    }

    synchronized protected void toContinue() {
        if (status == Status.WAIT) {
            endTimer();
            continueNotify();
            resume();
        }
    }

    synchronized protected void nextLeaf() {
        if (++complete >= jobcount.get()) {
            Log.i(filename + " completes");
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
                catch(IOException e) {
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
            endTimer();
            clear();
            file.delete();

            if (msg != null)
                ((MainActivity)context).alert("Error", msg);
        }
    }

    synchronized private void endTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    protected void got(int pageIndex, int tri, String uri) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.i(filename + " " + pageIndex + " start");
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
                            Log.i(filename + " " + pageIndex + " close");
                            baos.close();
                            is.close();
                            return;
                        }

                        baos.write(buf, 0, n);
                    }

                    is.close();
                    Log.i(filename + " " + pageIndex + " end");
                    msg.what = What.SUCCESS.get();
                    Bundle bundle = new Bundle();
                    bundle.putByteArray("src", baos.toByteArray());
                    msg.setData(bundle);
                    baos.close();
                }
                catch(IOException e) {
                    Log.i(filename + " " + pageIndex + " fail");
                    e.printStackTrace();
                    if (e.getMessage().indexOf("429") > -1) {   // too many request
                        msg.what = What.REJECT_429.get();
                    }
                    else {
                        msg.what = What.ERROR.get();
                    }
                }
                finally {
                    if (status == Status.START)
                        msg.sendToTarget();
                }
            }
        });
    }

    protected void costMessage(Message msg) {
        int pageIndex = msg.arg1;

        // return book
        if (msg.what == What.RETURN_BOOK.get()) {
            if (msg.arg1 == HttpsURLConnection.HTTP_OK) {
                v.reload();
            }
            else {
                Log.i(filename + " return failed: " + msg.arg1);
            }
            return;
        }
        if (msg.what == What.SUCCESS.get()) {
            byte[] src = msg.getData().getByteArray("src");
            try {
                synchronized(docLock) {
                    if (doc != null) {
                        doc.newImagePage(pageIndex, src);
                    }
                    else return;
                }
            }
            catch(InvalidImageException | IOException e) {
                Log.i(filename + " " + pageIndex + " bad");
                e.printStackTrace();
                // for debug
                // if (e instanceof InvalidImageException) {
                //     saveToFile(src, fileId + "_" + pageIndex + ".jpg");
                // }
                msg.what = What.ERROR.get();
            }
            if (msg.what == What.SUCCESS.get()) {
                nextLeaf();
                return;
            }
        }
        // what: 1/2
        int tri = msg.arg2;
        if (tri < TRI_LIMIT || msg.what == What.REJECT_429.get()) {
            if (msg.what == What.REJECT_429.get()) {
                toWait();
            }
            else {
                tri++;
            }
            jobs.offer(new JobInfo(pageIndex, tri));
            jobcount.incrementAndGet();
            nextLeaf();
        }
        else {
            Log.i(filename + " " + pageIndex + " too many errors");
            abort("Network errors. Abort.");
        }
    }

    // for debug
    // private void saveToFile(byte[] src, String fn) {
    //     File f = new File(downloadPath, fn);
    //     FileOutputStream fos = null;

    //     try {
    //         fos = new FileOutputStream(f);
    //         fos.write(src);
    //     }
    //     catch(IOException e) {
    //         e.printStackTrace();
    //     }
    //     finally {
    //         fos = null;
    //     }
    // }

    protected void returnBook() {}

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
        runJs2(0, "waitprogressnotify.js", "progress", waitlen);
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
