package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;

import java.util.ArrayList;
import java.util.List;

public class LocationOptionAdapter extends RecyclerView.Adapter<LocationOptionAdapter.ViewHolder> {
    private List<LocationOption> options;
    private OnOptionClickListener listener;

    public interface OnOptionClickListener {
        void onOptionClick(String optionType);
    }

    public static class LocationOption {
        private String type;
        private String title;
        private String example;
        private int iconRes;
        private int iconColorRes;

        public LocationOption(String type, String title, String example, int iconRes, int iconColorRes) {
            this.type = type;
            this.title = title;
            this.example = example;
            this.iconRes = iconRes;
            this.iconColorRes = iconColorRes;
        }

        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getExample() { return example; }
        public int getIconRes() { return iconRes; }
        public int getIconColorRes() { return iconColorRes; }
    }

    public LocationOptionAdapter(OnOptionClickListener listener) {
        this.options = new ArrayList<>();
        this.listener = listener;
    }

    public void setOptions(List<LocationOption> options) {
        this.options = options != null ? options : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location_option, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= options.size()) {
            return;
        }
        
        LocationOption option = options.get(position);
        if (option == null) {
            return;
        }
        
        if (holder.imageViewIcon != null) {
            holder.imageViewIcon.setImageResource(option.getIconRes());
            holder.imageViewIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), option.getIconColorRes()));
        }
        
        if (holder.textViewTitle != null) {
            holder.textViewTitle.setText(option.getTitle());
        }
        
        if (holder.textViewExample != null) {
            holder.textViewExample.setText(option.getExample());
        }
        
        // Hide divider on last item
        View divider = holder.itemView.findViewById(R.id.divider);
        if (divider != null) {
            divider.setVisibility(position == options.size() - 1 ? View.GONE : View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOptionClick(option.getType());
            }
        });
    }

    @Override
    public int getItemCount() {
        return options != null ? options.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewIcon;
        TextView textViewTitle;
        TextView textViewExample;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewExample = itemView.findViewById(R.id.textViewExample);
        }
    }
}
