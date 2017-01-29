package com.ilsecondodasinistra.catchit;

import android.content.Context;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

/**
 * Created by marco on 27/06/15.
 */
public class MyDatabase extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "actv.db";
    private static final int DATABASE_VERSION = 2;

    public MyDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
}