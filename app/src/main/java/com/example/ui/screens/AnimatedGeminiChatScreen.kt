package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.ui.animations.TypingIndicator
import com.example.ui.animations.rememberBouncyClick
import com.example.ui.effects.BlurredScrim
import com.example.ui.effects.glassCard
import com.example.ui.viewmodel.GeminiQueryState
import com.example.ui.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedGeminiChatScreen(
    viewModel: NotesViewModel,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val activeChatSession by viewModel.activeChatSession.collectAsState()
    val geminiState by viewModel.geminiState.collectAsState()
    val isGeminiLoading = geminiState is GeminiQueryState.Loading

    var inputPrompt by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {
        // Blurred Scrim behind
        BlurredScrim(
            visible = visible,
            onDismiss = onDismiss,
            reduceTransparency = settings.reduceTransparency
        )

        // Chat Panel Sliding up
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.3f, stiffness = 350f),
                initialOffsetY = { fullHeight -> fullHeight }
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                animationSpec = tween(250),
                targetOffsetY = { fullHeight -> fullHeight }
            ) + fadeOut(tween(200))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .glassCard(
                        blurRadius = 32.dp,
                        cornerRadius = 28.dp,
                        tint = Color(0xFF0F172A).copy(alpha = 0.9f),
                        borderColor = Color.White.copy(alpha = 0.2f),
                        reduceTransparency = settings.reduceTransparency
                    )
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF8AB4F8), Color(0xFFC58AF9), Color(0xFFFF8BCB))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Gemini AI Assistant",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Powered by Google Gemini",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.startNewChatSession() },
                            modifier = Modifier.rememberBouncyClick { viewModel.startNewChatSession() }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New Chat", tint = Color.White)
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.rememberBouncyClick { onDismiss() }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Shimmer Loading Bar when AI is thinking
                AnimatedVisibility(visible = isGeminiLoading) {
                    ShimmerLoadingBar()
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Message Stream
                val messages = activeChatSession?.messages ?: emptyList()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    reverseLayout = true
                ) {
                    if (isGeminiLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TypingIndicator()
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Gemini is thinking...",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    items(messages.reversed(), key = { it.id }) { msg ->
                        val isUser = msg.sender == "user"
                        AnimatedContent(
                            targetState = msg,
                            transitionSpec = {
                                val slideEnter = if (isUser) {
                                    slideInHorizontally(
                                        animationSpec = spring(0.34f, 400f),
                                        initialOffsetX = { fullWidth -> fullWidth }
                                    )
                                } else {
                                    slideInHorizontally(
                                        animationSpec = spring(0.34f, 400f),
                                        initialOffsetX = { fullWidth -> -fullWidth }
                                    )
                                }
                                (slideEnter + fadeIn()).togetherWith(fadeOut())
                            },
                            label = "ChatMessageAnim"
                        ) { chatMsg ->
                            ChatMessageBubble(
                                message = chatMsg,
                                onSaveAsNote = {
                                    viewModel.createNote { newId ->
                                        viewModel.saveNote(
                                            id = newId,
                                            title = "AI Note",
                                            content = chatMsg.content
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input Box Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputPrompt,
                        onValueChange = { inputPrompt = it },
                        placeholder = { Text("Ask Gemini anything...", color = Color.White.copy(alpha = 0.5f)) },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputPrompt.isNotBlank()) {
                                viewModel.sendChatPrompt(inputPrompt)
                                inputPrompt = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6))
                            .rememberBouncyClick {
                                if (inputPrompt.isNotBlank()) {
                                    viewModel.sendChatPrompt(inputPrompt)
                                    inputPrompt = ""
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    onSaveAsNote: () -> Unit
) {
    val isUser = message.sender == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    )
                )
                .background(
                    if (isUser) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.12f)
                )
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )

                if (!isUser) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .rememberBouncyClick { onSaveAsNote() }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save as Note",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShimmerLoadingBar() {
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    val shimmerColors = listOf(
        Color(0xFF3B82F6).copy(alpha = 0.2f),
        Color(0xFF8AB4F8).copy(alpha = 0.8f),
        Color(0xFF3B82F6).copy(alpha = 0.2f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(brush)
    )
}
