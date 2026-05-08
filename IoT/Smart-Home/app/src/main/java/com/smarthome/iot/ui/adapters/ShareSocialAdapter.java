package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;

import java.util.List;

public class ShareSocialAdapter extends RecyclerView.Adapter<ShareSocialAdapter.ViewHolder> {
    private List<SocialItem> socialApps;
    private OnSocialClickListener listener;

    public static class SocialItem {
        private String name;
        private int iconResId;

        public SocialItem(String name, int iconResId) {
            this.name = name;
            this.iconResId = iconResId;
        }

        public String getName() { return name; }
        public int getIconResId() { return iconResId; }
    }

    public interface OnSocialClickListener {
        void onSocialClick(SocialItem app);
    }

    public ShareSocialAdapter(List<SocialItem> socialApps, OnSocialClickListener listener) {
        this.socialApps = socialApps;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_share_social, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SocialItem app = socialApps.get(position);
        holder.textViewName.setText(app.getName());
        holder.imageViewIcon.setImageResource(app.getIconResId());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSocialClick(app);
            }
        });
    }

    @Override
    public int getItemCount() {
        return socialApps != null ? socialApps.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewIcon;
        TextView textViewName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
            textViewName = itemView.findViewById(R.id.textViewName);
        }
    }
}
