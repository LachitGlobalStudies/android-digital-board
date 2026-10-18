package com.adnan.digitalboard.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.adnan.digitalboard.R;

import java.io.File;
import java.util.ArrayList;

public class FileListAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<File> files;

    public FileListAdapter(Context context, ArrayList<File> files) {
        this.context = context;
        this.files = files;
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public Object getItem(int position) {
        return files.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_file, parent, false);
        }

        ImageView icon = convertView.findViewById(R.id.imageViewIcon);
        TextView name = convertView.findViewById(R.id.textViewName);

        File file = files.get(position);
        name.setText(file.getName());

        if (file.isDirectory()) {
            icon.setImageResource(R.drawable.ic_folder);
        } else {
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") || fileName.endsWith(".gif")) {
                icon.setImageResource(R.drawable.ic_image);
            } else if (fileName.endsWith(".pdf")) {
                icon.setImageResource(R.drawable.ic_pdf);
            } else if (fileName.endsWith(".mp4") || fileName.endsWith(".mkv") || fileName.endsWith(".avi")) {
                icon.setImageResource(R.drawable.ic_video);
            } else {
                icon.setImageResource(R.drawable.ic_file);
            }
        }

        return convertView;
    }

}
