package com.adnan.digitalboard.adapters;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.adnan.digitalboard.R;

import java.io.File;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    public interface OnFileClickListener {
        void onFileClick(File file);
    }

    private final List<File> files;
    private final OnFileClickListener listener;

    public FileAdapter(List<File> files, OnFileClickListener listener) {
        this.files = files;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

   @Override
public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
    final File file = files.get(position);
    holder.tvFileName.setText(file.getName());

    if (file.isDirectory()) {
        holder.ivIcon.setImageResource(R.drawable.ic_folder);
    } else {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") || fileName.endsWith(".gif")) {
            holder.ivIcon.setImageResource(R.drawable.ic_image);
        } else if (fileName.endsWith(".pdf")) {
            holder.ivIcon.setImageResource(R.drawable.ic_pdf);
        } else if (fileName.endsWith(".mp4") || fileName.endsWith(".mkv") || fileName.endsWith(".avi")) {
            holder.ivIcon.setImageResource(R.drawable.ic_video);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_file); // default for unknown file types
        }
    }

    holder.itemView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            listener.onFileClick(file);
        }
    });
}


    @Override
    public int getItemCount() {
        return files.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName;
        ImageView ivIcon;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            ivIcon = itemView.findViewById(R.id.ivIcon);
        }
    }
}
