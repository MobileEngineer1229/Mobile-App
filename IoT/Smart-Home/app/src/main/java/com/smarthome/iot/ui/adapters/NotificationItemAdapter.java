package com.smarthome.iot.ui.adapters;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.Notification;

import java.util.List;

public class NotificationItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_NOTIFICATION = 1;
    
    private List<NotificationItem> items;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public static class NotificationItem {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_NOTIFICATION = 1;
        
        public int type;
        public String headerText;
        public Notification notification;

        public NotificationItem(String headerText) {
            this.type = TYPE_HEADER;
            this.headerText = headerText;
        }

        public NotificationItem(Notification notification) {
            this.type = TYPE_NOTIFICATION;
            this.notification = notification;
        }
    }

    public NotificationItemAdapter(List<NotificationItem> items, OnNotificationClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification_section_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new NotificationViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        NotificationItem item = items.get(position);
        
        if (item.type == TYPE_HEADER) {
            ((HeaderViewHolder) holder).bind(item.headerText);
        } else {
            ((NotificationViewHolder) holder).bind(item.notification);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(item.notification);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewHeader;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewHeader = (TextView) itemView;
        }

        void bind(String headerText) {
            textViewHeader.setText(headerText);
        }
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageViewIcon;
        private TextView textViewTitle;
        private TextView textViewDescription;
        private TextView textViewTimestamp;
        private View viewUnreadIndicator;
        private TextView textViewBadge;
        private ImageView imageViewTitleIcon;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewDescription = itemView.findViewById(R.id.textViewDescription);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
            viewUnreadIndicator = itemView.findViewById(R.id.viewUnreadIndicator);
            textViewBadge = itemView.findViewById(R.id.textViewBadge);
            imageViewTitleIcon = itemView.findViewById(R.id.imageViewTitleIcon);
        }

        void bind(Notification notification) {
            textViewTitle.setText(notification.getTitle());
            // Use message as description (API field name)
            String description = notification.getMessage() != null ? notification.getMessage() : "";
            textViewDescription.setText(description);
            textViewTimestamp.setText(notification.getTimestamp());
            
            // Set icon based on iconType
            int iconRes = getIconResource(notification.getIconType());
            if (iconRes != 0) {
                imageViewIcon.setImageResource(iconRes);
                imageViewIcon.setVisibility(View.VISIBLE);
            } else {
                imageViewIcon.setVisibility(View.GONE);
            }
            
            // Set title icon based on notification type
            if (notification.getIconType() != null && notification.getIconType().equals("password")) {
                // Password reset successful - show checkmark
                imageViewTitleIcon.setImageResource(R.drawable.ic_check_circle);
                imageViewTitleIcon.setVisibility(View.VISIBLE);
                imageViewTitleIcon.setColorFilter(itemView.getContext().getColor(R.color.success), PorterDuff.Mode.SRC_IN);
            } else if (notification.getBadge() != null && notification.getBadge().equals("LOCK")) {
                // Security alert - show lock icon
                imageViewTitleIcon.setImageResource(R.drawable.ic_lock);
                imageViewTitleIcon.setVisibility(View.VISIBLE);
                imageViewTitleIcon.setColorFilter(itemView.getContext().getColor(R.color.white_alpha_70), PorterDuff.Mode.SRC_IN);
            } else if (notification.getIconType() != null && notification.getIconType().equals("update")) {
                // System update - show refresh/info icon
                imageViewTitleIcon.setImageResource(R.drawable.ic_info);
                imageViewTitleIcon.setVisibility(View.VISIBLE);
                imageViewTitleIcon.setColorFilter(itemView.getContext().getColor(R.color.white_alpha_70), PorterDuff.Mode.SRC_IN);
            } else {
                imageViewTitleIcon.setVisibility(View.GONE);
            }
            
            // Show/hide unread indicator
            viewUnreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
            
            // Show/hide badge text (NEW, etc.)
            if (notification.getBadge() != null && !notification.getBadge().isEmpty() && !notification.getBadge().equals("LOCK")) {
                textViewBadge.setText(notification.getBadge());
                textViewBadge.setVisibility(View.VISIBLE);
            } else {
                textViewBadge.setVisibility(View.GONE);
            }
        }

        private int getIconResource(String iconType) {
            if (iconType == null) return R.drawable.ic_notifications;
            
            switch (iconType) {
                case "security":
                    return R.drawable.ic_shield;
                case "update":
                    return R.drawable.ic_info;
                case "password":
                    return R.drawable.ic_lock;
                case "feature":
                    return R.drawable.ic_star;
                case "event":
                    return R.drawable.ic_calendar;
                case "device":
                    return R.drawable.ic_device;
                case "energy":
                    return R.drawable.ic_energy;
                default:
                    return R.drawable.ic_notifications;
            }
        }
    }
}
