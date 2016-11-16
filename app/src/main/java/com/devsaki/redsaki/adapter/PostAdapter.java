package com.devsaki.redsaki.adapter;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.devsaki.redsaki.R;
import com.devsaki.redsaki.data.PostColumns;
import com.devsaki.redsaki.data.RedSakiProvider;
import com.devsaki.redsaki.data.SubredditColumns;
import com.devsaki.redsaki.dto.PostDTO;
import com.devsaki.redsaki.util.CommonMethods;
import com.devsaki.redsaki.util.HttpCallUtil;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

/**
 * Created by DevSaki on 16/10/2016.
 */

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {

    private List<PostDTO> items;
    private Context context;

    public PostAdapter(List<PostDTO> items, Context context) {
        this.items = items;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_item_post, parent, false);
        return new ViewHolder(v);

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final PostDTO item = items.get(position);
        String title = "<strong><font color=\"#006aba\">@title</font></strong>  <small>(@domain)</small>".replace("@title", item.getTitle()).replace("@domain", item.getDomain());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            holder.tvTitle.setText(Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY));
        } else {
            holder.tvTitle.setText(Html.fromHtml(title));
        }
        if (item.getThumbnail() != null && URLUtil.isValidUrl(item.getThumbnail())) {
            Glide.with(context).load(item.getThumbnail()).into(holder.ivImage);
            holder.ivImage.setVisibility(View.VISIBLE);
        } else
            holder.ivImage.setVisibility(View.GONE);

        CharSequence time = DateUtils.getRelativeTimeSpanString(item.getCreated() * 1000l, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
        String data = "<small><font color=\"#808080\">submitted <b>@time</b> by <b>@author</b> to <b>@subreddit</b></font></small>"
                .replace("@author", item.getAuthor()).replace("@time", time).replace("@subreddit", item.getSubreddit());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            holder.tvData.setText(Html.fromHtml(data, Html.FROM_HTML_MODE_LEGACY));
        } else {
            holder.tvData.setText(Html.fromHtml(data));
        }

        int color = ContextCompat.getColor(context, R.color.colorAccent);
        if (item.isSaved()) {
            holder.btnSave.setColorFilter(color);
        }
        if (item.isUp()) {
            holder.btnUp.setColorFilter(color);
        }
        if (item.isDown()) {
            holder.btnDown.setColorFilter(color);
        }

        holder.llContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl((Activity) context, Uri.parse(item.getUrl()));
            }
        });
        holder.btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, item.getTitle());
                sendIntent.putExtra(Intent.EXTRA_TEXT, item.getUrl());
                sendIntent.setType("text/plain");
                context.startActivity(Intent.createChooser(sendIntent, "Share link!"));
            }
        });
        holder.btnComments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl((Activity) context, Uri.parse("https://www.reddit.com" + item.getPermanlink()));
            }
        });
        holder.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonMethods.checkLogin(context)) {
                    new PostAsyncTask((Activity) context, item, PostAsyncTask.Action.SAVE, holder.btnSave, null).execute();
                }
            }
        });
        holder.btnUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonMethods.checkLogin(context)) {
                    new PostAsyncTask((Activity) context, item, PostAsyncTask.Action.UP, holder.btnUp, holder.btnDown).execute();
                }
            }
        });
        holder.btnDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonMethods.checkLogin(context)) {
                    new PostAsyncTask((Activity) context, item, PostAsyncTask.Action.DOWN, holder.btnDown, holder.btnUp).execute();
                }
            }
        });

        holder.itemView.setTag(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView ivImage;
        public TextView tvTitle;
        public TextView tvData;
        public ImageButton btnUp;
        public ImageButton btnDown;
        public ImageButton btnShare;
        public ImageButton btnComments;
        public ImageButton btnSave;
        public LinearLayout llContent;


        public ViewHolder(View itemView) {
            super(itemView);
            ivImage = (ImageView) itemView.findViewById(R.id.ivImage);
            tvTitle = (TextView) itemView.findViewById(R.id.tvTitle);
            tvData = (TextView) itemView.findViewById(R.id.tvData);
            btnUp = (ImageButton) itemView.findViewById(R.id.btnUp);
            btnDown = (ImageButton) itemView.findViewById(R.id.btnDown);
            btnShare = (ImageButton) itemView.findViewById(R.id.btnShare);
            btnComments = (ImageButton) itemView.findViewById(R.id.btnComments);
            btnSave = (ImageButton) itemView.findViewById(R.id.btnSave);
            llContent = (LinearLayout) itemView.findViewById(R.id.llContent);
        }
    }


    public static class PostAsyncTask extends AsyncTask<Void, Void, Boolean> {

        private Activity activity;
        private PostDTO item;
        private Action action;
        private ImageButton imageButton;
        private ImageButton imageButton2;

        enum Action {
            SAVE, UP, DOWN
        }

        public PostAsyncTask(Activity activity, PostDTO item, Action action, ImageButton imageButton, ImageButton imageButton2) {
            this.activity = activity;
            this.item = item;
            this.action = action;
            this.imageButton = imageButton;
            this.imageButton2 = imageButton2;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if (action == Action.SAVE) {
                    HttpCallUtil.savedPost(activity, item.getId(), !item.isSaved());
                    item.setSaved(!item.isSaved());
                    return item.isSaved();
                } else if (action == Action.UP) {
                    HttpCallUtil.votePost(activity, item.getId(), item.isUp() ? 0 : 1);
                    item.setUp(!item.isUp());
                    item.setDown(false);
                    return item.isUp();
                } else if (action == Action.DOWN) {
                    HttpCallUtil.votePost(activity, item.getId(), item.isDown() ? 0 : -1);
                    item.setUp(false);
                    item.setDown(!item.isDown());
                    return item.isDown();
                }
            } catch (IOException | JSONException e) {
                Log.e("PostAdapter", "btnSave", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(aBoolean!=null){
                int color = ContextCompat.getColor(activity, R.color.colorAccent);
                if (aBoolean) {
                    imageButton.setColorFilter(color);
                    if(action==Action.DOWN || action==Action.UP){
                        imageButton2.setColorFilter(null);
                    }
                } else {
                    imageButton.setColorFilter(null);
                }
                ContentValues contentValues = new ContentValues();
                contentValues.put(PostColumns.UP, item.isUp()?1:0);
                contentValues.put(PostColumns.DOWN, item.isDown()?1:0);
                contentValues.put(PostColumns.SAVED, item.isSaved()?1:0);
                activity.getContentResolver().update(RedSakiProvider.Posts.CONTENT_URI, contentValues, "ID=?", new String[]{item.getId()});
            }
        }
    }
}
