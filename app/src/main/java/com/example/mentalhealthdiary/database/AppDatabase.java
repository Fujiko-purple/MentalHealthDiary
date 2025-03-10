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

@Database(
    entities = {
        MoodEntry.class, 
        BreathingSession.class, 
        ChatHistory.class
    }, 
    version = 3,
    exportSchema = false
)
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "mood_diary_db";
    private static AppDatabase instance;

    public abstract MoodEntryDao moodEntryDao();
    public abstract BreathingSessionDao breathingSessionDao();
    public abstract ChatHistoryDao chatHistoryDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `chat_histories` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`timestamp` INTEGER, " +
                "`title` TEXT, " +
                "`messages` TEXT)"
            );
        }
    };
} 