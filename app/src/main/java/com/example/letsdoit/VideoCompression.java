package com.example.letsdoit;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class VideoCompression extends AppCompatActivity {
    private Button btnSelectVideo, btnCompress, btnStopCompression;
    private CheckBox cbReplaceOriginal;
    private ProgressBar progressBar;
    public LinearLayout liner;
//    private TextView tvStatus, tvSelectedCount;
//    private RecyclerView recyclerView;
//    private VideoAdapter videoAdapter;

    private final List<Uri> selectedUris = new ArrayList<>();

    private final ActivityResultLauncher<Intent> videoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedUris.clear();
                    Intent data = result.getData();

                    if (data.getData() != null) {
                        selectedUris.add(data.getData());
                    } else {
                        ClipData clipData = data.getClipData();
                        if (clipData != null) {
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                selectedUris.add(clipData.getItemAt(i).getUri());
                            }
                        }
                    }
                    updateUIAfterSelection();
                    Toast.makeText(this, "Video Selected", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_main);

        btnSelectVideo = findViewById(R.id.btnSelectVideo);
        btnCompress = findViewById(R.id.btnCompress);
        btnStopCompression = findViewById(R.id.btnStopCompression);
        cbReplaceOriginal = findViewById(R.id.cbReplaceOriginal);
        progressBar = findViewById(R.id.progressBar);
        liner=findViewById(R.id.layout);
//        tvStatus = findViewById(R.id.tvStatus);
//        tvSelectedCount = findViewById(R.id.tv_selected_count);
//        recyclerView = findViewById(R.id.rv_selected_videos);


        btnSelectVideo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            videoPickerLauncher.launch(Intent.createChooser(intent, "Select Videos"));
        });

        btnCompress.setOnClickListener(v -> {
            if (!selectedUris.isEmpty()) {
                progressBar.setVisibility(View.VISIBLE);
                liner.setVisibility(View.VISIBLE);

//              tvStatus.setText("Compressing...");
                VideoCompressionService.startCompression(
                        this,
                        selectedUris,
                        cbReplaceOriginal.isChecked()

                );
            } else {
                Toast.makeText(this, "No video selected!", Toast.LENGTH_SHORT).show();
            }

        });

        btnStopCompression.setOnClickListener(v -> {
            VideoCompressionService.stopCompression(this);
//            tvStatus.setText("Stopped");
            progressBar.setVisibility(View.GONE);
            liner.setVisibility(View.GONE);
        });
    }

    private void updateUIAfterSelection() {
        if (!selectedUris.isEmpty()) {

            btnSelectVideo.setVisibility(View.VISIBLE);

//            tvSelectedCount.setText(selectedUris.size() + " selected");
//            videoAdapter = new VideoAdapter(selectedUris, this);
//            recyclerView.setAdapter(videoAdapter);
        } else {
            btnCompress.setVisibility(View.GONE);
            liner.setVisibility(View.GONE);
//            tvSelectedCount.setText("0 selected");
        }
    }
}


