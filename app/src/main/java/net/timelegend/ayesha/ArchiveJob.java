package net.timelegend.ayesha;

import android.content.Context;
import android.os.Message;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;

public class ArchiveJob extends Job {
    private JSONArray data;

    public ArchiveJob(Context context, MyWebView v) {
        super(context, v);
        origin = Coordinator.archive;
    }

    @Override
    public void setFileId(String data) {
        fileId = data;
        referer = Coordinator.archiveDetail + fileId;
    }

    @Override
    public void setData(Object data) {
        this.data = (JSONArray)data;
        pagecount = this.data.length();
    }

    @Override
    public void setScale(String data) {
        scale = data;
        filename = fileId + "_" + scale + ".pdf";
        tasks = availableProcessors;
    }

    @Override
    public void begin2() {
        super.begin2();
        Log.i("get bookId");
        runJs(21, "return window.br?.bookId;");
    }

    @Override
    protected void dispatch() {
        JobInfo jobInfo = jobs.poll();

        if (jobInfo == null) {
            return;
        }

        int pageIndex = jobInfo.pageIndex;
        int tri = jobInfo.tri;
        String uri = null;

        try {
            uri = data.getJSONObject(pageIndex).getString("uri");
        }
        catch(JSONException e) {
            e.printStackTrace();
            abort(e.getMessage());
            return;
        }

        uri += uri.indexOf("?") > -1 ? "&" : "?";
        uri += "scale=" + scale + "&rotate=0";
        got(pageIndex, tri, uri);
    }

    @Override
    protected void returnBook() {
        byte[] query = ("action=return_loan&identifier=" + fileId).getBytes();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.i(filename + " return");
                Message msg = handler.obtainMessage();

                try {
                    URL url = new URL(Coordinator.archiveLoan);
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setFixedLengthStreamingMode(query.length);
                    conn.setRequestProperty("cookie", cookies);
                    conn.setRequestProperty("Referer", referer);
                    conn.connect();
                    OutputStream out = conn.getOutputStream();
                    out.write(query);
                    int responseCode = conn.getResponseCode();
                    conn.disconnect();
                    msg.arg1 = responseCode;
                    msg.what = What.RETURN_BOOK.get();
                    msg.sendToTarget();
                }
                catch(IOException e) {
                    Log.i(filename + " return fail");
                    e.printStackTrace();
                }
                finally {
                    endExecutor();
                }
            }
        });
    }
}
