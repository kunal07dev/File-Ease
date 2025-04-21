package com.example.letsdoit;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
public class Imagepdf extends AppCompatActivity {
    private List<Uri> selectedImageUris = new ArrayList<>();
    private Uri selectedPdfUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.imagepdf_main);

        PDFBoxResourceLoader.init(getApplicationContext());

        Button selectImagesBtn = findViewById(R.id.selectImageBtn);
        Button convertToPdfBtn = findViewById(R.id.convertToPdfBtn);
        //Button selectPdfBtn = findViewById(R.id.selectPdfBtn);
        // Button convertToImagesBtn = findViewById(R.id.convertToImagesBtn);

        ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                selectedImageUris.add(result.getData().getClipData().getItemAt(i).getUri());
                            }
                        } else if (result.getData().getData() != null) {
                            selectedImageUris.add(result.getData().getData());
                        }
                        convertToPdfBtn.setVisibility(View.VISIBLE);
                    }
                });

        selectImagesBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            imagePickerLauncher.launch(intent);
        });

        convertToPdfBtn.setOnClickListener(v -> {
            if (!selectedImageUris.isEmpty()) {
                convertImagesToPdf();
            }
        });

        ActivityResultLauncher<Intent> pdfPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedPdfUri = result.getData().getData();
                        //convertToImagesBtn.setVisibility(View.VISIBLE);
                    }
                });

//        selectPdfBtn.setOnClickListener(v -> {
//            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//            intent.setType("application/pdf");
//            pdfPickerLauncher.launch(intent);
//        });

//        convertToImagesBtn.setOnClickListener(v -> {
//            if (selectedPdfUri != null) {
//                extractImagesFromPdf(selectedPdfUri);
//            }
//        });
    }

    private void convertImagesToPdf() {
        try {
            PDDocument document = new PDDocument();
            for (Uri imageUri : selectedImageUris) {
                Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                if (bitmap == null) continue;

                PDPage page = new PDPage();
                document.addPage(page);

                // Resize image while maintaining aspect ratio
                Bitmap resizedBitmap = resizeBitmap(bitmap, (int) page.getMediaBox().getWidth(), (int) page.getMediaBox().getHeight());
                File tempFile = convertBitmapToJpeg(resizedBitmap);
                PDImageXObject pdImage = PDImageXObject.createFromFile(tempFile.getAbsolutePath(), document);

                PDPageContentStream contentStream = new PDPageContentStream(document, page);
                float x = (page.getMediaBox().getWidth() - pdImage.getWidth()) / 2;
                float y = (page.getMediaBox().getHeight() - pdImage.getHeight()) / 2;
                contentStream.drawImage(pdImage, x, y, pdImage.getWidth(), pdImage.getHeight());
                contentStream.close();
            }

            File pdfFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "combined_images.pdf");
            MediaScannerConnection.scanFile(
                    this,
                    new String[]{pdfFile.getAbsolutePath()},
                    new String[]{"application/pdf"},
                    null
            );

            document.save(pdfFile);
            document.close();
            Toast.makeText(this, "PDF saved in Downloads: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private Bitmap resizeBitmap(Bitmap bitmap, int width, int height) {
        float aspectRatio = bitmap.getWidth() / (float) bitmap.getHeight();
        int newWidth = width;
        int newHeight = (int) (width / aspectRatio);

        if (newHeight > height) {
            newHeight = height;
            newWidth = (int) (height * aspectRatio);
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
    private void saveImageToGallery(Bitmap bitmap, String filename) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        try {
            OutputStream out = getContentResolver().openOutputStream(uri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
            Toast.makeText(this, "Image saved: " + filename, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving image: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private File convertBitmapToJpeg(Bitmap bitmap) throws Exception {
        File file = new File(getCacheDir(), "temp.jpg");
        FileOutputStream fos = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.close();
        return file;
    }

}
