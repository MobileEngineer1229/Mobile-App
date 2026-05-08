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
import java.util.Set;

public class RoomSelectionAdapter extends RecyclerView.Adapter<RoomSelectionAdapter.ViewHolder> {
    private List<RoomItem> rooms;
    private Set<Integer> selectedRoomIds;
    private OnRoomToggleListener listener;

    public static class RoomItem {
        private int id;
        private String name;
        private boolean isSelected;

        public RoomItem(int id, String name, boolean isSelected) {
            this.id = id;
            this.name = name;
            this.isSelected = isSelected;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public boolean isSelected() { return isSelected; }
        public void setSelected(boolean selected) { isSelected = selected; }
    }

    public interface OnRoomToggleListener {
        void onRoomToggle(int roomId, boolean isSelected);
    }

    public RoomSelectionAdapter(List<RoomItem> rooms, Set<Integer> selectedRoomIds, OnRoomToggleListener listener) {
        this.rooms = rooms;
        this.selectedRoomIds = selectedRoomIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_room_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RoomItem room = rooms.get(position);
        holder.textViewRoomName.setText(room.getName());
        
        boolean isSelected = selectedRoomIds.contains(room.getId());
        holder.imageViewCheckmark.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            boolean newSelection = !isSelected;
            if (newSelection) {
                selectedRoomIds.add(room.getId());
            } else {
                selectedRoomIds.remove(room.getId());
            }
            holder.imageViewCheckmark.setVisibility(newSelection ? View.VISIBLE : View.GONE);
            
            if (listener != null) {
                listener.onRoomToggle(room.getId(), newSelection);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rooms != null ? rooms.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewRoomName;
        ImageView imageViewCheckmark;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewRoomName = itemView.findViewById(R.id.textViewRoomName);
            imageViewCheckmark = itemView.findViewById(R.id.imageViewCheckmark);
        }
    }
}
