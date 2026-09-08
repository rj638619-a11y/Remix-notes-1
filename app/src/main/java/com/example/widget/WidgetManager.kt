package com.example.widget

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.AppDatabase
import com.example.data.model.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WidgetTodoItem(
    val lineIndex: Int,
    val text: String,
    val isDone: Boolean
)

data class WidgetData(
    val mode: String, // "selected", "pinned", "recent", "todos"
    val notes: List<NoteEntity>,
    val currentIndex: Int,
    val activeNote: NoteEntity?,
    val todoItems: List<WidgetTodoItem>,
    val totalNotesCount: Int
)

object WidgetManager {
    private const val PREFS_NAME = "glass_notes_widget_prefs"
    const val KEY_SELECTED_NOTE_ID = "selected_note_id"
    const val KEY_WIDGET_MODE = "widget_mode"
    const val KEY_CYCLE_INDEX = "widget_cycle_index"

    const val MODE_SELECTED = "selected"
    const val MODE_PINNED = "pinned"
    const val MODE_RECENT = "recent"
    const val MODE_TODOS = "todos"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSelectedNoteId(context: Context): String? {
        return getPrefs(context).getString(KEY_SELECTED_NOTE_ID, null)
    }

    fun setSelectedNoteId(context: Context, noteId: String?) {
        getPrefs(context).edit()
            .putString(KEY_SELECTED_NOTE_ID, noteId)
            .putString(KEY_WIDGET_MODE, MODE_SELECTED)
            .putInt(KEY_CYCLE_INDEX, 0)
            .apply()
    }

    fun getWidgetMode(context: Context): String {
        return getPrefs(context).getString(KEY_WIDGET_MODE, MODE_SELECTED) ?: MODE_SELECTED
    }

    fun setWidgetMode(context: Context, mode: String) {
        getPrefs(context).edit()
            .putString(KEY_WIDGET_MODE, mode)
            .putInt(KEY_CYCLE_INDEX, 0)
            .apply()
    }

    fun getCycleIndex(context: Context): Int {
        return getPrefs(context).getInt(KEY_CYCLE_INDEX, 0)
    }

    fun setCycleIndex(context: Context, index: Int) {
        getPrefs(context).edit()
            .putInt(KEY_CYCLE_INDEX, index.coerceAtLeast(0))
            .apply()
    }

    suspend fun cycleNote(context: Context, direction: Int) = withContext(Dispatchers.IO) {
        val currentData = getWidgetData(context)
        if (currentData.notes.isEmpty()) return@withContext

        val count = currentData.notes.size
        var newIndex = (currentData.currentIndex + direction) % count
        if (newIndex < 0) newIndex += count

        setCycleIndex(context, newIndex)
    }

    suspend fun togglePinNote(context: Context, noteId: String) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val note = db.noteDao().getNoteByIdDirect(noteId) ?: return@withContext
            val updated = note.copy(pinned = !note.pinned, updatedAt = System.currentTimeMillis())
            db.noteDao().updateNote(updated)
            GlassNotesWidgetReceiver.updateAllWidgets(context)
        } catch (_: Exception) {
        }
    }

    suspend fun toggleTodoItem(context: Context, noteId: String, lineIndex: Int) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val note = db.noteDao().getNoteByIdDirect(noteId) ?: return@withContext
            val updatedContent = toggleTodoInContent(note.content, lineIndex)
            db.noteDao().updateNoteContent(noteId, updatedContent, System.currentTimeMillis())
            GlassNotesWidgetReceiver.updateAllWidgets(context)
        } catch (_: Exception) {
        }
    }

    fun parseTodoItems(content: String): List<WidgetTodoItem> {
        val items = mutableListOf<WidgetTodoItem>()
        val lines = content.lines()
        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()
            val match = Regex("""^[-*]?\s*\[([ xX])\]\s*(.*)$""").find(trimmed)
            if (match != null) {
                val isDone = match.groupValues[1].equals("x", ignoreCase = true)
                val text = match.groupValues[2].trim()
                items.add(WidgetTodoItem(lineIndex = index, text = text, isDone = isDone))
            }
        }
        return items
    }

    private fun toggleTodoInContent(content: String, targetLineIndex: Int): String {
        val lines = content.lines().toMutableList()
        if (targetLineIndex in lines.indices) {
            val line = lines[targetLineIndex]
            val match = Regex("""^(\s*[-*]?\s*\[)([ xX])(\]\s*.*)$""").find(line)
            if (match != null) {
                val prefix = match.groupValues[1]
                val currentChecked = match.groupValues[2].equals("x", ignoreCase = true)
                val suffix = match.groupValues[3]
                val newChar = if (currentChecked) " " else "x"
                lines[targetLineIndex] = "$prefix$newChar$suffix"
            }
        }
        return lines.joinToString("\n")
    }

    suspend fun getWidgetData(context: Context): WidgetData = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val allNotes = try {
            db.noteDao().getAllNotesDirect()
        } catch (_: Exception) {
            emptyList()
        }

        val mode = getWidgetMode(context)
        val selectedId = getSelectedNoteId(context)

        val candidateNotes: List<NoteEntity> = when (mode) {
            MODE_SELECTED -> {
                val selectedNote = if (!selectedId.isNullOrBlank()) {
                    allNotes.find { it.id == selectedId }
                } else null
                if (selectedNote != null) {
                    listOf(selectedNote)
                } else {
                    // Fallback to pinned or first available note
                    val pinned = allNotes.filter { it.pinned }
                    if (pinned.isNotEmpty()) pinned else allNotes.take(5)
                }
            }
            MODE_PINNED -> {
                val pinned = allNotes.filter { it.pinned }
                if (pinned.isNotEmpty()) pinned else allNotes.take(5)
            }
            MODE_RECENT -> {
                allNotes.sortedByDescending { it.updatedAt }.take(10)
            }
            MODE_TODOS -> {
                val notesWithTodos = allNotes.filter { note ->
                    note.content.contains("[ ]") || note.content.contains("[x]") || note.content.contains("[X]")
                }
                if (notesWithTodos.isNotEmpty()) notesWithTodos else allNotes.take(5)
            }
            else -> allNotes.take(5)
        }

        val storedIndex = getCycleIndex(context)
        val safeIndex = if (candidateNotes.isNotEmpty()) {
            storedIndex.coerceIn(0, candidateNotes.size - 1)
        } else {
            0
        }

        val activeNote = candidateNotes.getOrNull(safeIndex)
        val todos = if (activeNote != null) parseTodoItems(activeNote.content) else emptyList()

        WidgetData(
            mode = mode,
            notes = candidateNotes,
            currentIndex = safeIndex,
            activeNote = activeNote,
            todoItems = todos,
            totalNotesCount = allNotes.size
        )
    }
}
