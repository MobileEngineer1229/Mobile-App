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

public class ReportsDeviceDetailAdapter extends RecyclerView.Adapter<ReportsDeviceDetailAdapter.ViewHolder> {
    private List<DeviceConsumptionSummary> devices;

    public ReportsDeviceDetailAdapter(List<DeviceConsumptionSummary> devices) {
        this.devices = devices;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_reports_device_detail, parent, false);
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

            // Set device icon based on type
            if (holder.imageViewDeviceIcon != null) {
                int iconRes = R.drawable.ic_lightbulb; // Default
                String deviceType = device.getDeviceType();
                if (deviceType != null) {
                    switch (deviceType.toLowerCase()) {
                        case "lighting":
                        case "lamp":
                            iconRes = R.drawable.ic_lightbulb;
                            break;
                        case "camera":
                        case "cctv":
                            iconRes = R.drawable.ic_camera;
                            break;
                        case "outlet":
                        case "plug":
                            iconRes = R.drawable.ic_plug;
                            break;
                        default:
                            iconRes = R.drawable.ic_smart;
                            break;
                    }
                }
                holder.imageViewDeviceIcon.setImageResource(iconRes);
            }

            // Set consumption
            if (holder.textViewConsumption != null) {
                Double consumption = device.getTotalConsumptionKwh();
                if (consumption != null) {
                    holder.textViewConsumption.setText(df.format(consumption));
                } else {
                    holder.textViewConsumption.setText("0.00");
                }
            }

            // Set cost
            if (holder.textViewCost != null) {
                Double cost = device.getTotalCostUsd();
                if (cost != null) {
                    holder.textViewCost.setText("$" + costFormat.format(cost));
                } else {
                    holder.textViewCost.setText("$0.00");
                }
            }

            // Set device name
            if (holder.textViewDeviceName != null) {
                String deviceName = device.getDeviceName();
                holder.textViewDeviceName.setText(deviceName != null ? deviceName : "Unknown Device");
            }

            // Set room name
            if (holder.textViewRoomName != null) {
                String roomName = device.getRoomName();
                holder.textViewRoomName.setText(roomName != null ? roomName : "No Room");
            }
        } catch (Exception e) {
            android.util.Log.e("ReportsDeviceDetailAdapter", "Error binding view holder", e);
        }
    }

    @Override
    public int getItemCount() {
        return devices != null ? devices.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewDeviceIcon;
        TextView textViewConsumption;
        TextView textViewCost;
        TextView textViewDeviceName;
        TextView textViewRoomName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewDeviceIcon = itemView.findViewById(R.id.imageViewDeviceIcon);
            textViewConsumption = itemView.findViewById(R.id.textViewConsumption);
            textViewCost = itemView.findViewById(R.id.textViewCost);
            textViewDeviceName = itemView.findViewById(R.id.textViewDeviceName);
            textViewRoomName = itemView.findViewById(R.id.textViewRoomName);
        }
    }
}
