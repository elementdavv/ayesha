package net.timelegend.ayesha;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;

public class ArchiveJs extends Js {
    public ArchiveJs(Context context, MyWebView v) {
        super(context, v);
    }

    @Override
    protected void schedule() {
        if (step == 0)
            runJs(1, "return document.querySelector('meta[property=mediatype]')?.content;");
        else
            runJs(2, "return window.br?.protected;");
    }

    @Override
    public void consume(int stage, String value) {
        switch (stage) {
            case 1:
                if (value.indexOf("texts") > -1) {
                    runJs(2, "return window.br?.protected;");
                }
                else {
                    Log.i("not texts media, quit");
                    timer.cancel(); // no texts media, quit
                }
                break;
            case 2:
                if ("true".equals(value)) {
                    timer.cancel();
                    runJs(3, "return window.br?.options?.lendingInfo?.loanId;");
                }
                else if ("false".equals(value)) {
                    timer.cancel(); // not protected, quit
                    Log.i("book is always available, quit");
                }
                else if (++step >= stepLimit) {
                    timer.cancel();
                    Log.i("timeout, quit");
                }
                else {
                    Log.i("wait for book info: " + step);
                }
                break;
            case 3:
                if (!"null".equals(value)) {
                    Log.i("done");
                    Log.i("load css");
                    runJs2(11, "loadcss.js");
                }
                else {
                    Log.i("book not borrowed, quit");
                }
                break;
            case 11:
                if ("true".equals(value)) {
                    Log.i("load buttons");
                    runJs2(12, "ia/loadbuttons.js");
                }
                else {
                    Log.i("fail");
                }
                break;
            case 12:
                if ("true".equals(value)) {
                    Log.i("load scales");
                    runJs2(13, "ia/loadscales.js");
                }
                else {
                    Log.i("fail");
                }
                break;
            case 21:
                String fileId = value.substring(1, value.length() - 1);
                job.setFileId(fileId);
                Log.i("get book data");
                runJs(22, "return window.br?.data?.flat();");
                break;
            case 22:
                try {
                    JSONArray data = new JSONArray(value);
                    job.setData(data);
                    Log.i("get metadata");
                    runJs2(23, "ia/getmetadata.js");
                }
                catch(JSONException e) {
                    e.printStackTrace();
                    ((MainActivity)context).alert("Error", e.getMessage());
                }
                break;
            default:
                break;
        }
        super.consume(stage, value);
    }
}
