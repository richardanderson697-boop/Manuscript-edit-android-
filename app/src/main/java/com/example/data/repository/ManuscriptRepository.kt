package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class ManuscriptRepository(private val db: AppDatabase) {
    val manuscriptDao = db.manuscriptDao()
    val chapterDao = db.chapterDao()
    val inconsistencyDao = db.inconsistencyDao()
    val characterDao = db.characterDao()

    fun getAllManuscripts(): Flow<List<Manuscript>> = manuscriptDao.getAllManuscripts()

    fun getManuscriptById(id: Long): Flow<Manuscript?> = manuscriptDao.getManuscriptById(id)

    fun getChaptersForManuscript(manuscriptId: Long): Flow<List<Chapter>> =
        chapterDao.getChaptersForManuscript(manuscriptId)

    suspend fun getChaptersForManuscriptDirect(manuscriptId: Long): List<Chapter> =
        chapterDao.getChaptersForManuscriptDirect(manuscriptId)

    fun getInconsistenciesForManuscript(manuscriptId: Long): Flow<List<Inconsistency>> =
        inconsistencyDao.getInconsistenciesForManuscript(manuscriptId)

    fun getInconsistenciesForChapter(chapterId: Long): Flow<List<Inconsistency>> =
        inconsistencyDao.getInconsistenciesForChapter(chapterId)

    fun getCharactersForManuscript(manuscriptId: Long): Flow<List<CharacterProfile>> =
        characterDao.getCharactersForManuscript(manuscriptId)

    suspend fun getCharactersForManuscriptDirect(manuscriptId: Long): List<CharacterProfile> =
        characterDao.getCharactersForManuscriptDirect(manuscriptId)

    suspend fun insertManuscript(manuscript: Manuscript): Long = manuscriptDao.insertManuscript(manuscript)

    suspend fun updateManuscript(manuscript: Manuscript) = manuscriptDao.updateManuscript(manuscript)

    suspend fun insertChapters(chapters: List<Chapter>) = chapterDao.insertChapters(chapters)

    suspend fun updateChapter(chapter: Chapter) = chapterDao.updateChapter(chapter)

    suspend fun insertInconsistencies(inconsistencies: List<Inconsistency>) =
        inconsistencyDao.insertInconsistencies(inconsistencies)

    suspend fun updateInconsistency(inconsistency: Inconsistency) =
        inconsistencyDao.updateInconsistency(inconsistency)

    suspend fun deleteInconsistency(id: Long) = inconsistencyDao.deleteInconsistencyById(id)

    suspend fun insertCharacters(characters: List<CharacterProfile>) =
        characterDao.insertCharacters(characters)

    suspend fun updateCharacter(character: CharacterProfile) =
        characterDao.updateCharacter(character)

    suspend fun deleteCharacter(id: Long) = characterDao.deleteCharacterById(id)

    suspend fun deleteManuscript(id: Long) {
        manuscriptDao.deleteManuscriptById(id)
        chapterDao.deleteChaptersByManuscriptId(id)
        inconsistencyDao.deleteInconsistenciesForManuscript(id)
        characterDao.deleteCharactersForManuscript(id)
    }

    suspend fun updateChapterFix(chapterId: Long, newContent: String) {
        val chapter = chapterDao.getChapterById(chapterId)
        if (chapter != null) {
            chapterDao.updateChapter(chapter.copy(modifiedContent = newContent, hasAppliedFixes = true))
        }
    }

    /**
     * Seeds initial sample manuscript "The Clockwork Heirloom" if no manuscript exists yet.
     */
    suspend fun seedSampleManuscriptIfEmpty() {
        val existing = manuscriptDao.getManuscriptByIdDirect(1L)
        if (existing == null) {
            val sampleManuscript = Manuscript(
                id = 1L,
                title = "The Clockwork Heirloom",
                author = "Evelyn V. Sinclair",
                totalChapters = 4,
                totalInconsistenciesCount = 5,
                sourceType = "SAMPLE"
            )
            manuscriptDao.insertManuscript(sampleManuscript)

            val chapter1 = Chapter(
                manuscriptId = 1L,
                chapterIndex = 1,
                title = "Chapter 1: The Silver Key",
                rawContent = """
                    The heavy rain lashed against the leaded glass windows of Blackwood Manor. Inside the study, Lord Richard Sterling stood admiring the pocket watch he had inherited. At twenty-eight years old, Richard possessed an unsettlingly sharp intellect and striking amber eyes that seemed to catch every flickering candle beam.
                    
                    "We must leave for Bristol by Tuesday, October 12th," Richard declared to his butler, Vance. Vance nodded gravely, polishing the silver tray.
                    
                    Lord Richard closed the brass casing with a crisp click. He tucked the key into his vest pocket, unaware that behind the bookcase, Someone was listening.
                """.trimIndent()
            )

            val chapter2 = Chapter(
                manuscriptId = 1L,
                chapterIndex = 2,
                title = "Chapter 2: Shadows in Bristol",
                rawContent = """
                    Upon their arrival in Bristol on Thursday, October 18th, the harbor was shrouded in dense fog. Richard adjusted his high collar.
                    
                    Vance stepped off the carriage first. "My Lord, the informant awaits at the Rusty Anchor."
                    
                    Inside the tavern, Richard met Captain Thorne. Thorne leaned across the oak table, his gaze locking onto Richard's piercing emerald eyes. "You carry your father's heirloom, Lord Stirling," Thorne whispered. "He died for that key in 1884."
                    
                    Richard frowned. His father had died in 1892, not 1884.
                """.trimIndent()
            )

            val chapter3 = Chapter(
                manuscriptId = 1L,
                chapterIndex = 3,
                title = "Chapter 3: The Crypt beneath Blackwood",
                rawContent = """
                    Returning to Blackwood Manor three weeks later, Richard confronted Vance.
                    
                    "Vance! Where is the silver tray you took from Bristol?" Richard demanded.
                    
                    Vance looked bewildered. "My Lord, Vance passed away two winters ago in London, as you well know."
                    
                    Richard backed away, his heart hammering against his ribs. At thirty-five years old, he had never felt so vulnerable.
                """.trimIndent()
            )

            val chapter4 = Chapter(
                manuscriptId = 1L,
                chapterIndex = 4,
                title = "Chapter 4: The Unwinding Mechanism",
                rawContent = """
                    In the subterranean vault, Richard placed the golden key into the ancient clockwork lock. 
                    
                    Vance appeared from the dark corridor behind him. "Careful with the lock, Lord Stirling," Vance advised quietly.
                    
                    The gears turned with a deep metallic groan. Lord Richard Sterling realized that every entry in his journal had been manipulated.
                """.trimIndent()
            )

            val chapters = listOf(chapter1, chapter2, chapter3, chapter4)
            chapterDao.insertChapters(chapters)

            // Seed sample inconsistencies
            val initialInconsistencies = listOf(
                Inconsistency(
                    manuscriptId = 1L,
                    chapterId = 1L, // ch 1
                    chapterIndex = 2,
                    type = InconsistencyType.CHARACTER_TRAIT,
                    severity = InconsistencySeverity.HIGH,
                    originalText = "striking amber eyes",
                    contextSnippet = "Richard possessed an unsettlingly sharp intellect and striking amber eyes ... [In Ch 2: piercing emerald eyes]",
                    explanation = "Lord Richard's eye color changes from Amber in Chapter 1 to Emerald in Chapter 2.",
                    suggestedFix = "striking emerald eyes",
                    lineNumber = 3
                ),
                Inconsistency(
                    manuscriptId = 1L,
                    chapterId = 2L,
                    chapterIndex = 2,
                    type = InconsistencyType.DATE_CHRONOLOGY,
                    severity = InconsistencySeverity.MEDIUM,
                    originalText = "Thursday, October 18th",
                    contextSnippet = "We must leave for Bristol by Tuesday, October 12th ... Upon their arrival in Bristol on Thursday, October 18th",
                    explanation = "Date progression conflict: Tuesday Oct 12th to Thursday Oct 18th leaves an unaccounted gap, or calendar day mismatch for October 1888.",
                    suggestedFix = "Friday, October 15th",
                    lineNumber = 1
                ),
                Inconsistency(
                    manuscriptId = 1L,
                    chapterId = 2L,
                    chapterIndex = 2,
                    type = InconsistencyType.NAME_MISMATCH,
                    severity = InconsistencySeverity.LOW,
                    originalText = "Lord Stirling",
                    contextSnippet = "You carry your father's heirloom, Lord Stirling",
                    explanation = "Spelling mismatch: Character name is spelled 'Lord Sterling' in Chapter 1 & 4, but 'Lord Stirling' in Chapter 2.",
                    suggestedFix = "Lord Sterling",
                    lineNumber = 5
                ),
                Inconsistency(
                    manuscriptId = 1L,
                    chapterId = 3L,
                    chapterIndex = 3,
                    type = InconsistencyType.PLOT_TIMELINE,
                    severity = InconsistencySeverity.HIGH,
                    originalText = "At thirty-five years old",
                    contextSnippet = "At twenty-eight years old, Richard possessed ... [In Ch 3]: At thirty-five years old, he had never felt so vulnerable",
                    explanation = "Character age conflict: Richard is 28 years old in Chapter 1, but suddenly 35 years old three weeks later in Chapter 3.",
                    suggestedFix = "At twenty-eight years old",
                    lineNumber = 8
                ),
                Inconsistency(
                    manuscriptId = 1L,
                    chapterId = 4L,
                    chapterIndex = 4,
                    type = InconsistencyType.PLOT_TIMELINE,
                    severity = InconsistencySeverity.HIGH,
                    originalText = "Vance appeared from the dark corridor behind him.",
                    contextSnippet = "Vance passed away two winters ago ... [In Ch 4]: Vance appeared from the dark corridor behind him.",
                    explanation = "Continuity violation: Chapter 3 states butler Vance died two winters ago, yet he actively speaks and walks in Chapter 4.",
                    suggestedFix = "A shadowy figure appearing like Vance appeared from the dark corridor behind him.",
                    lineNumber = 3
                )
            )
            inconsistencyDao.insertInconsistencies(initialInconsistencies)

            // Seed sample character profiles
            val initialCharacters = listOf(
                CharacterProfile(
                    manuscriptId = 1L,
                    primaryName = "Lord Richard Sterling",
                    aliases = "Richard, Lord Stirling",
                    role = "Protagonist",
                    ageTimeline = "Ch 1: Age 28 | Ch 3: Age 35 [CONFLICT]",
                    physicalAttributes = "Amber eyes (Ch 1) / Emerald eyes (Ch 2), High collar vest",
                    notes = "Heir to Blackwood Manor. Possesses pocket watch key.",
                    conflictCount = 3
                ),
                CharacterProfile(
                    manuscriptId = 1L,
                    primaryName = "Vance",
                    aliases = "The Butler",
                    role = "Supporting",
                    ageTimeline = "Active Ch 1-2 | Passed away in Ch 3 | Reappears Ch 4 [CONFLICT]",
                    physicalAttributes = "Elderly butler, silver tray carrier",
                    notes = "Allegedly died two winters ago in London according to Ch 3.",
                    conflictCount = 2
                ),
                CharacterProfile(
                    manuscriptId = 1L,
                    primaryName = "Captain Thorne",
                    aliases = "Thorne",
                    role = "Supporting",
                    ageTimeline = "Meets Richard in Ch 2 at Rusty Anchor",
                    physicalAttributes = "Weathered sailor, low whisper voice",
                    notes = "Knows history of Richard's father dying in 1884/1892.",
                    conflictCount = 1
                )
            )
            characterDao.insertCharacters(initialCharacters)
        }
    }
}
