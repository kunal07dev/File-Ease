package com.example.letsdoit;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
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
    public void selectAllFiles() {
        selectedFiles.clear();
        selectedFiles.addAll(files);
    }
    public void deselectAllFiles() {
        selectedFiles.clear();
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_duplicate_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = files.get(position);
        holder.fileName.setText(file.getName());
        holder.filePath.setText(file.getAbsolutePath());

        // Load thumbnail or icon
        new ThumbnailLoader(holder.thumbnail, file).execute();

        // Set up the checkbox listener
        holder.checkBox.setOnCheckedChangeListener(null); // Clear previous listener
        holder.checkBox.setChecked(selectedFiles.contains(file));
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

    private static class ThumbnailLoader extends AsyncTask<Void, Void, Bitmap> {
        private final ImageView imageView;
        private final File file;

        ThumbnailLoader(ImageView imageView, File file) {
            this.imageView = imageView;
            this.file = file;
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            String fileName = file.getName().toLowerCase();

            try {
                // Handle images
                if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                        fileName.endsWith(".png") || fileName.endsWith(".webp") ||
                        fileName.endsWith(".gif") || fileName.endsWith(".bmp")) {
                    // Decode image thumbnail
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = false;
                    options.inSampleSize = 4; // Reduce size for thumbnail
                    return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                }
                // Handle videos
                else if (fileName.endsWith(".mp4") || fileName.endsWith(".mkv") ||
                        fileName.endsWith(".avi") || fileName.endsWith(".mov")) {
                    return ThumbnailUtils.createVideoThumbnail(
                            file.getAbsolutePath(), MediaStore.Images.Thumbnails.MINI_KIND);
                }
                // Handle PDF
                else if (fileName.endsWith(".pdf")) {
                    return BitmapFactory.decodeResource(imageView.getContext().getResources(),
                            R.drawable.pdf1); // You need to add this drawable
                }
                // Handle Word documents
                else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
                    return BitmapFactory.decodeResource(imageView.getContext().getResources(),
                            R.drawable.doc1); // You need to add this drawable
                }
                // Handle Excel
                else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
                    return BitmapFactory.decodeResource(imageView.getContext().getResources(),
                            R.drawable.excel_file); // You need to add this drawable
                }
                // Handle PowerPoint
                else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
                    return BitmapFactory.decodeResource(imageView.getContext().getResources(),
                            R.drawable.ppt); // You need to add this drawable
                }
                // Handle text files
                else if (fileName.endsWith(".txt")) {
                    return BitmapFactory.decodeResource(imageView.getContext().getResources(),
                            R.drawable.text); // You need to add this drawable
                }
                else {
                    return BitmapFactory.decodeResource(imageView.getContext().getResources(),
                            R.drawable.files);

                }
            } catch (Exception e) {
                Log.e("ThumbnailLoader", "Error loading thumbnail for " + file.getName(), e);
            }

            // Default file icon
            return BitmapFactory.decodeResource(imageView.getContext().getResources(),
                    R.drawable.ic_file); // You need to add this drawable
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }



    private Uri getUriForFile(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", file);
        } else {
            return Uri.fromFile(file);
        }
    }



    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView fileName, filePath;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.image_thumbnail);
            fileName = itemView.findViewById(R.id.text_file_name);
            filePath = itemView.findViewById(R.id.text_file_path);
            checkBox = itemView.findViewById(R.id.checkbox_select_file);
        }
    }


        private void openFile(File file) {
            try {
                Uri fileUri = getUriForFile(file);
                String mimeType = getMimeType(file);

                // Create main intent with specific MIME type
                Intent intent = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(fileUri, mimeType)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                // Create generic intent as fallback
                Intent genericIntent = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(fileUri, "*/*")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                // Try specific MIME type first
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(intent);
                }
                // Then try generic MIME type
                else if (genericIntent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(genericIntent);
                }
                // Finally show chooser if both fail
                else {
                    Intent chooser = Intent.createChooser(genericIntent, "Open with");
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(chooser);
                }
            } catch (Exception e) {
                Log.e("FileOpen", "Error opening file: " + file.getName(), e);
                Toast.makeText(context, "Cannot open file", Toast.LENGTH_SHORT).show();

                // Ultimate fallback - let user pick any app
                Intent fallback = new Intent(Intent.ACTION_GET_CONTENT)
                        .setType("*/*")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallback);
            }
        }

        // Enhanced MIME type detection
        private String getMimeType(File file) {
            String fileName = file.getName().toLowerCase();

            // Images
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
            if (fileName.endsWith(".png")) return "image/png";
            if (fileName.endsWith(".webp")) return "image/webp";
            if (fileName.endsWith(".gif")) return "image/gif";
            if (fileName.endsWith(".bmp")) return "image/bmp";

            // Documents
            if (fileName.endsWith(".pdf")) return "application/pdf";
            if (fileName.endsWith(".txt")) return "text/plain";
            if (fileName.endsWith(".doc")) return "application/msword";
            if (fileName.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            if (fileName.endsWith(".xls")) return "application/vnd.ms-excel";
            if (fileName.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            if (fileName.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
            if (fileName.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";

            // Archives
            if (fileName.endsWith(".zip")) return "application/zip";
            if (fileName.endsWith(".rar")) return "application/x-rar-compressed";

            // Audio
            if (fileName.endsWith(".mp3")) return "audio/mpeg";
            if (fileName.endsWith(".wav")) return "audio/wav";
            if (fileName.endsWith(".ogg")) return "audio/ogg";

            // Video
            if (fileName.endsWith(".mp4")) return "video/mp4";
            if (fileName.endsWith(".mkv")) return "video/x-matroska";
            if (fileName.endsWith(".avi")) return "video/x-msvideo";
            if (fileName.endsWith(".mov")) return "video/quicktime";
            if (fileName.endsWith(".flv")) return "video/x-flv";

            return "*/*";
        }


}