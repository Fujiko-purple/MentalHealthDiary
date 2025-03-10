package com.example.mentalhealthdiary.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import com.example.mentalhealthdiary.model.ChatMessage;
import java.util.List;

@Dao
public interface ChatMessageDao {
    @Insert
    void insert(ChatMessage message);
    
    @Query("SELECT * FROM chat_message WHERE chat_id = :chatId ORDER BY timestamp ASC")
    List<ChatMessage> getMessagesForChat(long chatId);
    
    @Delete
    void delete(ChatMessage message);
    
    @Query("DELETE FROM chat_message WHERE chat_id = :chatId")
    void deleteAllForChat(long chatId);
} 