package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import com.google.android.material.card.MaterialCardView;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.VoiceAssistant;

import java.util.List;

public class VoiceAssistantAdapter extends RecyclerView.Adapter<VoiceAssistantAdapter.ViewHolder> {
    private List<VoiceAssistant> voiceAssistants;
    private OnVoiceAssistantClickListener listener;

    public interface OnVoiceAssistantClickListener {
        void onVoiceAssistantClick(VoiceAssistant assistant);
    }

    public VoiceAssistantAdapter(List<VoiceAssistant> voiceAssistants, OnVoiceAssistantClickListener listener) {
        this.voiceAssistants = voiceAssistants;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_voice_assistant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VoiceAssistant assistant = voiceAssistants.get(position);
        holder.textViewName.setText(assistant.getName());
        holder.textViewStatus.setText(assistant.isLinked() ? 
                holder.itemView.getContext().getString(R.string.linked) : 
                holder.itemView.getContext().getString(R.string.unlinked));
        holder.imageViewIcon.setImageResource(assistant.getIconResId());

        // Highlight selected/unlinked items
        if (!assistant.isLinked()) {
            holder.cardView.setStrokeWidth((int) (2 * holder.itemView.getContext().getResources().getDisplayMetrics().density));
            holder.cardView.setStrokeColor(holder.itemView.getContext().getColor(R.color.primary));
        } else {
            holder.cardView.setStrokeWidth(0);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVoiceAssistantClick(assistant);
            }
        });
    }

    @Override
    public int getItemCount() {
        return voiceAssistants != null ? voiceAssistants.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView imageViewIcon;
        TextView textViewName;
        TextView textViewStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
        }
    }
}

