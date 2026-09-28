package com.example.figurecollection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class FigureAdapter extends RecyclerView.Adapter<FigureAdapter.VH> {

    public interface OnItemClick {
        void onClick(Figure figure);
    }

    private final List<Figure> data;
    private final OnItemClick listener;

    public FigureAdapter(List<Figure> data, OnItemClick listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_figure, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Figure f = data.get(position);
        holder.name.setText(f.name);
        holder.size.setText(f.sizeText());

        if (f.photoPath != null && new File(f.photoPath).exists()) {
            holder.image.setImageBitmap(decode(f.photoPath, 300));
        } else {
            holder.image.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(f));
    }

    private Bitmap decode(String path, int req) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, o);
        int scale = 1;
        while (o.outWidth / scale > req || o.outHeight / scale > req) scale *= 2;
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeFile(path, o2);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, size;
        VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.itemImage);
            name = itemView.findViewById(R.id.itemName);
            size = itemView.findViewById(R.id.itemSize);
        }
    }
}
