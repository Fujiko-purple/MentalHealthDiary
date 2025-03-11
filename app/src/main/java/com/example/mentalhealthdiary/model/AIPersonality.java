package com.example.mentalhealthdiary.model;

public class AIPersonality {
    private String id;          // 性格唯一标识
    private String name;        // 性格名称
    private String avatar;      // 头像资源名称
    private String description; // 性格描述
    private String systemPrompt; // 系统提示词
    private String welcomeMessage; // 欢迎消息
    private String modelName;   // 假设已有 modelName 字段

    public AIPersonality(String id, String name, String avatar, 
                        String description, String systemPrompt, 
                        String welcomeMessage, String modelName) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
        this.description = description;
        this.systemPrompt = systemPrompt;
        this.welcomeMessage = welcomeMessage;
        this.modelName = modelName;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getAvatar() { return avatar; }
    public String getDescription() { return description; }
    public String getSystemPrompt() { return systemPrompt; }
    public String getWelcomeMessage() { return welcomeMessage; }
    public String getModelName() {
        return modelName;
    }
} 