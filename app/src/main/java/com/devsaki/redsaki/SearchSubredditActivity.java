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

import com.devsaki.redsaki.data.RedSakiProvider;
import com.devsaki.redsaki.data.SubredditColumns;
import com.devsaki.redsaki.dto.SubredditDTO;
import com.devsaki.redsaki.fragments.SubredditListFragment;
import com.devsaki.redsaki.services.SubredditIntentService;
import com.devsaki.redsaki.util.CommonMethods;
import com.devsaki.redsaki.util.RefresherActivity;

import java.util.ArrayList;
import java.util.List;

public class SearchSubredditActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, RefresherActivity {

    SubredditListFragment subredditListFragment;
    SharedPreferences sharedPref;
    String query;
    public static final String ARG_QUERY = "arg_query";
    List<SubredditDTO> subredditDTOs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_subreddit);
        sharedPref = getSharedPreferences("private", Context.MODE_PRIVATE);
        query = getIntent().getStringExtra(ARG_QUERY);


        subredditListFragment = SubredditListFragment.newInstance(this);

        getFragmentManager().beginTransaction().add(R.id.frame_layout, subredditListFragment).commit();

        Toolbar toolbar = (Toolbar) findViewById(R.id.appbar);
        setSupportActionBar(toolbar);

        setTitle(query);

        refreshList();
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
        if(id==("search" + query).hashCode()){
            return new CursorLoader(this, RedSakiProvider.Subreddits.CONTENT_URI,
                    new String[]{SubredditColumns._ID, SubredditColumns.ID, SubredditColumns.DISPLAYNAME,
                            SubredditColumns.TYPE, SubredditColumns.URL, SubredditColumns.SUSCRIBER,
                            SubredditColumns.PUBLICDESCRIPTION},
                    SubredditColumns.TYPE + " = ?",
                    new String[]{"search" + query},
                    null);
        }else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(loader.getId()==("search" + query).hashCode()){
            int i = 0;
            subredditDTOs = new ArrayList<>();
            while (data.moveToNext()) {
                SubredditDTO dto = new SubredditDTO();
                dto.setUrl(data.getString(data.getColumnIndex(SubredditColumns.URL)));
                dto.setSuscriber(data.getInt(data.getColumnIndex(SubredditColumns.SUSCRIBER))==1);
                dto.setPublicDescription(data.getString(data.getColumnIndex(SubredditColumns.PUBLICDESCRIPTION)));
                dto.setId(data.getString(data.getColumnIndex(SubredditColumns.ID)));
                dto.setDisplayName(data.getString(data.getColumnIndex(SubredditColumns.DISPLAYNAME)));
                subredditDTOs.add(dto);
            }
            subredditListFragment.refresh(subredditDTOs, false);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void refreshList() {
        subredditListFragment.startLoad();
        Intent mServiceIntent = new Intent(this, SubredditIntentService.class);
        mServiceIntent.putExtra("tag", "search");
        mServiceIntent.putExtra("query", query);
        startService(mServiceIntent);
        getLoaderManager().initLoader(("search" + query).hashCode(), null, this);
    }
}
