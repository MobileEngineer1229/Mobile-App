package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.DiscoveredDevice;

import java.util.List;

public class DiscoveredDeviceAdapter extends RecyclerView.Adapter<DiscoveredDeviceAdapter.DeviceViewHolder> {

    private List<DiscoveredDevice> deviceList;
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(DiscoveredDevice device);
    }

    public DiscoveredDeviceAdapter(List<DiscoveredDevice> deviceList) {
        this.deviceList = deviceList;
    }

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_discovered_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        DiscoveredDevice device = deviceList.get(position);
        holder.bind(device);
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageViewDeviceIcon;
        private ImageView imageViewRadio;
        private TextView textViewDeviceName;
        private TextView textViewDeviceType;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewDeviceIcon = itemView.findViewById(R.id.imageViewDeviceIcon);
            imageViewRadio = itemView.findViewById(R.id.imageViewRadio);
            textViewDeviceName = itemView.findViewById(R.id.textViewDeviceName);
            textViewDeviceType = itemView.findViewById(R.id.textViewDeviceType);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        DiscoveredDevice device = deviceList.get(position);
                        device.setSelected(!device.isSelected());
                        notifyItemChanged(position);
                        listener.onDeviceClick(device);
                    }
                }
            });
        }

        void bind(DiscoveredDevice device) {
            textViewDeviceName.setText(device.getName());
            textViewDeviceType.setText(device.getType());
            imageViewDeviceIcon.setImageResource(device.getIconResId());

            if (device.isSelected()) {
                imageViewRadio.setImageResource(R.drawable.ic_radio_button_checked);
            } else {
                imageViewRadio.setImageResource(R.drawable.ic_radio_button_unchecked);
            }
        }
    }
}

