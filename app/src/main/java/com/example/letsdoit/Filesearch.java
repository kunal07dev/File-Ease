package com.example.letsdoit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

    public class Filesearch extends BaseActivity {
        private GridView gridView;
        private ProgressBar progressBar;

        // For selections (file type, year, month)
        private List<String> displayList = new ArrayList<>();
        // For file items (with thumbnails)
        private List<File> fileList = new ArrayList<>();
        private String currentSelection = "fileType"; // "fileType", "year", "month", "files"
        private String selectedFileType = "", selectedYear = "";

        private static final int STORAGE_PERMISSION_CODE = 101;
        private static final String TAG = "FileExplorer";

        // Cache the complete file list once scanned to avoid re-scanning.
        private List<File> allFilesCache = null;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);


            gridView = findViewById(R.id.gridView);
            progressBar = findViewById(R.id.progressBar);


            displayFileTypes();

            gridView.setOnItemClickListener((parent, view, position, id) -> {
                if (currentSelection.equals("fileType")) {
                    selectedFileType = displayList.get(position);
                    displayYears();
                } else if (currentSelection.equals("year")) {
                    selectedYear = displayList.get(position);
                    displayMonths();
                } else if (currentSelection.equals("month")) {
                    String selectedMonth = displayList.get(position);
                    displayFiles(selectedYear, selectedMonth);
                } else if (currentSelection.equals("files")) {
                    // Open file with an external application
                    File file = fileList.get(position);
                    openFile(file);
                }
            });
        }

        @Override
        public void onBackPressed() {
            if (currentSelection.equals("files")) {
                displayMonths();
            } else if (currentSelection.equals("month")) {
                displayYears();
            } else if (currentSelection.equals("year")) {
                displayFileTypes();
            } else {
                super.onBackPressed();
            }
        }
        @Override
        protected int getLayoutId() {
            return R.layout.file_main;
        }

        @Override
        protected int getNavigationMenuItemId() {
            return R.id.Files;
        }
        @Override
        public void onRequestPermissionsResult(int requestCode,
                                               @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == STORAGE_PERMISSION_CODE) {
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    displayFileTypes();
                } else {
                    Toast.makeText(this, "Permission denied! The app may not work properly.",Toast.LENGTH_SHORT).show();
                }
            }
        }

        // Display file type options
        private void displayFileTypes() {
            displayList = Arrays.asList("Images", "Videos", "PDFs", "Docs", "Presentations", "Spreadsheets", "Audio", "Others");
            updateGridView(displayList, "fileType");
            allFilesCache = null;
        }

        // Asynchronously load all files (and cache the result) so that subsequent filtering is fast.
        private void loadAllFilesAsync(final AllFilesCallback callback) {
            // Show loading indicator
            progressBar.setVisibility(View.VISIBLE);
            new Thread(() -> {
                List<File> files = getAllFiles();
                allFilesCache = files;
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    callback.onFilesLoaded(files);
                });
            }).start();
        }

        interface AllFilesCallback {
            void onFilesLoaded(List<File> files);
        }

        // Display available years using cached file list or load them if needed.
        private void displayYears() {
            Log.d(TAG, "Fetching available years for: " + selectedFileType);
            if (allFilesCache != null) {
                List<String> years = computeAvailableYears(allFilesCache);
                if (years.isEmpty()) {
                    Toast.makeText(this, "No files found for this type", Toast.LENGTH_SHORT).show();
                    displayFileTypes();
                } else {
                    displayList = years;
                    updateGridView(displayList, "year");
                }
            } else {
                loadAllFilesAsync(files -> {
                    List<String> years = computeAvailableYears(files);
                    if (years.isEmpty()) {
                        Toast.makeText(Filesearch.this, "No files found for this type",
                                Toast.LENGTH_SHORT).show();
                        displayFileTypes();
                    } else {
                        displayList = years;
                        updateGridView(displayList, "year");
                    }
                });
            }
        }

        private List<String> computeAvailableYears(List<File> files) {
            Set<String> years = new HashSet<>();
            SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.getDefault());
            for (File file : files) {
                years.add(sdfYear.format(new Date(file.lastModified())));
            }
            return new ArrayList<>(years);
        }

        // Display months for the selected year
        private void displayMonths() {
            Log.d(TAG, "Fetching months for year: " + selectedYear);
            if (allFilesCache != null) {
                List<String> months = computeAvailableMonths(allFilesCache, selectedYear);
                if (months.isEmpty()) {
                    Toast.makeText(this, "No files found for this year", Toast.LENGTH_SHORT).show();
                    displayYears();
                } else {
                    displayList = months;
                    updateGridView(displayList, "month");
                }
            } else {
                loadAllFilesAsync(files -> {
                    List<String> months = computeAvailableMonths(files, selectedYear);
                    if (months.isEmpty()) {
                        Toast.makeText(Filesearch.this, "No files found for this year",
                                Toast.LENGTH_SHORT).show();
                        displayYears();
                    } else {
                        displayList = months;
                        updateGridView(displayList, "month");
                    }
                });
            }
        }

        private List<String> computeAvailableMonths(List<File> files, String year) {
            Set<String> months = new HashSet<>();
            SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.getDefault());
            SimpleDateFormat sdfMonth = new SimpleDateFormat("MMMM", Locale.getDefault());
            for (File file : files) {
                String fileYear = sdfYear.format(new Date(file.lastModified()));
                if (fileYear.equals(year)) {
                    months.add(sdfMonth.format(new Date(file.lastModified())));
                }
            }
            return new ArrayList<>(months);
        }

        // Display files for the selected year and month
        private void displayFiles(String year, String month) {
            Log.d(TAG, "Fetching files for " + year + "/" + month);
            if (allFilesCache != null) {
                List<File> filtered = filterFiles(allFilesCache, year, month);
                if (filtered.isEmpty()) {
                    Toast.makeText(this, "No files found for this period", Toast.LENGTH_SHORT).show();
                    displayMonths();
                } else {
                    fileList = filtered;
                    updateGridViewWithFiles(fileList, "files");
                }
            } else {
                loadAllFilesAsync(files -> {
                    List<File> filtered = filterFiles(files, year, month);
                    if (filtered.isEmpty()) {
                        Toast.makeText(Filesearch.this, "No files found for this period",
                                Toast.LENGTH_SHORT).show();
                        displayMonths();
                    } else {
                        fileList = filtered;
                        updateGridViewWithFiles(fileList, "files");
                    }
                });
            }
        }

        private List<File> filterFiles(List<File> files, String year, String month) {
            List<File> list = new ArrayList<>();
            SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.getDefault());
            SimpleDateFormat sdfMonth = new SimpleDateFormat("MMMM", Locale.getDefault());
            for (File file : files) {
                String fileYear = sdfYear.format(new Date(file.lastModified()));
                String fileMonth = sdfMonth.format(new Date(file.lastModified()));
                if (fileYear.equals(year) && fileMonth.equals(month)) {
                    list.add(file);
                }
            }
            return list;
        }

        // Synchronously retrieve all files using MediaStore and fallback scanning.
        private List<File> getAllFiles() {
            List<File> files = new ArrayList<>();

            switch (selectedFileType) {
                case "Images":
                    files.addAll(getMediaFiles(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null));
                    break;
                case "Videos":
                    files.addAll(getMediaFiles(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null));
                    break;
                case "PDFs":
                    files.addAll(getDocumentFiles("pdf"));
                    break;
                case "Docs":
                    files.addAll(getDocumentFiles("doc"));
                    break;
                case "Presentations":
                    files.addAll(getDocumentFiles("ppt"));
                    break;
                case "Spreadsheets":
                    files.addAll(getDocumentFiles("xls"));
                    break;
                case "Audio":
                    files.addAll(getAudioFiles());
                    break;
                case "Others":
                    files.addAll(getOtherFiles());
                    break;
            }

            if (files.isEmpty()) {
                files.addAll(scanStorageForFiles());
            }
            return files;
        }
        private List<File> getMediaFiles(Uri uri, String[] mimeTypes) {
            List<File> files = new ArrayList<>();
            String[] projection = {MediaStore.MediaColumns.DATA};
            String selection = null;
            String[] selectionArgs = null;
            String sortOrder = MediaStore.MediaColumns.DATE_MODIFIED + " DESC";

            if (mimeTypes != null && mimeTypes.length > 0) {
                StringBuilder selectionBuilder = new StringBuilder();
                for (int i = 0; i < mimeTypes.length; i++) {
                    if (i > 0) selectionBuilder.append(" OR ");
                    selectionBuilder.append(MediaStore.MediaColumns.MIME_TYPE).append("=?");
                }
                selection = selectionBuilder.toString();
                selectionArgs = mimeTypes;
            }

            try (Cursor cursor = getContentResolver().query(
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder)) {

                if (cursor != null) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                    while (cursor.moveToNext()) {
                        String filePath = cursor.getString(columnIndex);
                        if (filePath != null) {
                            File file = new File(filePath);
                            if (file.exists()) {
                                files.add(file);
                                Log.d(TAG, "Found media file: " + file.getAbsolutePath());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error querying media files", e);
            }
            return files;
        }

        private List<File> getDocumentFiles(String type) {
            List<File> files = new ArrayList<>();
            Uri uri = MediaStore.Files.getContentUri("external");
            String[] projection = {MediaStore.Files.FileColumns.DATA};
            String selection;
            String[] selectionArgs;
            String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";

            if (type.equals("pdf")) {
                selection = MediaStore.Files.FileColumns.MIME_TYPE + "=? OR " +
                        MediaStore.Files.FileColumns.MIME_TYPE + "=? OR " +
                        MediaStore.Files.FileColumns.MIME_TYPE + "=? OR " +
                        MediaStore.Files.FileColumns.DATA + " LIKE ?";
                selectionArgs = new String[]{
                        "application/pdf",
                        "application/x-pdf",
                        "application/vnd.pdf",
                        "%.pdf"
                };
            }
            else if (type.equals("ppt")) {
                selection = MediaStore.Files.FileColumns.MIME_TYPE + "=? OR " +
                        MediaStore.Files.FileColumns.MIME_TYPE + "=? OR " +
                        MediaStore.Files.FileColumns.DATA + " LIKE ? OR " +
                        MediaStore.Files.FileColumns.DATA + " LIKE ?";
                selectionArgs = new String[]{
                        "application/vnd.ms-powerpoint",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "%.ppt",
                        "%.pptx"
                };
            } else if (type.equals("xls")) {
                selection = MediaStore.Files.FileColumns.MIME_TYPE + "=? OR " +
                        MediaStore.Files.FileColumns.MIME_TYPE + "=? OR " +
                        MediaStore.Files.FileColumns.DATA + " LIKE ? OR " +
                        MediaStore.Files.FileColumns.DATA + " LIKE ?";
                selectionArgs = new String[]{
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "%.xls",
                        "%.xlsx"
                };
            }else { // "doc"
                selection = MediaStore.Files.FileColumns.MIME_TYPE + "=? OR " +
                        MediaStore.Files.FileColumns.MIME_TYPE + "=? OR " +
                        MediaStore.Files.FileColumns.MIME_TYPE + "=? OR " +
                        MediaStore.Files.FileColumns.MIME_TYPE + "=? OR " +
                        MediaStore.Files.FileColumns.DATA + " LIKE ? OR " +
                        MediaStore.Files.FileColumns.DATA + " LIKE ?";
                selectionArgs = new String[]{
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.ms-word",
                        "application/vnd.ms-word.document.macroEnabled.12",
                        "%.doc",
                        "%.docx"
                };
            }

            try (Cursor cursor = getContentResolver().query(
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder)) {

                if (cursor != null) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                    while (cursor.moveToNext()) {
                        String filePath = cursor.getString(columnIndex);
                        if (filePath != null) {
                            File file = new File(filePath);
                            if (file.exists()) {
                                files.add(file);
                                Log.d(TAG, "Found document: " + file.getAbsolutePath());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error querying document files", e);
            }
            return files;
        }

        private List<File> scanStorageForFiles() {
            List<File> files = new ArrayList<>();
            File[] storageDirectories;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                storageDirectories = getExternalFilesDirs(null);
            } else {
                storageDirectories = new File[]{Environment.getExternalStorageDirectory()};
            }

            for (File directory : storageDirectories) {
                if (directory != null) {
                    File parent = directory.getParentFile();
                    if (parent != null) {
                        scanDirectory(parent, files);
                    }
                }
            }
            return files;
        }

        private void scanDirectory(File directory, List<File> files) {
            File[] fileList = directory.listFiles();
            if (fileList != null) {
                for (File file : fileList) {
                    if (file.isDirectory()) {
                        if (!file.getName().equals("Android") &&
                                !file.getName().equals("data") &&
                                !file.getName().startsWith(".")) {
                            scanDirectory(file, files);
                        }
                    } else {
                        if (matchesFileType(file)) {
                            files.add(file);
                            Log.d(TAG, "Found file in scan: " + file.getAbsolutePath());
                        }
                    }
                }
            }
        }

        private boolean matchesFileType(File file) {
            String fileName = file.getName().toLowerCase();
            switch (selectedFileType) {
                case "Images":
                    return fileName.matches(".*\\.(jpg|jpeg|png|gif|webp)$");
                case "Videos":
                    return fileName.matches(".*\\.(mp4|mkv|avi|mov|flv|wmv)$");
                case "PDFs":
                    return fileName.endsWith(".pdf");
                case "Docs":
                    return fileName.matches(".*\\.(doc|docx|rtf|odt)$");
                case "Presentations":
                    return fileName.matches(".*\\.(ppt|pptx|odp)$");
                case "Spreadsheets":
                    return fileName.matches(".*\\.(xls|xlsx|ods)$");
                case "Audio":
                    return fileName.matches(".*\\.(mp3|wav|ogg|flac|aac)$");
                case "Others":
                    return fileName.matches(".*\\.(txt|zip|rar|7z|apk|html|xml|json)$");
                default:
                    return false;
            }
        }

        // Update GridView for String selections (file type, year, month)
        private void updateGridView(List<String> list, String selectionType) {
            TextAdapter adapter = new TextAdapter(this, list);
            gridView.setAdapter(adapter);
            currentSelection = selectionType;
        }

        // Update GridView for file items (displaying thumbnails and file names)
        private void updateGridViewWithFiles(List<File> files, String selectionType) {
            GenericAdapter adapter = new GenericAdapter(this, files);
            gridView.setAdapter(adapter);
            currentSelection = selectionType;
        }

        private void openFile(File file) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String mimeType;
            String fileName = file.getName().toLowerCase();

            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                    fileName.endsWith(".png") || fileName.endsWith(".gif")) {
                mimeType = "image/*";
            } else if (fileName.endsWith(".mp4") || fileName.endsWith(".mkv") ||
                    fileName.endsWith(".avi") || fileName.endsWith(".mov")) {
                mimeType = "video/*";
            } else if (fileName.endsWith(".pdf")) {
                mimeType = "application/pdf";
            } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
                mimeType = "application/msword";
            } else {
                mimeType = "*/*";
            }

            Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file);
            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "No application found to open this file",
                        Toast.LENGTH_SHORT).show();
            }
        }

        private void checkPermissions() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    } catch (Exception e) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivity(intent);
                    }
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                            },
                            STORAGE_PERMISSION_CODE);
                }
            }
        }
        private List<File> getAudioFiles() {
            List<File> files = new ArrayList<>();
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {MediaStore.Audio.Media.DATA};

            try (Cursor cursor = getContentResolver().query(
                    uri,
                    projection,
                    null,
                    null,
                    MediaStore.Audio.Media.DATE_MODIFIED + " DESC")) {

                if (cursor != null) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    while (cursor.moveToNext()) {
                        String filePath = cursor.getString(columnIndex);
                        if (filePath != null) {
                            File file = new File(filePath);
                            if (file.exists()) {
                                files.add(file);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error querying audio files", e);
            }
            return files;
        }

        private List<File> getOtherFiles() {
            List<File> files = new ArrayList<>();
            Uri uri = MediaStore.Files.getContentUri("external");
            String[] projection = {MediaStore.Files.FileColumns.DATA};
            String selection = MediaStore.Files.FileColumns.MIME_TYPE + " NOT IN (?,?,?,?,?,?,?,?,?)";
            String[] selectionArgs = {
                    "image/*",
                    "video/*",
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-powerpoint",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            };

            try (Cursor cursor = getContentResolver().query(
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC")) {

                if (cursor != null) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
                    while (cursor.moveToNext()) {
                        String filePath = cursor.getString(columnIndex);
                        if (filePath != null) {
                            File file = new File(filePath);
                            if (file.exists()) {
                                files.add(file);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error querying other files", e);
            }
            return files;
        }
    }
