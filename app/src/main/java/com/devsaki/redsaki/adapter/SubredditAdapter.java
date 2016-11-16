package com.devsaki.redsaki.adapter;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.devsaki.redsaki.MainActivity;
import com.devsaki.redsaki.MeActivity;
import com.devsaki.redsaki.R;
import com.devsaki.redsaki.data.RedSakiProvider;
import com.devsaki.redsaki.data.SubredditColumns;
import com.devsaki.redsaki.dto.PostDTO;
import com.devsaki.redsaki.dto.SubredditDTO;
import com.devsaki.redsaki.util.CommonMethods;
import com.devsaki.redsaki.util.HttpCallUtil;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by DevSaki on 16/10/2016.
 */

public class SubredditAdapter extends RecyclerView.Adapter<SubredditAdapter.ViewHolder> {

    private List<SubredditDTO> items;
    private Context context;

    public SubredditAdapter(List<SubredditDTO> items, Context context) {
        this.items = items;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_item_subreddit, parent, false);
        return new ViewHolder(v);

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final SubredditDTO item = items.get(position);
        String title = "<strong><font color=\"#006aba\">@title</font></strong>  <small>(@url)</small>".replace("@title", item.getDisplayName()).replace("@url", item.getUrl());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            holder.tvTitle.setText(Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY));
        } else {
            holder.tvTitle.setText(Html.fromHtml(title));
        }

        int color = ContextCompat.getColor(context, R.color.colorAccent);
        if (item.isSuscriber()) {
            holder.btnSubscribe.setText(R.string.unsubscribe);
            holder.btnSubscribe.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
        } else {
            holder.btnSubscribe.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
            holder.btnSubscribe.setText(R.string.subscribe);
        }
        holder.btnSubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonMethods.checkLogin(context)) {
                    new SubredditAsyncTask((Activity) context, holder.btnSubscribe, item).execute();
                }
            }
        });
        holder.llContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra(MainActivity.ARG_SUBREDDIT, item.getDisplayName());
                context.startActivity(intent);
            }
        });
        holder.tvPublicDescription.setText(item.getPublicDescription());
        holder.itemView.setTag(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvTitle;
        public TextView tvPublicDescription;
        public Button btnSubscribe;
        public LinearLayout llContent;

        public ViewHolder(View itemView) {
            super(itemView);
            tvTitle = (TextView) itemView.findViewById(R.id.tvTitle);
            tvPublicDescription = (TextView) itemView.findViewById(R.id.tvPublicDescription);
            btnSubscribe = (Button) itemView.findViewById(R.id.btnSubscribe);
            llContent = (LinearLayout) itemView.findViewById(R.id.llContent);
        }
    }


    public static class SubredditAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private Activity activity;
        private SubredditDTO item;
        private Button button;

        public SubredditAsyncTask(Activity activity, Button button, SubredditDTO item) {
            this.activity = activity;
            this.item = item;
            this.button = button;

        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                HttpCallUtil.suscribe(activity, item.getId(), item.isSuscriber());
                item.setSuscriber(!item.isSuscriber());
                return item.isSuscriber();
            } catch (IOException | JSONException e) {
                Log.e("PostAdapter", "btnSave", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (aBoolean != null) {
                if (aBoolean) {
                    button.setText(R.string.unsubscribe);
                    button.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.white));
                    ArrayList<ContentProviderOperation> contents = new ArrayList<>(1);
                    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(RedSakiProvider.Subreddits.CONTENT_URI);
                    builder.withValue(SubredditColumns.ID, item.getId());
                    builder.withValue(SubredditColumns.SUSCRIBER, item.isSuscriber()?1:0);
                    builder.withValue(SubredditColumns.DISPLAYNAME, item.getDisplayName());
                    builder.withValue(SubredditColumns.URL, item.getUrl());
                    builder.withValue(SubredditColumns.PUBLICDESCRIPTION, item.getPublicDescription());
                    builder.withValue(SubredditColumns.TYPE, "list");
                    contents.add(builder.build());
                    try {
                        activity.getContentResolver().applyBatch(RedSakiProvider.AUTHORITY,
                                contents);
                    } catch (RemoteException e) {
                        Log.e("Error", "Error", e);
                    } catch (OperationApplicationException e) {
                        Log.e("Error", "Error", e);
                    }
                } else {
                    button.setBackgroundColor(ContextCompat.getColor(activity, R.color.colorAccent));
                    button.setText(R.string.subscribe);
                    activity.getContentResolver().delete(RedSakiProvider.Subreddits.CONTENT_URI, SubredditColumns.TYPE + "=? AND " + SubredditColumns.ID + "=?", new String[]{"list", item.getId()});
                }
                ContentValues contentValues = new ContentValues();
                contentValues.put(SubredditColumns.SUSCRIBER, item.isSuscriber()?1:0);
                activity.getContentResolver().update(RedSakiProvider.Subreddits.CONTENT_URI, contentValues, "ID=?", new String[]{item.getId()});
            }
        }
    }
}
