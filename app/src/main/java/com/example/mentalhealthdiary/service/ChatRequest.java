package com.example.mentalhealthdiary.service;

import java.util.List;
import java.util.Map;

public class ChatRequest {
    public List<Message> messages;
    public String model;
    public boolean stream = false;
    public double temperature = 0.7;
    public int max_tokens = 2000;
    public String stop = null;
    public double presence_penalty = 0;
    public double frequency_penalty = 0;
    public Map<String, Double> logit_bias = null;
    public String user = null;

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
        
        public String getRole() { return role; }
        public String getContent() { return content; }
    }
    
    public List<Message> getMessages() { return messages; }
    public String getModel() { return model; }
    public boolean isStream() { return stream; }
    public double getTemperature() { return temperature; }
    public int getMax_tokens() { return max_tokens; }
} 