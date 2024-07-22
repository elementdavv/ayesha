package net.timelegend.ayesha;

import android.content.Context;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class Js {
    protected Context context;
    protected MyWebView v;
    protected Job job;
    protected Timer timer;
    protected int step;
    protected int stepLimit;
    protected long period;

    public Js(Context context, MyWebView v) {
        this.context = context;
        this.v = v;
        stepLimit = 8;
        period = 1000L;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public void prepare() {
        Log.i("Ayesha 1.0 in action");
        step = 0;
        timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                schedule();
            }
        }, 0, period);
    }

    protected abstract void schedule();

    public void consume(int stage, String value) {
        switch (stage) {
            case 13:
                if ("true".equals(value)) {
                    job.initReady();
                    Log.i("init complete");
                }
                else {
                    Log.i("fail");
                }
                break;
            case 23:
                try {
                    Map<String, String> info = jsonObjectToMap(new JSONObject(value));
                    job.setInfo(info);
                    Log.i("get scale");
                    runJs2(24, "getscales.js");
                }
                catch(JSONException e) {
                    e.printStackTrace();
                    ((MainActivity)context).alert("Error", e.getMessage());
                }
                break;
            case 24:
                String data = value.substring(1, value.length() - 1);
                job.setScale(data);
                job.getFile();
                break;
            default:
                break;
        }
    }

    protected Map<String, String> jsonObjectToMap(JSONObject obj) throws JSONException {
        Map<String, String> map = new HashMap<>();
        Iterator<String> it = obj.keys();

        while (it.hasNext()) {
            String key = it.next();
            map.put(key, obj.getString(key));
        }

        return map;
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
