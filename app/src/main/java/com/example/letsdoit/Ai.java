package com.example.letsdoit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.annotation.SuppressLint;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.Call;

public class Ai extends BaseActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.ai_main;
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.Aifile;
    }
    private static final int PICK_FILE_REQUEST = 1;
    private static final String GEMINI_API_KEY = "AIzaSyAoGh4BIgJ_PS0qskDFdRAfuEnqk63ziNs";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;

    // UI Components
    private LinearLayout  contentLayout, bottomSearchBar;
    private EditText  queryInputBottom;
    private ImageButton  uploadButtonBottom, sendButtonBottom;
    private ProgressBar progressBar;
    private TextView summaryText;

    // State
    private String currentExtractedText = "";
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PDFBoxResourceLoader.init(getApplicationContext());

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {

        contentLayout = findViewById(R.id.contentLayout);
        bottomSearchBar = findViewById(R.id.bottomSearchBar);

        queryInputBottom = findViewById(R.id.queryInputBottom);

        uploadButtonBottom = findViewById(R.id.uploadButtonBottom);
        sendButtonBottom = findViewById(R.id.sendButtonBottom);
        progressBar = findViewById(R.id.progressBar);
        summaryText = findViewById(R.id.summaryText);
    }

    private void setupClickListeners() {
        // Center bar buttons


        // Bottom bar buttons
        uploadButtonBottom.setOnClickListener(v -> handleUploadClick(queryInputBottom));
        sendButtonBottom.setOnClickListener(v -> {
            summaryText.setGravity(Gravity.TOP | Gravity.START);
            handleSendAction(queryInputBottom.getText().toString().trim());
        });

    }

    private void handleUploadClick(EditText inputField) {
        String query = inputField.getText().toString().trim();
        if (query.isEmpty()) {
            showToast("Please enter a query first");
            return;
        }
        openFilePicker();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        String[] mimeTypes = {
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            handleFileSelection(fileUri);
        }
    }

    private void handleFileSelection(Uri uri) {
        showProcessingState();
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                currentExtractedText = extractTextFromFile(inputStream, getContentResolver().getType(uri));
                runOnUiThread(() -> {
                    if (currentExtractedText.isEmpty()) {
                        showErrorState("Failed to extract text from file");
                        return;
                    }
                    // Show content and set initial bottom query
                    showContentState();

                });
            } catch (Exception e) {
                runOnUiThread(() -> showErrorState("Error processing file: " + e.getMessage()));
            }
        }).start();
    }
    private void showContentState() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            contentLayout.setVisibility(View.VISIBLE);


            summaryText.setText("Document ready - enter your query and click send");

        });
    }

    private String extractTextFromFile(InputStream inputStream, String mimeType) throws IOException {
        if (mimeType.equals("application/pdf")) {
            PDDocument document = PDDocument.load(inputStream);
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);

        } else if (mimeType.equals("application/msword")) { // .doc
            HWPFDocument doc = new HWPFDocument(inputStream);
            WordExtractor extractor = new WordExtractor(doc);
            return extractor.getText();

            // DOCX (Word) Extraction
        } else if (mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) { // .docx
            XWPFDocument docx = new XWPFDocument(inputStream);
            XWPFWordExtractor extractor = new XWPFWordExtractor(docx);
            return extractor.getText();

            // PPT (PowerPoint) Extraction (Manual extraction)
        } else if (mimeType.equals("application/vnd.ms-powerpoint")) { // .ppt
            HSLFSlideShow ppt = new HSLFSlideShow(inputStream);
            StringBuilder pptText = new StringBuilder();
            for (HSLFSlide slide : ppt.getSlides()) {
                pptText.append(slide.getTitle()).append("\n");
            }
            return pptText.toString();



            // PPTX (PowerPoint) Extraction (Manual extraction)
        } else if (mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")) { // .pptx
            XMLSlideShow pptx = new XMLSlideShow(inputStream);
            StringBuilder pptxText = new StringBuilder();
            for (XSLFSlide slide : pptx.getSlides()) {
                pptxText.append(slide.getTitle()).append("\n");
            }
            return pptxText.toString();



            // XLS (Excel) Extraction
        } else if (mimeType.equals("application/vnd.ms-excel")) { // .xls
            HSSFWorkbook workbook = new HSSFWorkbook(inputStream);
            return extractTextFromWorkbook(workbook);


            // XLSX (Excel) Extraction
        } else if (mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { // .xlsx
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            return extractTextFromWorkbook(workbook);

        }else{
            return "";}
    }

    private void handleSendAction(String query) {
        if (currentExtractedText.isEmpty()) {
            showToast("Please select a document first");
            return;
        }
        if (query.isEmpty()) {
            showToast("Please enter a query");
            return;
        }

        // Update bottom query with current input if coming from center
        if (bottomSearchBar.getVisibility() != View.VISIBLE) {
            queryInputBottom.setText(query);
        }

        callGeminiAPI(query, currentExtractedText);
    }

    private void callGeminiAPI(String query, String context) {
        showProcessingState();
        try {
            JSONObject requestBody = new JSONObject()
                    .put("contents", new JSONArray()
                            .put(new JSONObject()
                                    .put("role", "user")
                                    .put("parts", new JSONArray()
                                            .put(new JSONObject()
                                                    .put("text", query + "\n\nContext:\n" + context)
                                            )
                                    )
                            )
                    );

            RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(GEMINI_API_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {



                @Override


                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> showErrorState("API Error: " + e.getMessage()));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> showErrorState("API Error: " + response.code()));
                        return;
                    }

                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String result = jsonResponse.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        runOnUiThread(() -> showResultState(result));
                    } catch (Exception e) {
                        runOnUiThread(() -> showErrorState("Response parsing error"));
                    }
                }
            });
        } catch (Exception e) {
            showErrorState("Request creation failed");
        }
    }

    // UI State Management
    private void showProcessingState() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            summaryText.setText("Processing your request...");
        });
    }

    private void showResultState(String result) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            summaryText.setText(result);
            bottomSearchBar.setVisibility(View.VISIBLE);
            contentLayout.setVisibility(View.VISIBLE);

        });
    }

    private void showErrorState(String message) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            summaryText.setText(message);
            bottomSearchBar.setVisibility(View.VISIBLE);
        });
    }



    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
    private String extractTextFromWorkbook(Workbook workbook) {
        StringBuilder text = new StringBuilder();
        for (Sheet sheet : workbook) {
            for (Row row : sheet) {
                for (Cell cell : row) {
                    switch (cell.getCellType()) {
                        case STRING:
                            text.append(cell.getStringCellValue()).append(" ");
                            break;
                        case NUMERIC:
                            text.append(cell.getNumericCellValue()).append(" ");
                            break;
                    }
                }
            }
        }
        return text.toString();
    }

}