package com.devsaki.redsaki;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.devsaki.redsaki.data.PostColumns;
import com.devsaki.redsaki.data.RedSakiProvider;
import com.devsaki.redsaki.data.SubredditColumns;
import com.devsaki.redsaki.dto.PostDTO;
import com.devsaki.redsaki.fragments.PostListFragment;
import com.devsaki.redsaki.services.PostIntentService;
import com.devsaki.redsaki.util.CommonMethods;
import com.devsaki.redsaki.util.RefresherActivity;

import org.apache.commons.lang3.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, RefresherActivity {

    private static final int[] SORTED = new int[]{
            R.string.search_sort_relevance, R.string.search_sort_top, R.string.search_sort_new, R.string.search_sort_comments};
    private static final String[] SORTED_KEYS = new String[]{
            "relevance", "top", "new", "comments"};
    private static final int[] FILTER = new int[]{
            R.string.search_filter_all_time, R.string.search_filter_past_hour, R.string.search_filter_past_24_hours,
            R.string.search_filter_past_week, R.string.search_filter_past_month, R.string.search_filter_past_year};
    private static final String[] FILTER_KEYS = new String[]{
            "all", "hour", "day",
            "week", "month", "year"};

    Spinner spinnerSort, spinnerFilter;
    PostListFragment postListFragment;
    SharedPreferences sharedPref;
    String query, sort, filter;
    public static final String ARG_QUERY = "arg_query";
    List<PostDTO> postDTOs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_post);
        sharedPref = getSharedPreferences("private", Context.MODE_PRIVATE);
        query = getIntent().getStringExtra(ARG_QUERY);


        postListFragment = PostListFragment.newInstance(this);

        getFragmentManager().beginTransaction().add(R.id.frame_layout, postListFragment).commit();

        Toolbar toolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(toolbar);

        setTitle(query);

        spinnerSort = (Spinner) findViewById(R.id.spinner_sort);
        spinnerFilter = (Spinner) findViewById(R.id.spinner_filter);

        final String[] sorted = new String[SORTED.length];
        for (int i = 0; i < SORTED.length; i++) {
            sorted[i] = getString(SORTED[i]);
        }
        final String[] filter = new String[FILTER.length];
        for (int i = 0; i < FILTER.length; i++) {
            filter[i] = getString(FILTER[i]);
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(SearchActivity.this, R.layout.spinner_item, sorted);
        spinnerSort.setAdapter(arrayAdapter);
        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshPosts(SORTED_KEYS[position].toLowerCase(), SearchActivity.this.filter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        ArrayAdapter<String> arrayAdapterFilter = new ArrayAdapter<String>(SearchActivity.this, R.layout.spinner_item, filter);
        spinnerFilter.setAdapter(arrayAdapterFilter);
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshPosts(SearchActivity.this.sort, FILTER_KEYS[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        CommonMethods.commonMenu(this, null, id);

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(id==("search" + query + filter + sort).hashCode()){
            return new CursorLoader(this, RedSakiProvider.Posts.CONTENT_URI,
                    new String[]{PostColumns._ID, PostColumns.ID, PostColumns.TITLE,
                            PostColumns.AUTHOR, PostColumns.DOMAIN, PostColumns.CREATED,
                            PostColumns.THUMBNAIL, PostColumns.SUBREDDIT, PostColumns.PERMANLINK,
                            PostColumns.URL, PostColumns.SAVED, PostColumns.VISITED,
                            PostColumns.UP, PostColumns.DOWN, PostColumns.TYPE},
                    SubredditColumns.TYPE + "=?",
                    new String[]{"search" + query + filter + sort},
                    null);
        }else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(loader.getId()==("search" + query + filter + sort).hashCode()){
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
        refreshPosts(sort, filter);
    }

    public void refreshPosts(String sort, String filter) {
        this.sort = sort;
        this.filter = filter;
        Intent mServiceIntentPost = new Intent(this, PostIntentService.class);
        mServiceIntentPost.putExtra("tag", "search");
        mServiceIntentPost.putExtra("query", query);
        mServiceIntentPost.putExtra("sort", sort);
        mServiceIntentPost.putExtra("filter", filter);
        startService(mServiceIntentPost);
        getLoaderManager().initLoader(("search" + query + filter + sort).hashCode(), null, this);
        postListFragment.startLoad();
    }
}
