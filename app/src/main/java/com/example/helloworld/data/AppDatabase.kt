package com.example.yueread.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class, Notebook::class, AiConfig::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun notebookDao(): NotebookDao
    abstract fun aiConfigDao(): AiConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 修改：去掉 private，改为公开
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ai_configs` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`protocol` TEXT NOT NULL, " +
                            "`baseUrl` TEXT NOT NULL, " +
                            "`apiKey` TEXT NOT NULL, " +
                            "`modelName` TEXT NOT NULL, " +
                            "`isActive` INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }

        // 修改：去掉 private，改为公开
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `ai_configs` ADD COLUMN `systemPrompt` TEXT NOT NULL DEFAULT ''")
                // 新增下面这行，为笔记本表也加上 systemPrompt
                database.execSQL("ALTER TABLE `notebooks` ADD COLUMN `systemPrompt` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notes_database"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                // 新增：加回这行作为安全兜底，防止找不到迁移路径时直接闪退
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
