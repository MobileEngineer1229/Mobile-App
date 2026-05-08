package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.smarthome.iot.R;
import com.smarthome.iot.models.Device;
import com.smarthome.iot.models.Room;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
    private List<Device> devices;
    private List<Room> rooms;
    private OnDeviceClickListener listener;
    private OnDeviceToggleListener toggleListener;
    private OnDeviceLongClickListener longClickListener;

    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);
    }

    public interface OnDeviceToggleListener {
        void onDeviceToggle(Device device, boolean isOn);
    }

    public interface OnDeviceLongClickListener {
        boolean onDeviceLongClick(Device device);
    }

    public DeviceAdapter() {
        this.devices = new ArrayList<>();
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices != null ? devices : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms != null ? rooms : new ArrayList<>();
    }

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    public void setOnDeviceToggleListener(OnDeviceToggleListener listener) {
        this.toggleListener = listener;
    }

    public void setOnDeviceLongClickListener(OnDeviceLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = devices.get(position);
        holder.bind(device);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageViewDeviceIcon;
        private TextView textViewDeviceName;
        private TextView textViewRoomName;
        private SwitchMaterial switchDeviceStatus;
        private CardView cardView;
        private LinearLayout layoutWifiConnectivity;
        private ImageView imageViewWifiIcon;
        private TextView textViewWifiText;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewDeviceIcon = itemView.findViewById(R.id.imageViewDeviceIcon);
            textViewDeviceName = itemView.findViewById(R.id.textViewDeviceName);
            textViewRoomName = itemView.findViewById(R.id.textViewRoomName);
            switchDeviceStatus = itemView.findViewById(R.id.switchDeviceStatus);
            cardView = (CardView) itemView;
            layoutWifiConnectivity = itemView.findViewById(R.id.layoutWifiConnectivity);
            imageViewWifiIcon = itemView.findViewById(R.id.imageViewWifiIcon);
            textViewWifiText = itemView.findViewById(R.id.textViewWifiText);
            
            // Ensure switch is visible and properly styled
            if (switchDeviceStatus != null) {
                com.smarthome.iot.utils.SwitchHelper.applySwitchStyle(itemView.getContext(), switchDeviceStatus);
            }
        }

        public void bind(Device device) {
            textViewDeviceName.setText(device.getName());
            
            // Apply switch styling before setting state
            if (switchDeviceStatus != null) {
                com.smarthome.iot.utils.SwitchHelper.applySwitchStyle(itemView.getContext(), switchDeviceStatus);
                // Clear previous listener to avoid conflicts
                switchDeviceStatus.setOnCheckedChangeListener(null);
                // Set checked state
                switchDeviceStatus.setChecked(device.isOn());
            }
            
            // Set device icon based on device type/name
            int iconResId = getDeviceIconResId(device.getName(), device.getType());
            if (imageViewDeviceIcon != null) {
                imageViewDeviceIcon.setImageResource(iconResId);
            }
            
            // Set room name (for now, use a default or get from roomId)
            String roomName = getRoomNameForDevice(device);
            if (textViewRoomName != null) {
                textViewRoomName.setText(roomName);
            }

            // Set Wi-Fi connectivity indicator
            String connectionType = getConnectionType(device);
            if (layoutWifiConnectivity != null) {
                if (connectionType != null && !connectionType.isEmpty()) {
                    layoutWifiConnectivity.setVisibility(View.VISIBLE);
                    if (textViewWifiText != null) {
                        textViewWifiText.setText(connectionType);
                    }
                } else {
                    layoutWifiConnectivity.setVisibility(View.GONE);
                }
            }

            // Set click listener on card view to navigate to device detail
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceClick(device);
                }
            });

            // Set toggle listener on switch (separate from card click)
            // The switch will consume click events, so they won't propagate to the card
            if (switchDeviceStatus != null) {
                switchDeviceStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (toggleListener != null) {
                        toggleListener.onDeviceToggle(device, isChecked);
                    }
                });
            }

            // Set long click listener for assigning device to room
            cardView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    return longClickListener.onDeviceLongClick(device);
                }
                return false;
            });
        }

        private int getDeviceIconResId(String deviceName, String deviceType) {
            if (deviceName == null && deviceType == null) {
                return R.drawable.lamp;
            }
            
            String nameLower = deviceName != null ? deviceName.toLowerCase() : "";
            String typeLower = deviceType != null ? deviceType.toLowerCase() : "";
            
            // For lightning category: differentiate between "Smart Lamp" and "Lamp"
            // "Smart Lamp" uses standard bulb icon, "Lamp" uses fluorescent tube icon
            if (nameLower.contains("lamp") || nameLower.contains("light") || 
                typeLower.contains("lamp") || typeLower.contains("light")) {
                // Check if it's "Smart Lamp" (standard bulb) or just "Lamp" (fluorescent)
                if (nameLower.equals("lamp") || (nameLower.startsWith("lamp") && !nameLower.contains("smart"))) {
                    // For "Lamp" without "Smart", use the same lamp icon but could be differentiated
                    // Currently using same icon, but can be updated if we have a fluorescent tube icon
                    return R.drawable.lamp;
                } else {
                    // "Smart Lamp" or other light devices
                    return R.drawable.lamp;
                }
            } else if (nameLower.contains("v1 cctv") || nameLower.contains("smart v1")) {
                return R.drawable.cctv_v1;
            } else if (nameLower.contains("v2 cctv") || nameLower.contains("smart v2")) {
                return R.drawable.cctv_v2;
            } else if ((nameLower.contains("camera") || typeLower.contains("camera")) && 
                       !nameLower.contains("cctv") && !typeLower.contains("cctv")) {
                return R.drawable.camera;
            } else if (nameLower.contains("cctv") || typeLower.contains("cctv")) {
                return R.drawable.cctv_v1;
            } else if (nameLower.contains("speaker") || typeLower.contains("speaker")) {
                return R.drawable.speaker;
            } else if (nameLower.contains("router") || nameLower.contains("wifi") ||
                       typeLower.contains("router")) {
                return R.drawable.router;
            } else if (nameLower.contains("webcam") || typeLower.contains("webcam")) {
                return R.drawable.webcam;
            } else if (nameLower.contains("air") || nameLower.contains("conditioner") ||
                       typeLower.contains("ac") || typeLower.contains("air")) {
                return R.drawable.air_conditioner;
            }
            return R.drawable.lamp;
        }

        private String getRoomNameForDevice(Device device) {
            Integer roomId = device.getRoomId();
            if (roomId == null) {
                return "No Room";
            }

            // Look up room name from rooms list
            if (rooms != null) {
                for (Room room : rooms) {
                    if (room.getId() == roomId) {
                        return room.getName();
                    }
                }
            }

            // Fallback if room not found in list
            return "Room " + roomId;
        }

        private String getConnectionType(Device device) {
            // For now, default to Wi-Fi for all devices
            // In production, this should come from device metadata or a connectionType field
            // Check if device has IP address (Wi-Fi) or only MAC (could be Bluetooth)
            if (device.getIpAddress() != null && !device.getIpAddress().isEmpty()) {
                return "Wi-Fi";
            } else if (device.getMacAddress() != null && !device.getMacAddress().isEmpty()) {
                // Could be Bluetooth or other connection type
                // For now, default to Wi-Fi
                return "Wi-Fi";
            }
            return "Wi-Fi"; // Default connection type
        }
    }
}

