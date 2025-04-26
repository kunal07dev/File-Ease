package com.example.letsdoit;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class jpgtopng extends AppCompatActivity {

    private MaterialButton selectImageBtn, convertToPngBtn;
    private Uri selectedImageUri;
    private Bitmap selectedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.jpgtopng); // your XML file name

        selectImageBtn = findViewById(R.id.selectImageBtn);
        convertToPngBtn = findViewById(R.id.convertToPngBtn);

        selectImageBtn.setOnClickListener(v -> openImagePicker());
        convertToPngBtn.setOnClickListener(v -> convertImageToPng());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/jpeg");
        imagePickerLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            selectedImageUri = result.getData().getData();
                            if (selectedImageUri != null) {
                                try {
                                    selectedBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImageUri));
                                    convertToPngBtn.setVisibility(View.VISIBLE);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });

    private void convertImageToPng() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "No image selected!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String fileName = "IMG_" + System.currentTimeMillis() + ".png";
            File directory = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES) + "/ConvertedImages");

            if (!directory.exists()) {
                directory.mkdirs();
            }

            File file = new File(directory, fileName);
            OutputStream out = new FileOutputStream(file);
            selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            // Update gallery
            MediaScannerConnection.scanFile(this,
                    new String[]{file.getAbsolutePath()},
                    new String[]{"image/png"},
                    null);


            Toast.makeText(this, "Converted Successfully! Saved in Pictures/ConvertedImages", Toast.LENGTH_LONG).show();
            convertToPngBtn.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Conversion Failed!", Toast.LENGTH_SHORT).show();
        }
    }
}

