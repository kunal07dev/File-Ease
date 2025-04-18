package com.example.letsdoit;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.letsdoit.R;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private final Context context;
    private  final List<Uri> imageUris;
    //private final List<Boolean> selectedImages; // To track selection state for each image

    public ImageAdapter(Context context, List<Uri> imageUris) {
        this.context = context;
        this.imageUris = imageUris;
      //  this.selectedImages = initializeSelectionList(imageUris.size());
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_file, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);

        // Set image thumbnail
        holder.ivImageThumbnail.setImageURI(imageUri);

        // Set file name (using Uri last segment as an example)
        String fileName = imageUri.getLastPathSegment();
        holder.tvFileName.setText(fileName != null ? fileName : "Unknown File");

        // Set checkbox based on selection state
       // holder.cbSelectImage.setChecked(selectedImages.get(position));

        // Set listener for checkbox
//        holder.cbSelectImage.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            selectedImages.set(position, isChecked);
//        });

        // Optionally, set file size (placeholder for now)
        String fileSize = getFileSizeFromUri(imageUri);
        holder.tvFileSize.setText(fileSize != null ? "File Size: " + fileSize : "Unknown size");

    }
    private String getFileSizeFromUri(Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        String fileSize = null;
        if (cursor != null) {
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            if (sizeIndex != -1 && cursor.moveToFirst()) {
                long sizeInBytes = cursor.getLong(sizeIndex);
                fileSize = formatFileSize(sizeInBytes); // Format the size
            }
            cursor.close();
        }
        return fileSize;
    }
    private String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        } else if (sizeInBytes < (1024 * 1024)) {
            return (sizeInBytes / 1024) + " KB";
        } else {
            return (sizeInBytes / (1024 * 1024)) + " MB";
        }
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    // Method to select or deselect all items
//    public void selectAll(boolean isSelected) {
//        for (int i = 0; i < selectedImages.size(); i++) {
//            selectedImages.set(i, isSelected);
//        }
//        notifyDataSetChanged();
//    }

    // Method to get selected image URIs
//    public List<Uri> getSelectedImages() {
//        List<Uri> selected = new java.util.ArrayList<>();
//        for (int i = 0; i < imageUris.size(); i++) {
//            if (selectedImages.get(i)) {
//                selected.add(imageUris.get(i));
//            }
//        }
//        return selected;
//    }

    // Initialize the selection list
    private List<Boolean> initializeSelectionList(int size) {
        List<Boolean> selectionList = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            selectionList.add(false); // Default: not selected
        }
        return selectionList;
    }

    // ViewHolder class
    static class ImageViewHolder extends RecyclerView.ViewHolder {

        ImageView ivImageThumbnail;
        TextView tvFileName, tvFileSize;
     //   CheckBox cbSelectImage;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImageThumbnail = itemView.findViewById(R.id.ivImageThumbnail);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            //cbSelectImage = itemView.findViewById(R.id.cbSelectImage);
        }
    }
    public  void clearData() {
        imageUris.clear();  // Clear the list of images
      //  selectedImages.clear(); // Clear the selection list (if you are using it)
        notifyDataSetChanged(); // Notify the adapter that the data has been changed
    }


}
