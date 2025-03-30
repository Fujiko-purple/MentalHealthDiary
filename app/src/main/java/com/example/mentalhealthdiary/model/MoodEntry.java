package com.example.mentalhealthdiary.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "mood_entries")
public class MoodEntry {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private Date date;
    
    private int moodScore; // 1-5 表示心情等级
    private String diaryContent;

    public MoodEntry(Date date, int moodScore, String diaryContent) {
        this.date = date;
        this.moodScore = moodScore;
        this.diaryContent = diaryContent;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getMoodScore() {
        return moodScore;
    }



    public String getDiaryContent() {
        return diaryContent;
    }

} 