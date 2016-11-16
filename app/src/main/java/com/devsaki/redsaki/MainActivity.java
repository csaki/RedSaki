package com.devsaki.redsaki;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.devsaki.redsaki.data.PostColumns;
import com.devsaki.redsaki.data.RedSakiProvider;
import com.devsaki.redsaki.data.SubredditColumns;
import com.devsaki.redsaki.dto.PostDTO;
import com.devsaki.redsaki.dto.SubredditDTO;
import com.devsaki.redsaki.fragments.PostListFragment;
import com.devsaki.redsaki.services.PostIntentService;
import com.devsaki.redsaki.services.PostTaskService;
import com.devsaki.redsaki.services.SubredditIntentService;
import com.devsaki.redsaki.util.CommonMethods;
import com.devsaki.redsaki.util.RefresherActivity;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;

import org.apache.commons.lang3.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, RefresherActivity {

    public static final String ARG_SUBREDDIT = "arg_subreddit";
    private static final String[] SORTS = new String[]{
            "hot", "new", "rising", "controversial", "top"};

    String subreddit, sort;
    Spinner spinner;
    Spinner spinnerSort;
    List<SubredditDTO> subredditDTOs;
    List<PostDTO> postDTOs;
    PostListFragment postListFragment;
    SharedPreferences sharedPref;
    private static final int CURSOR_LOADER_ID = 0;

    private Intent mServiceIntent;
    boolean isConnected;
    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mServiceIntent = new Intent(this, SubredditIntentService.class);

        sharedPref = getSharedPreferences("private", Context.MODE_PRIVATE);
        if (getIntent().getData() != null) {
            if (getIntent().getData().getQueryParameters("code") != null && getIntent().getData().getQueryParameters("code").size() > 0) {
                final String code = getIntent().getData().getQueryParameters("code").get(0);
                sharedPref.edit().putString("access_code", code).commit();
                subredditDTOs = null;
            }
        }
        subreddit = getIntent().getStringExtra(ARG_SUBREDDIT);

        postListFragment = PostListFragment.newInstance(this);
        getFragmentManager().beginTransaction().add(R.id.frame_layout, postListFragment).commit();

        Toolbar toolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(toolbar);

        spinner = (Spinner) findViewById(R.id.spinner_toolbar);
        spinnerSort = (Spinner) findViewById(R.id.spinner_sort);

        final String[] strings = new String[]{
                getString(R.string.sort_hot), getString(R.string.sort_new),
                getString(R.string.sort_rising), getString(R.string.sort_controversial),
                getString(R.string.sort_top)};

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MainActivity.this, R.layout.spinner_item, strings);
        spinnerSort.setAdapter(arrayAdapter);
        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshPosts(SORTS[position].toLowerCase());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra("tag", "list");
            if (isConnected) {
                startService(mServiceIntent);
                refreshPosts(SORTS[0].toLowerCase());
            } else {
                Toast.makeText(this, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
            }
        }
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        if (isConnected){
            long period = 3600L;
            long flex = 10L;
            String periodicTag = "periodic";

            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(PostTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }

        RedSakiApplication application = (RedSakiApplication) getApplication();
        mTracker = application.getDefaultTracker();

        mTracker.setScreenName("In the main screen");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    private void refreshPosts(String sort) {
        this.sort = sort;
        Intent mServiceIntentPost = new Intent(this, PostIntentService.class);
        mServiceIntentPost.putExtra("tag", "list");
        mServiceIntentPost.putExtra("sort", sort);
        mServiceIntentPost.putExtra("subreddit", subreddit);
        startService(mServiceIntentPost);
        getLoaderManager().initLoader(("list" + sort + subreddit).hashCode(), null, this);
        postListFragment.startLoad();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        CommonMethods.commonMenu(this, subreddit, id);

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(id==CURSOR_LOADER_ID){
            return new CursorLoader(this, RedSakiProvider.Subreddits.CONTENT_URI,
                    new String[]{SubredditColumns._ID, SubredditColumns.ID, SubredditColumns.DISPLAYNAME,
                            SubredditColumns.TYPE},
                    SubredditColumns.TYPE + " = ?",
                    new String[]{"list"},
                    null);
        }else if(id==("list" + sort + subreddit).hashCode()){
            return new CursorLoader(this, RedSakiProvider.Posts.CONTENT_URI,
                    new String[]{PostColumns._ID, PostColumns.ID, PostColumns.TITLE,
                            PostColumns.AUTHOR, PostColumns.DOMAIN, PostColumns.CREATED,
                            PostColumns.THUMBNAIL, PostColumns.SUBREDDIT, PostColumns.PERMANLINK,
                            PostColumns.URL, PostColumns.SAVED, PostColumns.VISITED,
                            PostColumns.UP, PostColumns.DOWN, PostColumns.TYPE},
                    SubredditColumns.TYPE + "=?",
                    new String[]{"list" + sort + subreddit},
                    null);
        }else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(loader.getId()==CURSOR_LOADER_ID){
            subredditDTOs = new ArrayList<>();
            List<String> strings = new ArrayList<>();
            strings.add("Front Page");
            int idx = -1;

            int i = 0;
            while (data.moveToNext()) {
                SubredditDTO dto = new SubredditDTO();
                dto.setDisplayName(data.getString(data.getColumnIndex(SubredditColumns.DISPLAYNAME)));
                if (dto.getDisplayName().equals(subreddit)) {
                    idx = i + 1;
                }
                strings.add(dto.getDisplayName());
                subredditDTOs.add(dto);
                i++;
            }
            if (idx == -1 && subreddit != null) {
                idx = subredditDTOs.size() + 1;
                strings.add(subreddit);
                SubredditDTO aux = new SubredditDTO();
                aux.setDisplayName(subreddit);
                subredditDTOs.add(aux);
            }

            if (idx == -1) {
                idx = 0;
            }

            spinner.setOnItemSelectedListener(null);
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MainActivity.this, R.layout.spinner_item, strings);
            spinner.setAdapter(arrayAdapter);
            spinner.setSelection(idx);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        subreddit = null;
                    } else {
                        subreddit = MainActivity.this.subredditDTOs.get(position - 1).getDisplayName();
                    }
                    refreshList();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }else if(loader.getId()==("list" + sort + subreddit).hashCode()){
            postDTOs = new ArrayList<>();
            while (data.moveToNext()) {
                PostDTO post = new PostDTO();
                post.setId(data.getString(data.getColumnIndex(PostColumns.ID)));
                post.setTitle(data.getString(data.getColumnIndex(PostColumns.TITLE)));
                post.setAuthor(data.getString(data.getColumnIndex(PostColumns.AUTHOR)));
                post.setDomain(data.getString(data.getColumnIndex(PostColumns.DOMAIN)));
//            post.setQtyUp(item.getInt("ups"));
//            post.setQtyDown(item.getInt("downs"));
                post.setCreated(data.getInt(data.getColumnIndex(PostColumns.CREATED)));
//            post.setQtyComments(item.getInt("num_comments"));
                post.setThumbnail(data.getString(data.getColumnIndex(PostColumns.THUMBNAIL)));
                post.setSubreddit(data.getString(data.getColumnIndex(PostColumns.SUBREDDIT)));
                post.setPermanlink(data.getString(data.getColumnIndex(PostColumns.PERMANLINK)));
                post.setUrl(StringEscapeUtils.unescapeHtml4((data.getString(data.getColumnIndex(PostColumns.URL)))));
                post.setSaved(data.getInt(data.getColumnIndex(PostColumns.SAVED))==1);
                post.setVisited(data.getInt(data.getColumnIndex(PostColumns.VISITED))==1);
                post.setUp(data.getInt(data.getColumnIndex(PostColumns.UP))==1);
                post.setDown(data.getInt(data.getColumnIndex(PostColumns.DOWN))==1);

                postDTOs.add(post);
            }
            postListFragment.refresh(postDTOs, false);
        }

    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void refreshList() {
        refreshPosts(sort);
    }
}
