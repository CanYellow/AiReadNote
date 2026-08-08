package com.yueread.data

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

    // 获取单个笔记本（用于 CaptureActivity 拼接提示词）
    @Query("SELECT * FROM notebooks WHERE name = :name LIMIT 1")
    suspend fun getNotebookByName(name: String): Notebook?

    // 更新笔记本详情（支持修改主键 name）
    @Query("UPDATE notebooks SET name = :newName, category = :category, systemPrompt = :systemPrompt WHERE name = :oldName")
    suspend fun updateNotebookDetails(oldName: String, newName: String, category: String, systemPrompt: String)
}
