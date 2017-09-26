package com.ilsecondodasinistra.catchit

import android.content.Context

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper

/**
 * Created by marco on 27/06/15.
 */
class MyDatabase(context: Context) : SQLiteAssetHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {

        private val DATABASE_NAME = "actv.db"
        private val DATABASE_VERSION = 3
    }
}