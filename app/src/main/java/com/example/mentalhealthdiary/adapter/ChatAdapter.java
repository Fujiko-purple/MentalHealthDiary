package com.example.mentalhealthdiary.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mentalhealthdiary.R;
import com.example.mentalhealthdiary.model.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        
        if (message.isLoading()) {
            holder.messageText.setText("正在思考...");
            holder.messageText.setBackgroundResource(R.drawable.chat_bubble_received);
            holder.itemView.setGravity(Gravity.START);
            return;
        }

        holder.messageText.setText(message.getMessage());
        
        if (message.isUser()) {
            holder.messageText.setBackgroundResource(R.drawable.chat_bubble_sent);
            holder.itemView.setGravity(Gravity.END);
        } else {
            holder.messageText.setBackgroundResource(R.drawable.chat_bubble_received);
            holder.itemView.setGravity(Gravity.START);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        LinearLayout itemView;

        ChatViewHolder(View view) {
            super(view);
            messageText = view.findViewById(R.id.messageText);
            itemView = (LinearLayout) view;
        }
    }
} 