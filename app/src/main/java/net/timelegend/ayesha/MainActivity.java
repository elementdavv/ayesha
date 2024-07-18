package net.timelegend.ayesha;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.net.Uri;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

// import java.io.IOException;
// import net.timelegend.crl.InvalidImageException;
// import net.timelegend.crl.PDFWriterDemo;

public class MainActivity extends AppCompatActivity {
    // const
    enum InStatus {IN_SITES, IN_MAIN, IN_SUM, IN_ABOUT};

    private final static String UrlGithub = "https://github.com/elementdavv/ayesha";

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

        // if (restoreInstance(savedInstanceState)) 
        //     return;

        if (restoreSession())
            return;

        current = -1;
        openSites();
    }

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
            ImageView img = (ImageView) sitesView.findViewById(site.intValue());

            img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MyWebView v1 = Coordinator.newView(site);
                    setupView(v1);
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
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(UrlGithub));
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
        AdRequest adRequest = new AdRequest.Builder().build();
        adView = findViewById(R.id.adview);
        adView.loadAd(adRequest);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
    }

    private boolean restoreInstance(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            ArrayList<CharSequence> urls = savedInstanceState.getCharSequenceArrayList("urls");
            ArrayList<Bundle> sites = savedInstanceState.getParcelableArrayList("sites", Bundle.class);
            int i = 0;

            while (i < urls.size()) {
                MyWebView v = Coordinator.newView(urls.get(i).toString());
                setupView(v, false);
                v.restoreState(sites.get(i));
                i++;
            }

            if (i > 0) {
                current = 0;
                tabView.setItemChecked(current, true);
                openMain();
                return true;
            }
        }

        return false;
    }

    private boolean restoreSession() {
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        Set<String> urls = prefs.getStringSet("urls", null);

        if (urls != null) {
            Iterator<String> it = urls.iterator();

            while (it.hasNext()) {
                MyWebView v = Coordinator.newView(it.next());
                setupView(v, false);
            }

            if (dataSet.size() > 0) {
                current = 0;
                tabView.setItemChecked(current, true);
                openMain();
                return true;
            }
        }

        return false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus)
            handleClipboard();
    }

    private void handleClipboard() {
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

    @Override
    protected void onSaveInstanceState(Bundle instanceState) {
        saveInstance(instanceState);
        super.onSaveInstanceState(instanceState);
    }

    private void saveInstance(Bundle instanceState) {
        if (dataSet.size() > 0) {
            ArrayList<CharSequence> urls = new ArrayList<>();
            ArrayList<Bundle> sites = new ArrayList<>();
            int i = 0;

            while (i < dataSet.size()) {
                CharSequence url = dataSet.get(i).getUrl();
                urls.add(url);
                Bundle site = new Bundle();
                dataSet.get(i).saveState(site);
                sites.add(site);
                i++;
            }

            instanceState.putCharSequenceArrayList("urls", urls);
            instanceState.putParcelableArrayList("sites", sites);
        }
    }

    @Override
    protected void onStop() {
        saveSession();
        super.onStop();
    }

    private void saveSession() {
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();

        if (dataSet.size() == 0)
            edit.remove("urls");
        else {
            Set<String> urls = new HashSet<>();
            int i = 0;

            while (i < dataSet.size()) {
                urls.add(dataSet.get(i++).getUrl());
            }

            edit.putStringSet("urls", urls);
        }

        edit.apply();
    }

    public void newTab(String url, boolean foreground, MyWebView creator) {
        MyWebView v = Coordinator.newView(url, creator);
        setupView(v, foreground);
    }

    private void setupView(MyWebView v) {
        setupView(v, true);
    }

    private void setupView(MyWebView v, boolean foreground) {
        if (v == null) return;

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

    public void onTabChanged() {
        adapter.notifyDataSetChanged();
    }

    public void onTabClose(int position) {
        if (dataSet.get(position).isDownloading()) {
            show("It is downloading.");
            return;
        }

        MyWebView v = dataSet.remove(position);
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

        onTabChanged();

        if (current > -1)
            tabView.setItemChecked(current, true);
    }

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
                else 
                    dataSet.get(current).reload();
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
            // try {
            //     PDFWriterDemo.helloworld(this);
            // }
            // catch(InvalidImageException | IOException e) {
            //     e.printStackTrace();
            //     show(e.getMessage());
            // }
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

    private void closeChild(int child, int creator) {
        MyWebView vw = dataSet.get(creator);
        vw.setVisibility(View.VISIBLE);
        vw.updateProgress();
        dataSet.get(child).setVisibility(View.GONE);
        MyWebView v = dataSet.remove(child);
        mainLayout.removeView(v);
        v.destroy();
        v = null;
        current = creator;
        onTabChanged();
        tabView.setItemChecked(current, true);
    }

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
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    getCurrentView().getJob().begin2();
                }
            });

            t.start();
        }
    }

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

    public boolean openSites() {
        if (inStatus == InStatus.IN_SITES) return false;

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

    public boolean openTabList() {
        if (inStatus == InStatus.IN_SUM) return false;

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

    public boolean openAbout() {
        if (inStatus == InStatus.IN_ABOUT) return false;

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
