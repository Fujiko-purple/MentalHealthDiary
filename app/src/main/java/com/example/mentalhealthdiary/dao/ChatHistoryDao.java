package com.example.mentalhealthdiary.dao;

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
    @Insert
    long insert(ChatHistory chatHistory);
    
    @Update
    void update(ChatHistory chatHistory);
    
    @Delete
    void delete(ChatHistory chatHistory);
    
    @Query("SELECT * FROM chat_histories ORDER BY timestamp DESC")
    LiveData<List<ChatHistory>> getAllHistories();
    
    @Query("SELECT * FROM chat_histories WHERE id = :id")
    ChatHistory getHistoryById(long id);

    @Query("SELECT * FROM chat_histories ORDER BY timestamp DESC")
    List<ChatHistory> getAllHistoriesSync();

    @Query("SELECT * FROM chat_histories WHERE messages = '' OR messages IS NULL")
    List<ChatHistory> getEmptyChats();

    @Delete
    void deleteAll(List<ChatHistory> chats);
} 