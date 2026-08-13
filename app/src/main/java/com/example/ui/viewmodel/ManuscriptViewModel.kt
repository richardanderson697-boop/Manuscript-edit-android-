package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.GeminiProofreadingRepository
import com.example.data.repository.ManuscriptRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiNotification(
    val message: String,
    val isError: Boolean = false
)

class ManuscriptViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val manuscriptRepository = ManuscriptRepository(db)
    val proofreaderRepository = GeminiProofreadingRepository()

    val allManuscripts: StateFlow<List<Manuscript>> = manuscriptRepository.getAllManuscripts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedManuscriptId = MutableStateFlow<Long?>(1L)
    val selectedManuscriptId: StateFlow<Long?> = _selectedManuscriptId.asStateFlow()

    val activeManuscript: StateFlow<Manuscript?> = _selectedManuscriptId
        .flatMapLatest { id ->
            if (id != null) manuscriptRepository.getManuscriptById(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeChapters: StateFlow<List<Chapter>> = _selectedManuscriptId
        .flatMapLatest { id ->
            if (id != null) manuscriptRepository.getChaptersForManuscript(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeInconsistencies: StateFlow<List<Inconsistency>> = _selectedManuscriptId
        .flatMapLatest { id ->
            if (id != null) manuscriptRepository.getInconsistenciesForManuscript(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCharacters: StateFlow<List<CharacterProfile>> = _selectedManuscriptId
        .flatMapLatest { id ->
            if (id != null) manuscriptRepository.getCharactersForManuscript(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedChapterIndex = MutableStateFlow<Int>(0) // 0 means "All Chapters (Full Manuscript)", 1+ means Chapter Index
    val selectedChapterIndex: StateFlow<Int> = _selectedChapterIndex.asStateFlow()

    private val _inconsistencyFilterType = MutableStateFlow<InconsistencyType?>(null)
    val inconsistencyFilterType: StateFlow<InconsistencyType?> = _inconsistencyFilterType.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgressText = MutableStateFlow("")
    val scanProgressText: StateFlow<String> = _scanProgressText.asStateFlow()

    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    init {
        viewModelScope.launch {
            manuscriptRepository.seedSampleManuscriptIfEmpty()
        }
    }

    fun selectManuscript(id: Long) {
        _selectedManuscriptId.value = id
        _selectedChapterIndex.value = 0
    }

    fun selectChapterIndex(index: Int) {
        _selectedChapterIndex.value = index
    }

    fun setInconsistencyFilter(type: InconsistencyType?) {
        _inconsistencyFilterType.value = type
    }

    fun dismissNotification() {
        _notification.value = null
    }

    fun scanActiveManuscript() {
        val msId = _selectedManuscriptId.value ?: return
        viewModelScope.launch {
            _isScanning.value = true
            _scanProgressText.value = "Analyzing narrative, dates, and character traits..."
            try {
                val chapters = manuscriptRepository.getChaptersForManuscriptDirect(msId)
                if (chapters.isNotEmpty()) {
                    val found = proofreaderRepository.scanManuscriptForInconsistencies(msId, chapters)
                    manuscriptRepository.inconsistencyDao.deleteInconsistenciesForManuscript(msId)
                    manuscriptRepository.insertInconsistencies(found)

                    // Also extract character profiles
                    val characters = proofreaderRepository.extractCharactersWithAI(msId, chapters)
                    if (characters.isNotEmpty()) {
                        manuscriptRepository.characterDao.deleteCharactersForManuscript(msId)
                        manuscriptRepository.insertCharacters(characters)
                    }

                    val ms = manuscriptRepository.manuscriptDao.getManuscriptByIdDirect(msId)
                    if (ms != null) {
                        manuscriptRepository.updateManuscript(
                            ms.copy(totalInconsistenciesCount = found.size)
                        )
                    }

                    _notification.value = UiNotification("AI Proofread complete! Found ${found.size} inconsistencies.")
                }
            } catch (e: Exception) {
                _notification.value = UiNotification("Scan error: ${e.message}", isError = true)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun acceptInconsistency(inc: Inconsistency) {
        viewModelScope.launch {
            val replacement = inc.suggestedFix
            applyReplacementToChapter(inc, replacement, InconsistencyStatus.ACCEPTED)
        }
    }

    fun applyCustomFix(inc: Inconsistency, customText: String) {
        viewModelScope.launch {
            applyReplacementToChapter(inc, customText, InconsistencyStatus.CUSTOM_FIX, customText)
        }
    }

    fun rejectInconsistency(inc: Inconsistency) {
        viewModelScope.launch {
            manuscriptRepository.updateInconsistency(inc.copy(status = InconsistencyStatus.REJECTED))
            _notification.value = UiNotification("Inconsistency marked as rejected.")
        }
    }

    fun aiGenerateAlternativeFix(inc: Inconsistency, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val chapter = activeChapters.value.find { it.id == inc.chapterId }
            val chapterText = chapter?.modifiedContent ?: chapter?.rawContent ?: ""
            val fix = proofreaderRepository.generateAiFix(inc, chapterText)
            onResult(fix)
        }
    }

    private suspend fun applyReplacementToChapter(
        inc: Inconsistency,
        replacementText: String,
        newStatus: InconsistencyStatus,
        customFixValue: String? = null
    ) {
        val chapter = activeChapters.value.find { it.id == inc.chapterId || it.chapterIndex == inc.chapterIndex }
        if (chapter != null) {
            val currentContent = chapter.modifiedContent
            if (currentContent.contains(inc.originalText)) {
                val updatedContent = currentContent.replaceFirst(inc.originalText, replacementText)
                manuscriptRepository.updateChapterFix(chapter.id, updatedContent)

                manuscriptRepository.updateInconsistency(
                    inc.copy(status = newStatus, userCustomFix = customFixValue)
                )
                _notification.value = UiNotification("Fix applied to Chapter ${chapter.chapterIndex}!")
            } else {
                // If text exact match wasn't found directly, update status anyway
                manuscriptRepository.updateInconsistency(
                    inc.copy(status = newStatus, userCustomFix = customFixValue)
                )
                _notification.value = UiNotification("Fix recorded for Chapter ${chapter.chapterIndex}.")
            }
        }
    }

    fun updateChapterContentManually(chapterId: Long, newText: String) {
        viewModelScope.launch {
            manuscriptRepository.updateChapterFix(chapterId, newText)
            _notification.value = UiNotification("Chapter updated manually.")
        }
    }

    fun saveCharacterProfile(character: CharacterProfile) {
        viewModelScope.launch {
            manuscriptRepository.insertCharacters(listOf(character))
            _notification.value = UiNotification("Character profile saved.")
        }
    }

    fun deleteCharacterProfile(characterId: Long) {
        viewModelScope.launch {
            manuscriptRepository.deleteCharacter(characterId)
            _notification.value = UiNotification("Character profile removed.")
        }
    }

    fun importManuscriptFromText(title: String, author: String, rawFullText: String, sourceType: String = "TXT_FILE") {
        viewModelScope.launch {
            _isScanning.value = true
            _scanProgressText.value = "Parsing chapters and importing manuscript..."

            try {
                val parsedChapters = parseRawTextIntoChapters(rawFullText)
                val newMs = Manuscript(
                    title = if (title.isBlank()) "Untitled Manuscript" else title,
                    author = if (author.isBlank()) "Anonymous" else author,
                    totalChapters = parsedChapters.size,
                    totalInconsistenciesCount = 0,
                    sourceType = sourceType
                )
                val msId = manuscriptRepository.insertManuscript(newMs)

                val chapterEntities = parsedChapters.mapIndexed { idx, (chTitle, chText) ->
                    Chapter(
                        manuscriptId = msId,
                        chapterIndex = idx + 1,
                        title = chTitle,
                        rawContent = chText
                    )
                }
                manuscriptRepository.insertChapters(chapterEntities)
                _selectedManuscriptId.value = msId

                // Automatically trigger scan on newly imported manuscript
                val insertedChapters = manuscriptRepository.getChaptersForManuscriptDirect(msId)
                val foundInconsistencies = proofreaderRepository.scanManuscriptForInconsistencies(msId, insertedChapters)
                manuscriptRepository.insertInconsistencies(foundInconsistencies)

                val characters = proofreaderRepository.extractCharactersWithAI(msId, insertedChapters)
                manuscriptRepository.insertCharacters(characters)

                manuscriptRepository.updateManuscript(
                    newMs.copy(id = msId, totalInconsistenciesCount = foundInconsistencies.size)
                )

                _notification.value = UiNotification("Imported '${newMs.title}' with ${parsedChapters.size} chapters and ${foundInconsistencies.size} initial findings!")
            } catch (e: Exception) {
                _notification.value = UiNotification("Import error: ${e.message}", isError = true)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun importFromGoogleDocs(url: String, title: String, author: String) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanProgressText.value = "Connecting to Google Docs..."
            val result = proofreaderRepository.fetchGoogleDocsContent(url)
            result.onSuccess { text ->
                importManuscriptFromText(
                    title = if (title.isNotBlank()) title else "Google Doc Manuscript",
                    author = if (author.isNotBlank()) author else "Google Docs Author",
                    rawFullText = text,
                    sourceType = "GOOGLE_DOCS"
                )
            }.onFailure { err ->
                _notification.value = UiNotification(err.message ?: "Failed to import from Google Docs", isError = true)
                _isScanning.value = false
            }
        }
    }

    private fun parseRawTextIntoChapters(fullText: String): List<Pair<String, String>> {
        val chapterRegex = Regex("(?i)(?=(?:chapter|ch\\.|act)\\s+(?:\\d+|[ivxlcdm]+))")
        val splits = fullText.split(chapterRegex).filter { it.isNotBlank() }

        if (splits.size <= 1) {
            // If no explicit "Chapter X" keyword found, attempt splitting by double empty lines or word chunking
            val paragraphs = fullText.split(Regex("\n\\s*\n\\s*\n"))
            if (paragraphs.size > 1) {
                return paragraphs.mapIndexed { idx, p ->
                    "Chapter ${idx + 1}" to p.trim()
                }
            }
            return listOf("Chapter 1" to fullText.trim())
        }

        return splits.mapIndexed { idx, section ->
            val firstLine = section.trim().lines().firstOrNull() ?: "Chapter ${idx + 1}"
            val chapterTitle = if (firstLine.length < 50) firstLine else "Chapter ${idx + 1}"
            val content = if (section.trim().lines().size > 1) {
                section.trim().lines().drop(1).joinToString("\n").trim()
            } else {
                section.trim()
            }
            chapterTitle to if (content.isBlank()) section.trim() else content
        }
    }

    fun deleteManuscript(msId: Long) {
        viewModelScope.launch {
            manuscriptRepository.deleteManuscript(msId)
            val all = manuscriptRepository.manuscriptDao.getAllManuscripts().firstOrNull() ?: emptyList()
            _selectedManuscriptId.value = all.firstOrNull()?.id
            _notification.value = UiNotification("Manuscript deleted.")
        }
    }
}
