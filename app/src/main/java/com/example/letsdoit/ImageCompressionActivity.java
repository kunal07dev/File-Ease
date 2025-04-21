package com.example.letsdoit;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageCompressionActivity extends AppCompatActivity {

    private static final String TAG = "ImageCompressionActivity";
    private RecyclerView recyclerView;
    private ImageAdapter adapter;
    private TextView title;
    private CheckBox cbSelectAll;
    private RadioGroup rgCompressionQuality, rgDeleteOption;
    private EditText etWidth, etHeight;
    private TextView btnCompressImages;
    private List<Uri> imageUris;

    // ActivityResultLauncher for image selection
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.getClipData() != null) { // Multiple images selected
                        int count = data.getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri imageUri = data.getClipData().getItemAt(i).getUri();
                            imageUris.add(imageUri);
                            Log.d(TAG, "Selected image URI: " + imageUri.toString());
                        }
                    } else if (data.getData() != null) { // Single image selected
                        Uri imageUri = data.getData();
                        imageUris.add(imageUri);
                        Log.d(TAG, "Selected image URI: " + imageUri.toString());
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Log.d(TAG, "Image selection failed or no data returned.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_compression);
        initializeUI(); // Initialize UI components
        title.setOnClickListener(v -> sel());
    }

    private void sel() {
        selectImages(); // Trigger image selection

        adapter.notifyDataSetChanged();

        // Set button actions
        btnCompressImages.setOnClickListener(v -> startCompression());
       // btnDecompressImages.setOnClickListener(v -> decompressImages());
    }

    private void initializeUI() {
      //  recyclerView = findViewById(R.id.rvImageFiles);

        rgCompressionQuality = findViewById(R.id.rgCompressionQuality);
        rgDeleteOption = findViewById(R.id.deleteOptionGroup);
        etWidth = findViewById(R.id.widthInput);
        etHeight = findViewById(R.id.heightInput);
        btnCompressImages = findViewById(R.id.compressButton);
        title = findViewById(R.id.browseButton);

        imageUris = new ArrayList<>();
        adapter = new ImageAdapter(this, imageUris);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
////        if (!imageUris.isEmpty()) {
//          recyclerView.setAdapter(adapter);
////        }
}

    // Method to open the file picker for images
    @SuppressLint("NotifyDataSetChanged")
    private void selectImages() {
        Log.d(TAG, "Opening image picker...");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Images"));


        adapter.notifyDataSetChanged(); // Notify the adapter to update the RecyclerView
//        recyclerView.setVisibility(View.VISIBLE);

    }

    // Method to compress images based on user input
    private void startCompression() {
        Log.d(TAG, "Starting compression...");
        if (imageUris.isEmpty()) {
            Log.w(TAG, "No images selected.");
            Toast.makeText(this, "No images selected.", Toast.LENGTH_SHORT).show();

            return;
        }

        int selectedQuality = getSelectedQuality();

        boolean deleteOriginal = rgDeleteOption.getCheckedRadioButtonId() == R.id.deleteYes;


        for (Uri uri : imageUris) {
            int width = getInputDimension(etWidth);
            int height = getInputDimension(etHeight);
            String originalFilePath = FileUtils.getPath(this, uri);  // Use the FileUtils class to get the path
            File originalFile = new File(originalFilePath);



            Log.d(TAG, "Original file path: " + originalFilePath);
            Log.d(TAG, "Compression Settings - Quality: " + selectedQuality + ", Width: " + width + ", Height: " + height + ", Delete Original: " + deleteOriginal);


            String compressedFilePath = originalFile.getParent() + "/compressed_" + originalFile.getName();
            File compressedFile = new File(compressedFilePath);




            Log.d(TAG, "Compressed file will be saved at: " + compressedFilePath);
            compressImage(uri, compressedFilePath, selectedQuality, width, height);

            if (deleteOriginal) {
                replaceOriginalFile(originalFile, compressedFile);
            }
        }

        Toast.makeText(this, "Compression complete!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Compression completed successfully.");
//        recyclerView.setVisibility(View.GONE);

        adapter.clearData();  // Clear the RecyclerView's data
        adapter.notifyDataSetChanged();
    }

    // Get the selected compression quality from the RadioGroup
    private int getSelectedQuality() {
        int selectedId = rgCompressionQuality.getCheckedRadioButtonId();
        if (selectedId == R.id.lowQuality) {
            Log.d(TAG, "Low compression quality selected.");
            return 30;
        } else if (selectedId == R.id.moderateQuality) {
            Log.d(TAG, "Moderate compression quality selected.");
            return 50;
        } else if (selectedId == R.id.highQuality) {
            Log.d(TAG, "High compression quality selected.");
            return 75;
        }
        Log.d(TAG, "Default high compression quality selected.");
        return 75; // Default to high quality
    }

    // Parse input dimensions from EditText
    private int getInputDimension(EditText editText) {
        String input = editText.getText().toString().trim();
        if (input.isEmpty()) {
            Log.d(TAG, "No input provided for dimensions, defaulting to 0.");
            return 0; // Return 0 if no input
        }
        int dimension = Integer.parseInt(input);
        Log.d(TAG, "Input dimension: " + dimension);
        return dimension;
    }

    // Method to compress an image
    private void compressImage(Uri imageUri, String outputFilePath, int quality, int width, int height) {
        Log.d(TAG, "Compressing image at URI: " + imageUri.toString());
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri), null, options);

            int scale = 1;
            if (width > 0 && height > 0){
            if (options.outHeight > height || options.outWidth > width) {
                scale = Math.min(options.outHeight / height, options.outWidth / width);
                Log.d(TAG, "Scaling factor: " + scale);
            }}

            options.inJustDecodeBounds = false;
            options.inSampleSize = scale;

            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri), null, options);

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode image.");
                return;
            }

            if (width > 0 && height > 0) {
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                Log.d(TAG, "Image resized to width: " + width + ", height: " + height);
            }

            FileOutputStream out = new FileOutputStream(outputFilePath);
            bitmap.compress(Bitmap.CompressFormat.WEBP, quality, out);
            out.close();
            Log.d(TAG, "Image compression successful at: " + outputFilePath);

            // Recycle the bitmap after use
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error during image compression: " + e.getMessage(), e);
        }
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbHelper.insertMetadata(outputFilePath,width,height,quality);
    }

    // Replace original file with compressed file
    private void replaceOriginalFile(File originalFile, File compressedFile) {
        try {
            long originalTimestamp = originalFile.lastModified(); // Get original timestamp

            if (originalFile.delete()) { // Delete original file
                if (compressedFile.renameTo(originalFile)) { // Rename compressed file to original file name
                    originalFile.setLastModified(originalTimestamp); // Restore original timestamp
                    updateMediaStore(originalFile);
                    //Toast.makeText(this, "Original file replaced successfully", Toast.LENGTH_SHORT).show();



                } else {
                    Log.e(TAG,"Failed to rename compressed file");
                }
            } else {
                Log.e(TAG,"Failed to delete original file");
                compressedFile.delete();
            }
        } catch (SecurityException e) {

            Log.e(TAG, "File replacement error", e);
            compressedFile.delete();
        }
    }
    private void updateMediaStore(File file) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, file.getName());
        values.put(MediaStore.Images.Media.DISPLAY_NAME, file.getName());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
        values.put(MediaStore.Images.Media.SIZE, file.length());

        ContentResolver resolver = getContentResolver();
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Log.d(TAG, "MediaStore updated: " + file.getAbsolutePath());
    }
    }