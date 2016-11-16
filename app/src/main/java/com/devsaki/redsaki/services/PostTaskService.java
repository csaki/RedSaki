package com.devsaki.redsaki.services;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;

import com.devsaki.redsaki.data.PostColumns;
import com.devsaki.redsaki.data.RedSakiProvider;
import com.devsaki.redsaki.dto.PostDTO;
import com.devsaki.redsaki.util.HttpCallUtil;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PostTaskService extends GcmTaskService {
    private String LOG_TAG = PostTaskService.class.getSimpleName();

    private Context mContext;

    public PostTaskService() {
    }

    public PostTaskService(Context context) {
        mContext = context;
    }

    @Override
    public int onRunTask(TaskParams params) {
        Cursor initQueryCursor;
        if (mContext == null) {
            mContext = this;
        }
        String type = null;
        List<PostDTO> posts = null;
        int result = GcmNetworkManager.RESULT_FAILURE;
        if (params.getTag().equals("list")) {
            String sort = params.getExtras().getString("sort");
            String subreddit = params.getExtras().getString("subreddit");
            type = "list" + sort + subreddit;
            try {
                posts = HttpCallUtil.list(mContext, subreddit, sort);
            } catch (IOException | JSONException e) {
                Log.e(LOG_TAG, "Error", e);
            }
        } else if (params.getTag().equals("search")) {
            String query = params.getExtras().getString("query");
            String filter = params.getExtras().getString("filter");
            String sort = params.getExtras().getString("sort");
            type = "search" + query + filter + sort;
            try {
                posts = HttpCallUtil.searchPosts(mContext, query, sort, filter);
            } catch (IOException | JSONException e) {
                Log.e(LOG_TAG, "Error", e);
            }
        } else if (params.getTag().equals("me")) {
            String category = params.getExtras().getString("category");
            String user = params.getExtras().getString("user");
            type = "me" + user + category;
            try {
                posts = HttpCallUtil.listUserCategory(mContext, user, category);
            } catch (IOException | JSONException e) {
                Log.e(LOG_TAG, "Error", e);
            }
        } else if (params.getTag().equals("periodic")) {
            String sort = "hot";
            String subreddit = null;
            type = "periodic";
            try {
                posts = HttpCallUtil.list(mContext, subreddit, sort);
            } catch (IOException | JSONException e) {
                Log.e(LOG_TAG, "Error", e);
            }
        }

        if (posts == null) {
            result = GcmNetworkManager.RESULT_FAILURE;
        } else {
            ArrayList<ContentProviderOperation> contents = new ArrayList<>(posts.size());
            for (PostDTO post : posts) {
                ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(RedSakiProvider.Posts.CONTENT_URI);
                builder.withValue(PostColumns.AUTHOR, post.getAuthor());
                builder.withValue(PostColumns.CREATED, post.getCreated());
                builder.withValue(PostColumns.DOMAIN, post.getDomain());
                builder.withValue(PostColumns.DOWN, post.isDown() ? 1 : 0);
                builder.withValue(PostColumns.UP, post.isUp() ? 1 : 0);
                builder.withValue(PostColumns.ID, post.getId());
                builder.withValue(PostColumns.PERMANLINK, post.getPermanlink());
                builder.withValue(PostColumns.TITLE, post.getTitle());
                builder.withValue(PostColumns.THUMBNAIL, post.getThumbnail());
                builder.withValue(PostColumns.SUBREDDIT, post.getSubreddit());
                builder.withValue(PostColumns.URL, post.getUrl());
                builder.withValue(PostColumns.SAVED, post.isSaved() ? 1 : 0);
                builder.withValue(PostColumns.VISITED, post.isVisited() ? 1 : 0);
                builder.withValue(PostColumns.TYPE, type);

                contents.add(builder.build());
            }
            try {
                mContext.getContentResolver().delete(RedSakiProvider.Posts.CONTENT_URI, PostColumns.TYPE + "=?", new String[]{type});
                mContext.getContentResolver().applyBatch(RedSakiProvider.AUTHORITY,
                        contents);
            } catch (RemoteException | OperationApplicationException e) {
                Log.e(LOG_TAG, "Error", e);
            }
        }

        return result;
    }

}
