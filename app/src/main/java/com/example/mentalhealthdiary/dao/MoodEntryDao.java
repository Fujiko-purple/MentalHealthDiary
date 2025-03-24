package com.example.mentalhealthdiary.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mentalhealthdiary.model.MoodEntry;

import java.util.List;

@Dao
public interface MoodEntryDao {
    @Insert
    long insert(MoodEntry entry);

    @Update
    void update(MoodEntry entry);

    @Delete
    void delete(MoodEntry entry);

    @Query("SELECT * FROM mood_entries ORDER BY date DESC")
    LiveData<List<MoodEntry>> getAllEntries();

    @Query("SELECT * FROM mood_entries WHERE id = :id")
    MoodEntry getEntryById(long id);

    @Query("SELECT * FROM mood_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<MoodEntry>> getEntriesBetweenDates(long startDate, long endDate);

    // 添加新方法：获取所有带内容的心情记录
    @Query("SELECT * FROM mood_entries ORDER BY date DESC")
    LiveData<List<MoodEntry>> getAllEntriesWithContent();
} 