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
fun AiChatbotSheet(
    chatSessions: List<ChatSession>,
    activeChatSession: ChatSession?,
    chatDetailedAnswers: Boolean,
    chatSelectedModel: String,
    geminiState: GeminiQueryState,
    geminiApiKey: String,
    onSetChatDetailedAnswers: (Boolean) -> Unit,
    onSetChatSelectedModel: (String) -> Unit,
    onSendChatPrompt: (String) -> Unit,
    onStartNewChat: () -> Unit,
    onSelectChatSession: (String) -> Unit,
    onDeleteChatSession: (String) -> Unit,
    onClearAllChats: () -> Unit,
    onSaveAsNote: (GeminiResult, String) -> Unit,
    onSetGeminiApiKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = GlassTheme.colors
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyDraft by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }
    val isKeyConnected = remember(geminiApiKey) {
        GeminiClient.isValidGeminiApiKey(geminiApiKey) || GeminiClient.isValidGeminiApiKey(GeminiClient.getApiKey())
    }

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
                .fillMaxHeight(0.95f)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
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
                            contentDescription = "AI",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "AI Chatbot",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                        Text(
                            text = "Powered by Gemini",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
                
                // Key Config
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
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.hairline))
            
            Box(modifier = Modifier.fillMaxSize()) {
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
        }
    }
    
    // API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Text(
                    text = "Gemini API Configuration",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your Gemini API Key to enable AI features.",
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = apiKeyDraft,
                        onValueChange = { apiKeyDraft = it },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = colors.text)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSetGeminiApiKey(apiKeyDraft.trim())
                        showApiKeyDialog = false
                    }
                ) {
                    Text("Save", color = Color(0xFF3B82F6))
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.card,
            textContentColor = colors.text
        )
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
