package com.conceptualsystems.container.database;

import android.provider.BaseColumns;

import com.conceptualsystems.android.database.DbSchema;

public class DbSchemaContainer extends DbSchema {
    public static final String DATABASE_NAME = "smscontainer";
    public static final int DATABASE_VERSION = 1;

    public static final class ManifestSchema implements BaseColumns {
        public static final String TABLE_NAME = "manifest";
        public static final String COLUMN_LABEL = "label";
        public static final String COLUMN_ADDRESS = "address";
        public static final String COLUMN_LATITUDE = "latitude";
        public static final String COLUMN_LONGITUDE = "longitude";
        public static final String COLUMN_TIME = "time";
        public static final String COLUMN_TYPE = "type";
        public static final String CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_LABEL + " TEXT, " +
                        COLUMN_ADDRESS + " TEXT, " +
                        COLUMN_TIME + " REAL, " +
                        COLUMN_LATITUDE + " REAL, " +
                        COLUMN_LONGITUDE + " REAL, " +
                        COLUMN_TYPE + " INTEGER NOT NULL" +
                        ");";
        public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
}
