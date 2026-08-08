package com.example.helloworld.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotebookDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(notebook: Notebook)

    @Update
    suspend fun update(notebook: Notebook)

    // 获取激活的笔记本，按最近使用时间倒序（解决弹窗记忆和排序问题）
    @Query("SELECT * FROM notebooks WHERE isActive = 1 ORDER BY lastUsed DESC")
    fun getActiveNotebooks(): Flow<List<Notebook>>

    // 获取所有笔记本（用于管理页面）
    @Query("SELECT * FROM notebooks ORDER BY lastUsed DESC")
    fun getAllNotebooks(): Flow<List<Notebook>>
}
