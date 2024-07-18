package net.timelegend.ayesha;

import android.content.Context;

public class HathitrustJs extends Js {
    public HathitrustJs(Context context, MyWebView v) {
        super(context, v);
        stepLimit = 4;
        period = 2000;
    }

    @Override
    protected void schedule() {
        runJs(1, "return window.manifest?.allowSinglePageDownload");
    }

    @Override
    public void consume(int stage, String value) {
        switch (stage) {
            case 1:
                if (timer == null) return;
                if ("true".equals(value)) {
                    timer.cancel();
                    timer = null;
                    Log.i("done");
                    Log.i("load css");
                    runJs2(11, "loadcss.js");
                }
                else if ("false".equals(value)) {
                    timer.cancel();
                    timer = null;
                    Log.i("book not available, quit");
                }
                else if (++step >= stepLimit) {
                    timer.cancel();
                    timer = null;
                    Log.i("timeout, quit");
                }
                else {
                    Log.i("wait for book info: " + step);
                }
                break;
            case 11:
                if ("true".equals(value)) {
                    Log.i("load buttons");
                    runJs2(12, "ht/loadbuttons.js");
                }
                else {
                    Log.i("fail");
                }
                break;
            case 12:
                if ("true".equals(value)) {
                    Log.i("load scales");
                    runJs2(13, "ht/loadscales.js");
                }
                else {
                    Log.i("fail");
                }
                break;
            case 21:
                String fileId = value.substring(1, value.length() - 1);
                job.setFileId(fileId);
                Log.i("get book data");
                runJs(22, "return window.manifest?.firstPageSeq + ',' + window.manifest?.totalSeq;");
                break;
            case 22:
                String data = value.substring(1, value.length() - 1);
                job.setData(data);
                Log.i("get metadata");
                runJs2(23, "ht/getmetadata.js");
                break;
            default:
                break;
        }
        super.consume(stage, value);
    }
}
