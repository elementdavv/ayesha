package net.timelegend.ayesha;

import android.content.Context;
import android.os.Build;
import android.util.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Coordinator {
    public final static Integer PERMISSION_REQUEST_CODE_JOB = 1;
    public final static Integer PERMISSION_REQUEST_CODE_DIRECT = 2;

    public final static String archive = "https://archive.org/";
    public final static String archiveDetail = "https://archive.org/details/";
    public final static String archiveLoan = "https://archive.org/services/loans/loan";

    public final static String hathitrust = "https://www.hathitrust.org/";
    public final static String hathitrustDetail = "https://babel.hathitrust.org/cgi/pt?id=";
    public final static String hathitrustAny = "hathitrust.org";
    public final static String hathitrustImage = "https://babel.hathitrust.org/cgi/imgsrv/image?id=";

    public static Context context;

    private final static Map<Integer, Pair<String, String>> idMap;

    static {
        Map<Integer, Pair<String, String>> amap = new HashMap<>();
        amap.put(R.id.site_ia, Pair.create(archive, archiveDetail));
        amap.put(R.id.site_ht, Pair.create(hathitrust, hathitrustDetail));
        idMap = Collections.unmodifiableMap(amap);
    }

    public static Set<Integer> getSites() {
        return idMap.keySet();
    }

    // from site list
    public static MyWebView newView(Integer site) {
        Pair<String, String> pair = idMap.get(site);
        return newView(pair.first, null);
    }

    // from restore, context menu, clipboard
    public static MyWebView newView(String url, MyWebView creator) {
        MyWebView myView = new MyWebView(context, creator);

        Log.i("new view: " + url);
        // the embedded javascriptinterface is not worked until the next page loads
        if (url.indexOf(archiveDetail) > -1 || url.indexOf(hathitrustDetail) > -1) {
            myView.initJsInterface(url);
            Log.i("load by data: " + url);
            String htmlData = "<script>window.location.href='" + url + "'</script>";
            myView.loadData(htmlData, "text/html", null);
        }
        else
            myView.loadUrl(url);

        return myView;
    }

    // from clipboard, needs check
    public static MyWebView newViewCheck(String url) {
        if (url != null) {
            Pair<String, String> pair = null;
            // stream was supported from jdk8 of android 7(api24)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                pair = idMap.entrySet().stream()
                        .filter(e -> url.indexOf(e.getValue().second) > -1)
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null);
            }
            else {
                Collection<Pair<String, String>> values = idMap.values();

                for (Pair<String, String> value : values) {
                    if (url.indexOf(value.second) > -1) {
                        pair = value;
                        break;
                    }
                }
            }

            if (pair != null) return newView(url, null);
        }

        return null;
    }
}
