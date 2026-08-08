package com.yueread

import androidx.room.Dao
import androidx.room.Delete

@Dao
interface NotebookDao {
    @Delete
    suspend fun delete(notebook: Notebook)
}
