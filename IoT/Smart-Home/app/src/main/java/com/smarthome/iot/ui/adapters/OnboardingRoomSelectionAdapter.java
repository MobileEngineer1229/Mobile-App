package com.smarthome.iot.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.smarthome.iot.R;

import java.util.List;
import java.util.Set;

public class OnboardingRoomSelectionAdapter extends BaseAdapter {
    private Context context;
    private List<String> rooms;
    private Set<String> selectedRooms;
    private OnRoomToggleListener listener;

    public interface OnRoomToggleListener {
        void onRoomToggle(String room);
    }

    public OnboardingRoomSelectionAdapter(Context context, List<String> rooms, Set<String> selectedRooms, OnRoomToggleListener listener) {
        this.context = context;
        this.rooms = rooms;
        this.selectedRooms = selectedRooms;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return rooms != null ? rooms.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return rooms != null ? rooms.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_room_selection, parent, false);
            holder = new ViewHolder();
            holder.textViewRoomName = convertView.findViewById(R.id.textViewRoomName);
            holder.imageViewRoomIcon = convertView.findViewById(R.id.imageViewRoomIcon);
            holder.imageViewCheckmark = convertView.findViewById(R.id.imageViewCheckmark);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String room = rooms.get(position);
        holder.textViewRoomName.setText(room);
        
        // Set room icon
        int iconRes = getRoomIcon(room);
        holder.imageViewRoomIcon.setImageResource(iconRes);
        
        // Handle "Add Room" special case
        boolean isAddRoom = room.equals(context.getString(R.string.add_room));
        if (isAddRoom) {
            holder.imageViewCheckmark.setVisibility(View.GONE);
        } else {
            boolean isSelected = selectedRooms.contains(room);
            holder.imageViewCheckmark.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        }

        convertView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRoomToggle(room);
            }
        });

        return convertView;
    }

    private int getRoomIcon(String roomName) {
        String roomLower = roomName.toLowerCase();
        String livingRoom = context.getString(R.string.living_room).toLowerCase();
        String bedroom = context.getString(R.string.bedroom).toLowerCase();
        String bathroom = context.getString(R.string.bathroom).toLowerCase();
        String kitchen = context.getString(R.string.kitchen).toLowerCase();
        String diningRoom = context.getString(R.string.dining_room).toLowerCase();
        String backyard = context.getString(R.string.backyard).toLowerCase();
        String studyRoom = context.getString(R.string.study_room).toLowerCase();
        String addRoom = context.getString(R.string.add_room).toLowerCase();
        
        if (roomLower.contains(livingRoom)) {
            return R.drawable.ic_home;
        } else if (roomLower.contains(bedroom)) {
            return R.drawable.ic_moon;
        } else if (roomLower.contains(bathroom)) {
            return R.drawable.ic_water_drop;
        } else if (roomLower.contains(kitchen)) {
            return R.drawable.ic_lightbulb;
        } else if (roomLower.contains(diningRoom)) {
            return R.drawable.ic_calendar;
        } else if (roomLower.contains(backyard)) {
            return R.drawable.ic_leaf;
        } else if (roomLower.contains(studyRoom)) {
            return R.drawable.ic_briefcase;
        } else if (roomLower.contains(addRoom)) {
            return R.drawable.ic_add;
        }
        return R.drawable.ic_home; // Default icon
    }

    static class ViewHolder {
        TextView textViewRoomName;
        ImageView imageViewRoomIcon;
        ImageView imageViewCheckmark;
    }
}
