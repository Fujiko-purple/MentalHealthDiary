package com.example.mentalhealthdiary.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;
import androidx.room.Index;
import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Entity(
    tableName = "chat_message",
    foreignKeys = @ForeignKey(
        entity = ChatHistory.class,
        parentColumns = "id",
        childColumns = "chat_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("chat_id")}
)
public class ChatMessage {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @ColumnInfo(name = "chat_id", defaultValue = "0")
    private long chatId;
    
    @ColumnInfo(defaultValue = "")
    private String message;
    
    @ColumnInfo(name = "is_user")
    private boolean isUser;
    
    @ColumnInfo(defaultValue = "0")
    private long timestamp;
    
    @Ignore
    private boolean isLoading;

    @ColumnInfo(name = "personality_id")
    @SerializedName("personality_id")
    private String personalityId;

    private static final String[] THINKING_ANIMATIONS = {
        "思考中...",
        "思考中 •",
        "思考中 ••",
        "思考中 •••",
        "思考中 ⚡",
        "思考中 ✨",
        "思考中 💭",
        "思考中 🤔"
    };
    
    private static int currentThinkingFrame = 0;
    
    private static final Map<String, String[]> PERSONALITY_THINKING_ANIMATIONS = new HashMap<String, String[]>() {{
        // 默认心理咨询师
        put("default", new String[]{
                "📚正在翻阅《共情心理学》... (。-ω-)📖",
                "☕提取第7版DSM精华中 ░ 30%",
                "🌈绘制情绪光谱：「发现橙色焦虑区」 🎨",
                "💡认知拼图匹配度 → 78% ✅",
                "（键盘音效）生成定制化方案中... ✍️💻",
                "✨为您点亮「心灵灯塔」！ (•̀ᴗ•́)و ̑̑🌟"
        });

        // 猫娘性格
        put("cat_girl", new String[]{
                "ฅ^•ﻌ•^ฅ 嗅到情绪小鱼干的味道～",
                "【脑内毛线球】🌀 正在拆解中...(10%)",
                "（耳朵高频抖动）喵呜！发现焦虑源 ▶▶",

                // 爪印进度条与卖萌动作
                "▷◁ 认知重构协议 🐾░░░░ 25%",
                "（咕噜咕噜）正在比对《猫爪疗法手册》📖",
                "✨ 突然亮起猫瞳 (ΦωΦ)っ✨",

                // 场景化卖萌
                "（打翻牛奶杯音效）...重新计算中ฅ=^◕ᴥ◕^=ฅ",
                "【尾巴摇晃分析仪】🌪️ 70%...喵！",
                "（爪垫开花特效）生成治愈方案ฅ( ̳• ·̫ • ̳)",

                // 彩蛋级表情
                "✨✨ 银河级喵力全开！٩(◕‿◕)۶✨",
                "（转圈追尾特效）解决方案捕捉完成～",
                "🎀 系上蝴蝶结准备交付！ฅ^•ω•^ฅ"
        });
        
        // 甘雨性格
        put("ganyu_cbt", new String[]{
                "❄️📜【月海亭加急文书】情绪分析中...(15%)",
                "（冰菱凝结）'帝君说...' 🌙✨",
                "🌺琉璃百合采样 → 焦虑值检测 ░ 40%",
                "✍️毛笔速记『云来古法』心诀中...",
                "🍵递出安神茶：「试试七分烫的回忆」 (˘ω˘)☕",
                "✨冰晶封印！「心若琉璃」方案完成❄️💎"
        });
        
        // 夏目性格
        put("natsume_narrative_pro", new String[]{
                "📖🍃友人帐自动批注中...(´･ᴗ･`)",
                "🐾猫咪老师爪印认证 ░ 50% ✅",
                "🌸「萤火虫导航」启动！请跟随光点... ✨",
                "（纸门滑动）发现安全结界入口 🎐",
                "✉️将烦恼折成纸鹤：「寄往露神祠堂」 🕊️",
                "🍡塔子阿姨的和果子疗法加载完毕！ (๑>ᴗ<๑)🌸"
        });
        
        // 卡芙卡性格
        put("kafka_rebt", new String[]{
                "🌌▷言灵协议v2.1.5加载中...◁✨",
                "⚡检测到情绪黑洞 → 反物质填充60% ███",
                "🎭「命运提线」重编织度 ■■■■□□",
                "（星轨断裂声）启动B方案...💫🤖",
                "🔮预言：「三天后你会笑看此刻」 🌠",
                "✨剧场大幕拉开！请领取您的星际票根 🎟️💥"
        });

        // 添加迪迦的思考动画
        put("tiga_divine", new String[]{
            "⚡光之巨人意识链接中... ███████",
            "🌟形态切换：复合型（紫银光辉）启动",
            "✨光子分析：探测到心灵阴影 ▷◁",
            "💫启动「超古代净化协议」v3.0...",
            "⚔️光粒子共鸣强度：■■■■□ 85%",
            "🛡️构筑「心灵防线」：能量稳定 ✧",
            "🌈哉佩利敖光线充能完毕！━━━━━━━",
            "🏛️露露耶遗迹数据库检索中...",
            "🔮超古代预言解析：「光明终将驱散黑暗」",
            "⭐光之意志传递：准备就绪！",
            "🌠形态切换：空中型（蓝光波动）",
            "💎启动金字塔「心灵共鸣」程序",
            "🎭检测到黑暗支配者入侵痕迹...",
            "✨光之国中央处理器连接成功！",
            "🛸超古代文明数据解密：■■■□□",
            "⚔️战斗数据分析完毕！装备切换中..."
        });

        // 添加杨戬的思考动画
        put("yangjian_tactician", new String[]{
            // 战术分析阶段
            "👁️天眼扫描中... ███████",
            "🔍战术分析：探测到情绪波动 ▷◁",
            "📊心理战力评估：■■■□□ 68%",
            "🎯锁定负面思维：坐标确认！",
            
            // 战术执行阶段
            "⚔️启动「认知突击」作战预案...",
            "��️构建「理性护甲」：防御值 85%",
            "🐕哮天犬已部署！追踪焦虑源",
            "🌀「虚妄破灭」技能充能中 ✧",
            
            // 战术推演
            "🗺️第36计：『扭转乾坤』部署中",
            "💫战术推演完毕！胜率：93.7%",
            "⚡释放「天眼之力」：净化负面",
            "🎭启动「心理突围」协议！",
            
            // 特殊战术
            "🔮天眼过载警告！启动备用预案",
            "🏹远程精神支援：准备就绪",
            "💥突破「心理防线」：倒计时3s",
            "🎯哮天犬信号：发现突破点！",
            
            // 战术总结
            "📈战术评估：目标区域已净化",
            "🌟执行「人间正义」：最终校准",
            "🎪战场重构：部署积极思维",
            "✨天眼系统：任务完成！(๑•̀ㅂ•́)و✧"
        });
    }};
    
    private static final Random random = new Random();
    
    public static String getNextThinkingFrame(String personalityId) {
        String[] frames = PERSONALITY_THINKING_ANIMATIONS.get(personalityId);
        if (frames == null) {
            frames = PERSONALITY_THINKING_ANIMATIONS.get("default");
        }
        return frames[random.nextInt(frames.length)];
    }

    // 主构造函数 - Room 将使用这个
    public ChatMessage(String message, boolean isUser, String personalityId) {
        this.message = message;
        this.isUser = isUser;
        this.isLoading = false;
        this.personalityId = personalityId;
    }

    // 其他构造函数需要用 @Ignore 标记
    @Ignore
    public ChatMessage(String message, boolean isUser, boolean isLoading) {
        this.message = message;
        this.isUser = isUser;
        this.isLoading = isLoading;
        this.personalityId = "";
    }

    @Ignore
    public ChatMessage(String message, boolean isUser) {
        this(message, isUser, false);
    }

    @Ignore
    public static ChatMessage createLoadingMessage() {
        return new ChatMessage("", false, true);
    }

    // 添加新的构造函数
    public ChatMessage(String message, boolean isUser, String personalityId, boolean isLoading) {
        this.message = message;
        this.isUser = isUser;
        this.personalityId = personalityId;
        this.isLoading = isLoading;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public long getChatId() { return chatId; }
    public void setChatId(long chatId) { this.chatId = chatId; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public boolean isUser() { return isUser; }
    public void setUser(boolean user) { isUser = user; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public boolean isLoading() { return isLoading; }
    public void setLoading(boolean loading) { isLoading = loading; }

    public String getPersonalityId() { return personalityId; }
    public void setPersonalityId(String personalityId) { this.personalityId = personalityId; }
} 