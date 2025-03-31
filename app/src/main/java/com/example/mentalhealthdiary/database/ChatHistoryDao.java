package com.example.mentalhealthdiary.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mentalhealthdiary.model.ChatHistory;

import java.util.List;

@Dao
public interface ChatHistoryDao {
    @Query("SELECT * FROM chat_history ORDER BY timestamp DESC")
    LiveData<List<ChatHistory>> getAllHistories();
    
    @Query("SELECT * FROM chat_history ORDER BY timestamp DESC")
    List<ChatHistory> getAllHistoriesSync();
    
    @Query("SELECT * FROM chat_history WHERE id = :id")
    ChatHistory getHistoryById(long id);
    
    @Insert
    long insert(ChatHistory history);
    
    @Update
    void update(ChatHistory history);
    
    @Delete
    void delete(ChatHistory history);
    
    @Query("DELETE FROM chat_history WHERE id IN (:ids)")
    void deleteByIds(List<Long> ids);
    
    @Query("SELECT * FROM chat_history WHERE title LIKE :query ORDER BY timestamp DESC")
    LiveData<List<ChatHistory>> searchHistories(String query);
    
    @Query("SELECT * FROM chat_history WHERE messages IS NULL OR messages = ''")
    List<ChatHistory> getEmptyChats();
    
    @Delete
    void deleteAll(List<ChatHistory> chats);
} 