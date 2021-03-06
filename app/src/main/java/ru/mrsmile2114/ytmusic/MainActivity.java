package ru.mrsmile2114.ytmusic;

import android.Manifest;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;

import ru.mrsmile2114.ytmusic.utils.DownloadFinishedReceiver;
import ru.mrsmile2114.ytmusic.download.DownloadStartFragment;
import ru.mrsmile2114.ytmusic.download.DownloadsFragment;
import ru.mrsmile2114.ytmusic.utils.HandlerOnDownloadCancelled;
import ru.mrsmile2114.ytmusic.download.PlaylistItemsFragment;
import ru.mrsmile2114.ytmusic.dummy.DownloadsItems;
import ru.mrsmile2114.ytmusic.dummy.PlaylistItems.PlaylistItem;
import ru.mrsmile2114.ytmusic.player.PlayFragment;
import ru.mrsmile2114.ytmusic.player.QueueItemFragment;
import ru.mrsmile2114.ytmusic.utils.YTExtract;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        PlaylistItemsFragment.OnListFragmentInteractionListener,
        DownloadStartFragment.OnFragmentInteractionListener,
        DownloadsFragment.OnListFragmentInteractionListener,
        PlayFragment.OnFragmentInteractionListener,
        QueueItemFragment.OnListFragmentInteractionListener{

    private FloatingActionButton fab;
    private NavigationView navigationView;
    private Menu mymenu;
    private ProgressDialog mProgressDialog;

    private DownloadsFragment mDownloadsFragment;
    private DownloadStartFragment mDownloadStartFragment;
    private PlaylistItemsFragment mPlaylistItemsFragment;
    private PlayFragment mPlayFragment;

    private final Handler handlerDownload = new HandlerOnDownloadCancelled(this);
    private DownloadFinishedReceiver onDownloadComplete;
    boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        StartContentObserver();

        fab = (FloatingActionButton) findViewById(R.id.fab);
        SetMainFabVisible(false);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        navigationView.setCheckedItem(R.id.nav_download);
        //TODO:MAKE FUNCTION
        mProgressDialog= new ProgressDialog(    this);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setTitle(getString(R.string.please_wait));

        onDownloadComplete = new DownloadFinishedReceiver(){//create a descendant of a class DownloadFinishedReceiver
            @Override
            public void onReceive(final Context context, Intent intent) {
                super.onReceive(context,intent);
                RemoveItemByDownloadId(String.valueOf(intent.getExtras().getLong(DownloadManager.EXTRA_DOWNLOAD_ID)),true);
            }

            @Override
            protected void removeTempOnFailure(Context con, long downloadId) {
                super.removeTempOnFailure(con, downloadId);
                Snackbar.make(findViewById(R.id.sample_content_fragment),
                        getString(R.string.extraction_error), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }

        };
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        if (mPlayFragment==null){
            mPlayFragment = new PlayFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.container, mPlayFragment, "FRAGMENT_PLAY");
            transaction.commit();
        }
        navigationView.setCheckedItem(R.id.nav_play);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(onDownloadComplete);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (doubleBackToExitPressedOnce||getSupportFragmentManager().getBackStackEntryCount()!=0) {
                super.onBackPressed();
                return;
            }

            this.doubleBackToExitPressedOnce = true;
            Snackbar.make(findViewById(R.id.sample_content_fragment),
                    getString(R.string.press_back), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    doubleBackToExitPressedOnce=false;
                }
            }, 2000);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        mymenu=menu;
        mymenu.findItem(R.id.action_check_box).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_check_box){
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_download) {
            GoToFragment(DownloadStartFragment.class);
        } else if (id == R.id.nav_manage) {
            GoToFragment(DownloadsFragment.class);
        }else if (id==R.id.nav_play){
            GoToFragment(PlayFragment.class);
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    public void onListFragmentInteraction(PlaylistItem item) {

    }

    public void GoToFragment(Class fragmentclass){//method of transition to the desired fragment
        //Fragment fragment;
        if (fragmentclass==DownloadStartFragment.class){//for DownloadStartFragment
            if (mDownloadStartFragment==null) {
                mDownloadStartFragment = new DownloadStartFragment();
            }
            BeginTransaction(mDownloadStartFragment,"FRAGMENT_DOWNLOAD_START");
            navigationView.setCheckedItem(R.id.nav_download);
        } else if (fragmentclass==DownloadsFragment.class){//for DownloadsFragment
            if (mDownloadsFragment==null){
                mDownloadsFragment = new DownloadsFragment();
            }
            if (mDownloadsFragment.isVisible()){
                mDownloadsFragment.RefreshRecyclerView();
            }else{
                BeginTransaction(mDownloadsFragment,"FRAGMENT_DOWNLOADS_MANAGE");
            }
            navigationView.setCheckedItem(R.id.nav_manage);
        } else if (fragmentclass==PlaylistItemsFragment.class){//for PlaylistItemsFragment
            if (mPlaylistItemsFragment==null){
                mPlaylistItemsFragment = new PlaylistItemsFragment();
            }
            if (mPlaylistItemsFragment.isVisible()){
                mPlaylistItemsFragment.RefreshRecyclerView();
            }else{
                BeginTransaction(mPlaylistItemsFragment,"FRAGMENT_PLAYLIST_ITEMS");

            }
        } else if (fragmentclass==PlayFragment.class){
            if (mPlayFragment==null){
                mPlayFragment = new PlayFragment();
            }
            BeginTransaction(mPlayFragment,"FRAGMENT_PLAY");
            navigationView.setCheckedItem(R.id.nav_play);
        }
    }

    private void BeginTransaction(Fragment fragment, String tag){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, fragment, tag);
        transaction.addToBackStack(tag);
        transaction.commit();
        getSupportFragmentManager().executePendingTransactions();
    }

    public void SetMainFabListener(View.OnClickListener listener){ fab.setOnClickListener(listener); }

    @Override
    public void RemoveItemByDownloadId(String downloadId, boolean completed) {
        DownloadsItems.DownloadsItem item = DownloadsItems.getITEMbyDownloadId(downloadId);
        if (item!=null){
            if(!completed){
                DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                manager.remove(Long.parseLong(downloadId));
            }
            if (mDownloadsFragment == null) {
                mDownloadsFragment = new DownloadsFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.addToBackStack("FRAGMENT_DOWNLOADS_MANAGE");
                transaction.commit();
            }
            mDownloadsFragment.RemoveItemByDownloadId(downloadId);
        }
    }

    public void SetMainFabImage(int imageResource){ fab.setImageResource(imageResource); }
    public void SetMainFabVisible(boolean visible){
        if (visible){
            fab.show();
        } else {
            fab.hide();
        }
    }

    public void SetMainCheckBoxVisible(boolean vis){
        mymenu.findItem(R.id.action_check_box).setChecked(false);
        mymenu.findItem(R.id.action_check_box).setVisible(vis);
    }
    public void SetMainCheckBoxListener(CheckBox.OnCheckedChangeListener listener){
        ((CheckBox)mymenu.findItem(R.id.action_check_box).getActionView()).setOnCheckedChangeListener(listener);
    }

    public void SetMainProgressDialogVisible(boolean visible){
        if (visible){
            mProgressDialog.show();
        } else {
            mProgressDialog.dismiss();
        }
    }

    public long downloadFromUrl(String youtubeDlUrl, String downloadTitle, String fileName, boolean hide) {
        Uri uri = Uri.parse(youtubeDlUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(downloadTitle);
        if (hide) {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            request.setVisibleInDownloadsUi(false);
        } else
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS+"/YTMusic", fileName);

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        return manager.enqueue(request);
    }


    @Override
    public void SetTitle(String title) { setTitle(title); }

    public void StartContentObserver(){
        getContentResolver().registerContentObserver(Uri.parse("content://downloads/my_downloads"),
                true, new ContentObserver(null) {
                    @Override
                    public void onChange(boolean selfChange, Uri uri) {
                        super.onChange(selfChange, uri);
                        if (uri.toString().matches(".*\\d+$")) {
                            String changedId;
                            changedId=uri.getLastPathSegment();
                            DownloadsItems.DownloadsItem item = DownloadsItems.getITEMbyDownloadId(changedId);
                            if (item!=null){
                                Log.d("DEBUG", "onChange: " + uri.toString() + " " + changedId );
                                try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                                    if (cursor != null && cursor.moveToFirst()) {
                                        Log.d("DEBUG", "onChange: running");
                                    } else {
                                        Log.w("DEBUG", "onChange: cancel");
                                        Message m = Message.obtain();
                                        m.obj = changedId;
                                        handlerDownload.sendMessage(m);
                                    }
                                }
                            }
                        }
                    }
                });
    }

    @Override
    public boolean HaveStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.e("Permission error","You have permission");
                return true;
            } else {
                Log.e("Permission error","You have asked for permission");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else {
            Log.e("Permission error","You already have the permission");
            return true;
        }
    }


    public YTExtract.ExtractCallBackInterface DownloadExtractCallBackInterface = new YTExtract.ExtractCallBackInterface() {
        @Override
        public void onSuccExtract(String url, String parsedUrl, String title) {
            SetMainProgressDialogVisible(false);
            String filename;
            if (title.length() > 55) {
                filename = title.substring(0, 55);
            } else {
                filename = title;
            }
            filename = filename.replaceAll("[\\\\><\"|*?%:#/]", "");
            filename += ".m4a";
            Long downloadId =downloadFromUrl(parsedUrl ,title, filename,false);
            DownloadsItems.addItem(DownloadsItems.createDummyItem(title, downloadId.toString()));
            GoToFragment(DownloadsFragment.class);
        }

        @Override
        public void onUnsuccExtractTryAgain( int attempt) {
            SetMainProgressDialogVisible(false);
            Snackbar.make(findViewById(R.id.container), String.format(
                            getString(R.string.extraction_error_try_again),
                            attempt),
                    Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

        }

        @Override
        public void onUnsuccExtract(String url) {
        }
    };

}
