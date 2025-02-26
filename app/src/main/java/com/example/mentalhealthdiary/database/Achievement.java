package com.example.mentalhealthdiary.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "achievements")
public class Achievement {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String title;
    public String description;
    public boolean unlocked;
    public long unlockedTime;
} 