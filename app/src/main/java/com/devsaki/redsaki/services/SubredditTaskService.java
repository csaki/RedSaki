package com.devsaki.redsaki.services;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;

import com.devsaki.redsaki.data.RedSakiProvider;
import com.devsaki.redsaki.data.SubredditColumns;
import com.devsaki.redsaki.dto.SubredditDTO;
import com.devsaki.redsaki.util.HttpCallUtil;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SubredditTaskService extends GcmTaskService {
    private String LOG_TAG = SubredditTaskService.class.getSimpleName();

    private Context mContext;

    public SubredditTaskService() {
    }

    public SubredditTaskService(Context context) {
        mContext = context;
    }

    @Override
    public int onRunTask(TaskParams params) {
        Cursor initQueryCursor;
        if (mContext == null) {
            mContext = this;
        }
        String type = null;
        List<SubredditDTO> subreddits = null;
        int result = GcmNetworkManager.RESULT_FAILURE;
        if (params.getTag().equals("list")) {
            type = "list";
            try {
                subreddits = HttpCallUtil.listSubreddits(mContext);
            } catch (IOException | JSONException e) {
                Log.e(LOG_TAG, "Error", e);
            }
        } else if (params.getTag().equals("search")) {
            String query = params.getExtras().getString("query");
            type = "search" + query;
            try {
                subreddits = HttpCallUtil.listSearchSubreddits(mContext, query);
            } catch (IOException | JSONException e) {
                Log.e(LOG_TAG, "Error", e);
            }
        }

        if (subreddits == null) {
            result = GcmNetworkManager.RESULT_FAILURE;
        } else {
            ArrayList<ContentProviderOperation> contents = new ArrayList<>(subreddits.size());
            for (SubredditDTO subreddit : subreddits) {
                ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(RedSakiProvider.Subreddits.CONTENT_URI);
                builder.withValue(SubredditColumns.ID, subreddit.getId());
                builder.withValue(SubredditColumns.SUSCRIBER, subreddit.isSuscriber()?1:0);
                builder.withValue(SubredditColumns.DISPLAYNAME, subreddit.getDisplayName());
                builder.withValue(SubredditColumns.URL, subreddit.getUrl());
                builder.withValue(SubredditColumns.PUBLICDESCRIPTION, subreddit.getPublicDescription());
                builder.withValue(SubredditColumns.TYPE, type);

                contents.add(builder.build());
            }
            try {
                mContext.getContentResolver().delete(RedSakiProvider.Subreddits.CONTENT_URI, SubredditColumns.TYPE + "=?", new String[]{type});
                mContext.getContentResolver().applyBatch(RedSakiProvider.AUTHORITY,
                        contents);
            } catch (RemoteException | OperationApplicationException e) {
                Log.e(LOG_TAG, "Error", e);
            }
        }

        return result;
    }

}
