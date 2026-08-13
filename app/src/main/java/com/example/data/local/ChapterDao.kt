package com.example.data.local

import androidx.room.*
import com.example.data.model.Chapter
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE manuscriptId = :manuscriptId ORDER BY chapterIndex ASC")
    fun getChaptersForManuscript(manuscriptId: Long): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE manuscriptId = :manuscriptId ORDER BY chapterIndex ASC")
    suspend fun getChaptersForManuscriptDirect(manuscriptId: Long): List<Chapter>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: Long): Chapter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: Chapter): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<Chapter>)

    @Update
    suspend fun updateChapter(chapter: Chapter)

    @Query("DELETE FROM chapters WHERE manuscriptId = :manuscriptId")
    suspend fun deleteChaptersByManuscriptId(manuscriptId: Long)
}
