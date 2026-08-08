package com.example.helloworld.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notebooks")
data class Notebook(
    @PrimaryKey val name: String,
    val isActive: Boolean = true,
    val lastUsed: Long = System.currentTimeMillis(),
    val category: String = "默认分类",
    val systemPrompt: String = "" // 新增：笔记本专属提示词
)
