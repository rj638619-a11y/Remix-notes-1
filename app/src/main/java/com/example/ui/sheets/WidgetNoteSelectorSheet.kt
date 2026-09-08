package com.example.ui.sheets

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteEntity
import com.example.ui.theme.GlassTheme
import com.example.util.VibrationHelper
import com.example.widget.GlassNotesWidgetReceiver
import com.example.widget.WidgetManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetNoteSelectorSheet(
    allNotes: List<NoteEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = GlassTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentMode by remember { mutableStateOf(WidgetManager.getWidgetMode(context)) }
    var selectedNoteId by remember { mutableStateOf(WidgetManager.getSelectedNoteId(context)) }
    var searchQuery by remember { mutableStateOf("") }

    val activeNote = remember(selectedNoteId, allNotes) {
        allNotes.find { it.id == selectedNoteId } ?: allNotes.firstOrNull()
    }

    val filteredNotes = remember(allNotes, searchQuery) {
        if (searchQuery.isBlank()) {
            allNotes
        } else {
            allNotes.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true) ||
                (it.category != null && it.category.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        scrimColor = colors.shadow.copy(alpha = 0.4f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33F2B90C)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = "Widget Icon",
                        tint = Color(0xFFF2B90C),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Home Screen Widget",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.text,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "Choose which note to feature on your home screen",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Selector Cards
            Text(
                text = "WIDGET DISPLAY MODE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary.copy(alpha = 0.8f),
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WidgetModeCard(
                    title = "Selected",
                    subtitle = "Specific note",
                    icon = Icons.Default.Star,
                    isSelected = currentMode == WidgetManager.MODE_SELECTED,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        currentMode = WidgetManager.MODE_SELECTED
                        WidgetManager.setWidgetMode(context, WidgetManager.MODE_SELECTED)
                        VibrationHelper.tick(context)
                        coroutineScope.launch {
                            GlassNotesWidgetReceiver.updateAllWidgets(context)
                        }
                    }
                )

                WidgetModeCard(
                    title = "Pinned",
                    subtitle = "Cycle pinned",
                    icon = Icons.Default.PushPin,
                    isSelected = currentMode == WidgetManager.MODE_PINNED,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        currentMode = WidgetManager.MODE_PINNED
                        WidgetManager.setWidgetMode(context, WidgetManager.MODE_PINNED)
                        VibrationHelper.tick(context)
                        coroutineScope.launch {
                            GlassNotesWidgetReceiver.updateAllWidgets(context)
                        }
                    }
                )

                WidgetModeCard(
                    title = "Recent",
                    subtitle = "Newest notes",
                    icon = Icons.Default.Schedule,
                    isSelected = currentMode == WidgetManager.MODE_RECENT,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        currentMode = WidgetManager.MODE_RECENT
                        WidgetManager.setWidgetMode(context, WidgetManager.MODE_RECENT)
                        VibrationHelper.tick(context)
                        coroutineScope.launch {
                            GlassNotesWidgetReceiver.updateAllWidgets(context)
                        }
                    }
                )

                WidgetModeCard(
                    title = "Tasks",
                    subtitle = "Checklists",
                    icon = Icons.Default.Checklist,
                    isSelected = currentMode == WidgetManager.MODE_TODOS,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        currentMode = WidgetManager.MODE_TODOS
                        WidgetManager.setWidgetMode(context, WidgetManager.MODE_TODOS)
                        VibrationHelper.tick(context)
                        coroutineScope.launch {
                            GlassNotesWidgetReceiver.updateAllWidgets(context)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Widget Preview
            if (activeNote != null) {
                Text(
                    text = "ACTIVE WIDGET PREVIEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary.copy(alpha = 0.8f),
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF141822))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📌 Glass Notes Widget",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF2B90C)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x33F2B90C))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = currentMode.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF2B90C)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = activeNote.displayTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = if (activeNote.snippet.isNotBlank()) activeNote.snippet else "No content preview available",
                            fontSize = 12.sp,
                            color = Color(0xFFB0B8C4),
                            maxLines = 2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Note List Header & Search
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TAP ANY NOTE TO SET TO WIDGET",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary.copy(alpha = 0.8f),
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${filteredNotes.size} notes",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search notes...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.card.copy(alpha = 0.5f),
                    unfocusedContainerColor = colors.card.copy(alpha = 0.3f),
                    focusedBorderColor = Color(0xFFF2B90C),
                    unfocusedBorderColor = colors.hairline
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Note List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredNotes, key = { it.id }) { note ->
                    val isSelected = selectedNoteId == note.id

                    val itemBg by animateColorAsState(
                        if (isSelected) Color(0x33F2B90C) else colors.card.copy(alpha = 0.4f),
                        label = "noteSelectBg"
                    )
                    val borderColor by animateColorAsState(
                        if (isSelected) Color(0xFFF2B90C) else colors.hairline.copy(alpha = 0.3f),
                        label = "noteSelectBorder"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(itemBg)
                            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                            .clickable {
                                selectedNoteId = note.id
                                currentMode = WidgetManager.MODE_SELECTED
                                WidgetManager.setSelectedNoteId(context, note.id)
                                VibrationHelper.tick(context)
                                Toast.makeText(context, "📌 Set to Home Screen Widget!", Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    GlassNotesWidgetReceiver.updateAllWidgets(context)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Radio / Check Indicator
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFF2B90C) else Color(0x22FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF1E1500),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = note.displayTitle,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFFF2B90C) else colors.text,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                if (note.pinned) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("📌", fontSize = 11.sp)
                                }

                                if (!note.category.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x3360A5FA))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "#${note.category}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF93C5FD)
                                        )
                                    }
                                }
                            }

                            if (note.snippet.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = note.snippet,
                                    fontSize = 11.5.sp,
                                    color = colors.textSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        if (isSelected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF2B90C))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "ON WIDGET",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1E1500)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun WidgetModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = GlassTheme.colors
    val bg by animateColorAsState(
        if (isSelected) Color(0x35F2B90C) else colors.card.copy(alpha = 0.4f),
        label = "modeBg"
    )
    val border by animateColorAsState(
        if (isSelected) Color(0xFFF2B90C) else colors.hairline.copy(alpha = 0.3f),
        label = "modeBorder"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) Color(0xFFF2B90C) else colors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFFF2B90C) else colors.text
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = subtitle,
            fontSize = 9.sp,
            color = colors.textSecondary,
            maxLines = 1
        )
    }
}
