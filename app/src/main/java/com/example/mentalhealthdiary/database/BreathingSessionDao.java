package com.example.mentalhealthdiary.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface BreathingSessionDao {
    @Insert
    void insert(BreathingSession session);

    @Query("SELECT * FROM breathing_sessions ORDER BY timestamp DESC")
    List<BreathingSession> getAllSessions();

    @Query("SELECT COALESCE(SUM(CAST(duration AS INTEGER)), 0) FROM breathing_sessions")
    int getTotalDuration();

    @Query("SELECT COUNT(*) FROM breathing_sessions")
    int getTotalSessions();

    @Delete
    void delete(BreathingSession session);

    @Query("DELETE FROM breathing_sessions")
    void deleteAll();
} 