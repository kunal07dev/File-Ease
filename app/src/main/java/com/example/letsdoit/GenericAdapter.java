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
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                fileName.endsWith(".png") || fileName.endsWith(".gif")) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            Bitmap thumb = ThumbnailUtils.extractThumbnail(bitmap, 150, 150);
            imageView.setImageBitmap(thumb);
        } else if (fileName.endsWith(".mp4") || fileName.endsWith(".mkv") ||
                fileName.endsWith(".avi") || fileName.endsWith(".mov")) {
            Bitmap thumb = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Images.Thumbnails.MINI_KIND);
            imageView.setImageBitmap(thumb);
        }else if (fileName.endsWith(".pdf")) {
            imageView.setImageResource(R.drawable.pdf1);
        } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
            imageView.setImageResource(R.drawable.doc1);
        }

        return gridItem;
    }
}
