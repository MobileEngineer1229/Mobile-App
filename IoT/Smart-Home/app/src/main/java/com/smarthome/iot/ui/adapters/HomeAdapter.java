package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.Home;

import java.util.List;

public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.ViewHolder> {
    private List<Home> homes;
    private OnHomeClickListener listener;

    public interface OnHomeClickListener {
        void onHomeClick(Home home);
    }

    public HomeAdapter(List<Home> homes, OnHomeClickListener listener) {
        this.homes = homes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Home home = homes.get(position);
        holder.textViewHomeName.setText(home.getName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHomeClick(home);
            }
        });
    }

    @Override
    public int getItemCount() {
        return homes != null ? homes.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewHomeName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewHomeName = itemView.findViewById(R.id.textViewHomeName);
        }
    }
}
