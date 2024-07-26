package net.timelegend.ayesha;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.ContentLoadingProgressBar;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Timer;
import java.util.TimerTask;

// import android.content.pm.PackageInfo;
// import crl.android.pdfwriter.PDFWriterDemo;

public class MainActivity extends AppCompatActivity {
    // const
    public enum InStatus {IN_NONE, IN_SITES, IN_MAIN, IN_SUM, IN_ABOUT};

    private final static String URLGITHUB = "https://github.com/elementdavv/ayesha";
    private final static String JOINEDURLS = "joinedUrls";
    private final static String CURRENT = "current";
    private final static String DELIMITER = "ayesha";

    // ui
    private ContentLoadingProgressBar progressBar;
    private RelativeLayout mainLayout;
    private RelativeLayout sumView;
    private ListView tabView;
    private AdView adView;
    private LinearLayout sitesView;
    private RelativeLayout aboutView;

    // data
    private List<MyWebView> dataSet;
    private TabAdapter adapter;
    private int current;
    private InStatus inStatus;;

    // delay exit
    private Toast toast;
    private Timer timer;
    private int toquit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Coordinator.context = this;
        setup();

        inStatus = InStatus.IN_NONE;

        if (restoreSession()) return;

        current = -1;
        openSites();
    }

    // UI initialization
    private void setup() {
        // general
        Toolbar t = (Toolbar) findViewById(R.id.toolbar); 
        setSupportActionBar(t);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        progressBar = findViewById(R.id.progressbar);
        mainLayout = (RelativeLayout) findViewById(R.id.mainlayout);

        // sites
        sitesView = (LinearLayout) getLayoutInflater().inflate(R.layout.sites, mainLayout, false);
        mainLayout.addView(sitesView);
        Set<Integer> sites = Coordinator.getSites();
        Iterator<Integer> it = sites.iterator();

        while (it.hasNext()) {
            Integer site = it.next();
            LinearLayout img = (LinearLayout) sitesView.findViewById(site);

            img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    newTab(site);
                }
            });
        }

        // about
        aboutView = (RelativeLayout) getLayoutInflater().inflate(R.layout.about, mainLayout, false);
        mainLayout.addView(aboutView);
        TextView githubView = (TextView) aboutView.findViewById(R.id.about_github);

        githubView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(URLGITHUB));
                startActivity(browserIntent);
            }
        });

        // sum
        sumView = (RelativeLayout) findViewById(R.id.sumview);

        // tab
        dataSet = new ArrayList<>();
        adapter = new TabAdapter(this, R.layout.row_item, dataSet);
        tabView = (ListView) findViewById(R.id.tabview);
        tabView.setAdapter(adapter);

        tabView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                current = position;
                openMain();
                tabView.setItemChecked(current, true);
            }
        });

        // ad
        adView = findViewById(R.id.adview);
        adView.setVisibility(View.GONE);
        // AdRequest adRequest = new AdRequest.Builder().build();
        // adView.loadAd(adRequest);

        // MobileAds.initialize(this, new OnInitializationCompleteListener() {
        //     @Override
        //     public void onInitializationComplete(InitializationStatus initializationStatus) {
        //     }
        // });
    }

    private boolean restoreSession() {
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        String joinedUrls = prefs.getString(JOINEDURLS, null);
        current = prefs.getInt(CURRENT, -1);

        if (joinedUrls != null) {
            String[] urls = joinedUrls.split(DELIMITER);
            Iterator<String> it = Arrays.asList(urls).iterator();

            while (it.hasNext()) {
                newTab(it.next(), null, false);
            }

            int sz = dataSet.size();

            if (sz > 0) {
                if (current >= sz) current = sz - 1;
                if (current < 0) current = 0;
                tabView.setItemChecked(current, true);
                openMain();
                return true;
            }
        }

        return false;
    }

    // onStop() may not be called, so save prefs in onPause()
    // do not use savedInstanceState, it causes crash on permission change
    @Override
    protected void onPause() {
        saveSession();
        super.onPause();
    }

    private void saveSession() {
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();

        if (dataSet.size() == 0) {
            edit.remove(JOINEDURLS);
        }
        else {
            String joinedUrls = "";
            // stream was supported from jdk8 of android 7(api24)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                joinedUrls = dataSet.stream()
                        .map(MyWebView::getUrl)
                        .collect(Collectors.joining(DELIMITER));
            }
            else {
                StringBuffer sb = new StringBuffer("");
                boolean b = true;

                for (MyWebView v : dataSet) {
                    if (b) {
                        b = false;
                    }
                    else {
                        sb.append(DELIMITER);
                    }

                    sb.append(v.getUrl());
                }

                joinedUrls = sb.toString();
            }

            edit.putString(JOINEDURLS, joinedUrls);
            edit.putInt(CURRENT, current);
        }

        edit.apply();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) watchClipboard();
    }

    private void watchClipboard() {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = cm.getPrimaryClip();

        if (clip == null) return;

        MyWebView v = null;
        ClipData.Item item = clip.getItemAt(0);
        Uri uri = item.getUri();

        if (uri != null) {
            String url = uri.toString();
            v = Coordinator.newViewCheck(url);
        }

        if (v == null) {
            String text = item.getText().toString();
            v = Coordinator.newViewCheck(text);
        }

        if (v != null) {
            setupView(v);
            cm.setPrimaryClip(ClipData.newPlainText(null, ""));
        }
    }

    /*
     * create a tab from restore and context menu
     * url: the url of the page
     * creator: the parent which create the page
     * foreground: foreground or background
     */
    public void newTab(String url, MyWebView creator, boolean foreground) {
        MyWebView v = Coordinator.newView(url, creator);
        setupView(v, foreground);
    }

    /*
     * create a tab from sites list
     * it should be no parent and be foreground
     */
    public void newTab(Integer site) {
        MyWebView v = Coordinator.newView(site);
        setupView(v);
    }

    private void setupView(MyWebView v) {
        setupView(v, true);
    }

    private void setupView(MyWebView v, boolean foreground) {
        if (v != null) {
            registerForContextMenu(v);
            mainLayout.addView(v);
            dataSet.add(v);
            onTabChanged();

            if (foreground) {
                if (inStatus == InStatus.IN_MAIN) {
                    dataSet.get(current).setVisibility(View.GONE);
                }
                current = dataSet.size() - 1;
                tabView.setItemChecked(current, true);
                openMain();
            }
            else {
                v.setVisibility(View.GONE);
            }
        }
    }

    /*
     * when tablist needs update
     * include item title
     * not include item highlight change
     */
    public void onTabChanged() {
        adapter.notifyDataSetChanged();
    }

    /*
     * user close tab from tablist
     */
    public void onTabClose(int position) {
        if (dataSet.get(position).isDownloading()) {
            show("It is downloading.");
            return;
        }

        MyWebView v = dataSet.remove(position);
        onTabChanged();
        mainLayout.removeView(v);
        v.destroy();
        v = null;

        if (dataSet.size() == 0) {
            current = -1;
            hideProgress();
        }
        else if (position <= current && current > 0) {
            current--;
        }

        if (current > -1) {
            tabView.setItemChecked(current, true);
        }
    }

    /*
     * hide progressBar
     */
    public void hideProgress() {
        if (progressBar.getVisibility() == View.VISIBLE) {
            progressBar.hide();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
         getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // ((MenuBuilder)menu).setOptionalIconsVisible(true);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_new) {
            openSites();
            return true;
        }
        else if (id == R.id.action_history) {
            openTabList();
            return true;
        }
        else if (id == R.id.action_refresh) {
            if (current > -1) {
                openMain();

                if (dataSet.get(current).isDownloading()) {
                    show("It is downloading.");
                }
                else {
                    dataSet.get(current).reload();
                }
            }

            return true;
        }
        else if (id == R.id.action_forward) {
            if (current > -1) {
                openMain();

                if (dataSet.get(current).isDownloading()) {
                    show("It is downloading.");
                }
                else if (dataSet.get(current).canGoForward()) {
                    dataSet.get(current).goForward();
                }
            }

            return true;
        }
        else if (id == R.id.action_about) {
            // PackageInfo pInfo = null;
            // try {
            //     pInfo = getPackageManager().getPackageInfo("com.android.chrome", 0);
            // }
            // catch (PackageManager.NameNotFoundException e) {
            // }
            // if (pInfo == null) {
            //     show ("chrome not found");
            // }
            // else {
            //     show ("chrome: " + pInfo.versionName);
            // }

            // Intent demo = new Intent(this, PDFWriterDemo.class);
            // startActivity(demo);

            openAbout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (current == -1) {
            if (!openSites()) {
                quit();
            }

            return;
        }

        if (openMain()) {
            return;
        }
        else if (dataSet.get(current).isDownloading()) {
            show("It is downloading.");
        }
        else if (dataSet.get(current).canGoBack()) {
            dataSet.get(current).goBack();
        }
        else {
            MyWebView creator = dataSet.get(current).getCreator();
            int position = locateView(creator);
            if (position > -1) {
                closeChild(current, position);
            }
            else {
                int i = 0;

                while (i < dataSet.size()) {
                    if (dataSet.get(i++).isDownloading()) {
                        show("There are downloadings.");
                        return;
                    }
                }

                quit();
            }
        }
    }

    /*
     * exit the app
     */
    private void quit() {
        if (toquit == 1) {
            toquit = 0;
            if (timer != null) timer.cancel();
            if (toast != null) toast.cancel();
            super.onBackPressed();
        }
        else {
            toquit = 1;
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    toquit = 0;
                    timer = null;
                }
            }, 2000);
            toast = Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    /*
     * a page created by another page can be closed by backpress
     */
    private void closeChild(int child, int creator) {
        MyWebView vw = dataSet.get(creator);
        vw.setVisibility(View.VISIBLE);
        vw.updateProgress();
        dataSet.get(child).setVisibility(View.GONE);
        MyWebView v = dataSet.remove(child);
        onTabChanged();
        mainLayout.removeView(v);
        v.destroy();
        v = null;
        current = creator;
        tabView.setItemChecked(current, true);
    }

    /*
     * find the position of a page
     */
    public int locateView(MyWebView v) {
        if (v == null) return -1;

        for (int i = 0; i < dataSet.size(); i++) {
            if (dataSet.get(i).equals(v))
                return i;
        }

        return -1;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Iterator<MyWebView> it = dataSet.iterator();

        while (it.hasNext()) {
            MyWebView v = it.next();
            it.remove();
            mainLayout.removeView(v);
            v.destroy();
            v = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (requestCode == Coordinator.PERMISSION_REQUEST_CODE_JOB) {
                        getCurrentView().getJob().begin2();
                    }
                    else if (requestCode == Coordinator.PERMISSION_REQUEST_CODE_DIRECT) {
                        getCurrentView().directDownload();
                    }

                    executor.shutdown();
                }
            });
        }
    }

    /*
     * open page view
     */
    public boolean openMain() {
        if (inStatus == InStatus.IN_MAIN) return false;

        if (current > -1) {
            MyWebView v = dataSet.get(current);
            v.setVisibility(View.VISIBLE);
            v.updateProgress();
            sitesView.setVisibility(View.GONE);
            sumView.setVisibility(View.GONE);
            aboutView.setVisibility(View.GONE);
            inStatus = InStatus.IN_MAIN;
            return true;
        }
        else
            return false;
    }

    /*
     * open sites list view
     */
    public boolean openSites() {
        if (inStatus == InStatus.IN_SITES) {
            return openMain();
        }

        sitesView.setVisibility(View.VISIBLE);
        sumView.setVisibility(View.GONE);
        aboutView.setVisibility(View.GONE);
        hideProgress();

        if (current > -1) {
            dataSet.get(current).setVisibility(View.GONE);
        }

        inStatus = InStatus.IN_SITES;
        return true;
    }

    /*
     * open tablist view
     */
    public boolean openTabList() {
        if (inStatus == InStatus.IN_SUM) {
            return openMain();
        }

        sumView.setVisibility(View.VISIBLE);
        sitesView.setVisibility(View.GONE);
        aboutView.setVisibility(View.GONE);
        hideProgress();

        if (current > -1) {
            dataSet.get(current).setVisibility(View.GONE);
        }

        inStatus = InStatus.IN_SUM;
        return true;
    }

    /*
     * open about view
     */
    public boolean openAbout() {
        if (inStatus == InStatus.IN_ABOUT) {
            return openMain();
        }

        aboutView.setVisibility(View.VISIBLE);
        sitesView.setVisibility(View.GONE);
        sumView.setVisibility(View.GONE);
        hideProgress();

        if (current > -1) {
            dataSet.get(current).setVisibility(View.GONE);
        }

        inStatus = InStatus.IN_ABOUT;
        return true;
    }

    public boolean isInMain() {
        return inStatus == InStatus.IN_MAIN;
    }

    public MyWebView getCurrentView() {
        return current == -1 ? null : dataSet.get(current);
    }

    public ContentLoadingProgressBar getProgressBar() {
        return progressBar;
    }

    public void alert(String title, String message) {
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(true)
                .create().show();
    }

    public void show(int i) {
        show("" + String.valueOf(i));
    }

    public void show(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

}
