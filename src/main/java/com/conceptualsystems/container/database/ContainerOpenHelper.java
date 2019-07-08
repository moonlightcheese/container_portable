package com.conceptualsystems.container.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.conceptualsystems.android.database.DatabaseHelperFactory;
import com.conceptualsystems.android.database.DbSchema;

public class ContainerOpenHelper extends SQLiteOpenHelper implements DatabaseHelperFactory {
    public ContainerOpenHelper getHelperImpl(Context context) {
        return new ContainerOpenHelper(context);
    }

    public ContainerOpenHelper(Context context) {
        super(context, DbSchemaContainer.DATABASE_NAME, null, DbSchemaContainer.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DbSchema.SiteSchema.CREATE_TABLE);
        db.execSQL(DbSchemaContainer.ManifestSchema.CREATE_TABLE);
        insertTestData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void insertTestData(SQLiteDatabase db) {
        ContentValues insertValues;
        insertValues = new ContentValues();
        insertValues.put(DbSchemaContainer.ManifestSchema.COLUMN_LABEL, "Name of place");
        insertValues.put(DbSchemaContainer.ManifestSchema.COLUMN_ADDRESS, "3662 Central Pike, Hermitage TN");
        insertValues.put(DbSchemaContainer.ManifestSchema.COLUMN_TYPE, 0);
        db.insert(DbSchemaContainer.ManifestSchema.TABLE_NAME, null, insertValues);

        insertValues = new ContentValues();
        insertValues.put(DbSchemaContainer.ManifestSchema.COLUMN_ADDRESS, "наб. канала Грибоедова, 49, Санкт-Петербург, 190031");
        insertValues.put(DbSchemaContainer.ManifestSchema.COLUMN_TYPE, 0);
        db.insert(DbSchemaContainer.ManifestSchema.TABLE_NAME, null, insertValues);

        //59.928367, 30.312029
        insertValues = new ContentValues();
        insertValues.put(DbSchemaContainer.ManifestSchema.COLUMN_LATITUDE, 59.928367);
        insertValues.put(DbSchemaContainer.ManifestSchema.COLUMN_LONGITUDE, 30.312029);
        insertValues.put(DbSchemaContainer.ManifestSchema.COLUMN_TYPE, 0);
        db.insert(DbSchemaContainer.ManifestSchema.TABLE_NAME, null, insertValues);
    }
}
