package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;

import java.util.List;

public class ShareContactAdapter extends RecyclerView.Adapter<ShareContactAdapter.ViewHolder> {
    private List<ContactItem> contacts;
    private OnContactClickListener listener;

    public static class ContactItem {
        private String name;
        private int iconResId;

        public ContactItem(String name, int iconResId) {
            this.name = name;
            this.iconResId = iconResId;
        }

        public String getName() { return name; }
        public int getIconResId() { return iconResId; }
    }

    public interface OnContactClickListener {
        void onContactClick(ContactItem contact);
    }

    public ShareContactAdapter(List<ContactItem> contacts, OnContactClickListener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_share_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactItem contact = contacts.get(position);
        holder.textViewName.setText(contact.getName());
        holder.imageViewIcon.setImageResource(contact.getIconResId());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onContactClick(contact);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts != null ? contacts.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewIcon;
        TextView textViewName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
            textViewName = itemView.findViewById(R.id.textViewName);
        }
    }
}
