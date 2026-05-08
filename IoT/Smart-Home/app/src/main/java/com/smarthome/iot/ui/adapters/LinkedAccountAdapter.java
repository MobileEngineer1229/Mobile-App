package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smarthome.iot.R;
import com.smarthome.iot.models.LinkedAccount;

import java.util.List;

public class LinkedAccountAdapter extends RecyclerView.Adapter<LinkedAccountAdapter.ViewHolder> {
    private List<LinkedAccount> linkedAccounts;
    private OnLinkedAccountClickListener listener;

    public interface OnLinkedAccountClickListener {
        void onConnectClick(LinkedAccount account);
        void onDisconnectClick(LinkedAccount account);
    }

    public LinkedAccountAdapter(List<LinkedAccount> linkedAccounts, OnLinkedAccountClickListener listener) {
        this.linkedAccounts = linkedAccounts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_linked_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LinkedAccount account = linkedAccounts.get(position);
        holder.textViewAccountName.setText(account.getProvider());
        holder.imageViewIcon.setImageResource(account.getIconResId());

        if (account.isConnected()) {
            holder.textViewStatus.setVisibility(View.VISIBLE);
            holder.textViewStatus.setText(holder.itemView.getContext().getString(R.string.account_connected));
            holder.buttonConnect.setVisibility(View.GONE);
        } else {
            holder.textViewStatus.setVisibility(View.GONE);
            holder.buttonConnect.setVisibility(View.VISIBLE);
            holder.buttonConnect.setText(holder.itemView.getContext().getString(R.string.account_connect));
            holder.buttonConnect.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConnectClick(account);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return linkedAccounts != null ? linkedAccounts.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView imageViewIcon;
        TextView textViewAccountName;
        TextView textViewStatus;
        MaterialButton buttonConnect;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
            textViewAccountName = itemView.findViewById(R.id.textViewAccountName);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
            buttonConnect = itemView.findViewById(R.id.buttonConnect);
        }
    }
}

