package com.example.letsdoit;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DuplicateFileAdapter extends RecyclerView.Adapter<DuplicateFileAdapter.ViewHolder> {

    private final List<File> files;
    private final List<File> selectedFiles;
    private final Context context;

    public DuplicateFileAdapter(List<File> files, Context context) {
        this.files = files;
        this.context = context;
        this.selectedFiles = new ArrayList<>();
    }

    public List<File> getSelectedFiles() {
        return selectedFiles;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_duplicate_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = files.get(position);
        holder.fileName.setText(file.getName());
        holder.filePath.setText(file.getAbsolutePath());

        // Set up the checkbox listener
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedFiles.add(file);
            } else {
                selectedFiles.remove(file);
            }
        });

        // Set up the click listener to open the file
        holder.itemView.setOnClickListener(v -> openFile(file));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    // Open the file with the appropriate app based on its MIME type
    private void openFile(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri fileUri = getUriForFile(file);
        String mimeType = getMimeType(file);

        intent.setDataAndType(fileUri, mimeType);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);  // Grant permission for reading the file

        // Check if there is an app that can handle this file type
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "No application found to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    // Get URI for file using FileProvider
    private Uri getUriForFile(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        } else {
            return Uri.fromFile(file); // For older versions of Android
        }
    }

    // Get the MIME type based on file extension
    private String getMimeType(File file) {
        String mimeType = "*/*"; // Default MIME type for unknown files

        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            mimeType = "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            mimeType = "image/png";
        } else if (fileName.endsWith(".pdf")) {
            mimeType = "application/pdf";
        } else if (fileName.endsWith(".txt")) {
            mimeType = "text/plain";
        } else if (fileName.endsWith(".mp4")) {
            mimeType = "video/mp4";
        } else if (fileName.endsWith(".docx")) {
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (fileName.endsWith(".xlsx")) {
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (fileName.endsWith(".pptx")) {
            mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        }

        Log.d("MimeType", "File: " + file.getName() + ", MIME Type: " + mimeType);
        return mimeType;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView fileName, filePath;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.text_file_name);
            filePath = itemView.findViewById(R.id.text_file_path);
            checkBox = itemView.findViewById(R.id.checkbox_select_file);
        }
    }
}
