package com.devsaki.redsaki.data;

import android.net.Uri;

import net.simonvt.schematic.annotation.ContentProvider;
import net.simonvt.schematic.annotation.ContentUri;
import net.simonvt.schematic.annotation.InexactContentUri;
import net.simonvt.schematic.annotation.TableEndpoint;

/**
 * Created by sam_chordas on 10/5/15.
 */
@ContentProvider(authority = RedSakiProvider.AUTHORITY, database = RedSakiDatabase.class)
public class RedSakiProvider {
  public static final String AUTHORITY = "com.devsaki.redsaki.data.RedSakiProvider";

  static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

  interface Path{
    String POSTS = "posts";
    String SUBREDDITS = "subreddits";
  }

  private static Uri buildUri(String... paths){
    Uri.Builder builder = BASE_CONTENT_URI.buildUpon();
    for (String path:paths){
      builder.appendPath(path);
    }
    return builder.build();
  }

  @TableEndpoint(table = RedSakiDatabase.POSTS)
  public static class Posts{
    @ContentUri(
        path = Path.POSTS,
        type = "vnd.android.cursor.dir/post"
    )
    public static final Uri CONTENT_URI = buildUri(Path.POSTS);

    @InexactContentUri(
        name = "POST_ID",
        path = Path.POSTS + "/*",
        type = "vnd.android.cursor.item/post",
        whereColumn = PostColumns.TYPE,
        pathSegment = 1
    )
    public static Uri withType(String type){
      return buildUri(Path.POSTS, type);
    }
  }

  @TableEndpoint(table = RedSakiDatabase.SUBREDDITS)
  public static class Subreddits{
    @ContentUri(
            path = Path.SUBREDDITS,
            type = "vnd.android.cursor.dir/subreddit"
    )
    public static final Uri CONTENT_URI = buildUri(Path.SUBREDDITS);

    @InexactContentUri(
            name = "SUBREDDIT_ID",
            path = Path.SUBREDDITS + "/*",
            type = "vnd.android.cursor.item/subreddit",
            whereColumn = PostColumns.TYPE,
            pathSegment = 1
    )
    public static Uri withType(String type){
      return buildUri(Path.SUBREDDITS, type);
    }
  }
}
