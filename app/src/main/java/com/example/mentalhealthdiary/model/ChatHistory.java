package com.example.mentalhealthdiary.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.TypeConverters;

import com.example.mentalhealthdiary.database.DateConverter;
import java.util.Date;

@Entity(tableName = "chat_history")
public class ChatHistory {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @TypeConverters(DateConverter.class)
    private Date timestamp;
    
    private String title;
    private String messages;
    
    @ColumnInfo(name = "personality_id")
    private String personalityId;
    
    public ChatHistory(Date timestamp, String title, String messages, String personalityId) {
        this.timestamp = timestamp;
        this.title = title;
        this.messages = messages;
        this.personalityId = personalityId;
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getMessages() {
        return messages;
    }
    
    public void setMessages(String messages) {
        this.messages = messages;
    }
    
    public String getPersonalityId() {
        return personalityId;
    }
    
    public void setPersonalityId(String personalityId) {
        this.personalityId = personalityId;
    }
} 