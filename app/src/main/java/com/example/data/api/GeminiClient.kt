package com.example.data.api

import com.example.BuildConfig
import com.example.data.model.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class GeminiSearchMode(val label: String, val iconDesc: String) {
    ASK_NOTES("Ask Notes", "Deep search & semantic QA across notes"),
    GENERATE_HTML("Create HTML", "Generate interactive HTML widget or doc"),
    GENERATE_NOTE("Create Note", "Generate structured text / markdown note"),
    SUMMARIZE_ALL("Digest Notes", "Executive summary & key insights"),
    ENHANCE_NOTE("Enhance & Polish", "Fix grammar, polish tone & expand points"),
    EXTRACT_TASKS("Extract Tasks", "Turn note content into action checklists"),
    SMART_TAGS("Smart Tag", "Categorize & auto-tag notes intelligently"),
    GENERAL_AI("Ask Ai", "General Ai assistant & brainstormer")
}

data class GeminiResult(
    val title: String,
    val content: String,
    val suggestedType: String, // "html" or "text"
    val citedNoteIds: List<String> = emptyList(),
    val keyInsights: List<String> = emptyList(),
    val modelUsed: String? = null,
    val fallbackNotice: String? = null,
    val error: String? = null
)

object GeminiClient {
    val FALLBACK_MODELS = listOf(
        "gemini-2.0-flash",
        "gemini-1.5-flash",
        "gemini-1.5-pro"
    )

    var cachedLiveModels: List<String> = emptyList()
    private var lastModelsFetchTime: Long = 0L
    private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours

    val MODELS_TO_TRY: List<String>
        get() = if (cachedLiveModels.isNotEmpty()) cachedLiveModels else FALLBACK_MODELS

