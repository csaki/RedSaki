package com.devsaki.redsaki.services;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.Html;
import android.text.format.DateUtils;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.devsaki.redsaki.R;
import com.devsaki.redsaki.data.PostColumns;
import com.devsaki.redsaki.data.RedSakiProvider;
import com.devsaki.redsaki.data.SubredditColumns;
import com.devsaki.redsaki.dto.PostDTO;

import org.apache.commons.lang3.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;

public class RedSakiWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RedSakiViewsFactory(this.getApplicationContext(),
                intent);
    }

    class RedSakiViewsFactory implements RemoteViewsService.RemoteViewsFactory {

        private Context ctxt;
        private int appWidgetId;
        private List<PostDTO> posts;

        public RedSakiViewsFactory(Context ctxt, Intent intent) {
            this.ctxt = ctxt;
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);

        }

        @Override
        public void onCreate() {
            // no-op
        }

        @Override
        public void onDestroy() {
            // no-op
        }

        @Override
        public int getCount() {
            return posts.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            RemoteViews row = new RemoteViews(ctxt.getPackageName(),
                    R.layout.widget_item_post);

            PostDTO item = posts.get(position);

            String title = "<strong><font color=\"#006aba\">@title</font></strong>  <small>(@domain)</small>".replace("@title", item.getTitle()).replace("@domain", item.getDomain());
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                row.setTextViewText(R.id.tvTitle, Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY));
            } else {
                row.setTextViewText(R.id.tvTitle, Html.fromHtml(title));
            }
            CharSequence time = DateUtils.getRelativeTimeSpanString(item.getCreated() * 1000l, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            String data = "<small><font color=\"#808080\">submitted <b>@time</b> by <b>@author</b> to <b>@subreddit</b></font></small>"
                    .replace("@author", item.getAuthor()).replace("@time", time).replace("@subreddit", item.getSubreddit());
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                row.setTextViewText(R.id.tvData, Html.fromHtml(data, Html.FROM_HTML_MODE_LEGACY));
            } else {
                row.setTextViewText(R.id.tvData, Html.fromHtml(data));
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(item.getUrl()));
            row.setOnClickFillInIntent(R.id.llContent, intent);
            return row;
        }

        @Override
        public RemoteViews getLoadingView() {
            return (null);
        }

        @Override
        public int getViewTypeCount() {
            return (1);
        }

        @Override
        public long getItemId(int position) {
            return (position);
        }

        @Override
        public boolean hasStableIds() {
            return (true);
        }

        @Override
        public void onDataSetChanged() {
            Cursor data = ctxt.getContentResolver().query(RedSakiProvider.Posts.CONTENT_URI,
                    new String[]{PostColumns._ID, PostColumns.ID, PostColumns.TITLE,
                            PostColumns.AUTHOR, PostColumns.DOMAIN, PostColumns.CREATED,
                            PostColumns.THUMBNAIL, PostColumns.SUBREDDIT, PostColumns.PERMANLINK,
                            PostColumns.URL, PostColumns.SAVED, PostColumns.VISITED,
                            PostColumns.UP, PostColumns.DOWN, PostColumns.TYPE},
                    SubredditColumns.TYPE + "=?",
                    new String[]{"periodic"},
                    null);
            if(data.getCount()==0){
                data = ctxt.getContentResolver().query(RedSakiProvider.Posts.CONTENT_URI,
                        new String[]{PostColumns._ID, PostColumns.ID, PostColumns.TITLE,
                                PostColumns.AUTHOR, PostColumns.DOMAIN, PostColumns.CREATED,
                                PostColumns.THUMBNAIL, PostColumns.SUBREDDIT, PostColumns.PERMANLINK,
                                PostColumns.URL, PostColumns.SAVED, PostColumns.VISITED,
                                PostColumns.UP, PostColumns.DOWN, PostColumns.TYPE},
                        SubredditColumns.TYPE + " like ?",
                        new String[]{"listhot%"},
                        null);
            }
            posts = new ArrayList<>(data.getCount());
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

                posts.add(post);
            }
            data.close();
        }
    }
}
