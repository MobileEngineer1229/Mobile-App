package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;

import java.util.ArrayList;
import java.util.List;

public class IconSwatchAdapter extends RecyclerView.Adapter<IconSwatchAdapter.ViewHolder> {
    private List<Integer> iconResList;
    private List<String> iconNames;
    private int selectedIconRes;
    private OnIconSelectedListener listener;

    public interface OnIconSelectedListener {
        void onIconSelected(String iconName, int iconRes);
    }

    public IconSwatchAdapter(int[] icons, String[] iconNames, int selectedIconRes, OnIconSelectedListener listener) {
        this.iconResList = new ArrayList<>();
        for (int icon : icons) {
            this.iconResList.add(icon);
        }
        this.iconNames = new ArrayList<>();
        for (String name : iconNames) {
            this.iconNames.add(name);
        }
        this.selectedIconRes = selectedIconRes;
        this.listener = listener;
    }

    public void setSelectedIcon(int iconRes) {
        this.selectedIconRes = iconRes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_icon_swatch, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int iconRes = iconResList.get(position);
        String iconName = position < iconNames.size() ? iconNames.get(position) : "ic_sun";
        
        holder.imageViewIcon.setImageResource(iconRes);
        
        // Highlight selected icon
        boolean isSelected = iconRes == selectedIconRes;
        if (isSelected) {
            holder.imageViewIcon.setBackgroundResource(R.drawable.bg_circle_orange);
        } else {
            holder.imageViewIcon.setBackgroundResource(R.drawable.bg_circle_primary);
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIconSelected(iconName, iconRes);
            }
        });
    }

    @Override
    public int getItemCount() {
        return iconResList != null ? iconResList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
        }
    }
}
