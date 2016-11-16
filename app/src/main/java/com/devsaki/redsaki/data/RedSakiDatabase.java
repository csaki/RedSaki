package com.devsaki.redsaki.data;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

/**
 * Created by sam_chordas on 10/5/15.
 */
@Database(version = RedSakiDatabase.VERSION)
public class RedSakiDatabase {
  private RedSakiDatabase(){}

  public static final int VERSION = 1;

  @Table(PostColumns.class) public static final String POSTS = "posts";
  @Table(SubredditColumns.class) public static final String SUBREDDITS = "subreddits";
}
