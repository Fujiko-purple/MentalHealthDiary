package com.example.mentalhealthdiary.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mentalhealthdiary.model.MoodEntry;
import java.util.Date;
import java.util.List;

@Dao
public interface MoodEntryDao {
    @Insert
    void insert(MoodEntry entry);

    @Update
    void update(MoodEntry entry);

    @Delete
    void delete(MoodEntry entry);

    @Query("SELECT * FROM mood_entries ORDER BY date DESC")
    LiveData<List<MoodEntry>> getAllEntries();

    @Query("SELECT * FROM mood_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    LiveData<List<MoodEntry>> getEntriesInRange(Date startDate, Date endDate);

    @Query("SELECT AVG(moodScore) FROM mood_entries WHERE date BETWEEN :startDate AND :endDate")
    float getAverageMoodForPeriod(long startDate, long endDate);

    @Query("SELECT * FROM mood_entries WHERE date >= :startDate ORDER BY date DESC")
    LiveData<List<MoodEntry>> getMoodEntriesForLastDays(long startDate);

    /**
     * 获取指定日期范围内的心情记录
     */
    @Query("SELECT * FROM mood_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    List<MoodEntry> getEntriesBetweenDates(Date startDate, Date endDate);

    /**
     * 获取所有心情记录列表（非LiveData版本）
     */
    @Query("SELECT * FROM mood_entries ORDER BY date DESC")
    List<MoodEntry> getAllEntriesAsList();

    /**
     * 获取最近指定天数的心情记录
     * @param days 要获取的天数
     * @return 最近的心情记录列表
     */
    @Query("SELECT * FROM mood_entries WHERE date >= date('now', '-' || :days || ' days') ORDER BY date DESC")
    LiveData<List<MoodEntry>> getRecentEntries(int days);
} 