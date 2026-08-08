package com.yueread

import androidx.room.Dao
import androidx.room.Query

@Dao
interface NoteDao {
    @Query("DELETE FROM notes WHERE notebookId = :notebookId")
    suspend fun deleteNotesByNotebookId(notebookId: Long)
}
