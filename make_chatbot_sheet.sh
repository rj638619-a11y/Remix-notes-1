#!/bin/bash
echo "package com.example.ui.sheets" > app/src/main/java/com/example/ui/sheets/AiChatbotSheet.kt
cat /tmp/imports.txt >> app/src/main/java/com/example/ui/sheets/AiChatbotSheet.kt
echo "" >> app/src/main/java/com/example/ui/sheets/AiChatbotSheet.kt

cat << 'INNER_EOF' >> app/src/main/java/com/example/ui/sheets/AiChatbotSheet.kt
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
INNER_EOF

cat /tmp/chatbot_view.txt >> app/src/main/java/com/example/ui/sheets/AiChatbotSheet.kt
