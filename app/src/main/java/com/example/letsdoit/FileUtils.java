package com.example.letsdoit;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    private static final String TAG = "FileUtils";

    /**
     * Get a list of all image files from the device storage.
     *
     * @param context the application context.
     * @return a list of URIs pointing to image files.
     */
    public static List<Uri> getImageFiles(Context context) {
        List<Uri> imageUris = new ArrayList<>();
        Uri collection;

        // For devices running Android Q or later
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE
        };

        try (Cursor cursor = context.getContentResolver().query(
                collection,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        )) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    Uri contentUri = Uri.withAppendedPath(collection, String.valueOf(id));
                    imageUris.add(contentUri);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching image files: ", e);
        }

        return imageUris;
    }

    /**
     * Get the size of a file in a human-readable format (e.g., KB, MB, etc.).
     *
     * @param file the file object.
     * @return the formatted file size as a string.
     */
    public static String getFileSize(File file) {
        long size = file.length();
        return formatFileSize(size);
    }

    /**
     * Format file size into a human-readable string.
     *
     * @param size the size in bytes.
     * @return formatted size string (e.g., KB, MB).
     */
    private static String formatFileSize(long size) {
        if (size <= 0) return "0B";

        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    /**
     * Deletes the original file.
     *
     * @param file the file to delete.
     * @return true if the file was successfully deleted, false otherwise.
     */
    public static boolean deleteFile(File file) {
        return file.exists() && file.delete();
    }

    /**
     * Get a file's name from its URI.
     *
     * @param context the application context.
     * @param uri     the file's URI.
     * @return the file name as a string.
     */
    public static String getFileName(Context context, Uri uri) {
        String result = null;

        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                    result = cursor.getString(nameIndex);
                }
            }
        }

        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }

        return result;
    }
    public static String getPath(ImageCompressionActivity context, Uri uri) {
        String path = null;
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {MediaStore.Images.Media.DATA};
            try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    path = cursor.getString(columnIndex);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return path;
    }
}
