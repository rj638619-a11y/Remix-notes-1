package com.example.ui.screens.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteEntity
import com.example.data.model.NoteSummary
import com.example.ui.components.GlassBox
import com.example.ui.components.NotesAppLogoIcon
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.theme.GlassTheme

enum class QuickCategoryFilter {
    ALL, PDF, HTML, FAVORITES
}

@Composable
fun HomeTab(
    notes: List<NoteSummary>,
    selectedFilter: QuickCategoryFilter,
    onFilterSelected: (QuickCategoryFilter) -> Unit,
    onNoteClick: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onDeleteNote: ((String) -> Unit)? = null,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCreateNote: () -> Unit,
    onSeeAllClick: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors

    val totalCount = notes.size
    val pdfCount = notes.count { it.type == "pdf" }
    val htmlCount = notes.count { it.type == "html" }
    val favCount = notes.count { it.pinned }

    val filteredNotes = when (selectedFilter) {
        QuickCategoryFilter.ALL -> notes
        QuickCategoryFilter.PDF -> notes.filter { it.type == "pdf" }
        QuickCategoryFilter.HTML -> notes.filter { it.type == "html" }
        QuickCategoryFilter.FAVORITES -> notes.filter { it.pinned }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = statusBarTop + 14.dp, bottom = navBarBottom + 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: "N O T E S" & Student Greeting
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NotesAppLogoIcon(size = 42.dp, animated = false)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "N O T E S ✨",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = colors.text
                            )
                            Text(
                                text = "Good Morning, Student 🎓",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.field)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, radius = 19.dp),
                                    onClick = onOpenSearch
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = colors.text,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        ThemeToggleIconButton(
                            isDark = colors.isDark,
                            onClick = onToggleTheme
                        )

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.field)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, radius = 19.dp),
                                    onClick = onOpenSettings
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = colors.text,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            // Glass Search Bar
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = colors.shadow, spotColor = colors.shadow)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.glass)
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onOpenSearch
                        )
                        .padding(horizontal = 16.dp, vertical = 13.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Search notes, files...",
                            fontSize = 14.sp,
                            color = colors.textTertiary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            // Quick Category Filter Cards (Horizontal Row)
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        QuickCategoryCard(
                            title = "All",
                            count = totalCount,
                            icon = Icons.Default.Description,
                            accentColor = colors.text,
                            accentBg = colors.field,
                            isSelected = selectedFilter == QuickCategoryFilter.ALL,
                            onClick = { onFilterSelected(QuickCategoryFilter.ALL) }
                        )
                    }
                    item {
                        QuickCategoryCard(
                            title = "PDF",
                            count = pdfCount,
                            icon = Icons.Default.PictureAsPdf,
                            accentColor = colors.pastelPdfRed,
                            accentBg = colors.pastelPdfRedBg,
                            isSelected = selectedFilter == QuickCategoryFilter.PDF,
                            onClick = { onFilterSelected(QuickCategoryFilter.PDF) }
                        )
                    }
                    item {
                        QuickCategoryCard(
                            title = "HTML",
                            count = htmlCount,
                            icon = Icons.Default.Code,
                            accentColor = colors.pastelMint,
                            accentBg = colors.pastelMintBg,
                            isSelected = selectedFilter == QuickCategoryFilter.HTML,
                            onClick = { onFilterSelected(QuickCategoryFilter.HTML) }
                        )
                    }
                    item {
                        QuickCategoryCard(
                            title = "Favorites",
                            count = favCount,
                            icon = Icons.Default.Star,
                            accentColor = colors.pastelFavYellow,
                            accentBg = colors.pastelFavYellowBg,
                            isSelected = selectedFilter == QuickCategoryFilter.FAVORITES,
                            onClick = { onFilterSelected(QuickCategoryFilter.FAVORITES) }
                        )
                    }
                }
            }

            // Recent Notes Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Notes",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                    Text(
                        text = "See all",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.pastelBlue,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onSeeAllClick)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            // Note List Items
            if (filteredNotes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No notes found in this category",
                            fontSize = 14.sp,
                            color = colors.textTertiary
                        )
                    }
                }
            } else {
                items(filteredNotes, key = { it.id }) { note ->
                    SwipeableRecentNoteWrapper(
                        note = note,
                        onClick = { onNoteClick(note.id) },
                        onTogglePin = { onTogglePin(note.id) },
                        onDelete = { onDeleteNote?.invoke(note.id) }
                    )
                }
            }
        }

        // Floating Action Button for Note Creation - elevated cleanly above navigation bar
        FloatingActionButton(
            onClick = onOpenCreateNote,
            containerColor = colors.pastelBlue,
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = navBarBottom + 96.dp)
                .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = colors.pastelBlue.copy(alpha = 0.4f))
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Note",
                tint = Color.White
            )
        }
    }
}

