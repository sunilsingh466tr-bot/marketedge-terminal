package com.example.marketedge.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ThesisNoteDao {
    @Query("SELECT * FROM thesis_notes WHERE symbol = :symbol LIMIT 1")
    fun getNoteForStock(symbol: String): Flow<ThesisNoteEntity?>

    @Query("SELECT * FROM thesis_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<ThesisNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNote(note: ThesisNoteEntity)

    @Query("DELETE FROM thesis_notes WHERE symbol = :symbol")
    suspend fun deleteNote(symbol: String)
}
