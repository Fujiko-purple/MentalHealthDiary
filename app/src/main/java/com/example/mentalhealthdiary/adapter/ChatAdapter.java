package com.example.mentalhealthdiary.adapter;

import android.os.Handler;
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

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_MESSAGE = 0;
    private static final int TYPE_LOADING = 1;
    private List<ChatMessage> messages;
    private Handler loadingAnimationHandler = new Handler();
    private int loadingDots = 0;
    private TextView currentLoadingView;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        return message.isLoading() ? TYPE_LOADING : TYPE_MESSAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_loading, parent, false);
            return new LoadingViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message, parent, false);
            return new MessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        
        if (holder.getItemViewType() == TYPE_LOADING) {
            LoadingViewHolder loadingHolder = (LoadingViewHolder) holder;
            startLoadingAnimation(loadingHolder.loadingDots);
        } else {
            MessageViewHolder messageHolder = (MessageViewHolder) holder;
            if (message.isLoading()) {
                messageHolder.messageText.setText("正在思考...");
                messageHolder.messageText.setBackgroundResource(R.drawable.chat_bubble_received);
                messageHolder.itemView.setGravity(Gravity.START);
                return;
            }

            messageHolder.messageText.setText(message.getMessage());
            
            if (message.isUser()) {
                messageHolder.messageText.setBackgroundResource(R.drawable.chat_bubble_sent);
                messageHolder.itemView.setGravity(Gravity.END);
            } else {
                messageHolder.messageText.setBackgroundResource(R.drawable.chat_bubble_received);
                messageHolder.itemView.setGravity(Gravity.START);
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.getItemViewType() == TYPE_LOADING) {
            stopLoadingAnimation();
        }
    }

    private void startLoadingAnimation(TextView dotsView) {
        currentLoadingView = dotsView;
        loadingDots = 0;
        updateLoadingDots();
    }

    private void stopLoadingAnimation() {
        loadingAnimationHandler.removeCallbacksAndMessages(null);
        currentLoadingView = null;
    }

    private void updateLoadingDots() {
        if (currentLoadingView != null) {
            String dots = "";
            for (int i = 0; i < loadingDots; i++) {
                dots += ".";
            }
            currentLoadingView.setText(dots);
            loadingDots = (loadingDots + 1) % 4;
            loadingAnimationHandler.postDelayed(this::updateLoadingDots, 500);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        TextView loadingDots;

        LoadingViewHolder(View view) {
            super(view);
            loadingDots = view.findViewById(R.id.loadingDots);
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        LinearLayout itemView;

        MessageViewHolder(View view) {
            super(view);
            messageText = view.findViewById(R.id.messageText);
            itemView = (LinearLayout) view;
        }
    }
} 