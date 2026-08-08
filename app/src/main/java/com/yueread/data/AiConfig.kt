package com.yueread.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_configs")
data class AiConfig(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val protocol: String,
    val baseUrl: String,
    val apiKey: String,
    val modelName: String,
    val isActive: Boolean = false,
    val systemPrompt: String = "" // 新增：AI 全局提示词
)
