package com.example.mentalhealthdiary.model;

public class ChatMessage {
    private String message;
    private boolean isUser;
    private boolean isLoading;

    public ChatMessage(String message, boolean isUser, boolean isLoading) {
        this.message = message;
        this.isUser = isUser;
        this.isLoading = isLoading;
    }

    public ChatMessage(String message, boolean isUser) {
        this(message, isUser, false);
    }

    public static ChatMessage createLoadingMessage() {
        return new ChatMessage("", false, true);
    }

    public String getMessage() {
        return message;
    }

    public boolean isUser() {
        return isUser;
    }

    public boolean isLoading() {
        return isLoading;
    }
} 