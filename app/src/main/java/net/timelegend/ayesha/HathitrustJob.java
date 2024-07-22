package net.timelegend.ayesha;

import android.content.Context;
import android.os.Build;

public class HathitrustJob extends Job {
    private int firstPageSeq;
    private String url;

    public HathitrustJob(Context context, MyWebView v) {
        super(context, v);
        origin = Coordinator.hathitrust;
    }

    @Override
    public void setFileId(String data) {
        fileId = data.replaceAll("[^a-zA-Z0-9_]", "_");
        url = Coordinator.hathitrustImage + data;
        referer = Coordinator.hathitrustDetail + data;
    }

    @Override
    public void setData(Object data) {
        String[] seqs = ((String)data).split(",");
        firstPageSeq = Integer.valueOf(seqs[0]);
        pagecount = Integer.valueOf(seqs[1]);
    }

    @Override
    public void setScale(String data) {
        String[] scales = data.split(",");
        scale = scales[0];
        filename = fileId + "_" + (scale.indexOf("full") > -1 ? "full" : scale.substring(7)) + ".pdf";
        tasks = Integer.valueOf(scales[1]);
    }

    @Override
    public void begin2() {
        // less than android 7.0 (api 24)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            additionalSSLSocketFactory = HathitrustSSLSocketFactory.get(context);

            if (additionalSSLSocketFactory == null) {
                ((MainActivity) context).show("Certificate error");
                return;
            }
        }

        super.begin2();
        Log.i("get bookId");
        runJs(21, "return window.manifest?.id;");
    }

    @Override
    protected void dispatch() {
        JobInfo jobInfo = jobs.poll();

        if (jobInfo == null) {
            return;
        }

        int pageIndex = jobInfo.pageIndex;
        int tri = jobInfo.tri;
        String uri = url + "&seq=" + (pageIndex + firstPageSeq) + '&' + scale;
        got(pageIndex, tri, uri);
    }
}
