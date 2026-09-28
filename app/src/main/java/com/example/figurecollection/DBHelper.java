package com.example.figurecollection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "figures.db";
    private static final int DB_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE figures (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "photo_path TEXT," +
                "length TEXT," +
                "width TEXT," +
                "height TEXT," +
                "created_at INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS figures");
        onCreate(db);
    }

    public long insertFigure(String name, String photoPath, String length, String width, String height) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("photo_path", photoPath);
        cv.put("length", length);
        cv.put("width", width);
        cv.put("height", height);
        cv.put("created_at", System.currentTimeMillis());
        return db.insert("figures", null, cv);
    }

    public void updateFigure(long id, String name, String photoPath, String length, String width, String height) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("photo_path", photoPath);
        cv.put("length", length);
        cv.put("width", width);
        cv.put("height", height);
        db.update("figures", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteFigure(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("figures", "id=?", new String[]{String.valueOf(id)});
    }

    public List<Figure> getAllFigures() {
        List<Figure> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("figures", null, null, null, null, null, "created_at DESC");
        while (c.moveToNext()) {
            list.add(new Figure(
                    c.getLong(c.getColumnIndexOrThrow("id")),
                    c.getString(c.getColumnIndexOrThrow("name")),
                    c.getString(c.getColumnIndexOrThrow("photo_path")),
                    c.getString(c.getColumnIndexOrThrow("length")),
                    c.getString(c.getColumnIndexOrThrow("width")),
                    c.getString(c.getColumnIndexOrThrow("height"))
            ));
        }
        c.close();
        return list;
    }
}
