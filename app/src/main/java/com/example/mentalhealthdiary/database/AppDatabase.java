package com.example.mentalhealthdiary.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.mentalhealthdiary.model.MoodEntry;
import com.example.mentalhealthdiary.database.BreathingSession;
import com.example.mentalhealthdiary.model.ChatHistory;
import com.example.mentalhealthdiary.dao.ChatHistoryDao;
import com.example.mentalhealthdiary.model.ChatMessage;
import com.example.mentalhealthdiary.dao.ChatMessageDao;

@Database(
    entities = {
        MoodEntry.class, 
        BreathingSession.class, 
        ChatHistory.class,
        ChatMessage.class
    }, 
    version = 5,
    exportSchema = false
)
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "mood_diary_db";
    private static AppDatabase instance;

    public abstract MoodEntryDao moodEntryDao();
    public abstract BreathingSessionDao breathingSessionDao();
    public abstract ChatHistoryDao chatHistoryDao();
    public abstract ChatMessageDao chatMessageDao();

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DROP INDEX IF EXISTS index_chat_message_chat_id");
            database.execSQL("DROP TABLE IF EXISTS chat_message");
            database.execSQL("DROP TABLE IF EXISTS chat_history");
            
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `chat_history` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`timestamp` INTEGER NOT NULL DEFAULT 0, " +
                "`title` TEXT, " +
                "`messages` TEXT)"
            );
            
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `chat_message` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`chat_id` INTEGER NOT NULL DEFAULT 0, " +
                "`message` TEXT DEFAULT '', " +
                "`is_user` INTEGER NOT NULL DEFAULT 0, " +
                "`timestamp` INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(`chat_id`) REFERENCES `chat_history`(`id`) ON DELETE CASCADE)"
            );
            
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_chat_message_chat_id` " +
                "ON `chat_message` (`chat_id`)"
            );
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class, 
                "mental_health_diary.db"
            )
            .fallbackToDestructiveMigration()
            .build();
        }
        return instance;
    }
} 