@Composable
fun QuickCategoryCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    accentBg: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .width(104.dp)
            .shadow(if (isSelected) 8.dp else 4.dp, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
            .clip(shape)
            .background(if (isSelected) colors.card else colors.glass)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accentColor.copy(alpha = 0.6f) else colors.glassBorder,
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text
                )
                Text(
                    text = "$count",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun RecentNoteCard(
    note: NoteSummary,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    val shape = RoundedCornerShape(22.dp)

    val (badgeIcon, badgeColor, badgeBg, badgeLabel) = when (note.type) {
        "pdf" -> Quadruple(Icons.Default.PictureAsPdf, colors.pastelPdfRed, colors.pastelPdfRedBg, "PDF")
        "html" -> Quadruple(Icons.Default.Code, colors.pastelMint, colors.pastelMintBg, "</>")
        else -> Quadruple(Icons.Default.Description, colors.pastelBlue, colors.pastelBlueBg, "TXT")
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, colors.glassBorder, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // File Type Pastel Badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = badgeLabel,
                        tint = badgeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = note.displayTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = note.metaSubtitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = colors.textTertiary
                        )
                        Text(
                            text = note.relativeTimeAgo,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.textTertiary
                        )
                    }
                }
            }

            if (note.pinned) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Pinned",
                    tint = colors.pastelFavYellow,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableRecentNoteWrapper(
    note: NoteSummary,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    var isDismissed by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                if (onDelete != null) {
                    isDismissed = true
                    onDelete()
                    true
                } else false
            } else if (value == SwipeToDismissBoxValue.StartToEnd) {
                onTogglePin()
                false // Retract/animate back smoothly after toggle
            } else false
        }
    )

    AnimatedVisibility(
        visible = !isDismissed,
        exit = shrinkVertically(animationSpec = tween(300))
    ) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val isStartToEnd = direction == SwipeToDismissBoxValue.StartToEnd
                val isEndToStart = direction == SwipeToDismissBoxValue.EndToStart

                val backgroundColor = when {
                    isStartToEnd -> colors.pastelBlue.copy(alpha = 0.22f)
                    isEndToStart -> Color(0xFFEF4444).copy(alpha = 0.22f)
                    else -> Color.Transparent
                }

                val alignment = when {
                    isStartToEnd -> Alignment.CenterStart
                    isEndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp))
                        .background(backgroundColor)
                        .padding(horizontal = 24.dp),
                    contentAlignment = alignment
                ) {
                    if (isStartToEnd) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = if (note.pinned) "Unstar" else "Star",
                                tint = colors.pastelBlue
                            )
                            Text(
                                text = if (note.pinned) "Unstar" else "Star",
                                color = colors.pastelBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    } else if (isEndToStart) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Move to Trash",
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            },
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = onDelete != null,
            modifier = modifier.fillMaxWidth(),
            content = {
                RecentNoteCard(
                    note = note,
                    onClick = onClick,
                    onTogglePin = onTogglePin,
                    onDelete = onDelete
                )
            }
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
