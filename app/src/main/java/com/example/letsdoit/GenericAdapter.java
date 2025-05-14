package com.example.letsdoit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;
import java.util.List;

public class GenericAdapter extends BaseAdapter {
    private Context context;
    private List<File> items;

    public GenericAdapter(Context context, List<File> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public File getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View gridItem = convertView;
        if (gridItem == null) {
            gridItem = LayoutInflater.from(context).inflate(R.layout.item_grid, parent, false);
        }
        ImageView imageView = gridItem.findViewById(R.id.imageView);
        TextView textView = gridItem.findViewById(R.id.textView);

        File file = getItem(position);
        textView.setText(file.getName());

        String fileName = file.getName().toLowerCase();
        Bitmap thumb = null;

        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                fileName.endsWith(".png") || fileName.endsWith(".gif")) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                thumb = ThumbnailUtils.extractThumbnail(bitmap, 150, 150);
            }
        } else if (fileName.endsWith(".mp4") || fileName.endsWith(".mkv") ||
                fileName.endsWith(".avi") || fileName.endsWith(".mov")) {
            thumb = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND);
        }

        if (thumb != null) {
            imageView.setImageBitmap(thumb);
        } else {
            // Fallback icon
            int iconRes = R.drawable.ic_file; // default
            if (fileName.endsWith(".pdf")) {
                iconRes = R.drawable.pdf1;
            } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
                iconRes = R.drawable.doc1;
            } else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
                iconRes = R.drawable.ppt;
            } else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
                iconRes = R.drawable.excel_file;
            } else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav")) {
                iconRes = R.drawable.video;
            } else if (fileName.endsWith(".zip") || fileName.endsWith(".rar") || fileName.endsWith(".apk")) {
                iconRes = R.drawable.files;
            }
            imageView.setImageResource(iconRes);
        }

        return gridItem;
    }
}
