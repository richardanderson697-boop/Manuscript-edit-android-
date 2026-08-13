package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manuscripts")
data class Manuscript(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val createdAt: Long = System.currentTimeMillis(),
    val totalChapters: Int = 0,
    val totalInconsistenciesCount: Int = 0,
    val sourceType: String = "TXT_FILE", // "TXT_FILE", "PASTE", "GOOGLE_DOCS"
    val sourceUriOrUrl: String? = null
)
