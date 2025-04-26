package com.example.letsdoit;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.InputStream;

public class pdftext extends AppCompatActivity {
    private EditText editText;
    private Button selectPdfFileBtn, convertToTextBtn;
    private Uri selectedFileUri;
    private ProgressDialog progressDialog; // Progress Dialog

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pdftext);

        // Initialize PDFBox
        PDFBoxResourceLoader.init(getApplicationContext());

        // Initialize Views
        editText = findViewById(R.id.editText);
        selectPdfFileBtn = findViewById(R.id.selectPdfFileBtn);
        convertToTextBtn = findViewById(R.id.convertToTextBtn);

        // Initially hide EditText and Convert button
        editText.setVisibility(View.GONE);
        convertToTextBtn.setVisibility(View.GONE);

        // File picker setup
        ActivityResultLauncher<Intent> pdfPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedFileUri = result.getData().getData();
                        convertToTextBtn.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "PDF selected! Now click 'Convert PDF to Text'", Toast.LENGTH_SHORT).show();
                    }
                });

        selectPdfFileBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            pdfPickerLauncher.launch(intent);
        });

        convertToTextBtn.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                extractTextFromPdf(selectedFileUri);
            } else {
                Toast.makeText(this, "Please select a PDF first!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void extractTextFromPdf(Uri pdfUri) {
        // Show Progress Dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Extracting text...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(pdfUri);
                PDDocument document = PDDocument.load(inputStream);

                if (document.isEncrypted()) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Cannot extract: PDF is encrypted!", Toast.LENGTH_LONG).show();
                    });
                    document.close();
                    return;
                }

                PDFTextStripper pdfStripper = new PDFTextStripper();
                final String text = pdfStripper.getText(document);
                document.close();

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    editText.setText(text);
                    editText.setVisibility(View.VISIBLE);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to extract text!", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        // Reset the screen state to initial state
        editText.setVisibility(View.GONE);
        convertToTextBtn.setVisibility(View.GONE);
        selectPdfFileBtn.setVisibility(View.VISIBLE); // Show the select button again
        super.onBackPressed();
    }
}
