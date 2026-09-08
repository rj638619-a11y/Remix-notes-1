package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteEntity
import com.example.ui.animations.BouncyHapticButton
import com.example.ui.animations.bouncyAppear
import com.example.ui.animations.rememberBouncyClick
import com.example.ui.effects.GlassBottomSheet
import com.example.ui.effects.glassCard
import com.example.ui.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedTrashScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allNotes by viewModel.allNotes.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val deletedNotes = remember(allNotes) {
        allNotes.filter { it.isDeleted }
    }

    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                    Text(
                        text = "TRASH",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (deletedNotes.isNotEmpty()) {
                    BouncyHapticButton(
                        text = "Empty Trash",
                        onClick = { showEmptyTrashDialog = true },
                        hapticsEnabled = settings.hapticsEnabled,
                        containerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                        contentColor = Color(0xFFEF4444)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (deletedNotes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .bouncyAppear(delayMs = 100),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Trash is empty",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(deletedNotes, key = { _, item -> item.id }) { index, note ->
                        TrashItemWrapper(
                            note = note,
                            index = index,
                            reduceTransparency = settings.reduceTransparency,
                            onRestore = { viewModel.restoreNote(note.id) },
                            onDeletePermanently = { viewModel.deletePermanently(note.id) }
                        )
                    }
                }
            }
        }

        // Empty Trash Dialog
        GlassBottomSheet(
            visible = showEmptyTrashDialog,
            onDismiss = { showEmptyTrashDialog = false },
            reduceTransparency = settings.reduceTransparency
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Empty Trash Bin?", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "All ${deletedNotes.size} deleted items will be permanently removed.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BouncyHapticButton(
                        text = "Cancel",
                        onClick = { showEmptyTrashDialog = false },
                        hapticsEnabled = settings.hapticsEnabled,
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    BouncyHapticButton(
                        text = "Empty",
                        onClick = {
                            viewModel.emptyTrash()
                            showEmptyTrashDialog = false
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashItemWrapper(
    note: NoteEntity,
    index: Int,
    reduceTransparency: Boolean,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    var isRemoved by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                isRemoved = true
                onRestore()
                true
            } else if (value == SwipeToDismissBoxValue.EndToStart) {
                isRemoved = true
                onDeletePermanently()
                true
            } else false
        }
    )

    AnimatedVisibility(
        visible = !isRemoved,
        exit = shrinkVertically(tween(300)) + fadeOut(tween(200))
    ) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Color(0xFF10B981) else Color(0xFFEF4444)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(color)
                        .padding(horizontal = 20.dp),
                    contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Restore else Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            },
            content = {
                val delayMs = (index * 40L).coerceAtMost(400L)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bouncyAppear(delayMs = delayMs)
                        .glassCard(
                            blurRadius = 16.dp,
                            cornerRadius = 20.dp,
                            tint = Color.White.copy(alpha = 0.08f),
                            borderColor = Color.White.copy(alpha = 0.15f),
                            reduceTransparency = reduceTransparency
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = note.displayTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note.content.take(80),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onRestore,
                                modifier = Modifier.rememberBouncyClick { onRestore() }
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = Color(0xFF10B981))
                            }
                            IconButton(
                                onClick = onDeletePermanently,
                                modifier = Modifier.rememberBouncyClick { onDeletePermanently() }
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete Permanently", tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }
        )
    }
}
