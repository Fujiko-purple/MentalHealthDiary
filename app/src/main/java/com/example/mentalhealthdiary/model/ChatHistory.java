package com.example.mentalhealthdiary.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.mentalhealthdiary.database.DateConverter;
import java.util.Date;

@Entity(tableName = "chat_histories")
@TypeConverters(DateConverter.class)
public class ChatHistory {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private Date timestamp;
    private String title;
    private String messages;
    
    public ChatHistory(Date timestamp, String title, String messages) {
        this.timestamp = timestamp;
        this.title = title;
        this.messages = messages;
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
} 