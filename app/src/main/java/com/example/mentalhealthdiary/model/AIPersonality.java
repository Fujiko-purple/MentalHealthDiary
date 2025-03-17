package com.example.mentalhealthdiary.model;

import com.example.mentalhealthdiary.R;

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
    public String getId() { 
        return id; 
    }
    public String getName() { return name; }
    public String getAvatar() { return avatar; }
    public String getDescription() { return description; }
    public String getSystemPrompt() { return systemPrompt; }
    public String getWelcomeMessage() { return welcomeMessage; }
    public String getModelName() {
        return modelName;
    }

    // 新增方法：根据字符串ID获取数字ID
    public static int getNumericId(String strId) {
        switch (strId) {
            case "ganyu_cbt":         // 甘雨
                return 1;
            case "natsume_narrative_pro":  // 夏目
                return 2;
            case "cat_girl":          // 猫娘
                return 3;
            case "kafka_rebt":        // 卡芙卡
                return 4;
            case "tiga_divine":       // 迪迦
                return 5;
            case "yangjian_tactician": // 杨戬
                return 6;
            case "dt_music":
                return 7;              //陶喆
            case "default":           // 默认心理咨询师
                return 0;
            default:
                return 0;
        }
    }

    public static int getAvatarResourceById(int personalityId) {
        String resourceName;
        switch (personalityId) {
            case 0:  // default
                resourceName = "ic_counselor";       // 默认心理咨询师头像
                break;
            case 1:  // ganyu_cbt
                resourceName = "ic_ganyu_counselor"; // 甘雨头像
                break;
            case 2:  // natsume_narrative_pro
                resourceName = "ic_natsume";         // 夏目头像
                break;
            case 3:  // cat_girl
                resourceName = "ic_cat_girl";        // 猫娘头像
                break;
            case 4:  // kafka_rebt
                resourceName = "ic_kafka";           // 卡芙卡头像
                break;
            case 5:  // tiga_divine
                resourceName = "ic_tiga_divine";     // 迪迦头像
                break;
            case 6:  // yangjian_tactician
                resourceName = "ic_yangjian";        // 杨戬头像
                break;
            case 7: //
                resourceName = "ic_davidtao";        //陶喆头像
                break;
            default:
                resourceName = "ic_ai_assistant";    // 默认头像
                break;
        }
        
        try {
            return R.drawable.class.getField(resourceName).getInt(null);
        } catch (Exception e) {
            e.printStackTrace();
            return R.drawable.ic_ai_assistant;
        }
    }
} 