package com.devsaki.redsaki.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.PrimaryKey;

/**
 * Created by DevSaki on 15/11/2016.
 */

public class SubredditColumns {

    @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement
    public static final String _ID="_id";
    @DataType(DataType.Type.TEXT)
    public static final String ID="id";
    @DataType(DataType.Type.TEXT)
    public static final String DISPLAYNAME="displayname";
    @DataType(DataType.Type.TEXT)
    public static final String URL="url";
    @DataType(DataType.Type.TEXT)
    public static final String PUBLICDESCRIPTION="publicdescription";
    @DataType(DataType.Type.INTEGER)
    public static final String SUSCRIBER="suscriber";
    @DataType(DataType.Type.TEXT)
    public static final String TYPE="type";
}
