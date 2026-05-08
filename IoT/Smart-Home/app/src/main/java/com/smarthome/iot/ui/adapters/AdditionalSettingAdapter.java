package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.AdditionalSetting;

import java.util.List;

public class AdditionalSettingAdapter extends RecyclerView.Adapter<AdditionalSettingAdapter.ViewHolder> {
    private List<AdditionalSetting> settings;
    private OnSettingClickListener listener;

    public interface OnSettingClickListener {
        void onSettingClick(AdditionalSetting setting);
    }

    public AdditionalSettingAdapter(List<AdditionalSetting> settings, OnSettingClickListener listener) {
        this.settings = settings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_additional_setting, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdditionalSetting setting = settings.get(position);
        holder.textViewTitle.setText(setting.getTitle());
        
        if (setting.getValue() != null && !setting.getValue().isEmpty()) {
            holder.textViewValue.setVisibility(View.VISIBLE);
            holder.textViewValue.setText(setting.getValue());
        } else {
            holder.textViewValue.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSettingClick(setting);
            }
        });
    }

    @Override
    public int getItemCount() {
        return settings != null ? settings.size() : 0;
    }

    public AdditionalSetting getItemAt(int position) {
        if (settings != null && position >= 0 && position < settings.size()) {
            return settings.get(position);
        }
        return null;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView textViewTitle;
        TextView textViewValue;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewValue = itemView.findViewById(R.id.textViewValue);
        }
    }
}

