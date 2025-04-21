package com.example.letsdoit;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.DocumentsContract;
import android.os.StatFs;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity {
    private ImageView photobtn, videobtn, pdfbtn, docbtn;
    private TextView photosize, videosize, pdfsize, docsize, stext;
    private RelativeLayout loadingOverlay;
    private ExecutorService executorService;
    private Handler mainThreadHandler;
    private BottomNavigationView bottomNavigationView;
    private ProgressBar sbar;
    private CardView photocard,videocard,pdfcard,Docs;
    private static final int PICK_DOCUMENT_REQUEST = 1;
    private ActivityResultLauncher<Intent> documentPickerLauncher;

    private static final int STORAGE_PERMISSION_CODE = 101;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
        photocard = findViewById(R.id.photocard);

        pdfbtn = findViewById(R.id.pdfbtn);
        docbtn = findViewById(R.id.docbtn);
        photobtn = findViewById(R.id.photobtn);
        videobtn = findViewById(R.id.videobtn);
        pdfbtn = findViewById(R.id.pdfbtn);
        docbtn = findViewById(R.id.docbtn);
        stext = findViewById(R.id.stext);
        sbar = findViewById(R.id.sbar);

        photosize = findViewById(R.id.photosize);
        videosize = findViewById(R.id.videosize);
        pdfsize = findViewById(R.id.pdfsize);
        docsize = findViewById(R.id.docsize);
        checkPermissions();


        photocard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ImageCompressionActivity.class);
            startActivity(intent);
        });

        documentPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri documentUri = data.getData();
                            if (documentUri != null) {
                                long size = getMediaSize(documentUri);
                                pdfsize.setText(formatSize(size));
                                docsize.setText(formatSize(size));
                                Log.d("FileSize", "Selected document size: " + formatSize(size));
                            }
                        }
                    }
                }
        );

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            executorService = Executors.newSingleThreadExecutor();
            mainThreadHandler = new Handler(Looper.getMainLooper());
            scanStorage();
        }
    }
    @Override
    protected int getLayoutId() {
        return R.layout.activity_main; // Your existing activity layout
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.Home;
    }
    private void scanStorage() {
        Log.d("StorageScan", "Scanning storage...");
        executorService.execute(() -> {
            StorageStats stats = new StorageStats();

            stats.photos = getMediaSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            stats.videos = getMediaSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            stats.pdfs = getFileSize("application/pdf");
            stats.docs = getFileSize("application/msword");

            stats.totalStorage = getTotalStorage();
            stats.availableStorage = getAvailableStorage();
            stats.usedStorage = stats.totalStorage - stats.availableStorage;

            int pro_percent = (int) ((stats.usedStorage * 100) / stats.totalStorage);
            sbar.setProgress(pro_percent);

            Log.d("FileSize", "Photos size: " + formatSize(stats.photos));
            Log.d("FileSize", "Videos size: " + formatSize(stats.videos));
            Log.d("FileSize", "PDF size: " + formatSize(stats.pdfs));
            Log.d("FileSize", "DOC size: " + formatSize(stats.docs));

            mainThreadHandler.post(() -> {
                photosize.setText(formatSize(stats.photos));
                videosize.setText(formatSize(stats.videos));
                pdfsize.setText(formatSize(stats.pdfs));
                docsize.setText(formatSize(stats.docs));
                stext.setText(String.format("  %s  Left out of %s",
                        formatSize(stats.availableStorage), formatSize(stats.totalStorage))
                );
                Log.d("StorageScan", "UI updated with scanned storage data.");
            });
        });
    }

    private long getMediaSize(Uri uri) {
        long size = 0;
        String[] projection = {MediaStore.MediaColumns.SIZE};
        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE);
                if (sizeIndex != -1) {
                    do {
                        size += cursor.getLong(sizeIndex);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            Log.e("FileSize", "Error fetching media size", e);
        }
        Log.d("FileSize", "Media file size: " + size + " bytes");
        return size;
    }

    private long getFileSize(String mimeType) {
        long size = 0;
        Uri contentUri = MediaStore.Files.getContentUri("external");
        String[] projection = {DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_MIME_TYPE};

        try (Cursor cursor = getContentResolver().query(contentUri,
                projection,
                DocumentsContract.Document.COLUMN_MIME_TYPE + " = ?",
                new String[]{mimeType},
                null)) {
            if (cursor != null) {
                int sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE);
                if (sizeIndex != -1) {
                    while (cursor.moveToNext()) {
                        size += cursor.getLong(sizeIndex);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("FileSize", "Error fetching file size for " + mimeType, e);
        }
        Log.d("FileSize", mimeType + " size: " + size + " bytes");
        return size;
    }

    private long getAvailableStorage() {
        File path = getFilesDir();
        StatFs stat = new StatFs(path.getAbsolutePath());
        long availableBlocks = stat.getAvailableBlocksLong();
        long blockSize = stat.getBlockSizeLong();
        Log.d("StorageScan", "Available storage: " + (availableBlocks * blockSize) + " bytes");
        return availableBlocks * blockSize;
    }

    private String formatSize(long sizeInBytes) {
        double sizeInMB = sizeInBytes / (1024.0 * 1024.0);
        if (sizeInMB >= 1024) {
            double sizeInGB = sizeInMB / 1024.0;
            return String.format("%.1f GB", sizeInGB);
        } else {
            return String.format("%.1f MB", sizeInMB);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        Log.d("ActivityLifecycle", "MainActivity destroyed");
    }

    private long getTotalStorage() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        Log.d("StorageScan", "Total storage: " + (totalBlocks * blockSize) + " bytes");
        return totalBlocks * blockSize;
    }

    private static class StorageStats {
        long totalStorage;
        long availableStorage;
        long usedStorage;
        long photos;
        long videos;
        long pdfs;
        long docs;
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
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        STORAGE_PERMISSION_CODE);
            }
        }
    }
}
