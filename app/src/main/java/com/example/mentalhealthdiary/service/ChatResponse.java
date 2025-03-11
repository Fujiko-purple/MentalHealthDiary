package com.example.mentalhealthdiary.service;

import java.util.List;

public class ChatResponse {
    public String id;
    public String object;
    public long created;
    public String model;
    public List<Choice> choices;
    public Usage usage;

    public static class Choice {
        public Message message;
        public String finish_reason;
        public int index;
    }

    public static class Message {
        public String role;
        public String content;
    }

    public static class Usage {
        public int prompt_tokens;
        public int completion_tokens;
        public int total_tokens;
    }
} 