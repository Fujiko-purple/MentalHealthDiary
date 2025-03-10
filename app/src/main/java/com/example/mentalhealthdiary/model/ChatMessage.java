package com.example.mentalhealthdiary.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;
import androidx.room.Index;
import com.google.gson.annotations.SerializedName;

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
        this.personalityId = null;
    }

    @Ignore
    public ChatMessage(String message, boolean isUser) {
        this(message, isUser, false);
    }

    @Ignore
    public static ChatMessage createLoadingMessage() {
        return new ChatMessage("", false, true);
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