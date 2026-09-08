package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.animations.BouncyHapticButton
import com.example.ui.animations.rememberBouncyClick
import com.example.ui.effects.GlassBottomSheet
import com.example.ui.effects.glassCard
import com.example.ui.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedNoteEditorScreen(
    viewModel: NotesViewModel,
    noteId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()

    val existingNote = remember(allNotes, noteId) {
        allNotes.find { it.id == noteId }
    }

    var title by remember(existingNote) { mutableStateOf(existingNote?.title ?: "") }
    var content by remember(existingNote) { mutableStateOf(existingNote?.content ?: "") }
    var isPinned by remember(existingNote) { mutableStateOf(existingNote?.pinned ?: false) }
    var isLocked by remember(existingNote) { mutableStateOf(existingNote?.isLocked ?: false) }
    var isReaderMode by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val isToolbarVisible = remember { mutableStateOf(true) }

    LaunchedEffect(scrollState.value) {
        isToolbarVisible.value = scrollState.value < 100 || !scrollState.isScrollInProgress
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Animated Toolbar
            AnimatedVisibility(
                visible = isToolbarVisible.value,
                enter = slideInVertically(
                    animationSpec = spring(0.34f, 400f),
                    initialOffsetY = { -it }
                ) + fadeIn(tween(200)),
                exit = slideOutVertically(
                    animationSpec = tween(200),
                    targetOffsetY = { -it }
                ) + fadeOut(tween(200))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(
                            blurRadius = 20.dp,
                            cornerRadius = 24.dp,
                            tint = Color.White.copy(alpha = 0.08f),
                            borderColor = Color.White.copy(alpha = 0.15f),
                            reduceTransparency = settings.reduceTransparency
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.rememberBouncyClick { onBack() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pin Toggle
                        IconButton(
                            onClick = {
                                isPinned = !isPinned
                                if (noteId.isNotBlank()) viewModel.togglePin(noteId)
                            },
                            modifier = Modifier.rememberBouncyClick {
                                isPinned = !isPinned
                                if (noteId.isNotBlank()) viewModel.togglePin(noteId)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pin Note",
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }

                        // Lock Toggle
                        IconButton(
                            onClick = {
                                isLocked = !isLocked
                                if (noteId.isNotBlank()) viewModel.toggleNoteLock(noteId)
                            },
                            modifier = Modifier.rememberBouncyClick {
                                isLocked = !isLocked
                                if (noteId.isNotBlank()) viewModel.toggleNoteLock(noteId)
                            }
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock Note",
                                tint = if (isLocked) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }

                        // Reader Mode Toggle
                        IconButton(
                            onClick = { isReaderMode = !isReaderMode },
                            modifier = Modifier.rememberBouncyClick { isReaderMode = !isReaderMode }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Reader Mode",
                                tint = if (isReaderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }

                        // Share
                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, title)
                                    putExtra(Intent.EXTRA_TEXT, "$title\n\n$content")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
                            },
                            modifier = Modifier.rememberBouncyClick {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, title)
                                    putExtra(Intent.EXTRA_TEXT, "$title\n\n$content")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Delete
                        IconButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.rememberBouncyClick { showDeleteConfirmation = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Editor Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .animateContentSize()
            ) {
                Crossfade(
                    targetState = isReaderMode,
                    animationSpec = tween(300),
                    label = "ReaderModeCrossfade"
                ) { reader ->
                    if (reader) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .glassCard(
                                    blurRadius = 16.dp,
                                    cornerRadius = 24.dp,
                                    tint = Color.White.copy(alpha = 0.05f),
                                    reduceTransparency = settings.reduceTransparency
                                )
                                .padding(20.dp)
                        ) {
                            Text(
                                text = title.ifBlank { "Untitled Note" },
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = content,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                                lineHeight = 24.sp
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                placeholder = {
                                    Text(
                                        "Note Title...",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                    )
                                },
                                textStyle = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = content,
                                onValueChange = { content = it },
                                placeholder = {
                                    Text(
                                        "Start typing your note here...",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                    )
                                },
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    lineHeight = 24.sp
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Save Button
            BouncyHapticButton(
                text = "Save Note",
                onClick = {
                    if (noteId.isBlank()) {
                        viewModel.createNote { newId ->
                            viewModel.saveNote(
                                id = newId,
                                title = title,
                                content = content
                            )
                        }
                    } else {
                        viewModel.saveNote(
                            id = noteId,
                            title = title,
                            content = content
                        )
                    }
                    onBack()
                },
                hapticsEnabled = settings.hapticsEnabled,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        // Delete Confirmation Glass Bottom Sheet
        GlassBottomSheet(
            visible = showDeleteConfirmation,
            onDismiss = { showDeleteConfirmation = false },
            reduceTransparency = settings.reduceTransparency
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Move to Trash?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "This note will be moved to the Trash bin where you can restore it anytime.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BouncyHapticButton(
                        text = "Cancel",
                        onClick = { showDeleteConfirmation = false },
                        hapticsEnabled = settings.hapticsEnabled,
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    BouncyHapticButton(
                        text = "Delete",
                        onClick = {
                            showDeleteConfirmation = false
                            if (noteId.isNotBlank()) {
                                viewModel.moveToTrash(noteId)
                            }
                            onBack()
                        },
                        hapticsEnabled = settings.hapticsEnabled,
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
