package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;

import java.util.List;

public class ManageRoomsAdapter extends RecyclerView.Adapter<ManageRoomsAdapter.ViewHolder> {
    private List<RoomItem> rooms;
    private OnRoomDeleteListener deleteListener;
    private OnDragStartListener dragStartListener;

    public static class RoomItem {
        private int id;
        private String name;

        public RoomItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() { return id; }
        public String getName() { return name; }
    }

    public interface OnRoomDeleteListener {
        void onRoomDelete(RoomItem room);
    }

    public interface OnDragStartListener {
        void onDragStart(RecyclerView.ViewHolder viewHolder);
    }

    public ManageRoomsAdapter(List<RoomItem> rooms, OnRoomDeleteListener deleteListener, 
                             OnDragStartListener dragStartListener) {
        this.rooms = rooms;
        this.deleteListener = deleteListener;
        this.dragStartListener = dragStartListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manage_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RoomItem room = rooms.get(position);
        holder.textViewRoomName.setText(room.getName());

        holder.imageViewDragHandle.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN && dragStartListener != null) {
                dragStartListener.onDragStart(holder);
            }
            return false;
        });

        holder.buttonDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onRoomDelete(room);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rooms != null ? rooms.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewDragHandle;
        TextView textViewRoomName;
        ImageButton buttonDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewDragHandle = itemView.findViewById(R.id.imageViewDragHandle);
            textViewRoomName = itemView.findViewById(R.id.textViewRoomName);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}
