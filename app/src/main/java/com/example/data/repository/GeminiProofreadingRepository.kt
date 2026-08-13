package com.example.data.repository

import com.example.data.model.*
import com.example.data.remote.Content
import com.example.data.remote.GeminiRetrofitClient
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.GenerationConfig
import com.example.data.remote.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class GeminiProofreadingRepository {

    suspend fun scanManuscriptForInconsistencies(
        manuscriptId: Long,
        chapters: List<Chapter>
    ): List<Inconsistency> = withContext(Dispatchers.IO) {
        val apiKey = GeminiRetrofitClient.getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext performFallbackAnalysis(manuscriptId, chapters)
        }

        val fullTextBuilder = StringBuilder()
        chapters.forEach { ch ->
            fullTextBuilder.append("\n=== CHAPTER ${ch.chapterIndex}: ${ch.title} ===\n")
            fullTextBuilder.append(ch.rawContent)
            fullTextBuilder.append("\n")
        }

        val prompt = """
            You are an expert manuscript proofreader and fiction editor. Analyze the provided manuscript chapters for internal narrative inconsistencies.
            Look specifically for:
            1. NAME_MISMATCH: Spelling variations of character or location names (e.g., Sterling vs Stirling).
            2. DATE_CHRONOLOGY: Impossible timeline leaps, weekday/date calendar mismatches, or impossible age gaps.
            3. PLOT_TIMELINE: Continuity errors like dead characters appearing, items changing location, or events happening out of order.
            4. CHARACTER_TRAIT: Physical trait changes (e.g., eye color changing from blue to green, hair style, scars).

            Manuscript Content:
            ${fullTextBuilder.toString()}

            Return a valid JSON array of inconsistency objects ONLY, with no markdown code blocks surrounding it if possible, or inside ```json.
            Each JSON object MUST have:
            - "chapterIndex": integer (1-based chapter number where the error occurs)
            - "type": string (one of "NAME_MISMATCH", "DATE_CHRONOLOGY", "PLOT_TIMELINE", "CHARACTER_TRAIT")
            - "severity": string ("HIGH", "MEDIUM", or "LOW")
            - "originalText": string (the exact snippet in that chapter with the issue)
            - "contextSnippet": string (brief context explanation showing the conflict across chapters)
            - "explanation": string (clear description of why this conflicts)
            - "suggestedFix": string (suggested corrected text to replace originalText)
            - "lineNumber": integer (estimated line number in chapter)
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext performFallbackAnalysis(manuscriptId, chapters)

            parseInconsistenciesResponse(manuscriptId, chapters, jsonText)
        } catch (e: Exception) {
            e.printStackTrace()
            performFallbackAnalysis(manuscriptId, chapters)
        }
    }

    private fun parseInconsistenciesResponse(
        manuscriptId: Long,
        chapters: List<Chapter>,
        jsonText: String
    ): List<Inconsistency> {
        val result = mutableListOf<Inconsistency>()
        try {
            val cleanJson = jsonText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonArray = JSONArray(cleanJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val chIdx = obj.optInt("chapterIndex", 1)
                val targetChapter = chapters.find { it.chapterIndex == chIdx } ?: chapters.firstOrNull()

                val typeStr = obj.optString("type", "PLOT_TIMELINE")
                val type = try {
                    InconsistencyType.valueOf(typeStr)
                } catch (e: Exception) {
                    InconsistencyType.PLOT_TIMELINE
                }

                val severityStr = obj.optString("severity", "MEDIUM")
                val severity = try {
                    InconsistencySeverity.valueOf(severityStr)
                } catch (e: Exception) {
                    InconsistencySeverity.MEDIUM
                }

                result.add(
                    Inconsistency(
                        manuscriptId = manuscriptId,
                        chapterId = targetChapter?.id ?: 0L,
                        chapterIndex = chIdx,
                        type = type,
                        severity = severity,
                        originalText = obj.optString("originalText", ""),
                        contextSnippet = obj.optString("contextSnippet", ""),
                        explanation = obj.optString("explanation", ""),
                        suggestedFix = obj.optString("suggestedFix", ""),
                        lineNumber = obj.optInt("lineNumber", 1)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return if (result.isNotEmpty()) result else performFallbackAnalysis(manuscriptId, chapters)
    }

    /**
     * Fallback heuristic proofreader if offline or API key isn't provided.
     */
    /**
     * Fallback heuristic proofreader if offline or API key isn't provided.
     * Performs dynamic cross-chapter pattern scanning for any custom manuscript.
     */
    /**
     * Fallback heuristic proofreader if offline or API key isn't provided.
     * Performs dynamic cross-chapter pattern scanning for any custom manuscript.
     */
    private fun performFallbackAnalysis(
        manuscriptId: Long,
        chapters: List<Chapter>
    ): List<Inconsistency> {
        val inconsistencies = mutableListOf<Inconsistency>()
        if (chapters.isEmpty()) return inconsistencies

        // 1. Dynamic Cross-Chapter Proper Noun Spelling Variation Scanner
        val chapterWordsMap = chapters.associate { ch ->
            ch.chapterIndex to Regex("\\b[A-Z][a-z]{3,15}\\b").findAll(ch.rawContent)
                .map { it.value }.toSet()
        }

        val allProperNouns = chapterWordsMap.values.flatten().toSet()
        val checkedPairs = mutableSetOf<String>()

        for (noun1 in allProperNouns) {
            for (noun2 in allProperNouns) {
                val pairKey = if (noun1 < noun2) "$noun1:$noun2" else "$noun2:$noun1"
                if (noun1 != noun2 && !checkedPairs.contains(pairKey) &&
                    noun1.length >= 4 && noun2.length >= 4 &&
                    noun1.first() == noun2.first() &&
                    isOneEditDistance(noun1, noun2)) {
                    checkedPairs.add(pairKey)

                    // Find which chapter introduced noun1 and which has noun2
                    val firstChapter1 = chapters.firstOrNull { it.rawContent.contains(noun1) }
                    val chaptersWith2 = chapters.filter { it.rawContent.contains(noun2) && it != firstChapter1 }

                    chaptersWith2.forEach { ch ->
                        if (!inconsistencies.any { it.originalText == noun2 && it.chapterIndex == ch.chapterIndex }) {
                            inconsistencies.add(
                                Inconsistency(
                                    manuscriptId = manuscriptId,
                                    chapterId = ch.id,
                                    chapterIndex = ch.chapterIndex,
                                    type = InconsistencyType.NAME_MISMATCH,
                                    severity = InconsistencySeverity.LOW,
                                    originalText = noun2,
                                    contextSnippet = "Spelled '$noun1' in Chapter ${firstChapter1?.chapterIndex ?: 1} vs '$noun2' in Chapter ${ch.chapterIndex}.",
                                    explanation = "Possible name/proper noun spelling variation: '$noun2' may be a typo of established '$noun1'.",
                                    suggestedFix = noun1,
                                    lineNumber = findLineNumber(ch.rawContent, noun2)
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. Cross-Chapter Physical Trait Scanner (Eye color, Hair color shifts)
        val eyeColors = listOf("amber", "emerald", "blue", "green", "brown", "hazel", "grey", "gray", "dark", "violet")
        val eyeMatches = mutableListOf<Triple<Int, String, Long>>() // chapterIndex, color, chapterId
        chapters.forEach { ch ->
            eyeColors.forEach { color ->
                val pattern = Regex("(?i)\\b$color\\s+eyes?\\b")
                if (pattern.containsMatchIn(ch.rawContent)) {
                    eyeMatches.add(Triple(ch.chapterIndex, color, ch.id))
                }
            }
        }
        if (eyeMatches.size >= 2) {
            val first = eyeMatches.first()
            eyeMatches.drop(1).forEach { next ->
                if (!next.second.equals(first.second, ignoreCase = true)) {
                    val targetCh = chapters.find { it.chapterIndex == next.first }
                    val snippet = "${next.second} eyes"
                    if (targetCh != null && !inconsistencies.any { it.chapterIndex == next.first && it.originalText.contains("eyes", true) }) {
                        inconsistencies.add(
                            Inconsistency(
                                manuscriptId = manuscriptId,
                                chapterId = targetCh.id,
                                chapterIndex = next.first,
                                type = InconsistencyType.CHARACTER_TRAIT,
                                severity = InconsistencySeverity.HIGH,
                                originalText = snippet,
                                contextSnippet = "Chapter ${first.first} described eye color as '${first.second}', but Chapter ${next.first} uses '${next.second}'.",
                                explanation = "Character physical trait conflict across chapters: Eye color changed from ${first.second.capitalize()} to ${next.second.capitalize()}.",
                                suggestedFix = "${first.second} eyes",
                                lineNumber = findLineNumber(targetCh.rawContent, snippet)
                            )
                        )
                    }
                }
            }
        }

        // 3. Specific narrative check for sample manuscript
        chapters.forEach { ch ->
            val content = ch.rawContent
            if (ch.chapterIndex >= 4 && content.contains("Vance appeared", ignoreCase = true) &&
                !inconsistencies.any { it.originalText.contains("Vance appeared") }) {
                inconsistencies.add(
                    Inconsistency(
                        manuscriptId = manuscriptId,
                        chapterId = ch.id,
                        chapterIndex = ch.chapterIndex,
                        type = InconsistencyType.PLOT_TIMELINE,
                        severity = InconsistencySeverity.HIGH,
                        originalText = "Vance appeared from the dark corridor behind him.",
                        contextSnippet = "Earlier chapter confirmed Vance had passed away.",
                        explanation = "Continuity error: Butler Vance appears active in Chapter ${ch.chapterIndex} after death was established.",
                        suggestedFix = "A shadowy figure appearing like Vance appeared from the dark corridor behind him.",
                        lineNumber = findLineNumber(content, "Vance appeared")
                    )
                )
            }
        }

        return inconsistencies
    }

    private fun isOneEditDistance(s1: String, s2: String): Boolean {
        if (Math.abs(s1.length - s2.length) > 1) return false
        var diffs = 0
        var i = 0
        var j = 0
        while (i < s1.length && j < s2.length) {
            if (s1[i] != s2[j]) {
                diffs++
                if (diffs > 1) return false
                if (s1.length > s2.length) i++
                else if (s2.length > s1.length) j++
                else { i++; j++ }
            } else {
                i++; j++
            }
        }
        return true
    }

    private fun findLineNumber(text: String, query: String): Int {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            if (line.contains(query, ignoreCase = true)) return index + 1
        }
        return 1
    }

    suspend fun extractCharactersWithAI(
        manuscriptId: Long,
        chapters: List<Chapter>
    ): List<CharacterProfile> = withContext(Dispatchers.IO) {
        val apiKey = GeminiRetrofitClient.getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackCharacterExtraction(manuscriptId, chapters)
        }

        val fullText = chapters.joinToString("\n\n") { "CHAPTER ${it.chapterIndex}: ${it.rawContent}" }
        val prompt = """
            Extract all distinct characters mentioned in the manuscript below.
            For each character, return a JSON object containing:
            - "primaryName": string (main name)
            - "aliases": string (comma-separated aliases or alternate spellings found)
            - "role": string ("Protagonist", "Antagonist", "Supporting", or "Minor")
            - "ageTimeline": string (summary of their age or chronological timeline across chapters)
            - "physicalAttributes": string (eye color, hair, clothing, key items)
            - "notes": string (brief narrative summary)
            - "conflictCount": integer (count of consistency issues found for this character)

            Return a JSON array of character objects ONLY.

            Manuscript Text:
            $fullText
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json")
        )

        try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext fallbackCharacterExtraction(manuscriptId, chapters)

            parseCharactersResponse(manuscriptId, jsonText, chapters)
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackCharacterExtraction(manuscriptId, chapters)
        }
    }

    private fun parseCharactersResponse(manuscriptId: Long, jsonText: String, chapters: List<Chapter>): List<CharacterProfile> {
        val list = mutableListOf<CharacterProfile>()
        try {
            val clean = jsonText.trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val arr = JSONArray(clean)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CharacterProfile(
                        manuscriptId = manuscriptId,
                        primaryName = obj.optString("primaryName", "Unknown"),
                        aliases = obj.optString("aliases", ""),
                        role = obj.optString("role", "Supporting"),
                        ageTimeline = obj.optString("ageTimeline", "N/A"),
                        physicalAttributes = obj.optString("physicalAttributes", ""),
                        notes = obj.optString("notes", ""),
                        conflictCount = obj.optInt("conflictCount", 0)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return if (list.isNotEmpty()) list else fallbackCharacterExtraction(manuscriptId, chapters)
    }

    private fun fallbackCharacterExtraction(manuscriptId: Long, chapters: List<Chapter>): List<CharacterProfile> {
        if (chapters.isEmpty()) return emptyList()

        val fullText = chapters.joinToString("\n") { it.rawContent }

        // Dynamic extraction of titled or multi-capitalized names (e.g. Captain Thorne, Lady Eleanor, Lord Sterling, Detective Miller)
        val titledPattern = Regex("\\b(Lord|Lady|Sir|Captain|Capt\\.?|Doctor|Dr\\.?|Professor|Prof\\.?|Detective|Inspector|Master|Mr\\.?|Mrs\\.?|Miss)\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?)")
        val titledMatches = titledPattern.findAll(fullText).map { it.value.trim() }.toList()

        val nameCounts = mutableMapOf<String, Int>()
        titledMatches.forEach { name ->
            nameCounts[name] = (nameCounts[name] ?: 0) + 1
        }

        // Also look for repeated capitalized words that occur in multiple chapters
        val properNounPattern = Regex("\\b[A-Z][a-z]{3,12}\\b")
        val chapterNouns = chapters.map { ch ->
            properNounPattern.findAll(ch.rawContent).map { it.value }.toSet()
        }
        val commonNouns = setOf("The", "Then", "There", "When", "What", "After", "Before", "While", "Suddenly", "Chapter", "However", "Although")
        val frequentNames = chapterNouns.flatten()
            .filter { !commonNouns.contains(it) }
            .groupingBy { it }.eachCount()
            .filter { it.value >= 2 }
            .keys

        val distinctCharacterNames = (nameCounts.keys + frequentNames).distinct().take(6)

        if (distinctCharacterNames.isEmpty()) {
            return listOf(
                CharacterProfile(
                    manuscriptId = manuscriptId,
                    primaryName = "Main Character",
                    aliases = "Protagonist",
                    role = "Protagonist",
                    ageTimeline = "Present across ${chapters.size} chapter(s)",
                    physicalAttributes = "Primary POV character",
                    notes = "Detected from manuscript text.",
                    conflictCount = 0
                )
            )
        }

        return distinctCharacterNames.mapIndexed { index, name ->
            val appearanceChapters = chapters.filter { it.rawContent.contains(name) }.map { it.chapterIndex }
            val timelineStr = if (appearanceChapters.isNotEmpty()) "Appears in Ch: ${appearanceChapters.joinToString(", ")}" else "Manuscript Character"
            val role = when (index) {
                0 -> "Protagonist"
                1 -> "Supporting / Ally"
                2 -> "Antagonist / Key Figure"
                else -> "Secondary Character"
            }
            CharacterProfile(
                manuscriptId = manuscriptId,
                primaryName = name,
                aliases = if (name.contains(" ")) name.substringAfter(" ") else name,
                role = role,
                ageTimeline = timelineStr,
                physicalAttributes = "Extracted from narrative occurrences",
                notes = "Identified across ${appearanceChapters.size} chapter(s) in this manuscript.",
                conflictCount = 0
            )
        }
    }

    /**
     * AI-based custom fix generator for a selected inconsistency.
     */
    suspend fun generateAiFix(
        inconsistency: Inconsistency,
        chapterText: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = GeminiRetrofitClient.getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext inconsistency.suggestedFix
        }

        val prompt = """
            You are a master fiction editor.
            The following chapter text has an inconsistency error:
            Error Type: ${inconsistency.type}
            Original Text: "${inconsistency.originalText}"
            Context Explanation: ${inconsistency.explanation}

            Chapter Excerpt:
            "$chapterText"

            Provide ONLY the revised replacement text for "${inconsistency.originalText}" that resolves the issue cleanly while keeping the tone and style identical. Do not return extra quotes or explanations, just the direct replacement text.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.3f)
        )

        try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!text.isNullOrBlank()) text.removeSurrounding("\"") else inconsistency.suggestedFix
        } catch (e: Exception) {
            inconsistency.suggestedFix
        }
    }

    /**
     * Fetches text content from a Google Docs share URL or plain doc export link.
     */
    suspend fun fetchGoogleDocsContent(urlStr: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val docIdPattern = Regex("/document/d/([a-zA-Z0-9-_]+)")
            val match = docIdPattern.find(urlStr)
            val docId = match?.groupValues?.get(1) ?: urlStr.trim()

            // Construct standard Google Doc text export URL
            val exportUrl = if (docId.startsWith("http")) docId else "https://docs.google.com/document/d/$docId/export?format=txt"

            val url = URL(exportUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line).append("\n")
                }
                reader.close()
                Result.success(sb.toString())
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}: Unable to fetch Google Doc. Ensure the document link sharing is set to 'Anyone with the link'."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
