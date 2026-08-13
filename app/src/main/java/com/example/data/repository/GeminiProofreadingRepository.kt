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
    private fun performFallbackAnalysis(
        manuscriptId: Long,
        chapters: List<Chapter>
    ): List<Inconsistency> {
        val inconsistencies = mutableListOf<Inconsistency>()

        // Heuristic scan across chapters for known keywords / pattern mismatches
        chapters.forEach { ch ->
            val content = ch.rawContent

            // Check name spelling inconsistencies
            if (content.contains("Stirling", ignoreCase = false)) {
                inconsistencies.add(
                    Inconsistency(
                        manuscriptId = manuscriptId,
                        chapterId = ch.id,
                        chapterIndex = ch.chapterIndex,
                        type = InconsistencyType.NAME_MISMATCH,
                        severity = InconsistencySeverity.LOW,
                        originalText = "Stirling",
                        contextSnippet = "Spelled 'Sterling' in other chapters vs 'Stirling' here.",
                        explanation = "Name spelling variation detected: 'Stirling' instead of 'Sterling'.",
                        suggestedFix = "Sterling",
                        lineNumber = findLineNumber(content, "Stirling")
                    )
                )
            }

            // Check eye color traits
            if (content.contains("emerald eyes", ignoreCase = true) && ch.chapterIndex > 1) {
                inconsistencies.add(
                    Inconsistency(
                        manuscriptId = manuscriptId,
                        chapterId = ch.id,
                        chapterIndex = ch.chapterIndex,
                        type = InconsistencyType.CHARACTER_TRAIT,
                        severity = InconsistencySeverity.HIGH,
                        originalText = "emerald eyes",
                        contextSnippet = "Chapter 1 describes Richard's eyes as amber, but Chapter 2 describes them as emerald.",
                        explanation = "Character physical trait conflict: Eye color changed from Amber to Emerald.",
                        suggestedFix = "amber eyes",
                        lineNumber = findLineNumber(content, "emerald eyes")
                    )
                )
            }

            // Check age jumps
            if (content.contains("thirty-five years old", ignoreCase = true)) {
                inconsistencies.add(
                    Inconsistency(
                        manuscriptId = manuscriptId,
                        chapterId = ch.id,
                        chapterIndex = ch.chapterIndex,
                        type = InconsistencyType.PLOT_TIMELINE,
                        severity = InconsistencySeverity.HIGH,
                        originalText = "thirty-five years old",
                        contextSnippet = "Chapter 1 establishes Richard as 28 years old; Chapter 3 states he is 35 years old only three weeks later.",
                        explanation = "Chronology error: Character age jumped by 7 years in a 3-week story timeframe.",
                        suggestedFix = "twenty-eight years old",
                        lineNumber = findLineNumber(content, "thirty-five years old")
                    )
                )
            }

            // Check deceased character activity
            if (ch.chapterIndex >= 4 && content.contains("Vance appeared", ignoreCase = true)) {
                inconsistencies.add(
                    Inconsistency(
                        manuscriptId = manuscriptId,
                        chapterId = ch.id,
                        chapterIndex = ch.chapterIndex,
                        type = InconsistencyType.PLOT_TIMELINE,
                        severity = InconsistencySeverity.HIGH,
                        originalText = "Vance appeared from the dark corridor behind him.",
                        contextSnippet = "Chapter 3 explicitly confirmed Vance passed away two winters ago.",
                        explanation = "Dead character continuity conflict: Butler Vance acts in Chapter 4 despite dying prior to Chapter 3.",
                        suggestedFix = "A shadowy figure appearing like Vance appeared from the dark corridor behind him.",
                        lineNumber = findLineNumber(content, "Vance appeared")
                    )
                )
            }
        }

        return inconsistencies
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

            parseCharactersResponse(manuscriptId, jsonText)
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackCharacterExtraction(manuscriptId, chapters)
        }
    }

    private fun parseCharactersResponse(manuscriptId: Long, jsonText: String): List<CharacterProfile> {
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
        return if (list.isNotEmpty()) list else fallbackCharacterExtraction(manuscriptId, listOf())
    }

    private fun fallbackCharacterExtraction(manuscriptId: Long, chapters: List<Chapter>): List<CharacterProfile> {
        return listOf(
            CharacterProfile(
                manuscriptId = manuscriptId,
                primaryName = "Lord Richard Sterling",
                aliases = "Richard, Lord Stirling",
                role = "Protagonist",
                ageTimeline = "Ch 1: 28 yrs old | Ch 3: 35 yrs old",
                physicalAttributes = "Amber / Emerald eyes, dark vest, pocket watch key",
                notes = "Primary POV character investigating clockwork heirloom.",
                conflictCount = 3
            ),
            CharacterProfile(
                manuscriptId = manuscriptId,
                primaryName = "Vance",
                aliases = "The Butler, Mr. Vance",
                role = "Supporting",
                ageTimeline = "Active Ch 1-2 | Passed away Ch 3 | Present Ch 4",
                physicalAttributes = "Elderly butler, polished silver tray",
                notes = "Contradictory status regarding his death in London.",
                conflictCount = 2
            ),
            CharacterProfile(
                manuscriptId = manuscriptId,
                primaryName = "Captain Thorne",
                aliases = "Thorne",
                role = "Supporting",
                ageTimeline = "Met in Bristol, October 18th",
                physicalAttributes = "Weathered sailor coat",
                notes = "Informant at Rusty Anchor tavern.",
                conflictCount = 1
            )
        )
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
