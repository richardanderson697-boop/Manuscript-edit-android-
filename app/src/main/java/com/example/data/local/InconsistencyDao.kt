package com.example.data.local

import androidx.room.*
import com.example.data.model.Inconsistency
import kotlinx.coroutines.flow.Flow

@Dao
interface InconsistencyDao {
    @Query("SELECT * FROM inconsistencies WHERE manuscriptId = :manuscriptId ORDER BY chapterIndex ASC, lineNumber ASC")
    fun getInconsistenciesForManuscript(manuscriptId: Long): Flow<List<Inconsistency>>

    @Query("SELECT * FROM inconsistencies WHERE chapterId = :chapterId ORDER BY lineNumber ASC")
    fun getInconsistenciesForChapter(chapterId: Long): Flow<List<Inconsistency>>

    @Query("SELECT * FROM inconsistencies WHERE manuscriptId = :manuscriptId")
    suspend fun getInconsistenciesForManuscriptDirect(manuscriptId: Long): List<Inconsistency>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInconsistency(inconsistency: Inconsistency): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInconsistencies(inconsistencies: List<Inconsistency>)

    @Update
    suspend fun updateInconsistency(inconsistency: Inconsistency)

    @Query("DELETE FROM inconsistencies WHERE manuscriptId = :manuscriptId")
    suspend fun deleteInconsistenciesForManuscript(manuscriptId: Long)

    @Query("DELETE FROM inconsistencies WHERE id = :id")
    suspend fun deleteInconsistencyById(id: Long)
}
