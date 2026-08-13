package com.example.data.local

import androidx.room.*
import com.example.data.model.Manuscript
import kotlinx.coroutines.flow.Flow

@Dao
interface ManuscriptDao {
    @Query("SELECT * FROM manuscripts ORDER BY createdAt DESC")
    fun getAllManuscripts(): Flow<List<Manuscript>>

    @Query("SELECT * FROM manuscripts WHERE id = :id")
    fun getManuscriptById(id: Long): Flow<Manuscript?>

    @Query("SELECT * FROM manuscripts WHERE id = :id")
    suspend fun getManuscriptByIdDirect(id: Long): Manuscript?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManuscript(manuscript: Manuscript): Long

    @Update
    suspend fun updateManuscript(manuscript: Manuscript)

    @Query("DELETE FROM manuscripts WHERE id = :id")
    suspend fun deleteManuscriptById(id: Long)
}
