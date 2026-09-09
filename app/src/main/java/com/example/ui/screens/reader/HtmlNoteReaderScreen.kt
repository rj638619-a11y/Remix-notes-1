package com.example.ui.screens.reader

import com.example.ui.editor.HtmlPreviewView

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Toc
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteEntity
import com.example.ui.theme.GlassTheme
import com.example.ui.util.AmbientBackground
import kotlinx.coroutines.launch

enum class ReaderSegmentTab {
    READ, INFO, BOOKMARKS
}

enum class ReadingThemeMode {
    LIGHT, SEPIA, DARK
}

@Composable
fun HtmlNoteReaderScreen(
    note: NoteEntity,
    onBack: () -> Unit,
    onOpenAiSummary: (String) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    var activeSegment by remember { mutableStateOf(ReaderSegmentTab.READ) }
    var fontSizeMultiplier by remember { mutableFloatStateOf(1.0f) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var readingMode by remember { mutableStateOf(ReadingThemeMode.LIGHT) }
    var isBookmarked by remember { mutableStateOf(note.pinned) }
    var showSearchInNote by remember { mutableStateOf(false) }
    var inNoteQuery by remember { mutableStateOf("") }
    var showTocDialog by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }

    // Reading Mode Colors with high-contrast text guarantee
    val (readerBg, readerTx, readerCardBg) = when (readingMode) {
        ReadingThemeMode.LIGHT -> Triple(Color(0xFFF8F9FA), Color(0xFF111827), Color(0xFFFFFFFF))
        ReadingThemeMode.SEPIA -> Triple(Color(0xFFFAF4E8), Color(0xFF2C1D11), Color(0xFFF3E9D7))
        ReadingThemeMode.DARK -> Triple(Color(0xFF0F172A), Color(0xFFF8FAFC), Color(0xFF1E293B))
    }

    val readerPagerState = rememberPagerState(initialPage = activeSegment.ordinal) { ReaderSegmentTab.entries.size }
    val readerScope = rememberCoroutineScope()

    // Sync pager -> activeSegment
    LaunchedEffect(readerPagerState.currentPage) {
        activeSegment = ReaderSegmentTab.entries[readerPagerState.currentPage]
    }

    // Sync activeSegment -> pager
    LaunchedEffect(activeSegment) {
        if (readerPagerState.currentPage != activeSegment.ordinal) {
            readerPagerState.animateScrollToPage(activeSegment.ordinal)
        }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(readerBg)
    ) {
        AmbientBackground(isDark = readingMode == ReadingThemeMode.DARK)

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Navigation Bar (Hidden in Full Screen)
            AnimatedVisibility(
                visible = !isFullScreen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = statusBarTop + 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.field)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, radius = 19.dp),
                                    onClick = onBack
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = readerTx,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Text(
                            text = note.displayTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = readerTx,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Full Screen Toggle
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.field)
                                .clickable { isFullScreen = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Full Screen",
                                tint = readerTx,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Font size adjuster button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.field)
                                .clickable { showFontSizeDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = "Adjust Font Size",
                                tint = readerTx,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Options menu
                    Box {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.field)
                                .clickable { showOptionsMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = readerTx,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("AI Summary") },
                                onClick = {
                                    showOptionsMenu = false
                                    onOpenAiSummary(note.content)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = colors.pastelBlue)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isBookmarked) "Remove Bookmark" else "Bookmark Note") },
                                onClick = {
                                    showOptionsMenu = false
                                    isBookmarked = !isBookmarked
                                    onToggleBookmark(note.id)
                                },
                                leadingIcon = {
                                    Icon(if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Table of Contents") },
                                onClick = {
                                    showOptionsMenu = false
                                    showTocDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Toc, contentDescription = null)
                                }
                            )
                            if (onDelete != null) {
                                DropdownMenuItem(
                                    text = { Text("Delete Document", color = Color(0xFFEF4444)) },
                                    onClick = {
                                        showOptionsMenu = false
                                        onDelete()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

            // 2. Segmented Tab Pills: [Read] [Info] [Bookmarks] (Hidden in Full Screen)
            AnimatedVisibility(
                visible = !isFullScreen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.field)
                            .padding(4.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ReaderSegmentTab.entries.forEach { tab ->
                                val isSelected = activeSegment == tab
                                val pillBg by animateColorAsState(
                                    targetValue = if (isSelected) colors.pastelBlue else Color.Transparent,
                                    label = "seg_bg"
                                )
                                val pillTx by animateColorAsState(
                                    targetValue = if (isSelected) Color.White else readerTx.copy(alpha = 0.75f),
                                    label = "seg_tx"
                                )

                                Box(
                                    modifier = Modifier
                                        .shadow(if (isSelected) 3.dp else 0.dp, RoundedCornerShape(16.dp), ambientColor = colors.shadow)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(pillBg)
                                        .clickable {
                                            readerScope.launch {
                                                activeSegment = tab
                                                readerPagerState.animateScrollToPage(tab.ordinal)
                                            }
                                        }
                                        .padding(horizontal = 20.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (tab) {
                                            ReaderSegmentTab.READ -> "Read"
                                            ReaderSegmentTab.INFO -> "Info"
                                            ReaderSegmentTab.BOOKMARKS -> "Bookmarks"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = pillTx
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // In-Note Search Bar (Animated visibility)
            AnimatedVisibility(visible = showSearchInNote) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(readerCardBg)
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
                        OutlinedTextField(
                            value = inNoteQuery,
                            onValueChange = { inNoteQuery = it },
                            placeholder = { Text("Find in note...", fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                showSearchInNote = false
                                inNoteQuery = ""
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textTertiary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // 3. Content View using HorizontalPager for smooth swiping between tabs
            HorizontalPager(
                state = readerPagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = false
            ) { page ->
                when (ReaderSegmentTab.entries[page]) {
                    ReaderSegmentTab.READ -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            HtmlPreviewView(
                                htmlContent = if (note.content.isNotBlank()) note.content else "<h1>${note.displayTitle}</h1><p>No HTML content recorded yet.</p>",
                                fontSize = (16 * fontSizeMultiplier).toInt(),
                                isZenMode = isFullScreen
                            )
                        }
                    }

                    ReaderSegmentTab.INFO -> {
                        // Document & Reading Metadata
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "Document Information",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = readerTx
                                )
                            }

                            item {
                                InfoStatRow(label = "Subject Category", value = note.category ?: "General")
                            }
                            item {
                                InfoStatRow(label = "Format", value = note.type.uppercase())
                            }
                            item {
                                InfoStatRow(label = "Word Count", value = "${note.wordCount} words")
                            }
                            item {
                                InfoStatRow(label = "Estimated Reading Time", value = "${note.readingTimeMin} minutes")
                            }
                            item {
                                InfoStatRow(label = "Last Modified", value = note.relativeTimeAgo)
                            }
                            item {
                                InfoStatRow(label = "Estimated File Size", value = "18 pages (~4.2 MB)")
                            }
                        }
                    }

                    ReaderSegmentTab.BOOKMARKS -> {
                        // Bookmarked quotes & sections
                        val bookmarks = listOf(
                            "6CO₂ + 6H₂O + Sunlight ➔ C₆H₁₂O₆ + 6O₂",
                            "Light-Dependent Reactions occur in thylakoid membranes releasing O₂.",
                            "RuBisCO fixes atmospheric CO₂ into 3-PGA in the stroma (Calvin Cycle)."
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "Bookmarked Quotes & Takeaways",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = readerTx
                                )
                            }

                            items(bookmarks) { quote ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(2.dp, RoundedCornerShape(16.dp), ambientColor = colors.shadow)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(readerCardBg)
                                        .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp))
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = null,
                                            tint = colors.pastelFavYellow,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = quote,
                                            fontSize = 14.sp,
                                            color = readerTx,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Floating Glass Bottom Bar (Hidden in Full Screen)
        AnimatedVisibility(
            visible = !isFullScreen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = navBarBottom + 12.dp)
                    .shadow(16.dp, RoundedCornerShape(26.dp), ambientColor = colors.shadow, spotColor = colors.shadow)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colors.glass,
                                colors.glass.copy(alpha = colors.glass.alpha * 0.98f)
                            )
                        )
                    )
                    .border(1.dp, colors.glassBorder, RoundedCornerShape(26.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Table of contents
                    IconButton(onClick = { showTocDialog = true }) {
                        Icon(Icons.Default.Toc, contentDescription = "Table of Contents", tint = colors.text)
                    }

                    // Search inside note
                    IconButton(onClick = { showSearchInNote = !showSearchInNote }) {
                        Icon(Icons.Default.Search, contentDescription = "Find in Note", tint = colors.text)
                    }

                    // Reading Mode Toggle (Light -> Sepia -> Dark)
                    IconButton(onClick = {
                        readingMode = when (readingMode) {
                            ReadingThemeMode.LIGHT -> ReadingThemeMode.SEPIA
                            ReadingThemeMode.SEPIA -> ReadingThemeMode.DARK
                            ReadingThemeMode.DARK -> ReadingThemeMode.LIGHT
                        }
                    }) {
                        Icon(
                            imageVector = if (readingMode == ReadingThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Theme Mode",
                            tint = if (readingMode == ReadingThemeMode.DARK) Color(0xFFFFB74D) else colors.text
                        )
                    }

                    // Bookmark toggle
                    IconButton(onClick = {
                        isBookmarked = !isBookmarked
                        onToggleBookmark(note.id)
                    }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) colors.pastelFavYellow else colors.text
                        )
                    }

                    // AI Assistant Summary Button
                    IconButton(onClick = { onOpenAiSummary(note.content) }) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Summary",
                            tint = colors.pastelBlue
                        )
                    }
                }
            }
        }

        // Floating Exit Full Screen Button
        AnimatedVisibility(
            visible = isFullScreen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = statusBarTop + 12.dp, end = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = colors.shadow, spotColor = colors.shadow)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.card.copy(alpha = 0.95f))
                    .border(1.dp, colors.glassBorder, RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 20.dp),
                        onClick = { isFullScreen = false }
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Full Screen",
                        tint = colors.pastelBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Exit Full Screen",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = readerTx
                    )
                }
            }
        }
    }

    // Font Size Adjuster Dialog
    if (showFontSizeDialog) {
        AlertDialog(
            onDismissRequest = { showFontSizeDialog = false },
            title = {
                Text(
                    text = "Text Size",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Slider(
                        value = fontSizeMultiplier,
                        onValueChange = { fontSizeMultiplier = it },
                        valueRange = 0.8f..1.6f,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.pastelBlue,
                            activeTrackColor = colors.pastelBlue
                        )
                    )
                    Text(
                        text = "Sample: The quick brown fox jumps over the lazy dog.",
                        fontSize = (14 * fontSizeMultiplier).sp,
                        color = colors.text
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showFontSizeDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.pastelBlue)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.card,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Table of Contents Dialog
    if (showTocDialog) {
        AlertDialog(
            onDismissRequest = { showTocDialog = false },
            title = {
                Text(
                    text = "Table of Contents",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. Introduction & Overview", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.pastelBlue)
                    Text("2. Two Stages of Reactions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.pastelBlue)
                    Text("3. Factors Affecting Rate", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.pastelBlue)
                    Text("4. Summary & Review Questions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.pastelBlue)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showTocDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.pastelBlue)
                ) {
                    Text("Close")
                }
            },
            containerColor = colors.card,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun ChapterSection(
    number: String,
    title: String,
    content: String,
    fontSizeMultiplier: Float,
    readerTx: Color
) {
    val colors = GlassTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "$number. $title",
            fontSize = (18 * fontSizeMultiplier).sp,
            fontWeight = FontWeight.Bold,
            color = readerTx
        )
        Text(
            text = content,
            fontSize = (15 * fontSizeMultiplier).sp,
            lineHeight = (22 * fontSizeMultiplier).sp,
            color = readerTx.copy(alpha = 0.88f)
        )
    }
}

@Composable
private fun InfoStatRow(
    label: String,
    value: String
) {
    val colors = GlassTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp), ambientColor = colors.shadow)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 14.sp, color = colors.textSecondary)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
        }
    }
}
