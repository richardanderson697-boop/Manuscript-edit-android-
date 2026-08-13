package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.remote.Candidate
import com.example.data.remote.GeminiRetrofitClient
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.Content
import com.example.data.remote.Part
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

                // Automatically trigger full cross-chapter scan on newly imported manuscript
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

    /**
     * Appends new chapters to an existing manuscript (e.g. when adding subsequent chapters)
     * and re-runs cross-chapter consistency scan across ALL chapters together.
     */
    fun appendChaptersToManuscript(msId: Long, rawText: String) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanProgressText.value = "Appending chapters & evaluating cross-chapter continuity..."

            try {
                val existingChapters = manuscriptRepository.getChaptersForManuscriptDirect(msId)
                val currentMaxIndex = existingChapters.maxOfOrNull { it.chapterIndex } ?: 0

                val newParsedChapters = parseRawTextIntoChapters(rawText, startingIndex = currentMaxIndex + 1)
                val newEntities = newParsedChapters.mapIndexed { idx, (chTitle, chText) ->
                    Chapter(
                        manuscriptId = msId,
                        chapterIndex = currentMaxIndex + idx + 1,
                        title = chTitle,
                        rawContent = chText
                    )
                }
                manuscriptRepository.insertChapters(newEntities)

                val allChapters = manuscriptRepository.getChaptersForManuscriptDirect(msId)

                val currentMs = activeManuscript.value
                if (currentMs != null) {
                    manuscriptRepository.updateManuscript(
                        currentMs.copy(totalChapters = allChapters.size)
                    )
                }

                // Re-scan ALL chapters in sequence for cross-chapter context consistency
                val foundInconsistencies = proofreaderRepository.scanManuscriptForInconsistencies(msId, allChapters)
                manuscriptRepository.insertInconsistencies(foundInconsistencies)

                val updatedCharacters = proofreaderRepository.extractCharactersWithAI(msId, allChapters)
                manuscriptRepository.insertCharacters(updatedCharacters)

                _notification.value = UiNotification("Appended ${newEntities.size} new chapter(s)! Cross-chapter scan complete (${foundInconsistencies.size} issues found across ${allChapters.size} chapters).")
            } catch (e: Exception) {
                _notification.value = UiNotification("Error adding chapters: ${e.message}", isError = true)
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

    private fun parseRawTextIntoChapters(fullText: String, startingIndex: Int = 1): List<Pair<String, String>> {
        val chapterRegex = Regex("(?i)(?=(?:chapter|ch\\.|act|part)\\s+(?:\\d+|[ivxlcdm]+|one|two|three|four|five|six|seven|eight|nine|ten))")
        val splits = fullText.split(chapterRegex).filter { it.isNotBlank() }

        if (splits.size <= 1) {
            val paragraphs = fullText.split(Regex("\n\\s*\n\\s*\n"))
            if (paragraphs.size > 1) {
                return paragraphs.mapIndexed { idx, p ->
                    "Chapter ${startingIndex + idx}" to p.trim()
                }
            }
            return listOf("Chapter $startingIndex" to fullText.trim())
        }

        return splits.mapIndexed { idx, section ->
            val firstLine = section.trim().lines().firstOrNull() ?: "Chapter ${startingIndex + idx}"
            val chapterTitle = if (firstLine.length < 60) firstLine else "Chapter ${startingIndex + idx}"
            val content = if (section.trim().lines().size > 1) {
                section.trim().lines().drop(1).joinToString("\n").trim()
            } else {
                section.trim()
            }
            chapterTitle to if (content.isBlank()) section.trim() else content
        }
    }

    fun isApiKeyConfigured(): Boolean {
        return GeminiRetrofitClient.hasValidApiKey()
    }

    fun setCustomApiKey(key: String) {
        GeminiRetrofitClient.setCustomApiKey(key)
        _notification.value = UiNotification("Gemini API key updated.")
    }

    fun getCurrentApiKey(): String {
        return GeminiRetrofitClient.getApiKey()
    }

    fun testApiKeyConnection(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val key = GeminiRetrofitClient.getApiKey()
            if (key.isBlank()) {
                onResult(false, "No API key found. Please enter a key.")
                return@launch
            }
            try {
                val dummyRequest = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = "Respond with 'OK' only."))))
                )
                val resp = GeminiRetrofitClient.service.generateContent(key, dummyRequest)
                val candidate: Candidate? = resp.candidates?.firstOrNull()
                val candidateContent: Content? = candidate?.content
                val firstPart: Part? = candidateContent?.parts?.firstOrNull()
                val text: String? = firstPart?.text
                if (!text.isNullOrBlank()) {
                    onResult(true, "Gemini 2.5 Flash connected successfully!")
                } else {
                    onResult(false, "Received empty response from Gemini API.")
                }
            } catch (e: Exception) {
                onResult(false, "Connection error: ${e.message ?: "Invalid key or network failure"}")
            }
        }
    }

    fun deleteCharacter(charId: Long) {
        viewModelScope.launch {
            manuscriptRepository.characterDao.deleteCharacterById(charId)
            _notification.value = UiNotification("Character profile removed.")
        }
    }

    fun createNewBook(
        title: String,
        author: String,
        firstChapterTitle: String,
        firstChapterText: String
    ) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanProgressText.value = "Creating new manuscript..."
            try {
                val newMs = Manuscript(
                    title = if (title.isBlank()) "Untitled Novel" else title.trim(),
                    author = if (author.isBlank()) "Author" else author.trim(),
                    totalChapters = 1,
                    totalInconsistenciesCount = 0,
                    sourceType = "MANUAL_ENTRY"
                )
                val msId = manuscriptRepository.insertManuscript(newMs)
                val firstChapter = Chapter(
                    manuscriptId = msId,
                    chapterIndex = 1,
                    title = if (firstChapterTitle.isBlank()) "Chapter 1" else firstChapterTitle.trim(),
                    rawContent = if (firstChapterText.isBlank()) "Start writing your chapter here..." else firstChapterText.trim()
                )
                manuscriptRepository.insertChapters(listOf(firstChapter))
                _selectedManuscriptId.value = msId

                val chapters = listOf(firstChapter)
                val found = proofreaderRepository.scanManuscriptForInconsistencies(msId, chapters)
                manuscriptRepository.insertInconsistencies(found)

                val chars = proofreaderRepository.extractCharactersWithAI(msId, chapters)
                manuscriptRepository.insertCharacters(chars)

                _notification.value = UiNotification("Created '${newMs.title}' successfully!")
            } catch (e: Exception) {
                _notification.value = UiNotification("Creation error: ${e.message}", isError = true)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun deleteChapter(chapterId: Long) {
        val msId = _selectedManuscriptId.value ?: return
        viewModelScope.launch {
            manuscriptRepository.chapterDao.deleteChapterById(chapterId)
            val remaining = manuscriptRepository.getChaptersForManuscriptDirect(msId)
            val currentMs = activeManuscript.value
            if (currentMs != null) {
                manuscriptRepository.updateManuscript(currentMs.copy(totalChapters = remaining.size))
            }
            // Re-index remaining chapters if needed
            _notification.value = UiNotification("Chapter removed.")
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
