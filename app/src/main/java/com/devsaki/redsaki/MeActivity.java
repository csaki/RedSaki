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

public class MeActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, RefresherActivity {

    private static final String[] CATEGORIES = new String[]{
            "saved", "upvoted", "downvoted"};

    Spinner spinnerSort;
    PostListFragment postListFragment;
    SharedPreferences sharedPref;
    String username, category;
    List<PostDTO> postDTOs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_me);
        sharedPref = getSharedPreferences("private", Context.MODE_PRIVATE);

        username = sharedPref.getString("username", "");

        postListFragment = PostListFragment.newInstance(this);

        getFragmentManager().beginTransaction().add(R.id.frame_layout, postListFragment).commit();

        Toolbar toolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(toolbar);

        setTitle(getString(R.string.user_title).replace("@user", username));

        spinnerSort = (Spinner) findViewById(R.id.spinner_sort);

        final String[] strings = new String[]{
                getString(R.string.category_saved), getString(R.string.category_upvoted),
                getString(R.string.category_downvoted)};

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MeActivity.this, R.layout.spinner_item, strings);
        spinnerSort.setAdapter(arrayAdapter);
        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshPosts(CATEGORIES[position].toLowerCase());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        refreshPosts(CATEGORIES[0]);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_login) {
            Intent intent = new Intent(MeActivity.this, MainActivity.class);
            startActivity(intent);
        } else if (id == R.id.action_search) {
            CommonMethods.search(this, null);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void refreshList() {
        refreshPosts(category);
    }

    private void refreshPosts(String category){
        this.category = category;
        Intent mServiceIntentPost = new Intent(this, PostIntentService.class);
        mServiceIntentPost.putExtra("tag", "me");
        mServiceIntentPost.putExtra("user", username);
        mServiceIntentPost.putExtra("category", category);
        startService(mServiceIntentPost);
        getLoaderManager().initLoader(("me" + username + category).hashCode(), null, this);
        postListFragment.startLoad();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(id==("me" + username + category).hashCode()){
            return new CursorLoader(this, RedSakiProvider.Posts.CONTENT_URI,
                    new String[]{PostColumns._ID, PostColumns.ID, PostColumns.TITLE,
                            PostColumns.AUTHOR, PostColumns.DOMAIN, PostColumns.CREATED,
                            PostColumns.THUMBNAIL, PostColumns.SUBREDDIT, PostColumns.PERMANLINK,
                            PostColumns.URL, PostColumns.SAVED, PostColumns.VISITED,
                            PostColumns.UP, PostColumns.DOWN, PostColumns.TYPE},
                    SubredditColumns.TYPE + "=?",
                    new String[]{"me" + username + category},
                    null);
        }else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(loader.getId()==("me" + username + category).hashCode()){
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
}
