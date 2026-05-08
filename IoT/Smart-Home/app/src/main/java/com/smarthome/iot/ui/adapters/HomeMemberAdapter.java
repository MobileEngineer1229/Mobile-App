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

public class HomeMemberAdapter extends RecyclerView.Adapter<HomeMemberAdapter.ViewHolder> {
    private List<MemberItem> members;
    private OnMemberClickListener listener;

    public static class MemberItem {
        private int id;
        private String name;
        private String email;
        private String role;
        private boolean isYou;

        public MemberItem(int id, String name, String email, String role, boolean isYou) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
            this.isYou = isYou;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public boolean isYou() { return isYou; }
    }

    public interface OnMemberClickListener {
        void onMemberClick(MemberItem member);
    }

    public HomeMemberAdapter(List<MemberItem> members) {
        this.members = members;
    }

    public HomeMemberAdapter(List<MemberItem> members, OnMemberClickListener listener) {
        this.members = members;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MemberItem member = members.get(position);
        
        holder.textViewName.setText(member.getName() + (member.isYou() ? " (" + holder.itemView.getContext().getString(R.string.you) + ")" : ""));
        holder.textViewEmail.setText(member.getEmail());
        holder.textViewRole.setText(member.getRole());
        
        // TODO: Load profile picture
        holder.imageViewAvatar.setImageResource(R.drawable.ic_account_circle);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMemberClick(member);
            }
        });
    }

    @Override
    public int getItemCount() {
        return members != null ? members.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewAvatar;
        TextView textViewName;
        TextView textViewEmail;
        TextView textViewRole;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewAvatar = itemView.findViewById(R.id.imageViewAvatar);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            textViewRole = itemView.findViewById(R.id.textViewRole);
        }
    }
}
