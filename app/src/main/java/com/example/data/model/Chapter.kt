package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapters")
data class Chapter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val manuscriptId: Long,
    val chapterIndex: Int,
    val title: String,
    val rawContent: String,
    val modifiedContent: String = rawContent,
    val hasAppliedFixes: Boolean = false
)
