package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.DeviceConsumptionSummary;

import java.text.DecimalFormat;
import java.util.List;

public class DeviceConsumptionAdapter extends RecyclerView.Adapter<DeviceConsumptionAdapter.ViewHolder> {
    private List<DeviceConsumptionSummary> devices;
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceConsumptionSummary device);
    }

    public DeviceConsumptionAdapter(List<DeviceConsumptionSummary> devices, OnDeviceClickListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_device_consumption, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            if (devices == null || position >= devices.size()) {
                return;
            }

            DeviceConsumptionSummary device = devices.get(position);
            if (device == null) {
                return;
            }

            DecimalFormat df = new DecimalFormat("0.00");
            DecimalFormat costFormat = new DecimalFormat("0.00");

            if (holder.textViewDeviceName != null) {
                String deviceName = device.getDeviceName();
                holder.textViewDeviceName.setText(deviceName != null ? deviceName : "Unknown Device");
            }

            if (holder.textViewConsumption != null) {
                Double consumption = device.getTotalConsumptionKwh();
                if (consumption != null) {
                    holder.textViewConsumption.setText(df.format(consumption));
                } else {
                    holder.textViewConsumption.setText("0.00");
                }
            }

            if (holder.textViewCost != null) {
                Double cost = device.getTotalCostUsd();
                if (cost != null) {
                    holder.textViewCost.setText("$" + costFormat.format(cost));
                } else {
                    holder.textViewCost.setText("$0.00");
                }
            }

            if (holder.textViewDeviceCount != null) {
                Integer deviceCount = device.getDeviceCount();
                if (deviceCount != null && deviceCount > 0) {
                    if (deviceCount == 1) {
                        holder.textViewDeviceCount.setText("1 device");
                    } else {
                        holder.textViewDeviceCount.setText(deviceCount + " devices");
                    }
                    holder.textViewDeviceCount.setVisibility(View.VISIBLE);
                } else {
                    holder.textViewDeviceCount.setVisibility(View.GONE);
                }
            }

            // Set device icon based on type
            if (holder.imageViewDeviceIcon != null) {
                String deviceType = device.getDeviceType();
                int iconResId = R.drawable.ic_smart; // Default icon

                if (deviceType != null) {
                    String typeLower = deviceType.toLowerCase();
                    if (typeLower.contains("lighting") || typeLower.contains("light") || typeLower.contains("lamp")) {
                        iconResId = R.drawable.ic_lightbulb;
                    } else if (typeLower.contains("camera") || typeLower.contains("cctv")) {
                        iconResId = R.drawable.ic_camera;
                    } else if (typeLower.contains("outlet") || typeLower.contains("plug")) {
                        iconResId = R.drawable.ic_outlet;
                    } else if (typeLower.contains("security") || typeLower.contains("lock")) {
                        iconResId = R.drawable.ic_lock;
                    }
                }
                holder.imageViewDeviceIcon.setImageResource(iconResId);
                holder.imageViewDeviceIcon.setColorFilter(null);
                holder.imageViewDeviceIcon.clearColorFilter();
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceClick(device);
                }
            });
        } catch (Exception e) {
            android.util.Log.e("DeviceConsumptionAdapter", "Error binding view holder", e);
        }
    }

    @Override
    public int getItemCount() {
        return devices != null ? devices.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewDeviceIcon;
        TextView textViewDeviceName;
        TextView textViewConsumption;
        TextView textViewCost;
        TextView textViewDeviceCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewDeviceIcon = itemView.findViewById(R.id.imageViewDeviceIcon);
            textViewDeviceName = itemView.findViewById(R.id.textViewDeviceName);
            textViewConsumption = itemView.findViewById(R.id.textViewConsumption);
            textViewCost = itemView.findViewById(R.id.textViewCost);
            textViewDeviceCount = itemView.findViewById(R.id.textViewDeviceCount);
        }
    }
}

