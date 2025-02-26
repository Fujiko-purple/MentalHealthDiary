package com.example.mentalhealthdiary.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "breathing_sessions")
public class BreathingSession {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public long timestamp;
    
    @ColumnInfo(name = "duration")
    public int duration;  // 确保这是 int 类型

    // 添加一个toString方法用于调试
    @Override
    public String toString() {
        return "BreathingSession{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", duration=" + duration +
                '}';
    }
} 