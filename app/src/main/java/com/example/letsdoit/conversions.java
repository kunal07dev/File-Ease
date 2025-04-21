package com.example.letsdoit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

public class conversions extends BaseActivity {

    private LinearLayout pdfToWord;
    private LinearLayout imageToPdf;
    private LinearLayout pdfToImage;
    private LinearLayout pdfToText;
    private LinearLayout jpgToPng;
    private LinearLayout pngToJpg;

    @Override
    protected int getLayoutId() {
        // your layout XML should be named activity_conversions.xml or conversion.xml
        return R.layout.conversion;
    }

    @Override
    protected int getNavigationMenuItemId() {
        // the ID of the menu item this screen corresponds to
        return R.id.Conversion;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Now that BaseActivity has inflated 'conversion.xml',
        // we can safely findViewById all our conversion options:
        pdfToWord    = findViewById(R.id.pdftoword);
        imageToPdf   = findViewById(R.id.imagetopdf);
        pdfToImage   = findViewById(R.id.pdftoimage);
        pdfToText    = findViewById(R.id.pdftotext);
        jpgToPng     = findViewById(R.id.jpgtopng);
        pngToJpg     = findViewById(R.id.pngtojpg);

        // Set up the click listener for the “Image → PDF” option:
        imageToPdf.setOnClickListener(v -> {
            Intent intent = new Intent(conversions.this, Imagepdf.class);
            startActivity(intent);
        });
        pdfToImage.setOnClickListener(v -> {
            Intent intent = new Intent(conversions.this,pdftoimage.class);
            startActivity(intent);
        });

        // TODO: hook up your other conversions similarly:
        // pdfToWord.setOnClickListener(…);
        // pdfToImage.setOnClickListener(…);
        // etc.
    }
}
