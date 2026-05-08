package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;

import java.util.List;

public class SuggestedRoomAdapter extends RecyclerView.Adapter<SuggestedRoomAdapter.ViewHolder> {
    private List<String> suggestedRooms;
    private OnSuggestedRoomClickListener listener;

    public interface OnSuggestedRoomClickListener {
        void onSuggestedRoomClick(String roomName);
    }

    public SuggestedRoomAdapter(List<String> suggestedRooms, OnSuggestedRoomClickListener listener) {
        this.suggestedRooms = suggestedRooms;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suggested_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String roomName = suggestedRooms.get(position);
        holder.textViewRoomName.setText(roomName);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSuggestedRoomClick(roomName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return suggestedRooms != null ? suggestedRooms.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewRoomName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewRoomName = itemView.findViewById(R.id.textViewRoomName);
        }
    }
}
