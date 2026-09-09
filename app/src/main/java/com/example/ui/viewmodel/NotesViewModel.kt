package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.api.GeminiResult
import com.example.data.api.GeminiSearchMode
import com.example.data.db.AppDatabase
import com.example.data.model.AiHistoryItem
import com.example.data.model.AppSettings
import com.example.data.model.NoteEntity
import com.example.data.model.NoteSummary
import com.example.data.model.ChatSession
import com.example.data.model.ChatMessage
import com.example.data.repository.NoteRepository
import com.example.util.AudioTimerHelper
import com.example.util.DateFormatter
import com.example.util.HashUtil
import com.example.util.VibrationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface ToastEvent {
    data class Simple(val message: String) : ToastEvent
    data class WithAction(val message: String, val actionLabel: String, val onAction: () -> Unit) : ToastEvent
}

sealed interface GeminiQueryState {
    object Idle : GeminiQueryState
    data class Loading(val prompt: String, val mode: GeminiSearchMode) : GeminiQueryState
    data class Success(val result: GeminiResult, val prompt: String, val mode: GeminiSearchMode) : GeminiQueryState
    data class Error(val message: String, val prompt: String, val mode: GeminiSearchMode) : GeminiQueryState
}

sealed interface WidgetNavAction {
    data class OpenNote(val id: String) : WidgetNavAction
    object CreateNote : WidgetNavAction
    object OpenWidgetSettings : WidgetNavAction
    object OpenSearch : WidgetNavAction
}

data class GroupedNotes(
    val header: String?,
    val notes: List<NoteSummary>
)

