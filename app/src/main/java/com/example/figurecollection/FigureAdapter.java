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

public class FigureAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnItemClick {
        void onClick(Figure figure);
    }

    private final List<Object> items;
    private final OnItemClick listener;
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public FigureAdapter(List<Object> items, OnItemClick listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof String) ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_header, parent, false);
            return new HeaderVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_figure, parent, false);
            return new ItemVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderVH) {
            String title = (String) items.get(position);
            ((HeaderVH) holder).tvHeader.setText(title);
        } else if (holder instanceof ItemVH) {
            Figure f = (Figure) items.get(position);
            ItemVH itemHolder = (ItemVH) holder;
            itemHolder.name.setText(f.name);
            itemHolder.size.setText(f.sizeText());

            if (f.photoPath != null && new File(f.photoPath).exists()) {
                itemHolder.image.setImageBitmap(decode(f.photoPath, 300));
            } else {
                itemHolder.image.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            itemHolder.itemView.setOnClickListener(v -> listener.onClick(f));
        }
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
        return items.size();
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvHeader;
        HeaderVH(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvHeader);
        }
    }

    static class ItemVH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, size;
        ItemVH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.itemImage);
            name = itemView.findViewById(R.id.itemName);
            size = itemView.findViewById(R.id.itemSize);
        }
    }
}
