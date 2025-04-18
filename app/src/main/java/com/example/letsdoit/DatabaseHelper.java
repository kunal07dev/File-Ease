package com.example.letsdoit;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "FileManager.db";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "ImageMetadata";

    // Column definitions
     static final String COLUMN_ID = "id";
    static final String COLUMN_FILE_PATH = "filePath";
    static final String COLUMN_ORIGINAL_WIDTH = "originalWidth";
    static final String COLUMN_ORIGINAL_HEIGHT = "originalHeight";
    static final String COLUMN_COMPRESSED_SIZE = "compressedSize";
    public static final String COLUMN_COMPRESSED_QUALITY = "compressionQuality";
     static final String COLUMN_TIMESTAMP = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the table
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_FILE_PATH + " TEXT NOT NULL, " +
                COLUMN_ORIGINAL_WIDTH + " INTEGER, " +
                COLUMN_ORIGINAL_HEIGHT + " INTEGER, " +
                COLUMN_COMPRESSED_SIZE + " INTEGER, " + // Uncomment if needed
                COLUMN_COMPRESSED_QUALITY + " INTEGER, " +
                COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void insertMetadata(String filePath, int originalWidth, int originalHeight, int quality) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FILE_PATH, filePath);
        values.put(COLUMN_ORIGINAL_WIDTH, originalWidth);
        values.put(COLUMN_ORIGINAL_HEIGHT, originalHeight);
        // values.put(COLUMN_COMPRESSED_SIZE, compressedSize); // Uncomment if needed
        values.put(COLUMN_COMPRESSED_QUALITY, quality);

        db.insert(TABLE_NAME, null, values);
        db.close(); // Close database after insertion
    }

    public ImageMetadata getImageMetadataFromDb(String filePath) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_FILE_PATH + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{filePath});

        ImageMetadata metadata = null;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                @SuppressLint("Range") int width = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ORIGINAL_WIDTH));
                @SuppressLint("Range") int height = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ORIGINAL_HEIGHT));
                @SuppressLint("Range") int quality = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COMPRESSED_QUALITY));

                metadata = new ImageMetadata(filePath, width, height, quality);
            }
            cursor.close(); // Close cursor after use
        }
        db.close(); // Close database after query

        return metadata;
    }
}
