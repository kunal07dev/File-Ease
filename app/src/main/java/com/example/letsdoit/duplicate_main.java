package com.example.letsdoit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class duplicate_main extends BaseActivity {
    private static final String TAG = "DuplicateMain";
    private static final int REQUEST_PERMISSION = 1;

    private RecyclerView recyclerView;
    private DuplicateFileAdapter adapter;
    private List<File> duplicateFiles;
    private ExecutorService executorService;
    private ProgressBar progressBar;
    private CheckBox selectAllCheckBox;
    private MaterialCardView cardResults;
    private Button deleteButton;
    private Button scanButton;
    private TextView tvSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_duplicate_main);

        // Initialize views
        scanButton = findViewById(R.id.button_scan);
        deleteButton = findViewById(R.id.button_delete_files);
        recyclerView = findViewById(R.id.recycler_view_duplicates);
        progressBar = findViewById(R.id.progressBar);
        selectAllCheckBox = findViewById(R.id.checkbox_select_all);
        cardResults = findViewById(R.id.cardResults);
        tvSubtitle = findViewById(R.id.tvSubtitle);

        // Initialize list and adapter
        duplicateFiles = new ArrayList<>();
        adapter = new DuplicateFileAdapter(duplicateFiles, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Initialize executor service
        executorService = Executors.newSingleThreadExecutor();

        // Set up click listeners
        scanButton.setOnClickListener(v -> {
            if (hasStoragePermission()) {
                startScanning();
            } else {
                requestStoragePermission();
            }
        });

        deleteButton.setOnClickListener(v -> deleteSelectedFiles());

        selectAllCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                adapter.selectAllFiles();
            } else {
                adapter.deselectAllFiles();
            }
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_duplicate_main;
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.Dupli;
    }
    private void startScanning() {
        showProgressBar();
        tvSubtitle.setText("Scanning for duplicate files...");
        scanForDuplicates();
    }
    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startActivityForResult(
                    new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION),
                    REQUEST_PERMISSION
            );
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void scanForDuplicates() {
        executorService.submit(() -> {
            File storageDirectory = new File("/storage/emulated/0/");
            try {
                List<List<File>> groups = findDuplicates(storageDirectory);
                List<File> found = new ArrayList<>();

                for (List<File> group : groups) {
                    if (group.size() > 1) {
                        found.addAll(group.subList(1, group.size()));
                    }
                }

                runOnUiThread(() -> {
                    duplicateFiles.clear();
                    duplicateFiles.addAll(found);
                    adapter.notifyDataSetChanged();
                    updateUIAfterScan(found.size());
                });

            } catch (Exception e) {
                Log.e(TAG, "Error scanning for duplicates", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error scanning for duplicates", Toast.LENGTH_SHORT).show();
                    hideProgressBar();
                    tvSubtitle.setText("Scan failed. Please try again.");
                });
            }
        });
    }
    private void updateUIAfterScan(int duplicateCount) {
        hideProgressBar();
        selectAllCheckBox.setVisibility(View.VISIBLE);
        deleteButton.setVisibility(View.VISIBLE);
        cardResults.setVisibility(View.VISIBLE);

        if (duplicateCount > 0) {
            tvSubtitle.setText(String.format("Found %d duplicate files", duplicateCount));
            deleteButton.setText(String.format("Delete Selected (%d)", adapter.getSelectedFiles().size()));
        } else {
            tvSubtitle.setText("No duplicate files found");
            cardResults.setVisibility(View.GONE);
        }
    }
    private void deleteSelectedFiles() {
        List<File> selected = adapter.getSelectedFiles();
        if (selected.isEmpty()) {
            Toast.makeText(this, "No files selected", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            int deletedCount = 0;
            for (File f : selected) {
                if (f.exists() && f.delete()) {
                    deletedCount++;
                    Log.d(TAG, "Deleted: " + f.getName());
                } else {
                    Log.e(TAG, "Failed to delete: " + f.getName());
                }
            }

            runOnUiThread(() -> {
                duplicateFiles.removeAll(selected);
                adapter.notifyDataSetChanged();


                if (duplicateFiles.isEmpty()) {
                    cardResults.setVisibility(View.GONE);
                    tvSubtitle.setText("No duplicate files remaining");
                }
            });
        }).start();
    }


    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        scanButton.setEnabled(false);
        cardResults.setVisibility(View.GONE);
        deleteButton.setVisibility(View.GONE);
        selectAllCheckBox.setVisibility(View.GONE);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
        scanButton.setEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    /**
     * Recursively scans `directory` for duplicate files by MD5 hash,
     * using a thread pool to parallelize file-hashing.
     */
    public static List<List<File>> findDuplicates(File directory)
            throws InterruptedException, IOException,
            NoSuchAlgorithmException, ExecutionException {

        Map<String, List<File>> hashAndExtToFileList = new ConcurrentHashMap<>();

        // 1) Use a small fixed pool
        ExecutorService pool = Executors.newFixedThreadPool(4);

        // 2) Gather all files first
        List<File> allFiles = listFilesRecursively(directory);
        List<Future<Void>> tasks = new ArrayList<>();

        // 3) Batch submissions (100 files at a time)
        for (int i = 0; i < allFiles.size(); i += 100) {
            int end = Math.min(i + 100, allFiles.size());
            List<File> batch = allFiles.subList(i, end);

            for (File f : batch) {
                tasks.add(pool.submit(() -> {
                    String hash = getFileHash(f);
                    String ext = getFileExtension(f.getName()).toLowerCase();
                    String key = hash + ":" + ext; // Combine hash and extension
                    synchronized (hashAndExtToFileList) {
                        hashAndExtToFileList
                                .computeIfAbsent(key, k -> new ArrayList<>())
                                .add(f);
                    }
                    return null;
                }));
            }

            // give the OS a breather
            Thread.sleep(50);
        }

        // 4) Wait for all hashing to finish
        for (Future<Void> t : tasks) t.get();
        pool.shutdown();

        // 5) Collect only duplicates
        List<List<File>> duplicates = new ArrayList<>();
        for (List<File> group :hashAndExtToFileList.values()) {
            if (group.size() > 1) duplicates.add(group);
        }
        return duplicates;
    }
    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }
    /** Compute MD5 hash of a file */
    private static String getFileHash(File file)
            throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = fis.read(buf)) != -1) {
                md.update(buf, 0, len);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    /**
     * Recursively collects all files under `dir`.
     */
    private static List<File> listFilesRecursively(File dir) {
        List<File> fileList = new ArrayList<>();
        File[] entries = dir.listFiles();
        if (entries == null) return fileList;
        for (File f : entries) {
            if (f.isDirectory()) {
                fileList.addAll(listFilesRecursively(f));
            } else {
                fileList.add(f);
            }
        }
        return fileList;
    }


}
