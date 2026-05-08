package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;
import com.smarthome.iot.models.AccountAction;

import java.util.List;

public class AccountActionAdapter extends RecyclerView.Adapter<AccountActionAdapter.ViewHolder> {
    private List<AccountAction> accountActions;
    private OnAccountActionClickListener listener;

    public interface OnAccountActionClickListener {
        void onAccountActionClick(AccountAction action);
    }

    public AccountActionAdapter(List<AccountAction> accountActions, OnAccountActionClickListener listener) {
        this.accountActions = accountActions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account_action, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AccountAction action = accountActions.get(position);
        holder.textViewTitle.setText(action.getTitle());
        holder.textViewSubtitle.setText(action.getSubtitle());

        // Set text color for dangerous actions
        if (action.isDangerous()) {
            holder.textViewTitle.setTextColor(holder.itemView.getContext().getColor(R.color.error));
        } else {
            holder.textViewTitle.setTextColor(holder.itemView.getContext().getColor(R.color.white));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccountActionClick(action);
            }
        });
    }

    @Override
    public int getItemCount() {
        return accountActions != null ? accountActions.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView textViewTitle;
        TextView textViewSubtitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewSubtitle = itemView.findViewById(R.id.textViewSubtitle);
        }
    }
}

