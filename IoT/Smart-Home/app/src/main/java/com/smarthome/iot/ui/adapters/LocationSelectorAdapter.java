package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.Home;

import java.util.List;

public class LocationSelectorAdapter extends RecyclerView.Adapter<LocationSelectorAdapter.ViewHolder> {
    
    private List<Home> homes;
    private int selectedHomeId;
    private OnHomeSelectedListener listener;
    
    public interface OnHomeSelectedListener {
        void onHomeSelected(Home home);
    }
    
    public LocationSelectorAdapter(List<Home> homes, int selectedHomeId, OnHomeSelectedListener listener) {
        this.homes = homes;
        this.selectedHomeId = selectedHomeId;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location_selector, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Home home = homes.get(position);
        holder.textViewHomeName.setText(home.getName());
        
        // Show checkmark for selected home
        if (home.getId() == selectedHomeId) {
            holder.imageViewCheckmark.setVisibility(View.VISIBLE);
        } else {
            holder.imageViewCheckmark.setVisibility(View.GONE);
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHomeSelected(home);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return homes != null ? homes.size() : 0;
    }
    
    public void setSelectedHomeId(int homeId) {
        this.selectedHomeId = homeId;
        notifyDataSetChanged();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewCheckmark;
        TextView textViewHomeName;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewCheckmark = itemView.findViewById(R.id.imageViewCheckmark);
            textViewHomeName = itemView.findViewById(R.id.textViewHomeName);
        }
    }
}
