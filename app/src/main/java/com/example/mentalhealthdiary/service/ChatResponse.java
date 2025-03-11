package com.example.mentalhealthdiary.service;

public class ChatResponse {
    private String id;
    private String content;
    private String model;
    private long created;
    
    public String getId() { return id; }
    public String getContent() { return content; }
    public String getModel() { return model; }
    public long getCreated() { return created; }
} 