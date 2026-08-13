package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class InconsistencyType {
    NAME_MISMATCH,
    DATE_CHRONOLOGY,
    PLOT_TIMELINE,
    CHARACTER_TRAIT
}

enum class InconsistencySeverity {
    HIGH,
    MEDIUM,
    LOW
}

enum class InconsistencyStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CUSTOM_FIX
}

@Entity(tableName = "inconsistencies")
data class Inconsistency(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val manuscriptId: Long,
    val chapterId: Long,
    val chapterIndex: Int,
    val type: InconsistencyType,
    val severity: InconsistencySeverity,
    val originalText: String,
    val contextSnippet: String,
    val explanation: String,
    val suggestedFix: String,
    val userCustomFix: String? = null,
    val status: InconsistencyStatus = InconsistencyStatus.PENDING,
    val lineNumber: Int = 1
)
