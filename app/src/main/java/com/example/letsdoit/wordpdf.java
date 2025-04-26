package com.example.letsdoit;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import com.aspose.pdf.DocSaveOptions;

import java.io.File;


import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class wordpdf  extends AppCompatActivity {

    private Uri fileUri;
    private Button btnConvert;
    private String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wordpdf);

        Button btnPickFile = findViewById(R.id.selectWordBtn);  // Fix button ID
        btnConvert = findViewById(R.id.convertToPdfBtn);  // Initialize btnConvert

        ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        fileUri = result.getData().getData();
                        fileName = getFileName(fileUri);
                        btnConvert.setVisibility(View.VISIBLE);
                    }
                });

        btnPickFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerLauncher.launch(intent);
        });

        btnConvert.setOnClickListener(v -> {
            if (fileUri != null) {
                String extension = fileName.substring(fileName.lastIndexOf("."));
                if (extension.equalsIgnoreCase(".docx")) {
                    convertWordToPdf(this, fileUri);
                } else if (extension.equalsIgnoreCase(".pdf")) {
                    convertPdfToWord(this, fileUri);
                } else {
                    Toast.makeText(this, "Invalid file type", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        } catch (Exception e) {
            Log.e("FilePicker", "Error getting file name", e);
        }
        return result;
    }

    // ✅ Fix: Correct file path for Android 10+
    private File getOutputFile(String extension) {
        File dir = getExternalFilesDir(null);
        return new File(dir, "ConvertedFile" + extension);
    }

    // ✅ Fix: Word to PDF Conversion
    void convertWordToPdf(Context context, Uri inputUri) {
        try {
            // Read the Word document from URI
            InputStream inputStream = context.getContentResolver().openInputStream(inputUri);
            com.aspose.words.Document doc = new com.aspose.words.Document(inputStream);

            // Define output file location
            Uri outputUri = getOutputFileUri(".pdf");
            OutputStream outputStream = context.getContentResolver().openOutputStream(outputUri);

            // Save as PDF
            doc.save(outputStream, com.aspose.words.SaveFormat.PDF);
            outputStream.close();

            Toast.makeText(context, "PDF saved to: " + outputUri, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Conversion failed!", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ Fix: PDF to Word Conversion
    void convertPdfToWord(Context context, Uri inputUri) {
        try {
            ; // The URI of the selected file
            InputStream inputStream = getContentResolver().openInputStream(inputUri);
            com.aspose.pdf.Document pdfDoc = new com.aspose.pdf.Document(inputStream);

            if (inputStream == null) {
                Log.e("PDFtoWord", "InputStream is null");
                return;
            }


            OutputStream outputStream = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                outputStream = context.getContentResolver()
                        .openOutputStream(MediaStore.Downloads.EXTERNAL_CONTENT_URI);
            }

            if (outputStream == null) {
                Log.e("PDFtoWord", "OutputStream is null");
                return;
            }

            pdfDoc.save(outputStream, com.aspose.pdf.SaveFormat.DocX);
            outputStream.close();
            Log.d("PDFtoWord", "Conversion successful!");
        } catch (Exception e) {
            Log.e("PDFtoWord", "Error converting PDF to Word", e);
        }
    }

    private Uri getOutputFileUri(String extension) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "ConvertedFile" + extension);
        values.put(MediaStore.MediaColumns.MIME_TYPE, extension.equals(".pdf") ? "application/pdf" : "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);

        return getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
    }

}