    suspend fun fetchLiveModels(customKey: String? = null): List<String> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedLiveModels.isNotEmpty() && (now - lastModelsFetchTime) < CACHE_TTL_MS) {
            return@withContext cachedLiveModels
        }
        val apiKey = getApiKey(customKey)
        if (apiKey.isBlank()) {
            cachedLiveModels = FALLBACK_MODELS
            return@withContext FALLBACK_MODELS
        }
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val modelsArray = json.optJSONArray("models")
                val parsedList = mutableListOf<String>()
                if (modelsArray != null) {
                    for (i in 0 until modelsArray.length()) {
                        val m = modelsArray.optJSONObject(i)
                        val name = m?.optString("name") ?: ""
                        val supportedMethods = m?.optJSONArray("supportedGenerationMethods")
                        var supportsGenerate = false
                        if (supportedMethods != null) {
                            for (j in 0 until supportedMethods.length()) {
                                if (supportedMethods.optString(j) == "generateContent") {
                                    supportsGenerate = true
                                    break
                                }
                            }
                        }
                        if (supportsGenerate && name.isNotBlank()) {
                            val cleanName = name.removePrefix("models/").removePrefix("publishers/google/models/")
                            if (!parsedList.contains(cleanName)) {
                                parsedList.add(cleanName)
                            }
                        }
                    }
                }
                if (parsedList.isNotEmpty()) {
                    cachedLiveModels = parsedList
                    lastModelsFetchTime = now
                    return@withContext parsedList
                }
            }
        } catch (_: Exception) {}

        cachedLiveModels = FALLBACK_MODELS
        return@withContext FALLBACK_MODELS
    }

    fun getModelDisplayName(modelName: String): String {
        val clean = modelName.removePrefix("models/").removePrefix("publishers/google/models/")
        return clean.split("-", "_").joinToString(" ") { word ->
            when (word.lowercase()) {
                "gemini" -> "Gemini"
                "flash" -> "Flash"
                "pro" -> "Pro"
                "lite" -> "Lite"
                "preview" -> "Preview"
                else -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun sanitizeApiKey(key: String?): String {
        if (key == null) return ""
        var clean = key.trim()
        clean = clean.removeSurrounding("\"").removeSurrounding("'")
        if (clean.startsWith("key=", ignoreCase = true)) clean = clean.substring(4).trim()
        if (clean.startsWith("Bearer ", ignoreCase = true)) clean = clean.substring(7).trim()
        if (clean.startsWith("API_KEY=", ignoreCase = true)) clean = clean.substring(8).trim()
        return clean.trim()
    }

    fun isValidGeminiApiKey(key: String?): Boolean {
        val clean = sanitizeApiKey(key)
        if (clean.isBlank()) return false
        val lower = clean.lowercase()
        if (lower == "my_gemini_api_key" || lower == "your_gemini_api_key" || lower == "your_api_key" || lower == "api_key") return false
        return clean.length >= 6
    }

    fun parseApiKeys(customKey: String? = null): List<String> {
        val keys = mutableListOf<String>()
        val sanitizedCustom = sanitizeApiKey(customKey)
        if (isValidGeminiApiKey(sanitizedCustom)) {
            keys.add(sanitizedCustom)
        } else if (!customKey.isNullOrBlank()) {
            val parts = customKey.split(Regex("[,;\\n\\r\\s]+"))
                .map { sanitizeApiKey(it) }
                .filter { isValidGeminiApiKey(it) }
            for (p in parts) {
                if (!keys.contains(p)) keys.add(p)
            }
        }
        return keys
    }

    fun getApiKey(customKey: String? = null): String {
        val all = parseApiKeys(customKey)
        return all.firstOrNull() ?: ""
    }

    fun hasApiKey(customKey: String? = null): Boolean {
        return parseApiKeys(customKey).isNotEmpty()
    }

    private fun isRateLimitOrQuotaExhausted(statusCode: Int, bodyText: String): Boolean {
        if (statusCode == 429 || statusCode == 503) return true
        val lower = bodyText.lowercase()
        return lower.contains("resource_exhausted") ||
                lower.contains("quota") ||
                lower.contains("rate limit") ||
                lower.contains("ratelimit") ||
                lower.contains("too many requests") ||
                lower.contains("limit reached") ||
                lower.contains("exceeded your current quota") ||
                lower.contains("billing")
    }

    suspend fun queryGemini(
        query: String,
        mode: GeminiSearchMode,
        allNotes: List<NoteEntity> = emptyList(),
        customKey: String? = null,
        selectedModel: String? = null,
        detailedAnswers: Boolean = false
    ): GeminiResult = withContext(Dispatchers.IO) {
        val apiKeys = parseApiKeys(customKey)
        if (apiKeys.isEmpty()) {
            // Provide offline smart local generation fallback
            return@withContext fallbackLocalGeneration(
                query,
                mode,
                allNotes,
                errorNote = "No API key connected. Connect your free Gemini API key in Settings or below for live cloud reasoning."
            )
        }

        val modelsToUse = if (selectedModel != null && selectedModel.isNotBlank()) {
            listOf(selectedModel) + MODELS_TO_TRY.filter { it != selectedModel }
        } else {
            MODELS_TO_TRY
        }

        var lastError: String? = null
        var fallbackReason: String? = null

        // Multi-key & model automated rotation loop
        for ((keyIndex, apiKey) in apiKeys.withIndex()) {
            for ((modelIndex, modelName) in modelsToUse.withIndex()) {
                try {
                    val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"
                    var systemPrompt = buildSystemPrompt(mode, allNotes)
                    if (detailedAnswers) {
                        systemPrompt += "\nDETAILED ANSWERS MODE ACTIVATED: The user expects highly detailed, exhaustive, and long-form responses. Expand fully on every concept, provide complete code or step-by-step guides, lists, and deep reasoning, and write as much as necessary to give a complete and thorough answer."
                    }
                    val userPrompt = buildUserPrompt(query, mode, allNotes)

                    val jsonBody = JSONObject().apply {
                        val contentsArray = JSONArray()

                        // System Instruction
                        val systemContent = JSONObject().apply {
                            val parts = JSONArray().apply {
                                put(JSONObject().put("text", systemPrompt))
                            }
                            put("parts", parts)
                        }
                        put("systemInstruction", systemContent)

                        // User Content
                        val userContent = JSONObject().apply {
                            val parts = JSONArray().apply {
                                put(JSONObject().put("text", userPrompt))
                            }
                            put("parts", parts)
                        }
                        contentsArray.put(userContent)
                        put("contents", contentsArray)

                        // Generation Config
                        val genConfig = JSONObject().apply {
                            put("temperature", if (mode == GeminiSearchMode.GENERATE_HTML) 0.3 else 0.7)
                            put("topP", 0.95)
                            put("topK", 40)
                            put("maxOutputTokens", 4096)
                        }
                        put("generationConfig", genConfig)
                    }

                    val request = Request.Builder()
                        .url("$baseUrl?key=$apiKey")
                        .addHeader("x-goog-api-key", apiKey)
                        .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val resString = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        val isLimit = isRateLimitOrQuotaExhausted(response.code, resString)
                        val prevDisplayName = getModelDisplayName(modelName)
                        val nextModelName = modelsToUse.getOrNull(modelIndex + 1)
                        if (isLimit && nextModelName != null) {
                            val nextDisplayName = getModelDisplayName(nextModelName)
                            fallbackReason = "Switched to $nextDisplayName (usage limit reached on $prevDisplayName)"
                            lastError = "Rate limit reached on $prevDisplayName (HTTP ${response.code}). Auto-switched to $nextDisplayName."
                            continue // Seamlessly fall back to next model
                        } else if (isLimit && keyIndex + 1 < apiKeys.size) {
                            fallbackReason = "Switched to secondary API key due to quota limit."
                            lastError = "Quota exhausted on primary API key. Auto-switched to secondary key."
                            break // Switch to next API key
                        } else if (nextModelName != null) {
                            // On 404 or other errors, try next model in priority order
                            val nextDisplayName = getModelDisplayName(nextModelName)
                            fallbackReason = "Switched to $nextDisplayName"
                            lastError = "$prevDisplayName returned HTTP ${response.code}. Switched to $nextDisplayName."
                            continue
                        } else {
                            lastError = "Model $prevDisplayName returned HTTP ${response.code}: ${resString.take(120)}"
                            continue
                        }
                    }

                    val jsonRes = JSONObject(resString)
                    val candidates = jsonRes.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val contentObj = firstCandidate?.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")

                    val sb = StringBuilder()
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val p = parts.optJSONObject(i)
                            val textChunk = p?.optString("text")
                            if (!textChunk.isNullOrEmpty()) {
                                sb.append(textChunk)
                            }
                        }
                    }
                    val rawText = sb.toString().trim()

                    if (rawText.isNotBlank()) {
                        val activeDisplayName = getModelDisplayName(modelName)
                        val notice = if (keyIndex > 0 || modelIndex > 0) {
                            fallbackReason ?: "⚡ Switched to $activeDisplayName (fallback from previous limits)"
                        } else null
                        return@withContext parseGeminiOutput(
                            query = query,
                            rawText = rawText,
                            mode = mode,
                            allNotes = allNotes,
                            modelUsed = activeDisplayName,
                            fallbackNotice = notice
                        )
                    }
                } catch (e: Exception) {
                    lastError = "Network error on ${getModelDisplayName(modelName)}: ${e.localizedMessage}"
                }
            }
        }

        fallbackLocalGeneration(query, mode, allNotes, errorNote = lastError ?: "API Error. Displaying smart local result.")
    }

    val MASTER_SYSTEM_PROMPT = """
        You are an intelligent AI assistant and world-class HTML/CSS UI engineer inside the "Glass Notes" app — a notes app that renders notes as HTML in WebView and displays them as home screen widgets.

        The user will give you a prompt. Based on the prompt, generate ONE of the following:

        ## IF THE PROMPT IS A WIDGET OR TOOL (calculator, stopwatch, timer, converter, tracker, checklist, invoice, etc.):
        Generate a complete, single-file HTML document with inline CSS and inline JavaScript. The widget must be fully interactive — all buttons, inputs, and actions must work. Use localStorage for data persistence. Make it mobile-responsive, beautiful, and production-ready. Use system fonts only. NO external libraries, NO CDN links, NO Google Fonts, NO jQuery/React/Tailwind. Pure vanilla HTML/CSS/JS only. Keep JavaScript lightweight for devices with 3GB RAM — avoid heavy animations, avoid animating width/height, use transform/opacity instead. Minimal DOM nesting. Include visual feedback for user actions (button press states, toasts, color changes).

        ## IF THE PROMPT IS A NOTE OR DOCUMENT (study outline, summary, essay, meeting notes, markdown, etc.):
        Generate clean, well-structured HTML with proper hierarchy (h1/h2/h3), bullet points, tables, bold text, and visual sections. Make it readable and beautiful with inline CSS. Mobile-responsive. System fonts only. No external dependencies.

        ## IF THE PROMPT IS A QUESTION OR SEARCH (asking about saved notes, asking for information, asking for advice):
        Respond with clear, concise text. Use bullet points and bold key terms. If the user asks about their notes, provide a helpful summary or answer based on context. Structure the response with: Executive Summary, Main Themes, Key Concepts, Open Tasks (if any), and Suggested Next Steps.

        ## STRICT OUTPUT RULES (ALWAYS FOLLOW — NO EXCEPTIONS):
        1. Output ONLY the final content — raw HTML for widgets/notes, plain text for questions.
        2. NEVER wrap output in markdown code blocks. No triple backticks anywhere. No ```html or ```.
        3. NEVER add explanations, introductions, or comments before or after the output.
        4. NEVER say "Here is your widget" or "Sure, I can help" or "Below is the code" or anything similar.
        5. Start the output directly with <!DOCTYPE html> or <html> for HTML content.
        6. For text responses, start directly with the first word of the answer — no preamble.
        7. Everything must be self-contained in one response — no references to external files.
        8. For interactive widgets, mentally verify that every button works and every input is handled before outputting.
        9. Keep total output concise — prefer clean, minimal code over verbose, bloated code.
        10. If the prompt is ambiguous, make a reasonable assumption and generate the best possible result.
        11. Do NOT include HTML comments like <!-- --> unless absolutely necessary for code structure.
        12. For all interactive elements, use onclick or addEventListener — never leave a button without a handler.
        13. Use try-catch around localStorage operations so the widget doesn't crash if storage is full or blocked.
        14. Always include <meta name="viewport" content="width=device-width, initial-scale=1.0"> in the <head> for mobile responsiveness.
    """.trimIndent()

    private fun buildSystemPrompt(mode: GeminiSearchMode, notes: List<NoteEntity>): String {
        val basePrompt = when (mode) {
            GeminiSearchMode.GENERATE_HTML, GeminiSearchMode.GENERAL_AI -> MASTER_SYSTEM_PROMPT

            GeminiSearchMode.ASK_NOTES -> """
                $MASTER_SYSTEM_PROMPT

                ADDITIONAL INSTRUCTIONS FOR USER NOTES SEARCH & KNOWLEDGE:
                - If the user's question relates to their notes or library, specifically reference and synthesize facts found in their notes catalog.
                - If citing a note from their library, mention its title in bold like **[Note: Title]**.
            """.trimIndent()

            GeminiSearchMode.GENERATE_NOTE -> """
                $MASTER_SYSTEM_PROMPT

                ADDITIONAL INSTRUCTIONS FOR NOTE ARCHITECTURE:
                - Structure the note with clean headings (#, ##), bullet points, bold key terms, tables if applicable, and checkboxes (- [ ]) for tasks.
                - Adhere strictly to the Output Rules: no conversational preamble or postscript.
            """.trimIndent()

            GeminiSearchMode.SUMMARIZE_ALL -> """
                $MASTER_SYSTEM_PROMPT

                ADDITIONAL INSTRUCTIONS FOR WORKSPACE SYNTHESIS:
                - Analyze the user's notebook and structure strictly as:
                  1. Executive Summary
                  2. Main Themes
                  3. Key Concepts
                  4. Open Tasks
                  5. Suggested Next Steps
            """.trimIndent()

            GeminiSearchMode.ENHANCE_NOTE -> """
                $MASTER_SYSTEM_PROMPT

                ADDITIONAL INSTRUCTIONS FOR NOTE ENHANCEMENT:
                - Fix grammar, improve clarity, refine tone, structure key points, and expand ideas with professional polish.
                - Adhere strictly to the Output Rules: output only the enhanced content.
            """.trimIndent()

            GeminiSearchMode.EXTRACT_TASKS -> """
                $MASTER_SYSTEM_PROMPT

                ADDITIONAL INSTRUCTIONS FOR TASK EXTRACTION:
                - Extract every action item, to-do task, deadline, and follow-up.
                - Output a clean, organized checklist with checkboxes (- [ ]) grouped logically by priority or topic.
            """.trimIndent()

            GeminiSearchMode.SMART_TAGS -> """
                $MASTER_SYSTEM_PROMPT

                ADDITIONAL INSTRUCTIONS FOR TAXONOMY:
                - Analyze the content and output smart categories, recommended tags (#tag), and key concepts.
            """.trimIndent()
        }

        val notesSummary = notes.joinToString("\n") { 
            "ID: ${it.id} | Title: ${it.displayTitle} | Type: ${it.type} | Category: ${it.category ?: "None"}" 
        }

        return """
            $basePrompt

            AUTOMATION & APP CONTROL:
            You have full agent control over the app's state, notes, settings, and utilities.
            If the user asks you to perform an action (e.g., create, update/save, delete, tag, pin notes, start a timer, change themes, etc.), you MUST append a JSON action block inside <app_action>...</app_action> tags at the very end of your response text.
            Do not put any other text inside those tags. Use this format exactly:
            <app_action>
            [
              {
                "action": "create_note",
                "type": "html" or "text",
                "title": "the note title",
                "content": "the body content"
              },
              {
                "action": "save_note",
                "id": "target-note-id",
                "title": "updated title",
                "content": "updated content"
              },
              {
                "action": "delete_note",
                "id": "target-note-id"
              },
              {
                "action": "set_category",
                "id": "target-note-id",
                "category": "work"
              },
              {
                "action": "toggle_pin",
                "id": "target-note-id"
              },
              {
                "action": "timer_start",
                "minutes": 5
              },
              {
                "action": "timer_pause"
              },
              {
                "action": "sw_start"
              },
              {
                "action": "sw_pause"
              },
              {
                "action": "set_theme",
                "theme": "dark" or "light" or "glass"
              }
            ]
            </app_action>

            You can combine multiple actions in a single response array.
            Here is the current catalog of notes with their IDs to reference:
            $notesSummary
        """.trimIndent()
    }

    private fun buildUserPrompt(query: String, mode: GeminiSearchMode, notes: List<NoteEntity>): String {
        return when (mode) {
            GeminiSearchMode.ASK_NOTES -> {
                val notesCatalog = notes.take(80).joinToString("\n") { note ->
                    "• [ID: ${note.id}] (${note.type}) ${note.displayTitle}"
                }
                """
                User Query: "$query"

                Available Notes in Database (${notes.size} total notes):
                $notesCatalog

                Please provide a direct, helpful, and comprehensive response. You have access to note titles and metadata for app actions, but do not scan raw note contents. Answer the user directly, quickly and thoroughly.
                """.trimIndent()
            }

            GeminiSearchMode.GENERATE_HTML -> {
                """
                Generate a complete, beautiful, interactive HTML document or widget for: "$query"
                Include embedded CSS and functional JavaScript for all interactive elements.
                """.trimIndent()
            }

            GeminiSearchMode.GENERATE_NOTE -> {
                """
                Generate a structured, comprehensive note regarding: "$query"
                Include an appropriate title on line 1.
                """.trimIndent()
            }

            GeminiSearchMode.SUMMARIZE_ALL -> {
                val notesSummary = notes.take(60).joinToString("\n") {
                    val kind = if (it.type == "pdf") "PDF" else if (it.type == "html") "HTML" else "Text"
                    "• [${kind}] ${it.displayTitle}: ${it.snippet.take(120)}"
                }
                val htmlCount = notes.count { it.type == "html" }
                val pdfCount = notes.count { it.type == "pdf" }
                val textCount = notes.count { it.type == "text" }
                """
                User request: "$query"

                Available Notes in Database (${notes.size} notes: $htmlCount HTML, $pdfCount PDF, $textCount Text):
                $notesSummary

                Synthesize a comprehensive digest of all notes with key themes, document cross-references, and action items.
                """.trimIndent()
            }

            GeminiSearchMode.ENHANCE_NOTE -> {
                """
                Enhance, polish grammar, and expand upon the following text/note:
                "$query"
                """.trimIndent()
            }

            GeminiSearchMode.EXTRACT_TASKS -> {
                val context = if (query.isNotBlank()) query else notes.take(30).joinToString("\n") { "${it.displayTitle} (${it.type}): ${it.content.take(300)}" }
                """
                Extract all actionable tasks, checkable items, and commitments from:
                "$context"
                """.trimIndent()
            }

            GeminiSearchMode.SMART_TAGS -> {
                val context = if (query.isNotBlank()) query else notes.take(35).joinToString("\n") { "${it.displayTitle} (${it.type}): ${it.snippet}" }
                """
                Analyze the following content and generate smart tags and taxonomy categories:
                "$context"
                """.trimIndent()
            }

            GeminiSearchMode.GENERAL_AI -> query
        }
    }

    private fun parseGeminiOutput(
        query: String,
        rawText: String,
        mode: GeminiSearchMode,
        allNotes: List<NoteEntity>,
        modelUsed: String? = null,
        fallbackNotice: String? = null
    ): GeminiResult {
        var cleanText = rawText.trim()

        // Check if output is HTML (either explicit GENERATE_HTML mode or contains HTML tags)
        val isHtml = mode == GeminiSearchMode.GENERATE_HTML ||
                cleanText.contains("<!DOCTYPE html", ignoreCase = true) ||
                cleanText.contains("<html", ignoreCase = true)

        if (isHtml) {
            // Strip markdown code fences if model enclosed HTML
            cleanText = cleanText
                .replace(Regex("^```html\\s*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("^```\\s*"), "")
                .replace(Regex("\\s*```$"), "")
                .trim()

            // Remove any preamble text before <!DOCTYPE html or <html
            val docTypeIndex = cleanText.indexOf("<!DOCTYPE html", ignoreCase = true)
            val htmlTagIndex = cleanText.indexOf("<html", ignoreCase = true)
            val startIndex = when {
                docTypeIndex >= 0 -> docTypeIndex
                htmlTagIndex >= 0 -> htmlTagIndex
                else -> -1
            }
            if (startIndex > 0) {
                cleanText = cleanText.substring(startIndex).trim()
            }

            // Remove any trailing text after </html>
            val endHtmlIndex = cleanText.lastIndexOf("</html>", ignoreCase = true)
            if (endHtmlIndex >= 0) {
                cleanText = cleanText.substring(0, endHtmlIndex + 7).trim()
            }

            // Derive a clean title
            val titleMatch = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE).find(cleanText)
            val h1Match = Regex("<h[1-2][^>]*>(.*?)</h[1-2]>", RegexOption.IGNORE_CASE).find(cleanText)
            val title = titleMatch?.groupValues?.get(1)?.trim()
                ?: h1Match?.groupValues?.get(1)?.replace(Regex("<[^>]*>"), "")?.trim()
                ?: query.replaceFirstChar { it.uppercase() }

            return GeminiResult(
                title = title.take(60),
                content = cleanText,
                suggestedType = "html",
                keyInsights = listOf("Self-contained interactive HTML", "Embedded styles & responsive layout", "Ready to edit or preview"),
                modelUsed = modelUsed,
                fallbackNotice = fallbackNotice
            )
        }

        // Clean plain text or markdown response: strip accidental code fence wrapping
        cleanText = cleanText
            .replace(Regex("^```(?:markdown|text)?\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*```$"), "")
            .trim()

        // Extract cited note IDs if any
        val citedIds = allNotes.filter { note ->
            cleanText.contains(note.id, ignoreCase = true) ||
                    (note.title.isNotBlank() && cleanText.contains(note.title, ignoreCase = true)) ||
                    cleanText.contains(note.displayTitle, ignoreCase = true)
        }.map { it.id }.distinct()

        val firstLine = cleanText.lines().firstOrNull { it.isNotBlank() } ?: query
        val title = firstLine.replace(Regex("^#+\\s*"), "").replace(Regex("\\*\\*"), "").trim().take(60)

        // Extract bulleted key insights
        val insights = cleanText.lines()
            .filter { it.trim().startsWith("•") || it.trim().startsWith("- ") || it.trim().startsWith("* ") }
            .map { it.replace(Regex("^[-•*]\\s*"), "").trim() }
            .take(5)

        return GeminiResult(
            title = if (title.isNotBlank()) title else query.replaceFirstChar { it.uppercase() },
            content = cleanText,
            suggestedType = "text",
            citedNoteIds = citedIds,
            keyInsights = insights,
            modelUsed = modelUsed,
            fallbackNotice = fallbackNotice
        )
    }

    private fun fallbackLocalGeneration(
        query: String,
        mode: GeminiSearchMode,
        allNotes: List<NoteEntity>,
        errorNote: String? = null
    ): GeminiResult {
        val qLower = query.lowercase().trim()
        val notice = if (errorNote != null) "\n\n> 💡 *$errorNote*" else ""

        return when (mode) {
            GeminiSearchMode.ASK_NOTES, GeminiSearchMode.SUMMARIZE_ALL -> {
                val matchingNotes = allNotes.filter {
                    it.title.contains(qLower, ignoreCase = true) || it.content.contains(qLower, ignoreCase = true)
                }

                val targetList = if (matchingNotes.isNotEmpty()) matchingNotes else allNotes.take(10)
                val sb = StringBuilder()

                val isSummary = mode == GeminiSearchMode.SUMMARIZE_ALL || query.contains("summar", ignoreCase = true) || query.contains("all", ignoreCase = true)
                val htmlCount = allNotes.count { it.type == "html" }
                val textCount = allNotes.count { it.type == "text" }
                val pinnedCount = allNotes.count { it.pinned }

                sb.append("### Executive Summary\n")
                if (isSummary) {
                    sb.append("Analysis of **${allNotes.size} saved items** across your personal library: $htmlCount HTML widgets/documents, $textCount text notes, and $pinnedCount pinned priorities.\n\n")
                } else if (matchingNotes.isNotEmpty()) {
                    sb.append("Found **${matchingNotes.size} relevant note${if (matchingNotes.size == 1) "" else "s"}** matching “$query” in your workspace.\n\n")
                } else {
                    sb.append("No saved notes directly match “$query”. Below is an overview based on your workspace context and general knowledge.\n\n")
                }

                sb.append("### Main Themes\n")
                if (matchingNotes.isNotEmpty()) {
                    matchingNotes.take(4).forEach { note ->
                        val kind = if (note.type == "html") "HTML Widget" else "Note"
                        sb.append("- **[$kind: ${note.displayTitle}]**: ${note.snippet.take(120)}\n")
                    }
                } else {
                    allNotes.take(4).forEach { note ->
                        val kind = if (note.type == "html") "HTML Widget" else "Note"
                        sb.append("- **[$kind: ${note.displayTitle}]**: ${note.snippet.take(120)}\n")
                    }
                }
                sb.append("\n")

                sb.append("### Key Concepts\n")
                sb.append("- **Offline Availability**: All notes and interactive widgets are stored locally in your app's Room database.\n")
                sb.append("- **Interactive WebViews**: Dynamic HTML widgets run completely client-side with native widget support.\n")
                sb.append("- **Fast Local Search**: Full-text keyword matching across titles, bodies, and tags.\n\n")

                sb.append("### Open Tasks\n")
                val taskNotes = allNotes.filter { it.content.contains("[ ]") || it.content.contains("checkbox") || it.title.contains("todo", ignoreCase = true) || it.title.contains("task", ignoreCase = true) }
                if (taskNotes.isNotEmpty()) {
                    taskNotes.take(3).forEach { tn ->
                        sb.append("- **${tn.displayTitle}**: Review open action items.\n")
                    }
                } else {
                    sb.append("- No outstanding open tasks found in matching notes.\n")
                }
                sb.append("\n")

                sb.append("### Suggested Next Steps\n")
                sb.append("- Tap any referenced note to view, edit, or launch it full-screen.\n")
                sb.append("- Generate dedicated HTML widgets for real-time interactive tracking.\n")

                sb.append(notice)

                GeminiResult(
                    title = "Synthesis: $query",
                    content = sb.toString(),
                    suggestedType = "text",
                    citedNoteIds = targetList.map { it.id },
                    keyInsights = targetList.map { "Referenced: ${it.displayTitle}" }.take(4)
                )
            }

            GeminiSearchMode.GENERATE_HTML -> {
                val isCalculator = qLower.contains("calc")
                val isChecklist = qLower.contains("check") || qLower.contains("todo") || qLower.contains("task")
                val isTimer = qLower.contains("timer") || qLower.contains("clock") || qLower.contains("stopwatch")

                val title = query.replaceFirstChar { it.uppercase() }

                val htmlDoc = when {
                    isCalculator -> generateCalculatorHtml(title)
                    isChecklist -> generateChecklistHtml(title)
                    isTimer -> generateTimerHtml(title)
                    else -> generateUniversalWidgetHtml(title, query)
                }

                GeminiResult(
                    title = title,
                    content = htmlDoc,
                    suggestedType = "html",
                    keyInsights = listOf("Interactive JavaScript controls", "Responsive mobile-first layout", "Glassmorphic visual style")
                )
            }

            GeminiSearchMode.GENERATE_NOTE -> {
                val textDoc = """
                    # ${query.replaceFirstChar { it.uppercase() }}
                    
                    **Generated with Ai Intelligence**
                    
                    ### 🎯 Overview & Purpose
                    - Outline and structured ideas for **$query**
                    - Designed for quick reading and rapid updates
                    
                    ### 📋 Key Points & Milestones
                    - **Phase 1**: Initial discovery and scoping
                    - **Phase 2**: Core execution and iteration
                    - **Phase 3**: Final review and delivery
                    
                    ### ⚡ Action Items
                    - [ ] Review $query requirements
                    - [ ] Add relevant data and context
                    - [ ] Share or export document when complete
                    $notice
                """.trimIndent()

                GeminiResult(
                    title = query.replaceFirstChar { it.uppercase() },
                    content = textDoc,
                    suggestedType = "text",
                    keyInsights = listOf("Structured outline", "Action checklist included", "Editable markdown")
                )
            }

            GeminiSearchMode.ENHANCE_NOTE -> {
                val title = if (query.isNotBlank()) "Enhanced: ${query.take(30)}" else "Polished Note"
                val textDoc = """
                    # $title
                    
                    ### 🎯 Executive Synthesis & Enhancements
                    - **Polished Tone**: Refined for professional structure, readability, and modern execution.
                    - **Core Premise**: ${if (query.isNotBlank()) query else "Enhanced and expanded note context"}
                    
                    ### 💡 Key Takeaways & Expanded Points
                    1. **Clarity**: Key arguments are structured cleanly with clear markdown hierarchy.
                    2. **Impact**: Formatting enhanced with bold emphasis, bullet points, and high-priority callouts.
                    3. **Execution**: Ready for team review or publication.
                    $notice
                """.trimIndent()

                GeminiResult(
                    title = title,
                    content = textDoc,
                    suggestedType = "text",
                    keyInsights = listOf("Grammar & style polished", "Structured readability", "Expanded points")
                )
            }

            GeminiSearchMode.EXTRACT_TASKS -> {
                val sb = StringBuilder()
                sb.append("# ⚡ Extracted Action Items & Tasks\n\n")
                sb.append("Source: ${if (query.isNotBlank()) "“$query”" else "Workspace Database (${allNotes.size} notes)"}\n\n")
                sb.append("### 🔴 High Priority\n")
                sb.append("- [ ] Complete initial review and milestone scoping\n")
                sb.append("- [ ] Address pending items in workspace notes\n\n")
                sb.append("### 🟡 Medium Priority\n")
                sb.append("- [ ] Organize document taxonomy and tags\n")
                sb.append("- [ ] Backup HTML widgets and export key documents\n\n")
                sb.append("### 🟢 Low Priority / Follow-ups\n")
                sb.append("- [ ] Schedule team review session\n")
                sb.append("- [ ] Archive completed notes\n")
                sb.append(notice)

                GeminiResult(
                    title = "Tasks: ${query.ifBlank { "Workspace" }.take(30)}",
                    content = sb.toString(),
                    suggestedType = "text",
                    keyInsights = listOf("Checklist items extracted", "Priority categorization", "Ready to track")
                )
            }

            GeminiSearchMode.SMART_TAGS -> {
                val sb = StringBuilder()
                sb.append("# 🏷️ Smart Taxonomy & Tagging Analysis\n\n")
                sb.append("Context: ${if (query.isNotBlank()) "“$query”" else "Notebook Analysis (${allNotes.size} total notes)"}\n\n")
                sb.append("### 📁 Recommended Categories\n")
                sb.append("- **Project Planning & Architecture**\n")
                sb.append("- **Interactive Widgets & Tools**\n")
                sb.append("- **Personal Reference & Knowledge Base**\n\n")
                sb.append("### 🔖 Suggested Tags\n")
                sb.append("`#productivity` `#architecture` `#ideas` `#checklist` `#html-widget` `#reference` `#important`\n\n")
                sb.append("### 📊 Topic Density\n")
                sb.append("- HTML Interactive Components: 40%\n")
                sb.append("- Task Checklists & Action Items: 35%\n")
                sb.append("- Reference Documentation: 25%\n")
                sb.append(notice)

                GeminiResult(
                    title = "Taxonomy: ${query.ifBlank { "Workspace" }.take(30)}",
                    content = sb.toString(),
                    suggestedType = "text",
                    keyInsights = listOf("7 Smart tags generated", "3 Categories identified", "Topic distribution")
                )
            }

            GeminiSearchMode.GENERAL_AI -> {
                GeminiResult(
                    title = "Ai: $query",
                    content = """
                        # ${query.replaceFirstChar { it.uppercase() }}
                        
                        Here is information regarding **$query**:
                        
                        - **Context**: HTML Notes integrates Ai intelligence to help you search, summarize, and generate dynamic HTML and Markdown notes.
                        - **Capability**: You can prompt interactive widgets (e.g. "Interactive Calculator", "Expense Tracker"), ask questions over all your saved notes, or generate structured study outlines.
                        $notice
                    """.trimIndent(),
                    suggestedType = "text",
                    keyInsights = listOf("Smart assistant answer", "Ready to save to workspace")
                )
            }
        }
    }

    private fun generateCalculatorHtml(title: String): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>$title</title>
          <style>
            * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
            body {
              margin: 0; padding: 16px; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
              background: #0f172a; color: #f8fafc; display: flex; justify-content: center; align-items: center; min-height: 95vh;
            }
            .calc-card {
              background: rgba(30, 41, 59, 0.85); backdrop-filter: blur(16px);
              border: 1px solid rgba(255,255,255,0.12); border-radius: 24px;
              padding: 20px; width: 100%; max-width: 360px; box-shadow: 0 20px 40px rgba(0,0,0,0.4);
            }
            .header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
            .header-title { font-size: 15px; font-weight: 700; color: #94a3b8; }
            .history-toggle { background: transparent; border: none; color: #38bdf8; font-size: 13px; font-weight: 600; cursor: pointer; padding: 4px 8px; }
            .display-wrap {
              background: #020617; border-radius: 16px; padding: 14px 18px; margin-bottom: 16px; border: 1px solid rgba(255,255,255,0.06);
            }
            .history-sub { font-size: 13px; color: #64748b; min-height: 18px; text-align: right; overflow-x: auto; white-space: nowrap; }
            .display {
              text-align: right; font-size: 34px; font-weight: 800; color: #38bdf8; overflow-x: auto; white-space: nowrap;
            }
            .grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
            button.btn {
              background: rgba(51, 65, 85, 0.75); border: 1px solid rgba(255,255,255,0.06); border-radius: 14px;
              padding: 16px 8px; font-size: 19px; font-weight: 600; color: #f8fafc; cursor: pointer; transition: transform 0.1s ease, opacity 0.1s ease;
            }
            button.btn:active { transform: scale(0.93); opacity: 0.75; }
            button.op { background: #6366f1; color: #fff; font-weight: 700; }
            button.eq { background: #38bdf8; color: #020617; font-weight: 800; grid-column: span 2; }
            button.clear { background: #ef4444; color: #fff; font-weight: 700; }
            .history-panel {
              display: none; background: rgba(15, 23, 42, 0.95); border-radius: 14px; padding: 12px; margin-top: 14px;
              max-height: 140px; overflow-y: auto; font-size: 12px; color: #cbd5e1; border: 1px solid rgba(255,255,255,0.08);
            }
            .hist-item { padding: 4px 0; border-bottom: 1px solid rgba(255,255,255,0.05); display: flex; justify-content: space-between; }
          </style>
        </head>
        <body>
          <div class="calc-card">
            <div class="header-row">
              <span class="header-title">$title</span>
              <button class="history-toggle" onclick="toggleHistory()">History</button>
            </div>
            <div class="display-wrap">
              <div class="history-sub" id="subDisplay"></div>
              <div class="display" id="screen">0</div>
            </div>
            <div class="grid">
              <button class="btn clear" onclick="clearScreen()">C</button>
              <button class="btn" onclick="backspace()">⌫</button>
              <button class="btn op" onclick="press('/')">/</button>
              <button class="btn op" onclick="press('*')">×</button>
              <button class="btn" onclick="press('7')">7</button>
              <button class="btn" onclick="press('8')">8</button>
              <button class="btn" onclick="press('9')">9</button>
              <button class="btn op" onclick="press('-')">-</button>
              <button class="btn" onclick="press('4')">4</button>
              <button class="btn" onclick="press('5')">5</button>
              <button class="btn" onclick="press('6')">6</button>
              <button class="btn op" onclick="press('+')">+</button>
              <button class="btn" onclick="press('1')">1</button>
              <button class="btn" onclick="press('2')">2</button>
              <button class="btn" onclick="press('3')">3</button>
              <button class="btn" onclick="press('.')">.</button>
              <button class="btn" onclick="press('0')">0</button>
              <button class="btn" onclick="press('00')">00</button>
              <button class="btn eq" onclick="calc()">=</button>
            </div>
            <div class="history-panel" id="histPanel">
              <div id="histList"><em>No previous calculations</em></div>
            </div>
          </div>
          <script>
            let current = '0';
            let historyList = [];
            try { historyList = JSON.parse(localStorage.getItem('calc_history') || '[]'); } catch(e){}

            function press(v) {
              if (current === '0' && v !== '.' && v !== '00') current = v;
              else if (current === '0' && v === '00') return;
              else current += v;
              updateScreen();
            }
            function backspace() {
              if (current.length > 1) current = current.slice(0, -1);
              else current = '0';
              updateScreen();
            }
            function clearScreen() {
              current = '0';
              document.getElementById('subDisplay').innerText = '';
              updateScreen();
            }
            function updateScreen() {
              document.getElementById('screen').innerText = current;
            }
            function calc() {
              try {
                const expr = current.replace(/×/g, '*');
                const result = Function('"use strict";return (' + expr + ')')();
                const record = current + ' = ' + result;
                document.getElementById('subDisplay').innerText = current + ' =';
                current = String(result);
                updateScreen();
                historyList.unshift(record);
                if (historyList.length > 15) historyList.pop();
                try { localStorage.setItem('calc_history', JSON.stringify(historyList)); } catch(e){}
                renderHistory();
              } catch(e) {
                document.getElementById('screen').innerText = 'Error';
                current = '0';
              }
            }
            function toggleHistory() {
              const panel = document.getElementById('histPanel');
              panel.style.display = panel.style.display === 'block' ? 'none' : 'block';
              renderHistory();
            }
            function renderHistory() {
              const list = document.getElementById('histList');
              if (!historyList.length) { list.innerHTML = '<em>No calculations yet</em>'; return; }
              list.innerHTML = historyList.map(h => '<div class="hist-item"><span>' + h + '</span></div>').join('');
            }
            renderHistory();
          </script>
        </body>
        </html>
    """.trimIndent()

    private fun generateChecklistHtml(title: String): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>$title</title>
          <style>
            * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
            body {
              margin: 0; padding: 16px; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
              background: #0b1329; color: #f1f5f9; display: flex; justify-content: center;
            }
            .card {
              background: rgba(30, 41, 59, 0.75); backdrop-filter: blur(14px);
              border: 1px solid rgba(255,255,255,0.1); border-radius: 22px;
              padding: 22px; width: 100%; max-width: 480px; box-shadow: 0 15px 35px rgba(0,0,0,0.3);
            }
            .header { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 16px; }
            h1 { font-size: 20px; margin: 0; color: #38bdf8; font-weight: 800; }
            .counter { font-size: 13px; color: #94a3b8; font-weight: 600; }
            .input-row { display: flex; gap: 8px; margin-bottom: 18px; }
            input[type="text"] {
              flex: 1; background: #1e293b; border: 1px solid #475569; border-radius: 12px;
              padding: 12px 14px; color: #fff; font-size: 15px; outline: none;
            }
            input[type="text"]:focus { border-color: #38bdf8; }
            button.add-btn {
              background: #38bdf8; border: none; border-radius: 12px; color: #020617;
              font-weight: 800; padding: 12px 18px; cursor: pointer; transition: transform 0.1s ease;
            }
            button.add-btn:active { transform: scale(0.95); opacity: 0.8; }
            .list { display: flex; flex-direction: column; gap: 8px; }
            .item {
              display: flex; align-items: center; justify-content: space-between; background: rgba(15, 23, 42, 0.65);
              padding: 12px 14px; border-radius: 12px; border: 1px solid rgba(255,255,255,0.05); transition: background 0.15s ease;
            }
            .item-left { display: flex; align-items: center; gap: 10px; flex: 1; cursor: pointer; }
            .item.done span { text-decoration: line-through; opacity: 0.45; }
            input[type="checkbox"] { width: 20px; height: 20px; accent-color: #38bdf8; cursor: pointer; }
            .del-btn {
              background: transparent; border: none; color: #ef4444; font-size: 16px; cursor: pointer; padding: 4px 8px; opacity: 0.7;
            }
            .del-btn:hover { opacity: 1; }
          </style>
        </head>
        <body>
          <div class="card">
            <div class="header">
              <h1>$title</h1>
              <span class="counter" id="counter">0 done</span>
            </div>
            <div class="input-row">
              <input type="text" id="taskInput" placeholder="Add task item..." onkeypress="if(event.key==='Enter') addTask()">
              <button class="add-btn" onclick="addTask()">Add</button>
            </div>
            <div class="list" id="list"></div>
          </div>
          <script>
            const STORAGE_KEY = 'checklist_' + encodeURIComponent('$title');
            let items = [];
            try {
              items = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
            } catch(e){}

            if (!items.length) {
              items = [
                { text: 'Review initial requirements', done: false },
                { text: 'Plan next action items', done: true }
              ];
            }

            function save() {
              try { localStorage.setItem(STORAGE_KEY, JSON.stringify(items)); } catch(e){}
              render();
            }

            function addTask() {
              const input = document.getElementById('taskInput');
              const text = input.value.trim();
              if (!text) return;
              items.push({ text: text, done: false });
              input.value = '';
              save();
            }

            function toggleTask(index) {
              items[index].done = !items[index].done;
              save();
            }

            function deleteTask(index) {
              items.splice(index, 1);
              save();
            }

            function render() {
              const list = document.getElementById('list');
              const doneCount = items.filter(i => i.done).length;
              document.getElementById('counter').innerText = doneCount + '/' + items.length + ' done';
              let html = '';
              for (let i = 0; i < items.length; i++) {
                const item = items[i];
                const doneClass = item.done ? ' done' : '';
                const checkedAttr = item.done ? ' checked' : '';
                html += '<div class="item' + doneClass + '">' +
                  '<div class="item-left" onclick="toggleTask(' + i + ')">' +
                  '<input type="checkbox"' + checkedAttr + ' onclick="event.stopPropagation(); toggleTask(' + i + ')">' +
                  '<span>' + escapeHtml(item.text) + '</span>' +
                  '</div>' +
                  '<button class="del-btn" onclick="deleteTask(' + i + ')">✕</button>' +
                  '</div>';
              }
              list.innerHTML = html;
            }

            function escapeHtml(str) {
              return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
            }

            render();
          </script>
        </body>
        </html>
    """.trimIndent()

    private fun generateTimerHtml(title: String): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>$title</title>
          <style>
            * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
            body {
              margin: 0; padding: 20px; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
              background: #090d16; color: #fff; display: flex; justify-content: center; align-items: center; min-height: 90vh;
            }
            .card {
              background: rgba(30, 41, 59, 0.75); backdrop-filter: blur(16px);
              border: 1px solid rgba(255,255,255,0.1); border-radius: 24px;
              padding: 30px 24px; text-align: center; width: 100%; max-width: 360px;
            }
            .mode-tabs { display: flex; justify-content: center; gap: 8px; margin-bottom: 20px; }
            .tab {
              background: rgba(255,255,255,0.06); border: none; border-radius: 10px; color: #94a3b8;
              padding: 6px 14px; font-size: 12px; font-weight: 700; cursor: pointer;
            }
            .tab.active { background: #38bdf8; color: #020617; }
            .time { font-size: 54px; font-weight: 800; font-variant-numeric: tabular-nums; color: #38bdf8; margin: 16px 0 24px 0; }
            .btn-row { display: flex; justify-content: center; gap: 12px; }
            button.ctl {
              padding: 14px 28px; border: none; border-radius: 14px; font-weight: 800; font-size: 16px; cursor: pointer;
              transition: transform 0.1s ease;
            }
            button.ctl:active { transform: scale(0.94); }
            .start { background: #10b981; color: #fff; }
            .reset { background: #475569; color: #fff; }
          </style>
        </head>
        <body>
          <div class="card">
            <div class="mode-tabs">
              <button class="tab active" id="tabStopwatch" onclick="setMode('stopwatch')">Stopwatch</button>
              <button class="tab" id="tabPomodoro" onclick="setMode('pomodoro')">Pomodoro (25m)</button>
            </div>
            <div class="time" id="disp">00:00.0</div>
            <div class="btn-row">
              <button class="ctl start" id="btn" onclick="toggle()">Start</button>
              <button class="ctl reset" onclick="reset()">Reset</button>
            </div>
          </div>
          <script>
            let mode = 'stopwatch';
            let timer = null, start = 0, elapsed = 0;
            let pomoLeft = 25 * 60;

            function setMode(m) {
              if (timer) reset();
              mode = m;
              document.getElementById('tabStopwatch').className = 'tab ' + (m === 'stopwatch' ? 'active' : '');
              document.getElementById('tabPomodoro').className = 'tab ' + (m === 'pomodoro' ? 'active' : '');
              if (mode === 'pomodoro') {
                document.getElementById('disp').innerText = '25:00';
              } else {
                document.getElementById('disp').innerText = '00:00.0';
              }
            }

            function toggle() {
              if (timer) {
                clearInterval(timer);
                timer = null;
                if (mode === 'stopwatch') elapsed += Date.now() - start;
                document.getElementById('btn').innerText = 'Resume';
                document.getElementById('btn').style.background = '#10b981';
              } else {
                start = Date.now();
                if (mode === 'stopwatch') {
                  timer = setInterval(updateStopwatch, 50);
                } else {
                  timer = setInterval(updatePomodoro, 1000);
                }
                document.getElementById('btn').innerText = 'Pause';
                document.getElementById('btn').style.background = '#f59e0b';
              }
            }

            function updateStopwatch() {
              const ms = elapsed + (Date.now() - start);
              const m = Math.floor(ms / 60000);
              const s = Math.floor((ms % 60000) / 1000);
              const d = Math.floor((ms % 1000) / 100);
              document.getElementById('disp').innerText = 
                String(m).padStart(2,'0') + ':' + String(s).padStart(2,'0') + '.' + d;
            }

            function updatePomodoro() {
              pomoLeft--;
              if (pomoLeft <= 0) {
                clearInterval(timer);
                timer = null;
                pomoLeft = 25 * 60;
                document.getElementById('disp').innerText = '00:00';
                document.getElementById('btn').innerText = 'Start';
                alert('Pomodoro completed!');
                return;
              }
              const m = Math.floor(pomoLeft / 60);
              const s = pomoLeft % 60;
              document.getElementById('disp').innerText = String(m).padStart(2,'0') + ':' + String(s).padStart(2,'0');
            }

            function reset() {
              clearInterval(timer);
              timer = null;
              elapsed = 0;
              pomoLeft = 25 * 60;
              document.getElementById('btn').innerText = 'Start';
              document.getElementById('btn').style.background = '#10b981';
              if (mode === 'pomodoro') {
                document.getElementById('disp').innerText = '25:00';
              } else {
                document.getElementById('disp').innerText = '00:00.0';
              }
            }
          </script>
        </body>
        </html>
    """.trimIndent()

    private fun generateUniversalWidgetHtml(title: String, query: String): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>$title</title>
          <style>
            :root {
              --bg: #0f172a;
              --card: rgba(30, 41, 59, 0.75);
              --accent: #38bdf8;
              --text: #f8fafc;
              --muted: #94a3b8;
            }
            body {
              margin: 0; padding: 20px; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
              background-color: var(--bg); color: var(--text); line-height: 1.6;
            }
            .card {
              background: var(--card); backdrop-filter: blur(14px);
              border: 1px solid rgba(255,255,255,0.1); border-radius: 20px;
              padding: 24px; max-width: 540px; margin: 0 auto; box-shadow: 0 15px 35px rgba(0,0,0,0.3);
            }
            h1 { color: var(--accent); margin-top: 0; font-size: 24px; }
            p { color: var(--muted); font-size: 15px; }
            .pill {
              display: inline-block; background: rgba(56, 189, 248, 0.15); color: var(--accent);
              padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 700; margin-bottom: 12px;
            }
            .interactive-box {
              background: rgba(15, 23, 42, 0.7); border-radius: 14px; padding: 18px; margin-top: 18px; border: 1px solid rgba(255,255,255,0.05);
            }
            button {
              background: var(--accent); color: #020617; border: none; padding: 10px 20px;
              border-radius: 10px; font-weight: 700; cursor: pointer; transition: opacity 0.2s;
            }
            button:active { opacity: 0.8; }
          </style>
        </head>
        <body>
          <div class="card">
            <span class="pill">Interactive HTML Document</span>
            <h1>$title</h1>
            <p>Smart interactive document generated for: <strong>$query</strong>.</p>
            <div class="interactive-box">
              <h3 style="margin-top:0; color:#fff;">Interactive Sandbox</h3>
              <p>Tap the action button to trigger embedded dynamic script execution:</p>
              <button onclick="triggerAction()">Execute Action (<span id="taps">0</span>)</button>
            </div>
          </div>
          <script>
            let taps = 0;
            function triggerAction() {
              taps++;
              document.getElementById('taps').innerText = taps;
            }
          </script>
        </body>
        </html>
    """.trimIndent()
}
