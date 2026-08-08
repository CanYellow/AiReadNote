package com.yueread

import androidx.room.Dao
import androidx.room.Delete
import com.yueread.data.Notebook

@Dao
interface NotebookDao {
    @Delete
    suspend fun delete(notebook: Notebook)
}
