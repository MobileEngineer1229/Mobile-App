package com.smarthome.iot.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smarthome.iot.R;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private List<ChatMessage> messages;

    public ChatAdapter() {
        this.messages = new ArrayList<>();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout botMessageContainer;
        private LinearLayout userMessageContainer;
        private TextView textViewBotMessage;
        private TextView textViewUserMessage;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            botMessageContainer = itemView.findViewById(R.id.botMessageContainer);
            userMessageContainer = itemView.findViewById(R.id.userMessageContainer);
            textViewBotMessage = itemView.findViewById(R.id.textViewBotMessage);
            textViewUserMessage = itemView.findViewById(R.id.textViewUserMessage);
        }

        void bind(ChatMessage message) {
            if (message.isFromBot()) {
                botMessageContainer.setVisibility(View.VISIBLE);
                userMessageContainer.setVisibility(View.GONE);
                textViewBotMessage.setText(message.getText());
            } else {
                botMessageContainer.setVisibility(View.GONE);
                userMessageContainer.setVisibility(View.VISIBLE);
                textViewUserMessage.setText(message.getText());
            }
        }
    }

    public static class ChatMessage {
        private String text;
        private boolean isFromBot;

        public ChatMessage(String text, boolean isFromBot) {
            this.text = text;
            this.isFromBot = isFromBot;
        }

        public String getText() {
            return text;
        }

        public boolean isFromBot() {
            return isFromBot;
        }
    }
}

