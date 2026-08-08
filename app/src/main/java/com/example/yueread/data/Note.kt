package com.example.yueread.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val selectedText: String,
    val thought: String,
    val notebook: String,
    val aiResponse: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
