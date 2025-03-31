package com.example.mentalhealthdiary.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Entity(tableName = "mood_entries")
public class MoodEntry {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private Date date;
    
    private int moodScore; // 1-5 表示心情等级
    private String diaryContent;
    private String weather;

    public MoodEntry(Date date, int moodScore, String diaryContent, String weather) {
        this.date = date;
        this.moodScore = moodScore;
        this.diaryContent = diaryContent;
        this.weather = weather;
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

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    // 获取日记中的所有图片引用
    public List<String> getImageReferences() {
        List<String> images = new ArrayList<>();
        if (diaryContent == null) return images;
        
        // 使用正则表达式提取图片标记
        Pattern pattern = Pattern.compile("\\[\\[IMG:(.*?)\\]\\]");
        Matcher matcher = pattern.matcher(diaryContent);
        
        while (matcher.find()) {
            images.add(matcher.group(1));
        }
        
        return images;
    }
} 