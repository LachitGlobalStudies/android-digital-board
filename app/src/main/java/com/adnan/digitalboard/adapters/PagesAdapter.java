package com.adnan.digitalboard.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.adnan.digitalboard.R;
import com.adnan.digitalboard.activity.MainActivity;
import com.adnan.digitalboard.view.DrawingView;

import java.util.List;

public class PagesAdapter extends RecyclerView.Adapter<PagesAdapter.ViewHolder> {




    private final List<DrawingView.Page> pages;
   private DrawingView drawingView;
    private Context context;
    private MainActivity mainActivity;



    public PagesAdapter(List<DrawingView.Page> pages, DrawingView drawingView, Context context) {
        this.pages = pages;
        this.drawingView = drawingView;
        this.context = context;
        if (context instanceof MainActivity) {
            this.mainActivity = (MainActivity) context;
        }
    }



    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.bitmap_list,parent,false);
                return new ViewHolder(view);
    }
    @SuppressLint("RecyclerView")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        DrawingView.Page page = pages.get(position);


        // Convert page to Bitmap
      Bitmap compositeBitmap=drawingView.getCompositeBitmap(page);

        // Set Bitmap to ImageView
        holder.btmp.setImageBitmap(compositeBitmap);
        holder.btmp.setBackgroundColor(Color.WHITE);

        // Set Page Number
        holder.pgNo.setText(String.valueOf(position + 1));


        // Handle Delete Button Click
        holder.del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pages.size() > 1) {
                    // Remove the page
                    pages.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, pages.size());

                    // Load the appropriate page
                    if (position >= pages.size()) {
                        // If the deleted page was the last one, load the new last page
                        drawingView.updateCanvas(pages.size() - 1);

                    } else {
                        // Load the page at the current position
                        drawingView.updateCanvas(position);
                    }
                } else {
                    // Reload the current page since it cannot be deleted
                    drawingView.updateCanvas(position);
                    Toast.makeText(v.getContext(), "Cannot delete the last remaining page!", Toast.LENGTH_SHORT).show();
                }

                // Update page numbers
                mainActivity.loadPageNo();
            }
        });


        holder.btmp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.pageLayout.setVisibility(View.GONE);
                drawingView.updateCanvas(position);
                mainActivity.loadPageNo();


            }
        });
    }


    @Override
    public int getItemCount() {
        return pages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView btmp,del;
        private TextView pgNo;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            btmp=itemView.findViewById(R.id.btmp);
            del=itemView.findViewById(R.id.delete);
            pgNo=itemView.findViewById(R.id.pad_id);

        }
    }
}
