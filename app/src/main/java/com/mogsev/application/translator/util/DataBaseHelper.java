package com.mogsev.application.translator.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.mogsev.application.translator.model.Language;
import com.mogsev.application.translator.model.TranslateItem;

import java.util.ArrayList;

/**
 * Created by zhenya on 19.07.2016.
 */
public class DataBaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DataBaseHelper";

    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "Translate";
    private static DataBaseHelper mInstance = null;
    private Context context;

    //Table English to Russian
    private static final String TBL_EN_RU = "en_ru";
    private static final String COL_ID = "ID";
    private static final String COL_TYPE = "type";
    private static final String COL_IN = "col_in";
    private static final String COL_OUT = "col_out";

    //Table Russian to English
    private static final String TBL_RU_EN = "ru_en";

    private DataBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
        this.context = context;
    }

    public static DataBaseHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DataBaseHelper(context.getApplicationContext());
        }
        return mInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String CREATE_TBL_EN_RU = "CREATE TABLE " + TBL_EN_RU
                + " (" + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + COL_IN + " TEXT NOT NULL, "
                + COL_OUT + " TEXT NOT NULL)";

        String CREATE_TBL_RU_EN = "CREATE TABLE " + TBL_RU_EN
                + " (" + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + COL_IN + " TEXT NOT NULL, "
                + COL_OUT + " TEXT NOT NULL)";

        sqLiteDatabase.execSQL(CREATE_TBL_EN_RU);
        sqLiteDatabase.execSQL(CREATE_TBL_RU_EN);

        Log.d(TAG, "onCreate finish");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TBL_EN_RU);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TBL_RU_EN);
    }

    public synchronized ArrayList<TranslateItem> getTranslateList(Language.TRANSLATE type) {
        Log.d(TAG, "getTranslateList");
        ArrayList<TranslateItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String tableName = getTableName(type);

        Cursor cursor = db.query(tableName, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                TranslateItem item = new TranslateItem();
                item.setID(cursor.getInt(cursor.getColumnIndex(COL_ID)));
                item.setIn(cursor.getString(cursor.getColumnIndex(COL_IN)));
                item.setOut(cursor.getString(cursor.getColumnIndex(COL_OUT)));
                list.add(item);
            } while (cursor.moveToNext());
        }

        db.close();
        return list;
    }

    public synchronized void addTranslateItem(TranslateItem item, Language.TRANSLATE type) {
        Log.d(TAG, "addTranslateItem");

        Log.d(TAG, "Alarm add: " + item.toString());
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        String tableName = getTableName(type);

        if (item.getID() != 0) {
            contentValues.put(COL_ID, item.getID());
        }
        contentValues.put(COL_IN, item.getIn());
        contentValues.put(COL_OUT, item.getOut());

        db.insertWithOnConflict(tableName, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);

        db.close();
        Log.d(TAG, "TranslateItem added");
    }

    public synchronized void removeTranslateItem(TranslateItem item, Language.TRANSLATE type) {
        Log.d(TAG, "removeTranslateItem");
        SQLiteDatabase db = getWritableDatabase();
        String tableName = getTableName(type);
        try {
            String[] id = { String.valueOf(item.getID()) };
            int i = db.delete(tableName, COL_ID + "=?", id);
            Log.d(TAG, "Delete alarm: " + i);
        } finally {
            db.close();
        }
    }

    private String getTableName(Language.TRANSLATE type) {
        Log.d(TAG, "getTableName");
        switch (type) {
            case EN_RU:
                return TBL_EN_RU;
            case RU_EN:
                return TBL_RU_EN;
            default:
                return TBL_EN_RU;
        }
    }
}
