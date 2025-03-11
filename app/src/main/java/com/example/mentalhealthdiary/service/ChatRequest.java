package com.example.mentalhealthdiary.service;

import java.util.List;

public class ChatRequest {
    private List<Message> messages;
    private String model;
    private boolean stream = false;
    private double temperature = 0.7;
    private int max_tokens = 2000;

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