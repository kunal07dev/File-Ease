package com.example.letsdoit;

import android.Manifest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class duplicate_main extends AppCompatActivity {
    private static final int REQUEST_PERMISSION = 1;
    private RecyclerView recyclerView;
    private DuplicateFileAdapter adapter;
    private List<File> duplicateFiles = new ArrayList<>();
    private ExecutorService executorService;
    private ProgressBar progressBar;
    private static final String TAG = "DuplicateMain";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duplicate_main);

        // Initialize UI components
        Button scanButton = findViewById(R.id.button_scan);
        Button deleteButton = findViewById(R.id.button_delete_files);
        recyclerView = findViewById(R.id.recycler_view_duplicates);
  progressBar=findViewById(R.id.progressBar);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DuplicateFileAdapter(duplicateFiles,this);
        recyclerView.setAdapter(adapter);

        // Initialize ExecutorService for background tasks
        executorService = Executors.newSingleThreadExecutor();

        Log.d(TAG, "onCreate: Activity created");

        // Handle scan button click
        scanButton.setOnClickListener(v -> {
            Log.d(TAG, "scanButton: Clicked");
            if (hasStoragePermission()) {
                Log.d(TAG, "Permission granted, starting scan");
                scanForDuplicates();
                showProgressBar();
            } else {
                Log.d(TAG, "Permission not granted, requesting permission");
                requestStoragePermission();
            }
        });

        // Handle delete button click
        deleteButton.setOnClickListener(v -> {
            Log.d(TAG, "deleteButton: Clicked");
            deleteSelectedFiles();
        });
    }

    private boolean hasStoragePermission() {
        Log.d(TAG, "hasStoragePermission: Checking permission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
            return Environment.isExternalStorageManager();
        }
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        Log.d(TAG, "requestStoragePermission: Requesting permission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            startActivityForResult(intent, REQUEST_PERMISSION);
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        }
    }

    private void scanForDuplicates() {
        Log.d(TAG, "scanForDuplicates: Scanning for duplicates started");
        // Execute the duplicate scanning task in a background thread
        executorService.submit(() -> {
            File storageDirectory = new File("/storage/emulated/0/");
            Log.d(TAG, "scanForDuplicates: Scanning directory: " + storageDirectory.getAbsolutePath());
            try {
                List<List<File>> duplicates = DuplicateFinder.findDuplicates(storageDirectory);
                Log.d(TAG, "scanForDuplicates: Found " + duplicates.size() + " groups of duplicates");

                List<File> foundDuplicates = new ArrayList<>();
                for (List<File> group : duplicates) {
                    foundDuplicates.addAll(group.subList(1, group.size())); // Add all but one to delete
                    Log.d(TAG, "scanForDuplicates: Found " + (group.size() - 1) + " duplicates in a group");
                }

                // Update the UI after scanning is done
                runOnUiThread(() -> {
                    hideProgressBar();
                    Log.d(TAG, "scanForDuplicates: Updating UI with found duplicates");
                    duplicateFiles.clear();
                    duplicateFiles.addAll(foundDuplicates);
                    adapter.notifyDataSetChanged();
                });
            } catch (IOException | NoSuchAlgorithmException e) {
                Log.e(TAG, "scanForDuplicates: Error scanning for duplicates", e);
                runOnUiThread(() -> Toast.makeText(this, "Error scanning for duplicates", Toast.LENGTH_SHORT).show());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void deleteSelectedFiles() {
        Log.d(TAG, "deleteSelectedFiles: Deleting selected files");
        List<File> selectedFiles = adapter.getSelectedFiles();
        Log.d(TAG, "deleteSelectedFiles: Selected " + selectedFiles.size() + " files for deletion");
        for (File file : selectedFiles) {
            if (file.exists() && file.delete()) {
                Log.d(TAG, "deleteSelectedFiles: Deleted: " + file.getName());
            } else {
                Log.e(TAG, "deleteSelectedFiles: Failed to delete: " + file.getName());
            }
        }
        duplicateFiles.removeAll(selectedFiles);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: Permission result received");
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: Permission granted");
                scanForDuplicates();
            } else {
                Log.d(TAG, "onRequestPermissionsResult: Permission denied");
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Activity destroyed, shutting down executor");
        executorService.shutdown();
    }
    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    // Hide the progress bar
    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }
}
