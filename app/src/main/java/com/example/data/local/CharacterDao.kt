package com.example.data.local

import androidx.room.*
import com.example.data.model.CharacterProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM character_profiles WHERE manuscriptId = :manuscriptId ORDER BY primaryName ASC")
    fun getCharactersForManuscript(manuscriptId: Long): Flow<List<CharacterProfile>>

    @Query("SELECT * FROM character_profiles WHERE manuscriptId = :manuscriptId ORDER BY primaryName ASC")
    suspend fun getCharactersForManuscriptDirect(manuscriptId: Long): List<CharacterProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterProfile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacters(characters: List<CharacterProfile>)

    @Update
    suspend fun updateCharacter(character: CharacterProfile)

    @Query("DELETE FROM character_profiles WHERE id = :id")
    suspend fun deleteCharacterById(id: Long)

    @Query("DELETE FROM character_profiles WHERE manuscriptId = :manuscriptId")
    suspend fun deleteCharactersForManuscript(manuscriptId: Long)
}
