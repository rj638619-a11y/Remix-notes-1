package com.example.ui.screens.tabs

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeminiClient
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.ui.theme.GlassTheme
import com.example.ui.viewmodel.GeminiQueryState
import com.example.util.VibrationHelper
import kotlinx.coroutines.launch

@Composable
fun AiAssistantTab(
    chatSessions: List<ChatSession> = emptyList(),
    activeChatSession: ChatSession? = null,
    geminiState: GeminiQueryState = GeminiQueryState.Idle,
    geminiApiKey: String = "",
    chatSelectedModel: String = "gemini-2.0-flash",
    chatDetailedAnswers: Boolean = false,
    onSendChatPrompt: (String) -> Unit = {},
    onStartNewChat: () -> Unit = {},
    onSelectChatSession: (String) -> Unit = {},
    onDeleteChatSession: (String) -> Unit = {},
    onClearAllChats: () -> Unit = {},
    onSetGeminiApiKey: (String) -> Unit = {},
    onSetChatDetailedAnswers: (Boolean) -> Unit = {},
    onSetChatSelectedModel: (String) -> Unit = {},
    initialPrompt: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var inputPrompt by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyDraft by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    val isKeyConnected = remember(geminiApiKey) {
        GeminiClient.isValidGeminiApiKey(geminiApiKey) || GeminiClient.isValidGeminiApiKey(GeminiClient.getApiKey())
    }

    val displayMessages = activeChatSession?.messages ?: emptyList()

    // Handle initial prompt if passed in from reader/editor
    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            onSendChatPrompt(initialPrompt)
        }
    }

    // Auto-scroll on new messages
    LaunchedEffect(displayMessages.size, geminiState) {
        if (displayMessages.isNotEmpty()) {
            listState.animateScrollToItem(displayMessages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "Summarize my study notes",
        "Explain Photosynthesis formula",
        "Create high-yield practice questions",
        "Make concise revision notes",
        "Explain Newton's Laws of Motion",
        "Chemical Bonding types summary"
    )

    fun sendUserPrompt(userText: String) {
        if (userText.isBlank()) return
        onSendChatPrompt(userText)
        inputPrompt = ""
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = statusBarTop + 12.dp, bottom = 8.dp),
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.pastelBlueBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = colors.pastelBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "AI Assistant ✨",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                        Text(
                            text = "Powered by Google Gemini",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // API Key badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isKeyConnected) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f))
                            .clickable {
                                apiKeyDraft = geminiApiKey
                                testResultText = null
                                showApiKeyDialog = true
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "API Key",
                                tint = if (isKeyConnected) Color(0xFF10B981) else Color(0xFFF59E0B),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isKeyConnected) "Connected" else "Set Key",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isKeyConnected) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = colors.text)
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Chat Session") },
                                onClick = {
                                    showMenu = false
                                    onStartNewChat()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = colors.pastelBlue)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Configure Gemini Key") },
                                onClick = {
                                    showMenu = false
                                    apiKeyDraft = geminiApiKey
                                    testResultText = null
                                    showApiKeyDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = colors.textSecondary)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear All Chats") },
                                onClick = {
                                    showMenu = false
                                    onClearAllChats()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = colors.danger)
                                }
                            )
                        }
                    }
                }
            }

            // Quick Prompt Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickPrompts) { prompt ->
                    Box(
                        modifier = Modifier
                            .shadow(2.dp, RoundedCornerShape(16.dp), ambientColor = colors.shadow)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.card)
                            .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp))
                            .clickable { sendUserPrompt(prompt) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = colors.pastelBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = prompt,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.text
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Message List / Chat Feed
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = navBarBottom + 160.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (displayMessages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(colors.pastelBlueBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = null,
                                        tint = colors.pastelBlue,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "Ready to study & learn! 🎓",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                                Text(
                                    text = "Ask anything about your study notes, formulas, or tap a quick chip above to generate flashcards and summaries.",
                                    fontSize = 13.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(displayMessages, key = { it.id }) { msg ->
                        AiTabBubble(
                            message = msg,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(msg.content))
                                VibrationHelper.vibrate(context, 10)
                            }
                        )
                    }
                }

                // Loading State
                if (geminiState is GeminiQueryState.Loading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = colors.pastelBlue,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Gemini is analyzing & generating answer...",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                // Error State with Retry
                if (geminiState is GeminiQueryState.Error) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Query Failed",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                                Text(
                                    text = geminiState.message,
                                    fontSize = 12.5.sp,
                                    color = colors.textSecondary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            if (geminiState.prompt.isNotBlank()) {
                                                onSendChatPrompt(geminiState.prompt)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Retry",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Chat Input Field
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = navBarBottom + 80.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(26.dp), ambientColor = colors.shadow, spotColor = colors.shadow)
                    .clip(RoundedCornerShape(26.dp))
                    .background(colors.card)
                    .border(1.dp, colors.glassBorder, RoundedCornerShape(26.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = inputPrompt,
                        onValueChange = { inputPrompt = it },
                        placeholder = {
                            Text(
                                "Ask anything about notes or subjects...",
                                fontSize = 14.sp,
                                color = colors.textTertiary
                            )
                        },
                        singleLine = false,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendUserPrompt(inputPrompt) }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (inputPrompt.isNotBlank() && geminiState !is GeminiQueryState.Loading) colors.pastelBlue else colors.field)
                            .clickable(enabled = inputPrompt.isNotBlank() && geminiState !is GeminiQueryState.Loading) {
                                sendUserPrompt(inputPrompt)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputPrompt.isNotBlank() && geminiState !is GeminiQueryState.Loading) Color.White else colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // API Key Dialog with Test Connection Button
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your Google Gemini API key to activate AI note search, summarization, and chat assistant features.",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    OutlinedTextField(
                        value = apiKeyDraft,
                        onValueChange = {
                            apiKeyDraft = it
                            testResultText = null
                        },
                        label = { Text("API Key (starts with AIzaSy...)") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Test connection row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    isTestingConnection = true
                                    testResultText = null
                                    val keyToTest = apiKeyDraft.trim()
                                    if (!GeminiClient.isValidGeminiApiKey(keyToTest)) {
                                        testResultText = "❌ Invalid API key format"
                                        isTestingConnection = false
                                        return@launch
                                    }
                                    val models = GeminiClient.fetchLiveModels(keyToTest)
                                    if (models.isNotEmpty()) {
                                        testResultText = "✅ Connected! Models available: ${models.take(2).joinToString()}"
                                    } else {
                                        testResultText = "❌ Connection failed. Verify key or internet."
                                    }
                                    isTestingConnection = false
                                }
                            },
                            enabled = !isTestingConnection
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.pastelBlue
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...")
                            } else {
                                Text("Test Connection")
                            }
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

                    testResultText?.let { res ->
                        Text(
                            text = res,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (res.startsWith("✅")) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
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
                    Text("Save Key", fontWeight = FontWeight.Bold, color = colors.pastelBlue)
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
fun AiTabBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    val isUser = message.sender == "user"

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.pastelBlueBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "AI",
                    tint = colors.pastelBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .shadow(if (isUser) 4.dp else 2.dp, RoundedCornerShape(18.dp), ambientColor = colors.shadow)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(if (isUser) colors.pastelBlue else colors.card)
                .border(
                    width = 1.dp,
                    color = if (isUser) colors.pastelBlue else colors.glassBorder,
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = if (isUser) Color.White else colors.text
                )

                if (!isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = colors.textTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
