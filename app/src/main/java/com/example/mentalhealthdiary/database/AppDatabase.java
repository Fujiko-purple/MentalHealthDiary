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
        ChatHistory.class,
        ChatMessage.class,
        BreathingSession.class
    },
    version = 2,
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

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS chat_history_new " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`timestamp` INTEGER, " +
                "`title` TEXT, " +
                "`messages` TEXT, " +
                "`personality_id` INTEGER NOT NULL DEFAULT 0)");

            database.execSQL("INSERT INTO chat_history_new " +
                "SELECT * FROM chat_history");

            database.execSQL("DROP TABLE chat_history");

            database.execSQL("ALTER TABLE chat_history_new RENAME TO chat_history");
        }
    };

    private static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS chat_history_new " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`timestamp` INTEGER, " +
                "`title` TEXT, " +
                "`messages` TEXT, " +
                "`personality_id` TEXT)");

            database.execSQL("INSERT INTO chat_history_new (id, timestamp, title, messages, personality_id) " +
                "SELECT id, timestamp, title, messages, CAST(personality_id AS TEXT) FROM chat_history");

            database.execSQL("DROP TABLE chat_history");

            database.execSQL("ALTER TABLE chat_history_new RENAME TO chat_history");
        }
    };

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // thinkingStartTime 是一个临时字段，不需要存储到数据库
            // 所以这里不需要实际的数据库修改
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                "mental_health_db"
            )
            .addMigrations(MIGRATION_1_2)
            .build();
        }
        return instance;
    }
} 