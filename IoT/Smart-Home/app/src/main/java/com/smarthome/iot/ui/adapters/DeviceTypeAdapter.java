package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.DeviceType;

import java.util.List;

public class DeviceTypeAdapter extends RecyclerView.Adapter<DeviceTypeAdapter.DeviceTypeViewHolder> {

    private List<DeviceType> deviceList;
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceType device);
    }

    public DeviceTypeAdapter(List<DeviceType> deviceList) {
        this.deviceList = deviceList;
    }

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceTypeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device_type, parent, false);
        return new DeviceTypeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceTypeViewHolder holder, int position) {
        if (deviceList != null && position < deviceList.size()) {
            DeviceType device = deviceList.get(position);
            holder.bind(device);
        }
    }

    @Override
    public int getItemCount() {
        return deviceList != null ? deviceList.size() : 0;
    }

    public void updateDevices(List<DeviceType> newDevices) {
        if (this.deviceList == null) {
            return;
        }
        
        if (newDevices == null || newDevices.isEmpty()) {
            this.deviceList.clear();
            notifyDataSetChanged();
            return;
        }
        
        int oldSize = this.deviceList.size();
        this.deviceList.clear();
        this.deviceList.addAll(newDevices);
        
        if (oldSize != this.deviceList.size()) {
            notifyDataSetChanged();
        } else {
            notifyItemRangeChanged(0, this.deviceList.size());
        }
    }

    class DeviceTypeViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageViewDevice;
        private TextView textViewDeviceName;

        DeviceTypeViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewDevice = itemView.findViewById(R.id.imageViewDevice);
            textViewDeviceName = itemView.findViewById(R.id.textViewDeviceName);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onDeviceClick(deviceList.get(position));
                    }
                }
            });
        }

        void bind(DeviceType device) {
            textViewDeviceName.setText(device.getName());
            int iconResId = device.getIconResId();
            imageViewDevice.setImageResource(iconResId);
            // Clear any previous color filter for PNG images
            imageViewDevice.clearColorFilter();
        }
    }
}