class NotesViewModel(
    application: Application,
    private val repository: NoteRepository
) : AndroidViewModel(application) {

    private val _widgetNavAction = MutableSharedFlow<WidgetNavAction>(replay = 1)
    val widgetNavAction = _widgetNavAction.asSharedFlow()

    fun handleWidgetOpenNote(id: String) {
        _widgetNavAction.tryEmit(WidgetNavAction.OpenNote(id))
    }

    fun handleWidgetCreateNote() {
        _widgetNavAction.tryEmit(WidgetNavAction.CreateNote)
    }

    fun handleWidgetConfig() {
        _widgetNavAction.tryEmit(WidgetNavAction.OpenWidgetSettings)
    }

    fun handleWidgetSearch() {
        _widgetNavAction.tryEmit(WidgetNavAction.OpenSearch)
    }

    fun consumeWidgetNavAction() {
        _widgetNavAction.resetReplayCache()
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("all") // "all", "text", "html", "pinned"
    val selectedFilter = _selectedFilter.asStateFlow()

    val settings: StateFlow<AppSettings> = repository.settingsFlow

    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allNoteSummaries: StateFlow<List<NoteSummary>> = repository.allNoteSummaries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val trashNotes: StateFlow<List<NoteEntity>> = repository.trashNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val trashNoteSummaries: StateFlow<List<NoteSummary>> = repository.trashNoteSummaries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val trashCount: StateFlow<Int> = repository.trashCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val categories: StateFlow<List<String>> = repository.allNoteSummaries.map { notes ->
        notes.mapNotNull { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _toastFlow = MutableSharedFlow<ToastEvent>()
    val toastFlow = _toastFlow.asSharedFlow()

    // Clock state (Timer & Stopwatch)
    private val _timerState = MutableStateFlow(TimerState())
    val timerState = _timerState.asStateFlow()

    private val _stopwatchState = MutableStateFlow(StopwatchState())
    val stopwatchState = _stopwatchState.asStateFlow()

    private val _runningPill = MutableStateFlow(RunningPillInfo())
    val runningPill = _runningPill.asStateFlow()

    // Gemini Search & Generative AI State
    private val _geminiState = MutableStateFlow<GeminiQueryState>(GeminiQueryState.Idle)
    val geminiState = _geminiState.asStateFlow()

    // Mode-specific AI History
    private val _aiHistory = MutableStateFlow<List<AiHistoryItem>>(emptyList())
    val aiHistory = _aiHistory.asStateFlow()

    // Chatbot States & Parameters
    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions = _chatSessions.asStateFlow()

    private val _activeChatSession = MutableStateFlow<ChatSession?>(null)
    val activeChatSession = _activeChatSession.asStateFlow()

    private val _chatDetailedAnswers = MutableStateFlow(false)
    val chatDetailedAnswers = _chatDetailedAnswers.asStateFlow()

    private val _chatSelectedModel = MutableStateFlow("gemini-3.5-flash")
    val chatSelectedModel = _chatSelectedModel.asStateFlow()

    private var clockTickerJob: Job? = null
    private var recentlyDeletedNote: NoteEntity? = null

    // Combined filtered & grouped notes
    val groupedNotes: StateFlow<List<GroupedNotes>> = combine(
        allNoteSummaries,
        trashNoteSummaries,
        _searchQuery,
        _selectedFilter,
        settings
    ) { notes, trashedNotes, query, filter, settings ->
        val q = query.trim().lowercase()
        val isPdfMode = settings.readerMode == "pdf"

        // Handle dedicated Trash folder view
        if (filter == "trash") {
            val filteredTrash = trashedNotes.filter { n ->
                if (q.isNotEmpty()) {
                    val matchesTitle = n.title.lowercase().contains(q)
                    val matchesContent = n.snippetPreview.lowercase().contains(q)
                    val matchesSource = n.source?.lowercase()?.contains(q) == true
                    if (!matchesTitle && !matchesContent && !matchesSource) return@filter false
                }
                true
            }.sortedByDescending { it.deletedAt ?: it.updatedAt }

            if (filteredTrash.isEmpty()) {
                return@combine emptyList()
            }
            return@combine listOf(GroupedNotes(header = "Trash Folder", notes = filteredTrash))
        }

        val filtered = notes.filter { n ->
            // Separate by reader mode: "html" mode shows text & html notes, "pdf" mode shows pdf documents
            if (isPdfMode) {
                if (n.type != "pdf") return@filter false
                if (filter == "pinned" && !n.pinned) return@filter false
                if (filter == "recent") {
                    val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                    if (n.updatedAt < sevenDaysAgo) return@filter false
                }
                if (filter == "imported" && (n.source.isNullOrBlank() || (!n.source.startsWith("content://") && !n.source.contains("cache")))) {
                    return@filter false
                }
                if (filter == "docs" && n.source?.contains("Download", ignoreCase = true) == true) {
                    return@filter false
                }
            } else {
                if (n.type == "pdf") return@filter false
                if (filter == "text" && n.type != "text") return@filter false
                if (filter == "html" && n.type != "html") return@filter false
                if (filter == "pinned" && !n.pinned) return@filter false
            }

            // Also check categories here
            if (filter.startsWith("cat_")) {
                val expectedCat = filter.removePrefix("cat_")
                if (n.category != expectedCat) return@filter false
            }

            if (q.isNotEmpty()) {
                val matchesTitle = n.title.lowercase().contains(q)
                val matchesContent = n.snippetPreview.lowercase().contains(q)
                val matchesSource = n.source?.lowercase()?.contains(q) == true
                if (!matchesTitle && !matchesContent && !matchesSource) return@filter false
            }
            true
        }

        val sorted = filtered.sortedWith { a, b ->
            if (a.pinned != b.pinned) {
                if (a.pinned) -1 else 1
            } else {
                when (settings.sortOrder) {
                    "title" -> (a.title).compareTo(b.title, ignoreCase = true)
                    "created" -> b.createdAt.compareTo(a.createdAt)
                    else -> b.updatedAt.compareTo(a.updatedAt) // "edited"
                }
            }
        }

        if (sorted.isEmpty()) {
            return@combine emptyList()
        }

        if (settings.sortOrder == "title") {
            listOf(GroupedNotes(header = null, notes = sorted))
        } else {
            val groups = mutableListOf<GroupedNotes>()
            var currentHeader: String? = null
            var currentBuffer = mutableListOf<NoteSummary>()

            for (note in sorted) {
                val groupKey = if (note.pinned) {
                    "Pinned"
                } else {
                    val ts = if (settings.sortOrder == "created") note.createdAt else note.updatedAt
                    DateFormatter.groupOf(ts)
                }

                if (groupKey != currentHeader) {
                    if (currentBuffer.isNotEmpty()) {
                        groups.add(GroupedNotes(header = currentHeader, notes = currentBuffer.toList()))
                        currentBuffer = mutableListOf()
                    }
                    currentHeader = groupKey
                }
                currentBuffer.add(note)
            }

            if (currentBuffer.isNotEmpty()) {
                groups.add(GroupedNotes(header = currentHeader, notes = currentBuffer.toList()))
            }

            groups
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Sync haptic settings with global vibration helper
        viewModelScope.launch {
            settings.collect { s ->
                VibrationHelper.isHapticsEnabled = s.hapticsEnabled
            }
        }

        // Defer background housekeeping so main thread renders initial frame with zero delay
        viewModelScope.launch(Dispatchers.IO) {
            delay(1500)
            loadAiHistory(settings.value.readerMode)
            loadChatSessions()
            repository.removeDuplicateNotes()
            try {
                com.example.data.sync.HtmlSyncWorker.schedulePeriodicSync(getApplication())
            } catch (_: Throwable) {}
            if (settings.value.autoSync) {
                if (settings.value.readerMode == "pdf") {
                    syncAllDevicePdfFiles(silent = true)
                } else {
                    syncAllDeviceHtmlFiles(silent = true)
                }
            }
        }
    }

    private fun ensureClockTickerRunning() {
        if (clockTickerJob?.isActive == true) return
        clockTickerJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val currentTimer = _timerState.value
                val currentSw = _stopwatchState.value

                if (!currentTimer.isRunning && !currentSw.isRunning && currentTimer.remainingMs == 0L && currentSw.currentElapsedMs == 0L) {
                    _runningPill.value = RunningPillInfo(visible = false)
                    break
                }

                val now = System.currentTimeMillis()

                // Handle timer ticking
                if (currentTimer.isRunning) {
                    if (now >= currentTimer.endTimeMs) {
                        // Timer completed!
                        _timerState.value = currentTimer.copy(
                            isRunning = false,
                            endTimeMs = 0,
                            remainingMs = 0,
                            totalMs = 0
                        )
                        AudioTimerHelper.playTimerAlarm(3)
                        VibrationHelper.vibratePattern(getApplication(), longArrayOf(0, 300, 120, 300, 120, 500))
                        showToast("Timer finished")
                    } else {
                        val remain = currentTimer.endTimeMs - now
                        _timerState.value = currentTimer.copy(remainingMs = remain)
                    }
                }

                // Handle stopwatch ticking
                if (currentSw.isRunning) {
                    val elapsed = currentSw.accumulatedMs + (now - currentSw.startAtMs)
                    _stopwatchState.value = currentSw.copy(currentElapsedMs = elapsed)
                }

                // Update running pill
                val updatedTimer = _timerState.value
                val updatedSw = _stopwatchState.value

                when {
                    updatedTimer.isRunning -> {
                        _runningPill.value = RunningPillInfo(
                            visible = true,
                            kind = "timer",
                            text = "Timer ${DateFormatter.fmtMS(updatedTimer.remainingMs)}"
                        )
                    }
                    updatedTimer.totalMs > 0 && !updatedTimer.isRunning -> {
                        _runningPill.value = RunningPillInfo(
                            visible = true,
                            kind = "timer",
                            text = "Timer ${DateFormatter.fmtMS(updatedTimer.remainingMs)}"
                        )
                    }
                    updatedSw.isRunning -> {
                        val elapsed = updatedSw.accumulatedMs + (now - updatedSw.startAtMs)
                        _runningPill.value = RunningPillInfo(
                            visible = true,
                            kind = "sw",
                            text = DateFormatter.fmtSW(elapsed)
                        )
                    }
                    updatedSw.accumulatedMs > 0 && !updatedSw.isRunning -> {
                        _runningPill.value = RunningPillInfo(
                            visible = true,
                            kind = "sw",
                            text = DateFormatter.fmtSW(updatedSw.accumulatedMs)
                        )
                    }
                    else -> {
                        _runningPill.value = RunningPillInfo(visible = false)
                    }
                }

                delay(50)
            }
        }
    }

    // --- Search & Filter Actions ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    // --- Gemini Search & Generation Actions ---
    fun askGemini(prompt: String, mode: GeminiSearchMode = GeminiSearchMode.ASK_NOTES) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return
        val currentModeType = settings.value.readerMode
        viewModelScope.launch {
            _geminiState.value = GeminiQueryState.Loading(trimmed, mode)
            val result = GeminiClient.queryGemini(trimmed, mode, allNotes.value, settings.value.geminiApiKey)
            if (result.error != null && result.content.isBlank()) {
                _geminiState.value = GeminiQueryState.Error(result.error, trimmed, mode)
            } else {
                _geminiState.value = GeminiQueryState.Success(result, trimmed, mode)
                // Automatically record to mode-specific history
                val historyItem = AiHistoryItem(
                    id = UUID.randomUUID().toString().take(10),
                    query = trimmed,
                    modeType = currentModeType,
                    aiSearchMode = mode.name,
                    responseTitle = result.title,
                    responseContent = result.content,
                    suggestedType = result.suggestedType,
                    timestamp = System.currentTimeMillis()
                )
                repository.saveAiHistoryItem(historyItem)
                _aiHistory.value = repository.getAiHistory(currentModeType)

                // Parse and execute any agent action blocks automatically
                val content = result.content
                if (content.contains("<app_action>") && content.contains("</app_action>")) {
                    try {
                        val startIndex = content.indexOf("<app_action>") + "<app_action>".length
                        val endIndex = content.indexOf("</app_action>")
                        val jsonString = content.substring(startIndex, endIndex).trim()
                        val jsonArray = org.json.JSONArray(jsonString)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            when (val action = obj.optString("action")) {
                                "create_note" -> {
                                    val type = obj.optString("type", "text")
                                    val title = obj.optString("title", "Untitled Note")
                                    val noteContent = obj.optString("content", "")
                                    createNote(type) { newId ->
                                        saveNote(newId, title, noteContent)
                                        showToast("Agent: Created note '$title'")
                                    }
                                }
                                "save_note" -> {
                                    val id = obj.optString("id")
                                    val title = obj.optString("title")
                                    val noteContent = obj.optString("content")
                                    if (id.isNotBlank()) {
                                        saveNote(id, title, noteContent)
                                        showToast("Agent: Saved note '$title'")
                                    }
                                }
                                "delete_note" -> {
                                    val id = obj.optString("id")
                                    if (id.isNotBlank()) {
                                        deleteNote(id)
                                        showToast("Agent: Moved note to trash")
                                    }
                                }
                                "restore_note" -> {
                                    val id = obj.optString("id")
                                    if (id.isNotBlank()) {
                                        restoreNote(id)
                                        showToast("Agent: Restored note")
                                    }
                                }
                                "empty_trash" -> {
                                    emptyTrash()
                                    showToast("Agent: Emptied trash")
                                }
                                "set_category" -> {
                                    val id = obj.optString("id")
                                    val category = obj.optString("category")
                                    if (id.isNotBlank()) {
                                        setNoteCategory(id, category.takeIf { it.isNotBlank() })
                                        showToast("Agent: Set category to '$category'")
                                    }
                                }
                                "toggle_pin" -> {
                                    val id = obj.optString("id")
                                    if (id.isNotBlank()) {
                                        togglePin(id)
                                        showToast("Agent: Toggled note pin")
                                    }
                                }
                                "timer_start" -> {
                                    val mins = obj.optInt("minutes", 5)
                                    timerStart(mins)
                                    showToast("Agent: Started $mins min timer")
                                }
                                "timer_pause" -> {
                                    timerPause()
                                    showToast("Agent: Paused timer")
                                }
                                "sw_start" -> {
                                    swStart()
                                    showToast("Agent: Started stopwatch")
                                }
                                "sw_pause" -> {
                                    swPause()
                                    showToast("Agent: Paused stopwatch")
                                }
                                "set_theme" -> {
                                    val theme = obj.optString("theme")
                                    if (theme.isNotBlank()) {
                                        setTheme(theme)
                                        showToast("Agent: Set theme to '$theme'")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun loadAiHistory(modeType: String = settings.value.readerMode) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getAiHistory(modeType)
            _aiHistory.value = list
        }
    }

    fun deleteAiHistoryItem(id: String, modeType: String = settings.value.readerMode) {
        viewModelScope.launch {
            repository.deleteAiHistoryItem(id, modeType)
            _aiHistory.value = repository.getAiHistory(modeType)
            showToast("History item removed")
        }
    }

    fun clearAiHistory(modeType: String = settings.value.readerMode) {
        viewModelScope.launch {
            repository.clearAiHistory(modeType)
            _aiHistory.value = emptyList()
            showToast("AI history cleared for ${if (modeType == "pdf") "PDF" else "HTML"} mode")
        }
    }

    // --- Chatbot Session & Send Actions ---

    fun setChatDetailedAnswers(enabled: Boolean) {
        _chatDetailedAnswers.value = enabled
    }

    fun setChatSelectedModel(model: String) {
        _chatSelectedModel.value = model
    }

    fun loadChatSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            val sessions = repository.getChatSessions()
            _chatSessions.value = sessions
            if (_activeChatSession.value == null && sessions.isNotEmpty()) {
                _activeChatSession.value = sessions.first()
            }
        }
    }

    fun startNewChatSession() {
        viewModelScope.launch {
            val newSession = ChatSession(
                id = UUID.randomUUID().toString(),
                title = "New Chat",
                messages = emptyList(),
                lastUpdated = System.currentTimeMillis()
            )
            repository.saveChatSession(newSession)
            _activeChatSession.value = newSession
            loadChatSessions()
        }
    }

    fun setActiveChatSession(sessionId: String) {
        val found = _chatSessions.value.find { it.id == sessionId }
        if (found != null) {
            _activeChatSession.value = found
        }
    }

    fun deleteChatSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteChatSession(sessionId)
            if (_activeChatSession.value?.id == sessionId) {
                _activeChatSession.value = null
            }
            loadChatSessions()
            showToast("Chat session deleted")
        }
    }

    fun clearAllChatSessions() {
        viewModelScope.launch {
            repository.clearChatSessions()
            _activeChatSession.value = null
            _chatSessions.value = emptyList()
            showToast("All chats cleared")
        }
    }

    fun sendChatPrompt(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return
        
        viewModelScope.launch {
            var session = _activeChatSession.value
            if (session == null) {
                session = ChatSession(
                    id = UUID.randomUUID().toString(),
                    title = trimmed.take(25),
                    messages = emptyList(),
                    lastUpdated = System.currentTimeMillis()
                )
            }
            
            // 1. Create user message
            val userMsg = ChatMessage(
                sender = "user",
                content = trimmed,
                modelUsed = _chatSelectedModel.value
            )
            
            // 2. Append to session
            val updatedMessages = session.messages + userMsg
            val updatedTitle = if (session.title == "New Chat" || session.title.isBlank()) {
                trimmed.take(25) + if (trimmed.length > 25) "..." else ""
            } else {
                session.title
            }
            
            val updatedSession = session.copy(
                title = updatedTitle,
                messages = updatedMessages,
                lastUpdated = System.currentTimeMillis()
            )
            
            _activeChatSession.value = updatedSession
            repository.saveChatSession(updatedSession)
            loadChatSessions()
            
            // 3. Set loading state
            _geminiState.value = GeminiQueryState.Loading(trimmed, GeminiSearchMode.ASK_NOTES)
            
            // 4. Query Gemini with history
            // Compile conversation history for the context
            val historyBlock = updatedSession.messages.joinToString("\n") { msg ->
                val role = if (msg.sender == "user") "User" else "Assistant"
                "$role: ${msg.content}"
            }
            
            val finalQuery = "$historyBlock\nUser: $trimmed"
            val result = GeminiClient.queryGemini(
                query = finalQuery,
                mode = GeminiSearchMode.ASK_NOTES,
                allNotes = allNotes.value,
                customKey = settings.value.geminiApiKey,
                selectedModel = _chatSelectedModel.value,
                detailedAnswers = _chatDetailedAnswers.value
            )
            
            if (result.error != null && result.content.isBlank()) {
                _geminiState.value = GeminiQueryState.Error(result.error, trimmed, GeminiSearchMode.ASK_NOTES)
                // Append error message as AI response for graceful experience
                val errorMsg = ChatMessage(
                    sender = "ai",
                    content = "Sorry, I encountered an error: ${result.error}. Please check your connection or API key.",
                    modelUsed = result.modelUsed ?: _chatSelectedModel.value
                )
                val errorSession = updatedSession.copy(
                    messages = updatedSession.messages + errorMsg,
                    lastUpdated = System.currentTimeMillis()
                )
                _activeChatSession.value = errorSession
                repository.saveChatSession(errorSession)
                loadChatSessions()
            } else {
                _geminiState.value = GeminiQueryState.Success(result, trimmed, GeminiSearchMode.ASK_NOTES)
                
                // Append AI response
                val aiMsg = ChatMessage(
                    sender = "ai",
                    content = result.content,
                    modelUsed = result.modelUsed ?: _chatSelectedModel.value
                )
                val finalSession = updatedSession.copy(
                    messages = updatedSession.messages + aiMsg,
                    lastUpdated = System.currentTimeMillis()
                )
                _activeChatSession.value = finalSession
                repository.saveChatSession(finalSession)
                loadChatSessions()
                
                // Parse and execute any agent action blocks automatically
                val content = result.content
                if (content.contains("<app_action>") && content.contains("</app_action>")) {
                    try {
                        val startIndex = content.indexOf("<app_action>") + "<app_action>".length
                        val endIndex = content.indexOf("</app_action>")
                        val jsonString = content.substring(startIndex, endIndex).trim()
                        val jsonArray = org.json.JSONArray(jsonString)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            when (val action = obj.optString("action")) {
                                "create_note" -> {
                                    val type = obj.optString("type", "text")
                                    val title = obj.optString("title", "Untitled Note")
                                    val noteContent = obj.optString("content", "")
                                    createNote(type) { newId ->
                                        saveNote(newId, title, noteContent)
                                        showToast("Agent: Created note '$title'")
                                    }
                                }
                                "save_note" -> {
                                    val id = obj.optString("id")
                                    val title = obj.optString("title")
                                    val noteContent = obj.optString("content")
                                    if (id.isNotBlank()) {
                                        saveNote(id, title, noteContent)
                                        showToast("Agent: Saved note '$title'")
                                    }
                                }
                                "delete_note" -> {
                                    val id = obj.optString("id")
                                    if (id.isNotBlank()) {
                                        deleteNote(id)
                                        showToast("Agent: Moved note to trash")
                                    }
                                }
                                "restore_note" -> {
                                    val id = obj.optString("id")
                                    if (id.isNotBlank()) {
                                        restoreNote(id)
                                        showToast("Agent: Restored note")
                                    }
                                }
                                "empty_trash" -> {
                                    emptyTrash()
                                    showToast("Agent: Emptied trash")
                                }
                                "set_category" -> {
                                    val id = obj.optString("id")
                                    val category = obj.optString("category")
                                    if (id.isNotBlank()) {
                                        setNoteCategory(id, category.takeIf { it.isNotBlank() })
                                        showToast("Agent: Set category to '$category'")
                                    }
                                }
                                "toggle_pin" -> {
                                    val id = obj.optString("id")
                                    if (id.isNotBlank()) {
                                        togglePin(id)
                                        showToast("Agent: Toggled note pin")
                                    }
                                }
                                "timer_start" -> {
                                    val mins = obj.optInt("minutes", 5)
                                    timerStart(mins)
                                    showToast("Agent: Started $mins min timer")
                                }
                                "timer_pause" -> {
                                    timerPause()
                                    showToast("Agent: Paused timer")
                                }
                                "sw_start" -> {
                                    swStart()
                                    showToast("Agent: Started stopwatch")
                                }
                                "sw_pause" -> {
                                    swPause()
                                    showToast("Agent: Paused stopwatch")
                                }
                                "set_theme" -> {
                                    val theme = obj.optString("theme")
                                    if (theme.isNotBlank()) {
                                        setTheme(theme)
                                        showToast("Agent: Set theme to '$theme'")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun clearGeminiState() {
        _geminiState.value = GeminiQueryState.Idle
    }

    fun saveGeminiResultAsNote(
        result: GeminiResult,
        type: String = result.suggestedType,
        onNoteCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val existing = repository.findDuplicateNote(result.title, result.content)
            if (existing != null) {
                showToast("Existing note found: ${existing.displayTitle}")
                onNoteCreated(existing.id)
                return@launch
            }

            val newNote = NoteEntity(
                title = result.title,
                type = type,
                content = result.content,
                hash = HashUtil.noteHash(result.title, result.content)
            )
            repository.insertOrUpdate(newNote)
            showToast("Note saved: ${newNote.displayTitle}")
            onNoteCreated(newNote.id)
        }
    }

    fun removeDuplicateNotes() {
        viewModelScope.launch {
            val count = repository.removeDuplicateNotes()
            if (count > 0) {
                showToast("Cleaned up $count duplicate note${if (count == 1) "" else "s"}")
            } else {
                showToast("No duplicate notes found")
            }
        }
    }

    fun cycleTheme() {
        val current = settings.value.theme
        val next = when (current) {
            "light" -> "dark"
            "dark" -> "light"
            else -> "dark"
        }
        setTheme(next)
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(theme = theme) }
        }
    }

    fun toggleViewMode() {
        val current = settings.value.viewMode
        val next = if (current == "grid") "list" else "grid"
        viewModelScope.launch {
            repository.updateSettings { it.copy(viewMode = next) }
            showToast(if (next == "grid") "Grid view" else "List view")
        }
    }

    fun setSortOrder(order: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(sortOrder = order) }
        }
    }

    fun setReduceTransparency(reduce: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(reduceTransparency = reduce) }
        }
    }

    fun setReadingFontSize(size: Int) {
        val clamped = size.coerceIn(13, 24)
        viewModelScope.launch {
            repository.updateSettings { it.copy(readingFontSize = clamped) }
        }
    }

    fun setGeminiApiKey(key: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(geminiApiKey = key.trim()) }
            showToast(if (key.trim().isBlank()) "Gemini API key reset" else "Gemini API key saved")
        }
    }

    fun setReaderMode(mode: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(readerMode = mode) }
            loadAiHistory(mode)
            showToast(if (mode == "pdf") "Switched to PDF Drive" else "Switched to HTML Notes")
        }
    }

    fun setPdfPageMode(mode: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(pdfPageMode = mode) }
            showToast(if (mode == "single") "Single Page Mode" else "Continuous Scroll Mode")
        }
    }

    fun setPdfColorFilter(filter: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(pdfColorFilter = filter) }
        }
    }

    fun setPdfRenderQuality(quality: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(pdfRenderQuality = quality) }
            showToast(if (quality == "sharp") "Ultra-Sharp 4x Dynamic Rendering" else "Balanced Low-End Mode")
        }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(biometricLockEnabled = enabled) }
            showToast(if (enabled) "Biometric security enabled" else "Biometric security disabled")
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(hapticsEnabled = enabled) }
            VibrationHelper.isHapticsEnabled = enabled
            showToast(if (enabled) "Realistic haptics enabled" else "Haptics disabled")
        }
    }

    fun setPinCode(pin: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(pinCode = pin) }
            showToast(if (pin.isNotBlank()) "Security PIN updated" else "Security PIN cleared")
        }
    }

    fun toggleNoteLock(noteId: String) {
        viewModelScope.launch {
            val note = repository.getNoteDirect(noteId) ?: return@launch
            val updated = note.copy(isLocked = !note.isLocked, updatedAt = System.currentTimeMillis())
            repository.insertOrUpdate(updated)
            showToast(if (updated.isLocked) "Note locked with Biometrics" else "Note unlocked")
        }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(autoSync = enabled) }
            if (enabled) {
                com.example.data.sync.HtmlSyncWorker.schedulePeriodicSync(getApplication())
                syncAllDeviceHtmlFiles(silent = false)
            } else {
                com.example.data.sync.HtmlSyncWorker.cancelPeriodicSync(getApplication())
            }
        }
    }

    // --- Note CRUD Actions ---
    fun createNote(type: String = "text", onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val initialTitle = if (type == "html") "HTML Note" else ""
            val initialContent = if (type == "html") {
                """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <style>
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      padding: 18px;
      line-height: 1.6;
      color: #1e293b;
      background-color: transparent;
    }
    h1 { color: #2563eb; margin-bottom: 8px; font-size: 24px; }
    p { margin: 8px 0; font-size: 15px; }
    .card {
      background: rgba(37, 99, 235, 0.08);
      border-left: 4px solid #2563eb;
      padding: 12px 16px;
      border-radius: 8px;
      margin: 16px 0;
    }
  </style>
</head>
<body>
  <h1>HTML Note</h1>
  <p>Start writing your HTML content here.</p>
  <div class="card">
    💡 <b>Tip:</b> Switch between <b>Code</b> and <b>Preview</b> above to inspect and edit your markup.
  </div>
</body>
</html>""".trimIndent()
            } else {
                ""
            }

            val newNote = NoteEntity(
                id = UUID.randomUUID().toString().take(12),
                title = initialTitle,
                type = type,
                content = initialContent,
                pinned = false,
                source = null,
                createdAt = now,
                updatedAt = now,
                hash = HashUtil.noteHash(initialTitle, initialContent)
            )
            repository.insertNote(newNote)
            onCreated(newNote.id)
        }
    }

    fun saveNote(id: String, title: String, content: String) {
        viewModelScope.launch {
            val current = repository.getNoteDirect(id) ?: return@launch
            val trimmedTitle = title.trim()
            if (current.title == trimmedTitle && current.content == content) return@launch

            val now = System.currentTimeMillis()
            val updated = current.copy(
                title = trimmedTitle,
                content = content,
                updatedAt = now,
                hash = HashUtil.noteHash(trimmedTitle, content)
            )
            repository.updateNote(updated)
        }
    }

    fun setNoteCategory(id: String, category: String?) {
        viewModelScope.launch {
            val current = repository.getNoteDirect(id) ?: return@launch
            if (current.category == category) return@launch

            val updated = current.copy(
                category = category,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateNote(updated)
            showToast(if (category == null) "Category cleared" else "Category updated")
        }
    }

    fun togglePin(id: String) {
        viewModelScope.launch {
            val note = repository.getNoteDirect(id) ?: return@launch
            val nextState = !note.pinned
            repository.togglePin(id)
            showToast(if (nextState) "Pinned" else "Unpinned")
        }
    }

    fun duplicateNote(id: String) {
        viewModelScope.launch {
            val dup = repository.duplicateNote(id)
            if (dup != null) {
                showToast("Duplicated")
            }
        }
    }

    fun deleteNote(id: String) {
        if (_selectedFilter.value == "trash") {
            deletePermanently(id)
        } else {
            moveToTrash(id)
        }
    }

    fun moveToTrash(id: String) {
        viewModelScope.launch {
            val note = repository.getNoteDirect(id) ?: return@launch
            recentlyDeletedNote = note
            repository.moveToTrash(id)
            _toastFlow.emit(
                ToastEvent.WithAction(
                    message = "Moved to Trash",
                    actionLabel = "Undo",
                    onAction = {
                        viewModelScope.launch {
                            repository.restoreFromTrash(id)
                        }
                    }
                )
            )
        }
    }

    fun restoreNote(id: String) {
        viewModelScope.launch {
            repository.restoreFromTrash(id)
            showToast("Note restored to library")
        }
    }

    fun restoreAllTrash() {
        viewModelScope.launch {
            repository.restoreAllFromTrash()
            showToast("All notes restored")
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            showToast("Trash emptied")
        }
    }

    fun deletePermanently(id: String) {
        viewModelScope.launch {
            repository.deletePermanently(id)
            showToast("Permanently deleted")
        }
    }

    fun deleteAllNotes() {
        viewModelScope.launch {
            repository.deleteAllNotes()
            showToast("All notes deleted")
        }
    }

    // --- Clock / Timer Actions ---
    fun timerSetMinutes(mins: Int) {
        _timerState.value = _timerState.value.copy(durationMinutes = mins.coerceIn(1, 999))
    }

    fun timerStart(minutes: Int = _timerState.value.durationMinutes) {
        val totalMs = minutes * 60_000L
        val now = System.currentTimeMillis()
        _timerState.value = _timerState.value.copy(
            isRunning = true,
            totalMs = totalMs,
            endTimeMs = now + totalMs,
            remainingMs = totalMs,
            durationMinutes = minutes
        )
        ensureClockTickerRunning()
    }

    fun timerPause() {
        val now = System.currentTimeMillis()
        val current = _timerState.value
        val remain = maxOf(0L, current.endTimeMs - now)
        _timerState.value = current.copy(
            isRunning = false,
            remainingMs = remain
        )
    }

    fun timerResume() {
        val current = _timerState.value
        val now = System.currentTimeMillis()
        val remain = if (current.remainingMs > 0) current.remainingMs else 1000L
        _timerState.value = current.copy(
            isRunning = true,
            endTimeMs = now + remain,
            remainingMs = remain
        )
        ensureClockTickerRunning()
    }

    fun timerReset() {
        _timerState.value = _timerState.value.copy(
            isRunning = false,
            totalMs = 0,
            remainingMs = 0,
            endTimeMs = 0
        )
        _runningPill.value = RunningPillInfo(visible = false)
    }

    // --- Stopwatch Actions ---
    fun swStart() {
        val now = System.currentTimeMillis()
        _stopwatchState.value = _stopwatchState.value.copy(
            isRunning = true,
            startAtMs = now,
            currentElapsedMs = _stopwatchState.value.accumulatedMs
        )
        ensureClockTickerRunning()
    }

    fun swPause() {
        val now = System.currentTimeMillis()
        val current = _stopwatchState.value
        val elapsed = current.accumulatedMs + if (current.isRunning) (now - current.startAtMs) else 0L
        _stopwatchState.value = current.copy(
            isRunning = false,
            accumulatedMs = elapsed,
            currentElapsedMs = elapsed
        )
    }

    fun swResume() {
        val now = System.currentTimeMillis()
        _stopwatchState.value = _stopwatchState.value.copy(
            isRunning = true,
            startAtMs = now,
            currentElapsedMs = _stopwatchState.value.accumulatedMs
        )
        ensureClockTickerRunning()
    }

    fun swLap() {
        val now = System.currentTimeMillis()
        val current = _stopwatchState.value
        val elapsed = current.accumulatedMs + if (current.isRunning) (now - current.startAtMs) else 0L
        val updatedLaps = (listOf(elapsed) + current.laps).take(30)
        _stopwatchState.value = current.copy(
            laps = updatedLaps,
            currentElapsedMs = elapsed
        )
    }

    fun swReset() {
        _stopwatchState.value = StopwatchState()
    }

    // --- Device Folder Sync Actions ---
    fun setSyncFolder(uriString: String?, folderName: String?) {
        viewModelScope.launch {
            repository.updateSettings {
                it.copy(syncFolderUri = uriString, syncFolderName = folderName)
            }
            if (uriString != null) {
                showToast("Sync folder set: ${folderName ?: "Device Folder"}")
                syncDeviceFolder(silent = false)
            } else {
                showToast("Sync folder cleared")
            }
        }
    }

    fun syncDeviceFolder(silent: Boolean = false) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val folderUri = currentSettings.syncFolderUri
            if (folderUri.isNullOrEmpty()) {
                // If no custom folder is set, perform device-wide HTML scan
                syncAllDeviceHtmlFiles(silent = silent)
                return@launch
            }
            val res = repository.syncFromDeviceFolder(folderUri)
            if (res.error != null) {
                if (!silent) showToast("Sync error: ${res.error}")
            } else {
                val parts = mutableListOf<String>()
                if (res.newCount > 0) parts.add("${res.newCount} new")
                if (res.updatedCount > 0) parts.add("${res.updatedCount} updated")
                if (res.dupeCount > 0) parts.add("${res.dupeCount} unchanged")
                val summary = if (parts.isNotEmpty()) "Synced: ${parts.joinToString(", ")}" else "No HTML/text files found in folder"
                if (!silent || res.newCount > 0 || res.updatedCount > 0) {
                    showToast(summary)
                }
            }
        }
    }

    fun syncAllDeviceHtmlFiles(silent: Boolean = false) {
        viewModelScope.launch {
            val res = repository.syncAllDeviceHtmlFiles()
            if (res.error != null) {
                if (!silent) showToast("Sync error: ${res.error}")
            } else {
                val parts = mutableListOf<String>()
                if (res.newCount > 0) parts.add("${res.newCount} new")
                if (res.updatedCount > 0) parts.add("${res.updatedCount} updated")
                if (res.dupeCount > 0) parts.add("${res.dupeCount} indexed")
                val summary = if (parts.isNotEmpty()) "HTML Sync: ${parts.joinToString(", ")}" else "Device HTML files up to date (${res.scanned} scanned)"
                if (!silent || res.newCount > 0 || res.updatedCount > 0) {
                    showToast(summary)
                }
            }
        }
    }

    fun syncAllDevicePdfFiles(silent: Boolean = false) {
        viewModelScope.launch {
            val res = repository.syncAllDevicePdfFiles()
            if (res.error != null) {
                if (!silent) showToast("PDF Sync error: ${res.error}")
            } else {
                val parts = mutableListOf<String>()
                if (res.newCount > 0) parts.add("${res.newCount} new")
                if (res.updatedCount > 0) parts.add("${res.updatedCount} updated")
                if (res.dupeCount > 0) parts.add("${res.dupeCount} indexed")
                val summary = if (parts.isNotEmpty()) "PDF Sync: ${parts.joinToString(", ")}" else "Device PDF files up to date (${res.scanned} scanned)"
                if (!silent || res.newCount > 0 || res.updatedCount > 0) {
                    showToast(summary)
                }
            }
        }
    }

    fun syncFullDevice(silent: Boolean = false, onResult: ((scanned: Int, imported: Int) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val htmlRes = repository.syncAllDeviceHtmlFiles()
                val pdfRes = repository.syncAllDevicePdfFiles()
                val totalScanned = htmlRes.scanned + pdfRes.scanned
                val totalNew = htmlRes.newCount + pdfRes.newCount
                val totalUpd = htmlRes.updatedCount + pdfRes.updatedCount

                val parts = mutableListOf<String>()
                if (totalNew > 0) parts.add("$totalNew new notes/docs")
                if (totalUpd > 0) parts.add("$totalUpd updated")
                val summary = if (parts.isNotEmpty()) {
                    "Device Sync Complete: ${parts.joinToString(", ")}"
                } else {
                    "Device Scan Complete: $totalScanned files checked (Up to date)"
                }
                if (!silent) {
                    showToast(summary)
                }
                onResult?.invoke(totalScanned, totalNew)
            } catch (e: Exception) {
                if (!silent) {
                    showToast("Sync encountered an issue: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    fun addNoteDirect(title: String, content: String, type: String = "text", category: String = "My Notes", onCreated: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val cleanTitle = title.trim().ifBlank { "Untitled Note" }
            val newNote = NoteEntity(
                id = UUID.randomUUID().toString().take(12),
                title = cleanTitle,
                type = type,
                content = content,
                category = category,
                pinned = false,
                source = null,
                createdAt = now,
                updatedAt = now,
                hash = HashUtil.noteHash(cleanTitle, content)
            )
            repository.insertNote(newNote)
            showToast("Saved note: $cleanTitle")
            onCreated?.invoke(newNote.id)
        }
    }

    fun syncCurrentMode(silent: Boolean = false) {
        if (settings.value.readerMode == "pdf") {
            syncAllDevicePdfFiles(silent = silent)
        } else {
            val folderUri = settings.value.syncFolderUri
            if (!folderUri.isNullOrEmpty()) {
                syncDeviceFolder(silent = silent)
            } else {
                syncAllDeviceHtmlFiles(silent = silent)
            }
        }
    }

    fun ensureNoteContentLoaded(note: NoteEntity) {
        if (note.type == "html" && note.content.isBlank() && !note.source.isNullOrBlank()) {
            viewModelScope.launch {
                repository.ensureNoteContentLoaded(note)
            }
        }
    }

    // --- Backup & Restore ---
    fun exportBackupJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportBackupJson()
            onReady(json)
            showToast("Backup exported")
        }
    }

    fun restoreBackupJson(jsonString: String) {
        viewModelScope.launch {
            val res = repository.importBackupJson(jsonString)
            if (res.errorCount > 0 && res.newCount == 0) {
                showToast("Invalid backup file")
            } else {
                showToast("Backup restored — ${res.newCount} added, ${res.dupeCount} duplicates skipped")
                _selectedFilter.value = "all"
                _searchQuery.value = ""
            }
        }
    }

    fun importFiles(fileList: List<Pair<String, String>>) {
        viewModelScope.launch {
            var neu = 0
            var upd = 0
            var dupe = 0
            for ((name, content) in fileList) {
                val res = repository.importTextFile(name, content)
                when (res) {
                    "new" -> neu++
                    "updated" -> upd++
                    else -> dupe++
                }
            }
            val parts = mutableListOf<String>()
            if (neu > 0) parts.add("$neu new")
            if (upd > 0) parts.add("$upd updated")
            if (dupe > 0) parts.add("$dupe duplicate${if (dupe == 1) "" else "s"} skipped")

            showToast(if (parts.isNotEmpty()) "Imported — ${parts.joinToString(", ")}" else "Nothing new (all duplicates)")
            if (neu > 0 || upd > 0) {
                _selectedFilter.value = "all"
                _searchQuery.value = ""
            }
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun importPdfUris(uris: List<android.net.Uri>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            for (uri in uris) {
                val name = getFileName(context, uri) ?: "Document_${System.currentTimeMillis()}.pdf"
                importPdf(uri, name)
            }
        }
    }

    fun importFileUris(uris: List<android.net.Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val pairs = mutableListOf<Pair<String, String>>()
            for (uri in uris) {
                try {
                    val name = getFileName(context, uri) ?: "Note_${System.currentTimeMillis()}.txt"
                    val content = context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().readText()
                    } ?: ""
                    if (content.isNotBlank()) {
                        pairs.add(Pair(name, content))
                    }
                } catch (_: Exception) {}
            }
            if (pairs.isNotEmpty()) {
                importFiles(pairs)
            }
        }
    }

    private fun getFileName(context: Context, uri: android.net.Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    fun importPdf(uri: android.net.Uri, name: String) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val cleanTitle = name.substringBeforeLast(".").replace(Regex("[_-]+"), " ").trim().ifBlank { "PDF Document" }
                val existing = allNotes.value.find {
                    it.type == "pdf" && (it.title.equals(cleanTitle, ignoreCase = true) || it.source == uri.toString())
                }
                if (existing != null) {
                    showToast("PDF already in library: ${existing.displayTitle}")
                    return@launch
                }

                val note = com.example.util.PdfHelper.saveImportedPdfUri(context, uri, name)
                repository.insertNote(note)
                showToast("Imported ${note.title} successfully")
            } catch (e: Exception) {
                showToast("Failed to import PDF: ${e.localizedMessage}")
            }
        }
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastFlow.emit(ToastEvent.Simple(message))
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application)
                    val repo = NoteRepository(db.noteDao(), application)
                    return NotesViewModel(application, repo) as T
                }
            }
    }
}
