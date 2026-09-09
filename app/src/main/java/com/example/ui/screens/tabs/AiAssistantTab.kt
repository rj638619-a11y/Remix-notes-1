package com.example.ui.screens.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER, AI
}

@Composable
fun AiAssistantTab(
    initialPrompt: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputPrompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = MessageSender.AI,
                text = "Hello! I'm your AI Study Assistant 🎓. I can help you summarize PDFs, generate practice exam questions, explain complex formulas, and create concise flashcard study notes. How can I assist your learning today?"
            )
        )
    }

    // Handle initial prompt if passed in from reader
    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            messages.add(ChatMessage(sender = MessageSender.USER, text = initialPrompt))
            isGenerating = true
            delay(1000L)
            val answer = generateSmartStudyAnswer(initialPrompt)
            messages.add(ChatMessage(sender = MessageSender.AI, text = answer))
            isGenerating = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "Summarize this PDF",
        "Explain Photosynthesis",
        "Create important exam questions",
        "Make short revision notes",
        "Explain Newton's Laws",
        "Chemical Bonding types"
    )

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        messages.add(ChatMessage(sender = MessageSender.USER, text = userText))
        inputPrompt = ""
        isGenerating = true

        coroutineScope.launch {
            delay(900L)
            val aiResponse = generateSmartStudyAnswer(userText)
            messages.add(ChatMessage(sender = MessageSender.AI, text = aiResponse))
            isGenerating = false
            listState.animateScrollToItem(messages.size - 1)
        }
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

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = colors.text)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Clear Chat History") },
                            onClick = {
                                showMenu = false
                                messages.clear()
                                messages.add(
                                    ChatMessage(
                                        sender = MessageSender.AI,
                                        text = "Chat history cleared. How can I help you study next?"
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = colors.danger)
                            }
                        )
                    }
                }
            }

            // Quick Prompt Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickPrompts) { prompt ->
                    Box(
                        modifier = Modifier
                            .shadow(2.dp, RoundedCornerShape(16.dp), ambientColor = colors.shadow)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.card)
                            .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp))
                            .clickable { sendMessage(prompt) }
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

            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = navBarBottom + 160.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(
                        message = msg,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(msg.text))
                        }
                    )
                }

                if (isGenerating) {
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
                                text = "AI is thinking & analyzing notes...",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
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
                        keyboardActions = KeyboardActions(onSend = { sendMessage(inputPrompt) }),
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
                            .background(if (inputPrompt.isNotBlank()) colors.pastelBlue else colors.field)
                            .clickable(enabled = inputPrompt.isNotBlank()) {
                                sendMessage(inputPrompt)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputPrompt.isNotBlank()) Color.White else colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    val isUser = message.sender == MessageSender.USER

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
                    text = message.text,
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

private fun generateSmartStudyAnswer(prompt: String): String {
    val p = prompt.lowercase()
    return when {
        p.contains("photosynthesis") ->
            "🌿 **Photosynthesis Summary**:\n\n" +
            "• **Equation**: 6CO₂ + 6H₂O + Sunlight ➔ C₆H₁₂O₆ + 6O₂\n" +
            "• **Light Reactions**: Occur in thylakoids; photolysis of water yields ATP, NADPH, and O₂.\n" +
            "• **Calvin Cycle**: Occurs in stroma; RuBisCO enzyme fixes CO₂ into glucose.\n\n" +
            "💡 *Exam Tip*: Remember that light reactions provide the chemical energy (ATP/NADPH) required for the dark reactions."

        p.contains("human reproduction") || p.contains("reproduction") ->
            "🧬 **Human Reproduction High-Yield Points**:\n\n" +
            "1. **Male System**: Testes in scrotum (2–2.5°C cooler for spermatogenesis). Leydig cells secrete testosterone.\n" +
            "2. **Female System**: Ovaries produce ova; fertilization takes place at the ampullary-isthmic junction of fallopian tubes.\n" +
            "3. **Implantation**: Morula transforms into blastocyst and embeds into the endometrium on ~day 7."

        p.contains("chemical bonding") || p.contains("bonding") ->
            "⚛️ **Chemical Bonding Core Review**:\n\n" +
            "• **Ionic**: Electrostatic attraction between cations and anions (e.g. NaCl).\n" +
            "• **Covalent**: Sharing of electron pairs (e.g. CH₄, H₂O).\n" +
            "• **VSEPR**: Predicts molecular geometries (Linear 180°, Trigonal Planar 120°, Tetrahedral 109.5°).\n" +
            "• **Hybridization**: sp³ (tetrahedral), sp² (planar), sp (linear)."

        p.contains("question") || p.contains("exam") ->
            "📝 **Top 3 High-Frequency Exam Questions**:\n\n" +
            "1. Differentiate between Light-Dependent and Light-Independent reactions in chloroplasts.\n" +
            "2. State and derive the work-energy theorem using calculus.\n" +
            "3. Explain the VSEPR theory and predict the geometry of ammonia (NH₃) and water (H₂O)."

        p.contains("newton") || p.contains("motion") || p.contains("kinematics") ->
            "🚀 **Newton's Laws & Kinematics**:\n\n" +
            "• **1st Law**: Law of Inertia (objects stay at rest or constant velocity unless acted on by net force).\n" +
            "• **2nd Law**: F = dp/dt = m · a\n" +
            "• **3rd Law**: Action = -Reaction\n" +
            "• **Motion**: v = u + at, s = ut + ½at², v² = u² + 2as"

        p.contains("summarize") ->
            "📋 **Document Key Summary**:\n\n" +
            "• Core Theme: Fundamental scientific definitions and formulas.\n" +
            "• Key Takeaways: Complete classification, step-by-step mechanisms, and practical diagrams.\n" +
            "• Action Items: Review end-of-chapter practice questions and memorize bolded formulas."

        else ->
            "✨ **Study Insight** on *\"$prompt\"*:\n\n" +
            "This topic has been indexed across your study library. Here are the core concepts:\n" +
            "• Master the basic terminology and units.\n" +
            "• Practice 2-3 numerical or diagrammatic questions daily.\n" +
            "• Create active recall flashcards for formula memorization.\n\n" +
            "Would you like me to generate a 5-question quick quiz on this topic?"
    }
}
