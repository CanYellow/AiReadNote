package com.yueread.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AiConfigDao {
    @Insert
    suspend fun insert(config: AiConfig)

    @Update
    suspend fun update(config: AiConfig)

    // 新增：删除配置
    @Delete
    suspend fun delete(config: AiConfig)

    @Query("SELECT * FROM ai_configs")
    fun getAllConfigs(): Flow<List<AiConfig>>

    @Query("SELECT * FROM ai_configs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveConfig(): AiConfig?

    @Query("UPDATE ai_configs SET isActive = 0")
    suspend fun deactivateAll()
}
