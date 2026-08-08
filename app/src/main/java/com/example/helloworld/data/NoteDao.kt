package com.example.yueread.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Update
    suspend fun update(note: Note)

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY id DESC LIMIT 1")
    suspend fun getLatestNote(): Note?

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    @Query("SELECT * FROM notes WHERE notebook = :notebookName ORDER BY timestamp DESC")
    fun getNotesByNotebook(notebookName: String): Flow<List<Note>>

    @Query("UPDATE notes SET notebook = :newName WHERE notebook = :oldName")
    suspend fun updateNotebookName(oldName: String, newName: String)
}
