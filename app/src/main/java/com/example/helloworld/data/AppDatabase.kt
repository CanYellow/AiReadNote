package com.example.helloworld.data

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

        // 定义从版本 4 到 5 的迁移逻辑（新建 ai_configs 表）
        private val MIGRATION_4_5 = object : Migration(4, 5) {
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

        // 定义从版本 5 到 6 的迁移逻辑（ai_configs 表新增 systemPrompt 字段）
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `ai_configs` ADD COLUMN `systemPrompt` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notes_database"
                )
                // 移除 .fallbackToDestructiveMigration()，替换为添加迁移脚本
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
