package com.example.mentalhealthdiary.api.model;

import java.util.List;

public class ChatRequest {
    private List<Message> messages;
    private String model;
    private boolean stream = false;

    // 构造函数
    public ChatRequest(List<Message> messages, String model) {
        this.messages = messages;
        this.model = model;
    }

    public static class Message {
        public String role;
        public String content;
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
} 