package com.example.letsdoit;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class TextAdapter extends BaseAdapter {
    private Context context;
    private List<String> items;

    public TextAdapter(Context context, List<String> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public String getItem(int position) {
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
            gridItem = LayoutInflater.from(context).inflate(R.layout.item_grid_text, parent, false);
        }
        TextView textView = gridItem.findViewById(R.id.textView);
        textView.setText(getItem(position));
        return gridItem;
    }
}
