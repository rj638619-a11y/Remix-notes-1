package com.example.ui.sheets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeminiClient
import com.example.data.api.GeminiResult
import com.example.data.api.GeminiSearchMode
import com.example.data.model.AiHistoryItem
import com.example.data.model.NoteEntity
import com.example.data.model.ChatSession
import com.example.data.model.ChatMessage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import com.example.ui.editor.HtmlPreviewView
import com.example.ui.theme.GlassTheme
import com.example.ui.viewmodel.GeminiQueryState
import com.example.util.DateFormatter
import com.example.util.VibrationHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiSearchSheet(
    initialQuery: String,
    initialMode: GeminiSearchMode,
    geminiState: GeminiQueryState,
    allNotes: List<NoteEntity>,
    aiHistory: List<AiHistoryItem>,
    currentReaderMode: String,
    geminiApiKey: String,
    onSetGeminiApiKey: (String) -> Unit,
    onDismiss: () -> Unit,
    onQuery: (String, GeminiSearchMode) -> Unit,
    onSaveAsNote: (GeminiResult, String) -> Unit,
    onOpenCitedNote: (String) -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    onClearHistory: () -> Unit,
    chatSessions: List<ChatSession>,
    activeChatSession: ChatSession?,
    chatDetailedAnswers: Boolean,
    chatSelectedModel: String,
    onSetChatDetailedAnswers: (Boolean) -> Unit,
    onSetChatSelectedModel: (String) -> Unit,
    onSendChatPrompt: (String) -> Unit,
    onStartNewChat: () -> Unit,
    onSelectChatSession: (String) -> Unit,
    onDeleteChatSession: (String) -> Unit,
    onClearAllChats: () -> Unit
) {
    val colors = GlassTheme.colors
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentPrompt by remember(initialQuery) { mutableStateOf(initialQuery) }
    var selectedMode by remember(initialMode) { mutableStateOf(initialMode) }
    var activeAiTab by remember { mutableStateOf("chat") } // "chat", "ask", or "history"
    var previewMode by remember { mutableStateOf("preview") } // "preview" or "code"
    var showCopiedToast by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyDraft by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }

    val isKeyConnected = remember(geminiApiKey) {
        GeminiClient.isValidGeminiApiKey(geminiApiKey) || GeminiClient.isValidGeminiApiKey(GeminiClient.getApiKey())
    }

    val htmlCount = remember(allNotes) { allNotes.count { it.type == "html" } }
    val pdfCount = remember(allNotes) { allNotes.count { it.type == "pdf" } }
    val textCount = remember(allNotes) { allNotes.count { it.type == "text" } }

    LaunchedEffect(Unit) {
        if (initialQuery.isNotBlank() && geminiState is GeminiQueryState.Idle) {
            onQuery(initialQuery, initialMode)
        }
    }

    // Glow and rotation animations
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_aura")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        contentColor = colors.text,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.hairline)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
        ) {
            // Header: Title, Mode indicator & Access pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF8AB4F8),
                                        Color(0xFFC58AF9),
                                        Color(0xFFFF8BCB)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini AI",
                            tint = Color.White,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(if (geminiState is GeminiQueryState.Loading) rotationAngle else 0f)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Gemini AI",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.text
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (currentReaderMode == "pdf") Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF3B82F6).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (currentReaderMode == "pdf") "PDF Drive" else "HTML Notes",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentReaderMode == "pdf") Color(0xFFEF4444) else Color(0xFF3B82F6)
                                )
                            }
                        }
                        Text(
                            text = "Accessing ${allNotes.size} items ($htmlCount HTML, $pdfCount PDF, $textCount Text)",
                            fontSize = 11.5.sp,
                            color = colors.textTertiary
                        )
                    }
                }

                // Three Tab Switcher & Key Config
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.field)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (activeAiTab == "chat") colors.card else Color.Transparent)
                            .clickable {
                                VibrationHelper.vibrate(context, 6)
                                activeAiTab = "chat"
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chat",
                            fontSize = 12.5.sp,
                            fontWeight = if (activeAiTab == "chat") FontWeight.Bold else FontWeight.Normal,
                            color = if (activeAiTab == "chat") colors.text else colors.textTertiary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (activeAiTab == "ask") colors.card else Color.Transparent)
                            .clickable {
                                VibrationHelper.vibrate(context, 6)
                                activeAiTab = "ask"
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AI Tools",
                            fontSize = 12.5.sp,
                            fontWeight = if (activeAiTab == "ask") FontWeight.Bold else FontWeight.Normal,
                            color = if (activeAiTab == "ask") colors.text else colors.textTertiary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (activeAiTab == "history") colors.card else Color.Transparent)
                            .clickable {
                                VibrationHelper.vibrate(context, 6)
                                activeAiTab = "history"
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "History",
                                fontSize = 12.5.sp,
                                fontWeight = if (activeAiTab == "history") FontWeight.Bold else FontWeight.Normal,
                                color = if (activeAiTab == "history") colors.text else colors.textTertiary
                            )
                            if (aiHistory.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF3B82F6).copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${aiHistory.size}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3B82F6)
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick API Key Status / Config Button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isKeyConnected) Color(0xFF10B981).copy(alpha = 0.16f) else Color(0xFFF59E0B).copy(alpha = 0.16f))
                        .clickable {
                            apiKeyDraft = geminiApiKey
                            showApiKeyDialog = true
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Configure Gemini API Key",
                        tint = if (isKeyConnected) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

            if (showApiKeyDialog) {
                AlertDialog(
                    onDismissRequest = { showApiKeyDialog = false },
                    title = {
                        Text(
                            text = "Gemini AI API Key",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Connect your personal Google Gemini API key to activate live cloud AI answers, note generation, and HTML coding.",
                                fontSize = 13.5.sp,
                                color = colors.textSecondary,
                                lineHeight = 19.sp
                            )
                            OutlinedTextField(
                                value = apiKeyDraft,
                                onValueChange = { apiKeyDraft = it },
                                label = { Text("API Key (starts with AIzaSy...)") },
                                placeholder = { Text("Paste your Gemini API key") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        val clip = clipboard.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            apiKeyDraft = clip.trim()
                                        }
                                    }
                                ) {
                                    Text("Paste")
                                }
                                TextButton(
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse("https://aistudio.google.com/app/apikey")
                                            )
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                ) {
                                    Text("Get Free Key ↗")
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onSetGeminiApiKey(apiKeyDraft.trim())
                                showApiKeyDialog = false
                            }
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        if (geminiApiKey.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    onSetGeminiApiKey("")
                                    apiKeyDraft = ""
                                    showApiKeyDialog = false
                                }
                            ) {
                                Text("Clear Key", color = Color(0xFFEF4444))
                            }
                        } else {
                            TextButton(onClick = { showApiKeyDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Content Area based on Tab
            Crossfade(
                targetState = activeAiTab,
                animationSpec = tween(180),
                label = "ai_tab_crossfade"
            ) { tab ->
                when (tab) {
                    "chat" -> {
                        AiChatbotView(
                            chatSessions = chatSessions,
                            activeChatSession = activeChatSession,
                            chatDetailedAnswers = chatDetailedAnswers,
                            chatSelectedModel = chatSelectedModel,
                            geminiState = geminiState,
                            onSetChatDetailedAnswers = onSetChatDetailedAnswers,
                            onSetChatSelectedModel = onSetChatSelectedModel,
                            onSendChatPrompt = onSendChatPrompt,
                            onStartNewChat = onStartNewChat,
                            onSelectChatSession = onSelectChatSession,
                            onDeleteChatSession = onDeleteChatSession,
                            onClearAllChats = onClearAllChats,
                            onSaveAsNote = onSaveAsNote,
                            isKeyConnected = isKeyConnected,
                            onConfigureApiKey = { showApiKeyDialog = true }
                        )
                    }
                    "history" -> {
                        AiHistoryView(
                            historyList = aiHistory,
                            currentModeType = currentReaderMode,
                            onClearAll = onClearHistory,
                            onDeleteItem = onDeleteHistoryItem,
                            onSelectHistory = { item ->
                                currentPrompt = item.query
                                selectedMode = try {
                                    GeminiSearchMode.valueOf(item.aiSearchMode)
                                } catch (_: Exception) {
                                    GeminiSearchMode.ASK_NOTES
                                }
                                activeAiTab = "ask"
                                onQuery(item.query, selectedMode)
                            },
                            onSaveAsNote = { item ->
                                val res = GeminiResult(
                                    title = item.responseTitle.ifBlank { item.query },
                                    content = item.responseContent,
                                    suggestedType = item.suggestedType
                                )
                                onSaveAsNote(res, item.suggestedType)
                            },
                            onCopyText = { text ->
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("AI Output", text))
                                VibrationHelper.vibrate(context, 10)
                                showCopiedToast = true
                            }
                        )
                    }
                    else -> {
                        AiPromptAndResultView(
                            currentPrompt = currentPrompt,
                            onPromptChange = { currentPrompt = it },
                            selectedMode = selectedMode,
                            onModeChange = { selectedMode = it },
                            geminiState = geminiState,
                            allNotes = allNotes,
                            currentReaderMode = currentReaderMode,
                            previewMode = previewMode,
                            onPreviewModeChange = { previewMode = it },
                            glowAlpha = glowAlpha,
                            onSubmit = { q, m ->
                                VibrationHelper.vibrate(context, 8)
                                onQuery(q, m)
                            },
                            onSaveAsNote = onSaveAsNote,
                            onOpenCitedNote = onOpenCitedNote,
                            onConfigureApiKey = { showApiKeyDialog = true },
                            onCopyText = { text ->
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("AI Result", text))
                                VibrationHelper.vibrate(context, 10)
                                showCopiedToast = true
                            }
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(showCopiedToast) {
        if (showCopiedToast) {
            delay(1800)
            showCopiedToast = false
        }
    }
}

@Composable
private fun AiPromptAndResultView(
    currentPrompt: String,
    onPromptChange: (String) -> Unit,
    selectedMode: GeminiSearchMode,
    onModeChange: (GeminiSearchMode) -> Unit,
    geminiState: GeminiQueryState,
    allNotes: List<NoteEntity>,
    currentReaderMode: String,
    previewMode: String,
    onPreviewModeChange: (String) -> Unit,
    glowAlpha: Float,
    onSubmit: (String, GeminiSearchMode) -> Unit,
    onSaveAsNote: (GeminiResult, String) -> Unit,
    onOpenCitedNote: (String) -> Unit,
    onConfigureApiKey: () -> Unit,
    onCopyText: (String) -> Unit
) {
    val colors = GlassTheme.colors
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        // Quick Action Presets (Horizontal bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (currentReaderMode == "pdf") {
                PresetChip(
                    icon = Icons.Default.Search,
                    label = "Ask PDFs",
                    isSelected = selectedMode == GeminiSearchMode.ASK_NOTES,
                    onClick = {
                        onModeChange(GeminiSearchMode.ASK_NOTES)
                    }
                )
                PresetChip(
                    icon = Icons.Default.Summarize,
                    label = "Summarize All PDFs",
                    isSelected = selectedMode == GeminiSearchMode.SUMMARIZE_ALL,
                    onClick = {
                        onModeChange(GeminiSearchMode.SUMMARIZE_ALL)
                        onPromptChange("Summarize key themes and topics across all PDF documents")
                    }
                )
                PresetChip(
                    icon = Icons.Default.FlashOn,
                    label = "Extract PDF Tasks",
                    isSelected = selectedMode == GeminiSearchMode.EXTRACT_TASKS,
                    onClick = {
                        onModeChange(GeminiSearchMode.EXTRACT_TASKS)
                        onPromptChange("Extract all action items and to-dos from my PDF library")
                    }
                )
                PresetChip(
                    icon = Icons.Default.Description,
                    label = "Create Study Notes",
                    isSelected = selectedMode == GeminiSearchMode.GENERATE_NOTE,
                    onClick = {
                        onModeChange(GeminiSearchMode.GENERATE_NOTE)
                        if (currentPrompt.isBlank()) onPromptChange("Create comprehensive study notes from my documents")
                    }
                )
            } else {
                PresetChip(
                    icon = Icons.Default.Search,
                    label = "Ask Notes",
                    isSelected = selectedMode == GeminiSearchMode.ASK_NOTES,
                    onClick = {
                        onModeChange(GeminiSearchMode.ASK_NOTES)
                    }
                )
                PresetChip(
                    icon = Icons.Default.Summarize,
                    label = "Summarize All",
                    isSelected = selectedMode == GeminiSearchMode.SUMMARIZE_ALL,
                    onClick = {
                        onModeChange(GeminiSearchMode.SUMMARIZE_ALL)
                        onPromptChange("Summarize all my notes and highlight main ideas")
                    }
                )
                PresetChip(
                    icon = Icons.Default.Code,
                    label = "Generate HTML Widget",
                    isSelected = selectedMode == GeminiSearchMode.GENERATE_HTML,
                    onClick = {
                        onModeChange(GeminiSearchMode.GENERATE_HTML)
                        if (currentPrompt.isBlank()) onPromptChange("Interactive financial loan calculator with charts")
                    }
                )
                PresetChip(
                    icon = Icons.Default.Description,
                    label = "Draft Note",
                    isSelected = selectedMode == GeminiSearchMode.GENERATE_NOTE,
                    onClick = {
                        onModeChange(GeminiSearchMode.GENERATE_NOTE)
                        if (currentPrompt.isBlank()) onPromptChange("Structured weekly planner and goals template")
                    }
                )
                PresetChip(
                    icon = Icons.Default.FlashOn,
                    label = "Extract Checklist",
                    isSelected = selectedMode == GeminiSearchMode.EXTRACT_TASKS,
                    onClick = {
                        onModeChange(GeminiSearchMode.EXTRACT_TASKS)
                        onPromptChange("Extract all action items and tasks")
                    }
                )
                PresetChip(
                    icon = Icons.Default.Bookmark,
                    label = "Smart Tags",
                    isSelected = selectedMode == GeminiSearchMode.SMART_TAGS,
                    onClick = {
                        onModeChange(GeminiSearchMode.SMART_TAGS)
                        onPromptChange("Generate categories and taxonomy for my notes")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Prompt Input Field Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.field)
                .border(
                    width = 1.dp,
                    color = if (geminiState is GeminiQueryState.Loading) {
                        Color(0xFF8AB4F8).copy(alpha = glowAlpha)
                    } else colors.hairline,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
        ) {
            Column {
                BasicTextField(
                    value = currentPrompt,
                    onValueChange = onPromptChange,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = colors.text,
                        fontFamily = FontFamily.Default
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    decorationBox = { innerTextField ->
                        if (currentPrompt.isEmpty()) {
                            Text(
                                text = if (currentReaderMode == "pdf") {
                                    "Ask questions across all PDF files or request synthesis..."
                                } else {
                                    "Ask anything across your HTML notes, or generate widgets..."
                                },
                                fontSize = 14.5.sp,
                                color = colors.textTertiary
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentPrompt.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onPromptChange("") }
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = colors.textTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Send Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (currentPrompt.isNotBlank() && geminiState !is GeminiQueryState.Loading) {
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                                    )
                                } else {
                                    SolidColor(colors.card)
                                }
                            )
                            .clickable(enabled = currentPrompt.isNotBlank() && geminiState !is GeminiQueryState.Loading) {
                                onSubmit(currentPrompt, selectedMode)
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Run",
                                tint = if (currentPrompt.isNotBlank()) Color.White else colors.textTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Ask Gemini",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (currentPrompt.isNotBlank()) Color.White else colors.textTertiary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Results Section
        when (val state = geminiState) {
            is GeminiQueryState.Loading -> {
                AiLoadingCard(prompt = state.prompt, glowAlpha = glowAlpha)
            }

            is GeminiQueryState.Success -> {
                AiSuccessCard(
                    result = state.result,
                    allNotes = allNotes,
                    previewMode = previewMode,
                    onPreviewModeChange = onPreviewModeChange,
                    onSaveAsNote = onSaveAsNote,
                    onOpenCitedNote = onOpenCitedNote,
                    onCopyText = onCopyText
                )
            }

            is GeminiQueryState.Error -> {
                AiErrorCard(
                    errorMessage = state.message,
                    onRetry = { onSubmit(state.prompt, state.mode) },
                    onConfigureApiKey = onConfigureApiKey
                )
            }

            GeminiQueryState.Idle -> {
                // Friendly hints
                AiIdleState(currentReaderMode = currentReaderMode)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AiLoadingCard(prompt: String, glowAlpha: Float) {
    val colors = GlassTheme.colors
    var phraseIndex by remember { mutableIntStateOf(0) }
    val phrases = listOf(
        "Gemini is analyzing your request...",
        "Consulting your personal knowledge vault...",
        "Synthesizing references and citations...",
        "Refining response clarity...",
        "Applying modern glass-morphic layout...",
        "Formatting code block styles..."
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(1600)
            phraseIndex = (phraseIndex + 1) % phrases.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, Color(0xFF8AB4F8).copy(alpha = glowAlpha), RoundedCornerShape(16.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = phrases[phraseIndex],
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AiSuccessCard(
    result: GeminiResult,
    allNotes: List<NoteEntity>,
    previewMode: String,
    onPreviewModeChange: (String) -> Unit,
    onSaveAsNote: (GeminiResult, String) -> Unit,
    onOpenCitedNote: (String) -> Unit,
    onCopyText: (String) -> Unit
) {
    val colors = GlassTheme.colors
    val context = LocalContext.current
    val isHtml = result.suggestedType == "html"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, colors.hairline, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Result Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = if (isHtml) "Interactive HTML Document" else "Synthesized Note",
                        fontSize = 11.5.sp,
                        color = if (isHtml) Color(0xFF10B981) else Color(0xFF3B82F6),
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!result.modelUsed.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.field)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = result.modelUsed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }

            // Preview vs Code toggle for HTML
            if (isHtml) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.field)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (previewMode == "preview") colors.card else Color.Transparent)
                            .clickable { onPreviewModeChange("preview") }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Preview",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (previewMode == "code") colors.card else Color.Transparent)
                            .clickable { onPreviewModeChange("code") }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Code",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                    }
                }
            }
        }

        // Automatic Model Fallback Notice Banner
        if (!result.fallbackNotice.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF59E0B).copy(alpha = 0.14f))
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = result.fallbackNotice,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (colors.isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content Body
        if (isHtml && previewMode == "preview") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, colors.hairline, RoundedCornerShape(10.dp))
            ) {
                HtmlPreviewView(
                    htmlContent = result.content,
                    fontSize = 15,
                    isZenMode = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.field)
                    .padding(12.dp)
            ) {
                Text(
                    text = result.content,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = colors.text,
                    fontFamily = if (isHtml) FontFamily.Monospace else FontFamily.Default
                )
            }
        }

        // Cited Sources
        if (result.citedNoteIds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Referenced in Library:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textTertiary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (id in result.citedNoteIds) {
                    val matching = allNotes.firstOrNull { it.id == id }
                    if (matching != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.field)
                                .border(1.dp, colors.hairline, RoundedCornerShape(8.dp))
                                .clickable { onOpenCitedNote(matching.id) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (matching.type == "pdf") Icons.Default.PictureAsPdf else Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (matching.type == "pdf") Color(0xFFEF4444) else colors.accent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = matching.displayTitle,
                                    fontSize = 11.5.sp,
                                    color = colors.text,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))))
                    .clickable {
                        onSaveAsNote(result, if (isHtml) "html" else "text")
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Note",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Save as Note",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.field)
                    .border(1.dp, colors.hairline, RoundedCornerShape(12.dp))
                    .clickable { onCopyText(result.content) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = colors.text,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Copy",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )
                }
            }
        }
    }
}

@Composable
private fun AiHistoryView(
    historyList: List<AiHistoryItem>,
    currentModeType: String,
    onClearAll: () -> Unit,
    onDeleteItem: (String) -> Unit,
    onSelectHistory: (AiHistoryItem) -> Unit,
    onSaveAsNote: (AiHistoryItem) -> Unit,
    onCopyText: (String) -> Unit
) {
    val colors = GlassTheme.colors
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        // Subheader with Clear All action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${if (currentModeType == "pdf") "PDF Drive" else "HTML Notes"} Query History",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary
            )

            if (historyList.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClearAll)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Clear History",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (historyList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.field),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "No saved AI history for this mode",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
                Text(
                    text = "Your questions and generated responses will appear here separately for HTML notes and PDF Drive.",
                    fontSize = 12.5.sp,
                    color = colors.textTertiary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (item in historyList) {
                    AiHistoryCard(
                        item = item,
                        onOpen = { onSelectHistory(item) },
                        onSaveAsNote = { onSaveAsNote(item) },
                        onCopy = { onCopyText(item.responseContent) },
                        onDelete = { onDeleteItem(item.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AiHistoryCard(
    item: AiHistoryItem,
    onOpen: () -> Unit,
    onSaveAsNote: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = GlassTheme.colors
    var isExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .border(1.dp, colors.hairline, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF3B82F6).copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.aiSearchMode.replace("_", " "),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6)
                        )
                    }
                    Text(
                        text = DateFormatter.groupOf(item.timestamp),
                        fontSize = 11.sp,
                        color = colors.textTertiary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onDelete)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = colors.textTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = "“${item.query}”",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text
            )

            if (item.responseTitle.isNotBlank() && item.responseTitle != item.query) {
                Text(
                    text = item.responseTitle,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent
                )
            }

            Text(
                text = item.responseContent,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = colors.textSecondary,
                maxLines = if (isExpanded) 40 else 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.responseContent.length > 140) {
                    Text(
                        text = if (isExpanded) "Show less" else "Show more",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B82F6),
                        modifier = Modifier
                            .clickable { isExpanded = !isExpanded }
                            .padding(vertical = 4.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.field)
                            .clickable(onClick = onCopy)
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Copy",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.text
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.field)
                            .clickable(onClick = onSaveAsNote)
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Save Note",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.text
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF3B82F6).copy(alpha = 0.15f))
                            .clickable(onClick = onOpen)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Load in AI",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiChatbotView(
    chatSessions: List<ChatSession>,
    activeChatSession: ChatSession?,
    chatDetailedAnswers: Boolean,
    chatSelectedModel: String,
    geminiState: GeminiQueryState,
    onSetChatDetailedAnswers: (Boolean) -> Unit,
    onSetChatSelectedModel: (String) -> Unit,
    onSendChatPrompt: (String) -> Unit,
    onStartNewChat: () -> Unit,
    onSelectChatSession: (String) -> Unit,
    onDeleteChatSession: (String) -> Unit,
    onClearAllChats: () -> Unit,
    onSaveAsNote: (GeminiResult, String) -> Unit,
    isKeyConnected: Boolean,
    onConfigureApiKey: () -> Unit
) {
    val colors = GlassTheme.colors
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val listState = rememberLazyListState()

    var draftPrompt by remember { mutableStateOf("") }
    var showModelDialog by remember { mutableStateOf(false) }
    var isHistoryExpanded by remember { mutableStateOf(false) }

    // Auto scroll to bottom when new messages arrive
    val messages = activeChatSession?.messages ?: emptyList()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 12.dp)
    ) {
        // Top Control Row: Model, Detailed, History toggles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Model Selector Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.field)
                    .clickable { showModelDialog = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Selected Model",
                        tint = Color(0xFFC58AF9),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = GeminiClient.getModelDisplayName(chatSelectedModel),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // Detailed Mode Badge Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (chatDetailedAnswers) Color(0xFF10B981).copy(alpha = 0.15f) else colors.field
                    )
                    .clickable { onSetChatDetailedAnswers(!chatDetailedAnswers) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (chatDetailedAnswers) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = "Detailed Answers",
                        fontSize = 11.5.sp,
                        fontWeight = if (chatDetailedAnswers) FontWeight.Bold else FontWeight.Normal,
                        color = if (chatDetailedAnswers) Color(0xFF10B981) else colors.textSecondary
                    )
                }
            }

            // History Panel Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isHistoryExpanded) Color(0xFF3B82F6).copy(alpha = 0.15f) else colors.field
                    )
                    .clickable { isHistoryExpanded = !isHistoryExpanded }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Chat History",
                        tint = if (isHistoryExpanded) Color(0xFF3B82F6) else colors.textTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Chats (${chatSessions.size})",
                        fontSize = 11.5.sp,
                        fontWeight = if (isHistoryExpanded) FontWeight.Bold else FontWeight.Normal,
                        color = if (isHistoryExpanded) Color(0xFF3B82F6) else colors.textSecondary
                    )
                    Icon(
                        imageVector = if (isHistoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle",
                        tint = colors.textTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // New Chat Action Button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6))
                    .clickable {
                        onStartNewChat()
                        isHistoryExpanded = false
                        draftPrompt = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Chat Session",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Expanded Recent Chats Drawer (History Pane)
        AnimatedVisibility(visible = isHistoryExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.field)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Chat Sessions",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary
                    )
                    if (chatSessions.isNotEmpty()) {
                        Text(
                            text = "Clear All",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.clickable { onClearAllChats() }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                if (chatSessions.isEmpty()) {
                    Text(
                        text = "No previous conversations. Start a chat above!",
                        fontSize = 11.sp,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (session in chatSessions) {
                            val isActive = activeChatSession?.id == session.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isActive) Color(0xFF3B82F6).copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        onSelectChatSession(session.id)
                                        isHistoryExpanded = false
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubble,
                                        contentDescription = "Session",
                                        tint = if (isActive) Color(0xFF3B82F6) else colors.textTertiary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = session.title.ifBlank { "Untitled Chat" },
                                        fontSize = 12.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isActive) Color(0xFF3B82F6) else colors.text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Session",
                                    tint = colors.textTertiary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onDeleteChatSession(session.id) }
                                        .padding(2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Message Feed or Suggestions Board
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                // SUGGESTIONS ONBOARDING BOARD
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF8AB4F8), Color(0xFFC58AF9))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = "Chat Welcome",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Hello! I am your AI Chatbot",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )

                    Text(
                        text = "Fast, direct, and capable of controlling the full app. Ask me anything or trigger commands:",
                        fontSize = 12.sp,
                        color = colors.textTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Suggestion Chip Cards
                    Text(
                        text = "TAP A QUICK COMMAND TO RUN IT:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val suggestions = listOf(
                        "Create a checklist note for my travel packing list" to "Create a structured note with checklists for travel packing list",
                        "Start a 5 minute meditation timer" to "Start a 5 minute timer",
                        "Design an interactive HTML stopwatch widget" to "Create an interactive HTML stopwatch widget with nice gradients and start/pause scripts",
                        "Empty the note trash folder" to "Empty trash",
                        "Set note category of note to work" to "Change the category of my notes"
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        for ((label, prompt) in suggestions) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.field)
                                    .border(width = 1.dp, color = colors.hairline, shape = RoundedCornerShape(12.dp))
                                    .clickable {
                                        draftPrompt = prompt
                                        onSendChatPrompt(prompt)
                                        draftPrompt = ""
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = "Command",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.text
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // CHAT FEED
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp, top = 4.dp)
                ) {
                    items(messages) { message ->
                        val isUser = message.sender == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(0.85f),
                                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isUser) 16.dp else 2.dp,
                                                bottomEnd = if (isUser) 2.dp else 16.dp
                                            )
                                        )
                                        .background(
                                            if (isUser) Color(0xFF3B82F6) else colors.field
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = message.content,
                                        fontSize = 13.5.sp,
                                        color = if (isUser) Color.White else colors.text,
                                        fontWeight = FontWeight.Normal
                                    )
                                }

                                if (!isUser) {
                                    // AI Bubble Footer Actions
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                    ) {
                                        Text(
                                            text = "via ${message.modelUsed ?: GeminiClient.getModelDisplayName(chatSelectedModel)}",
                                            fontSize = 9.5.sp,
                                            color = colors.textTertiary,
                                            fontWeight = FontWeight.Normal
                                        )
                                        
                                        // Copy Action
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy text",
                                            tint = colors.textTertiary,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                    cm?.setPrimaryClip(ClipData.newPlainText("AI Message", message.content))
                                                    VibrationHelper.vibrate(context, 10)
                                                    android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(2.dp)
                                        )

                                        // Save as Note Action
                                        Icon(
                                            imageVector = Icons.Default.Save,
                                            contentDescription = "Save as note",
                                            tint = colors.textTertiary,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    val res = GeminiResult(
                                                        title = "AI Chat Export",
                                                        content = message.content,
                                                        suggestedType = "text"
                                                    )
                                                    onSaveAsNote(res, "text")
                                                    android.widget.Toast.makeText(context, "Note Created!", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Loading Indicator Turn
                    if (geminiState is GeminiQueryState.Loading) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp))
                                        .background(colors.field)
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF3B82F6))
                                        )
                                        Text(
                                            text = "Gemini is writing...",
                                            fontSize = 12.sp,
                                            color = colors.textSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Send Composer Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.field)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = draftPrompt,
                onValueChange = { draftPrompt = it },
                textStyle = TextStyle(color = colors.text, fontSize = 14.sp),
                cursorBrush = SolidColor(colors.text),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp),
                decorationBox = { innerTextField ->
                    if (draftPrompt.isEmpty()) {
                        Text(
                            text = "Ask anything, or give a command...",
                            color = colors.textTertiary,
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            )

            val canSend = draftPrompt.isNotBlank() && geminiState !is GeminiQueryState.Loading
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (canSend) Color(0xFF3B82F6) else colors.hairline)
                    .clickable(enabled = canSend) {
                        onSendChatPrompt(draftPrompt)
                        draftPrompt = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Prompt",
                    tint = if (canSend) Color.White else colors.textTertiary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }

    // Model Selector Dialog
    if (showModelDialog) {
        AlertDialog(
            onDismissRequest = { showModelDialog = false },
            title = {
                Text(
                    text = "Select Gemini Model",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    for (model in GeminiClient.MODELS_TO_TRY) {
                        val isSelected = model == chatSelectedModel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    onSetChatSelectedModel(model)
                                    showModelDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = GeminiClient.getModelDisplayName(model),
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF3B82F6) else colors.text
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelDialog = false }) {
                    Text("Close", color = Color(0xFF3B82F6))
                }
            },
            containerColor = colors.card,
            textContentColor = colors.text
        )
    }
}

@Composable
private fun PresetChip(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = GlassTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.15f) else colors.field)
            .border(
                1.dp,
                if (isSelected) Color(0xFF3B82F6) else colors.hairline,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color(0xFF3B82F6) else colors.textSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color(0xFF3B82F6) else colors.text
            )
        }
    }
}

@Composable
private fun AiErrorCard(
    errorMessage: String,
    onRetry: () -> Unit,
    onConfigureApiKey: () -> Unit
) {
    val colors = GlassTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = "Error",
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = "Error Generating Response",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.text
        )
        Text(
            text = errorMessage,
            fontSize = 13.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onRetry) {
                Text("Retry", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onConfigureApiKey) {
                Text("API Key", color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun AiIdleState(currentReaderMode: String) {
    val colors = GlassTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFF3B82F6).copy(alpha = 0.6f),
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "Ask Gemini AI",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colors.text,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (currentReaderMode == "pdf") {
                "Select an action above to analyze or summarize your active PDF document."
            } else {
                "Ask questions, summarize contents, or generate interactive components from your notes."
            },
            fontSize = 13.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

