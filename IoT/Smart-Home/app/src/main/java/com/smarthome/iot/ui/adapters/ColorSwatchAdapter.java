package com.smarthome.iot.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import android.graphics.drawable.GradientDrawable;

import java.util.ArrayList;
import java.util.List;

public class ColorSwatchAdapter extends RecyclerView.Adapter<ColorSwatchAdapter.ViewHolder> {
    private List<String> colors;
    private String selectedColor;
    private OnColorSelectedListener listener;

    public interface OnColorSelectedListener {
        void onColorSelected(String color);
    }

    public ColorSwatchAdapter(String[] colors, String selectedColor, OnColorSelectedListener listener) {
        this.colors = new ArrayList<>();
        for (String color : colors) {
            this.colors.add(color);
        }
        this.selectedColor = selectedColor;
        this.listener = listener;
    }

    public void setSelectedColor(String color) {
        this.selectedColor = color;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_color_swatch, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String color = colors.get(position);
        
        // Set background color
        try {
            int colorInt = Color.parseColor(color);
            GradientDrawable drawable = (GradientDrawable) holder.viewColorCircle.getBackground();
            if (drawable != null) {
                drawable.setColor(colorInt);
            } else {
                holder.viewColorCircle.setBackgroundColor(colorInt);
            }
        } catch (IllegalArgumentException e) {
            holder.viewColorCircle.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
        }
        
        // Show checkmark if selected
        boolean isSelected = color.equals(selectedColor);
        holder.imageViewCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onColorSelected(color);
            }
        });
    }

    @Override
    public int getItemCount() {
        return colors != null ? colors.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View viewColorCircle;
        ImageView imageViewCheck;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            viewColorCircle = itemView.findViewById(R.id.viewColorCircle);
            imageViewCheck = itemView.findViewById(R.id.imageViewCheck);
        }
    }
}
