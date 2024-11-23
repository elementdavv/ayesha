package net.timelegend.ayesha;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.Manifest;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Environment;
import android.widget.RelativeLayout;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ContentLoadingProgressBar;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

public class MyWebView extends WebView {
    protected String target;

    protected Job job;
    protected Js js;
    protected Context context;
    protected MyWebView creator;
    protected final long time;
    protected String bookTitle;
    protected boolean downloading;
    protected int percent;
    protected int progress;
    protected DownloadManager dm;
    protected DownloadManager.Request rq;

    public MyWebView(Context context, MyWebView creator) {
        super(context);
        this.context = context;
        this.creator= creator;
        time = System.currentTimeMillis();
        bookTitle = null;
        downloading = false;
        percent = 0;
        progress = 0;
        dm = null;
        rq = null;

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        setLayoutParams(params);
        setWebViewClient(new MyWebViewClient());
        setWebChromeClient(new MyWebChromeClient());
        setDownloadListener(new MyDownloadListener());
        setOnLongClickListener(new MyOnLongClickListener());
        setupView();
    }

    protected void setupView() {
        WebSettings settings = this.getSettings();
        settings.setJavaScriptEnabled(true);
	    settings.setDomStorageEnabled(true);
	    settings.setAllowFileAccess(true);
	    settings.setLoadsImagesAutomatically(true);
	    settings.setCacheMode(WebSettings.LOAD_DEFAULT);
	    settings.setSupportZoom(true);
	    settings.setBuiltInZoomControls(true);
	    settings.setDisplayZoomControls(false);
	    settings.setUseWideViewPort(true);
	    settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL); //TEXT_AUTOSIZING
	    settings.setLoadWithOverviewMode(true);
	    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
    }

    class MyWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView v, String url, Bitmap favicon) {
            Log.i("start " + url);
            initJsInterface(url);
            super.onPageStarted(v, url, favicon);
        }

        /*
         * return true to cancel the loading
         */
        @Override
        public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
            Log.i("override " + req.getUrl().toString());
            return false;
        }

        /*
         * hathiturst use Let's Encrypt certificates
         * for api < 24(android 7.0), ignore and proceed
         * references:
         * https://stackoverflow.com/questions/47120633/webview-failed-to-validate-the-certificate-chain
         * https://stackoverflow.com/questions/59442126/handshake-failed-returned-1-ssl-error-code-1-net-error-202
         */
        @Override
        public void onReceivedSslError(WebView v, SslErrorHandler handler, SslError e) {
            handler.proceed();
        }

        @Override
        public void onPageFinished(WebView v, String url) {
            super.onPageFinished(v, url);
            Log.i("finished " + url);
            bookTitle = null;
            ((MainActivity)context).onTabChanged();

            if (url.indexOf(Coordinator.archiveDetail) > -1) {
                Log.i("prepare for: " + url);
                js.prepare();
            }
            else if (url.indexOf(Coordinator.hathitrustDetail) > -1) {
                if (url.equals(target)) {
                    target = "";
                    Log.i("prepare for: " + url);
                    js.prepare();
                }
                else {
                    target = url;
                }
            }
        }
    }

    public void initJsInterface(String url) {
        if (url.indexOf(Coordinator.archive) > -1) {
            if (js == null || !(js instanceof ArchiveJs)) {
                job = new ArchiveJob(context, getThis());
                js = new ArchiveJs(context, getThis());
                js.setJob(job);
                addJavascriptInterface(job, "job");
                Log.i("javascript interface added.");
            }
        }
        else if (url.indexOf(Coordinator.hathitrustAny) > -1) {
            if (js == null || !(js instanceof HathitrustJs)) {
                job = new HathitrustJob(context, getThis());
                js = new HathitrustJs(context, getThis());
                js.setJob(job);
                addJavascriptInterface(job, "job");
                Log.i("javascript interface added.");
            }
        }
        else {
            if (js != null) {
                removeJavascriptInterface("job");
                Log.i("javascript interface removed.");
                js = null;
                job = null;
            }
        }
    }

    protected class MyWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView v, int newProgress) {
            progress = newProgress == 100 ? 0 : newProgress;

            if (isCurrrent()) {
                updateProgress();
            }
        }
    }

    public void updateProgress() {
        ContentLoadingProgressBar progressBar = ((MainActivity)context).getProgressBar();

        if (progress == 0) {
            if (progressBar.getVisibility() == View.VISIBLE) {
                progressBar.hide();
            }
        }
        else {
            if (progressBar.getVisibility() != View.VISIBLE) {
                progressBar.show();
            }

            progressBar.setProgress(progress);
        }
    }

    protected class MyDownloadListener implements DownloadListener {
        @Override
        public void onDownloadStart(String url, String userAgent,
                String contentDisposition, String mimetype, long contentLength) {

            if (dm == null) {
                dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            }

            rq = new DownloadManager.Request(Uri.parse(url));
            rq.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            rq.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimetype));

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                directDownload();
            }
            else {
                ActivityCompat.requestPermissions(
                    (MainActivity) context
                    , new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}
                    , Coordinator.PERMISSION_REQUEST_CODE_DIRECT
                );
            }
        }
    }

    public void directDownload() {
        if (rq != null) {
            dm.enqueue(rq);
            rq = null;
        }
    }

    protected class MyOnLongClickListener implements OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            WebView.HitTestResult result = ((WebView) v).getHitTestResult();

            if (result == null) return false;

            int type = result.getType();
            String extra = result.getExtra();
            switch (type) {
                case WebView.HitTestResult.SRC_ANCHOR_TYPE:
                    showUrlActionDialog(extra);
                    break;
                case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
                    Message msg = new Message();
                    msg.setTarget(new LongClickHandler(Looper.getMainLooper()));
                    ((WebView) v).requestFocusNodeHref(msg);
                    break;
                default:
                    break;
            }
            return true; 
        }
    }

    protected void showUrlActionDialog(String url) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setItems(R.array.url_actions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((MainActivity)context).newTab(url, getThis(), which == 0);
            }
        }).show();
    }

    protected class LongClickHandler extends Handler {
        public LongClickHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            String url = msg.getData().getString("url");
            showUrlActionDialog(url);
        }
    }

    @Override
    public String toString() {
        if (bookTitle == null) {
            String title = getTitle();
            if (title != null && !"".equals(title)) {
                bookTitle = title;
            }
            else {
                bookTitle = getUrl();
            }
        }

        if (!downloading) return bookTitle;

        return "(" + percent + "%) " + bookTitle ;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MyWebView) {
            return ((MyWebView)obj).getTime() == time;   
        }

        return false;
    }

    @Override
    public int hashCode() {
        return (int)time;
    }

    @Override
    public void destroy() {
        if (downloading) job.abort();

        js = null;
        job = null;
        creator = null;
        stopLoading();
        clearHistory();
        clearCache(false);
        super.loadUrl("about:blank");
        super.destroy();
    }

    public boolean isCurrrent() {
        return (equals(((MainActivity) context).getCurrentView()) && ((MainActivity) context).isInMain());
    }

    public MyWebView getThis() {
        return this;
    }

    public MyWebView getCreator() {
        return creator;
    }

    public Job getJob() {
        return job;
    }

    public long getTime() {
        return time;
    }

    public boolean isDownloading() {
        return downloading;
    }

    public void start() {
        downloading = true;
        percent = 0;
        ((MainActivity)context).onTabChanged();
    }

    public void stop() {
        downloading = false;
        ((MainActivity)context).onTabChanged();
    }

    public void update(int percent) {
        this.percent = percent;
        ((MainActivity)context).onTabChanged();
    }

    public void runJs(int stage, String cmd) {
        String jstr = "javascript:(()=>{" + cmd + "})()";
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                evaluateJavascript(jstr, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        // the return value was surrounded by quatation mark if it is a String in business environment
                        if (stage > 0) {
                            js.consume(stage, value);
                        }
                    }
                });
            }
        });
    }

    public void runJs2(int stage, String filepath) {
        runJs(stage, getStringFromFile(filepath));
    }

    public void runJs2(int stage, String filepath, String search, String replace) {
        runJs(stage, getStringFromFile(filepath).replaceAll(search, replace));
    }

    protected String getStringFromFile(String filePath) {
        StringBuilder sb = new StringBuilder();
        try {
            InputStream is = context.getAssets().open(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            is.close();
        }
        catch(IOException e) {
            e.printStackTrace();
            ((MainActivity)context).alert("Error", e.getMessage());
        }
        return sb.toString();
    }
}
