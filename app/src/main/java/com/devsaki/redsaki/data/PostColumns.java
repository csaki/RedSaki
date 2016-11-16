package com.devsaki.redsaki.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.PrimaryKey;

/**
 * Created by DevSaki on 14/11/2016.
 */

public class PostColumns {

    @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement
    public static final String _ID="_id";
    @DataType(DataType.Type.TEXT)
    public static final String ID="id";
    @DataType(DataType.Type.TEXT)
    public static final String TITLE="title";
    @DataType(DataType.Type.INTEGER)
    public static final String CREATED="created";
    @DataType(DataType.Type.TEXT)
    public static final String AUTHOR="author";
    @DataType(DataType.Type.TEXT)
    public static final String DOMAIN="domain";
    @DataType(DataType.Type.TEXT)
    public static final String THUMBNAIL="thumbnail";
    @DataType(DataType.Type.TEXT)
    public static final String SUBREDDIT="subreddit";
    @DataType(DataType.Type.TEXT)
    public static final String URL="url";
    @DataType(DataType.Type.TEXT)
    public static final String PERMANLINK="permanlink";
    @DataType(DataType.Type.INTEGER)
    public static final String SAVED="saved";
    @DataType(DataType.Type.INTEGER)
    public static final String UP="up";
    @DataType(DataType.Type.INTEGER)
    public static final String DOWN="down";
    @DataType(DataType.Type.INTEGER)
    public static final String VISITED="visited";
    @DataType(DataType.Type.TEXT)
    public static final String TYPE="type";
}
