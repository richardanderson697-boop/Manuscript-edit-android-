package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "character_profiles")
data class CharacterProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val manuscriptId: Long,
    val primaryName: String,
    val aliases: String, // Comma-separated or JSON list
    val role: String, // e.g., Protagonist, Antagonist, Supporting
    val ageTimeline: String, // e.g., "Age 26 in Ch 1; Age 27 in Ch 8"
    val physicalAttributes: String, // e.g., "Amber eyes, dark hair, silver key necklace"
    val notes: String = "",
    val conflictCount: Int = 0
)
