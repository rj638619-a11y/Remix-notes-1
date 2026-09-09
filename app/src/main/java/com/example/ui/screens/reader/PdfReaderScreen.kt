package com.example.ui.screens.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteEntity
import com.example.ui.theme.GlassTheme
import com.example.ui.util.AmbientBackground

@Composable
fun PdfReaderScreen(
    note: NoteEntity,
    onBack: () -> Unit,
    onOpenAiAssistant: (String) -> Unit,
    onToggleBookmark: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    val totalPages = remember(note) {
        if (note.title.contains("Human", ignoreCase = true)) 28
        else if (note.title.contains("Kinematics", ignoreCase = true)) 16
        else if (note.title.contains("Bonding", ignoreCase = true)) 22
        else 14
    }

    var currentPage by remember { mutableIntStateOf(1) }
    var isBookmarked by remember { mutableStateOf(note.pinned) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showSearchInPdf by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var isFullScreen by remember { mutableStateOf(false) }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF26282B))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Bar (Hidden in Full Screen)
            AnimatedVisibility(
                visible = !isFullScreen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xE61F2124))
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
                                .background(Color(0x33FFFFFF))
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
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Red PDF Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEB4444))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "PDF",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = note.displayTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isFullScreen = true }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Full Screen", tint = Color.White)
                        }

                        IconButton(onClick = { showSearchInPdf = !showSearchInPdf }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }

                    IconButton(onClick = {
                        isBookmarked = !isBookmarked
                        onToggleBookmark(note.id)
                    }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) colors.pastelFavYellow else Color.White
                        )
                    }

                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ask AI to Explain Page") },
                                onClick = {
                                    showOptionsMenu = false
                                    onOpenAiAssistant("Summarize page $currentPage of ${note.displayTitle}")
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = colors.pastelBlue)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reset Zoom") },
                                onClick = {
                                    showOptionsMenu = false
                                    zoomScale = 1.0f
                                    panOffset = Offset.Zero
                                }
                            )
                        }
                    }
                }
            }
        }

            // PDF Search Bar
            AnimatedVisibility(visible = showSearchInPdf) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2C2F33))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search inside document...", color = Color.LightGray, fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = colors.pastelBlue,
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showSearchInPdf = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }

            // 2. Interactive Document Page Canvas View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(1.0f, 3.0f)
                            if (zoomScale > 1.0f) {
                                panOffset += pan
                            } else {
                                panOffset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // PDF Paper Sheet with high fidelity study page
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 12.dp)
                        .shadow(16.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFDFCFA))
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = panOffset.x
                            translationY = panOffset.y
                        }
                        .padding(20.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ACADEMIC STUDY GUIDE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Page $currentPage / $totalPages",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        item {
                            Text(
                                text = note.displayTitle,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1B1D20)
                            )
                        }

                        // Render scientific diagram on page
                        item {
                            PdfPageDiagram(title = note.displayTitle, page = currentPage)
                        }

                        // Formatted study body text
                        item {
                            val pageContent = when (currentPage) {
                                1 -> "1. Overview & Fundamental Axioms:\nHuman reproduction entails the sexual union and combination of haploid male (sperm) and female (oocyte) gametes. Through the physiological stages of gametogenesis, fertilization, cleavage, implantation, and organogenesis, embryonic growth is facilitated in the maternal uterus."
                                2 -> "2. Gametogenesis & Meiotic Division:\nSpermatogenesis occurs in the seminiferous tubules under pituitary gonadotropin stimulation (LH & FSH). Primary spermatocytes complete meiosis I to yield secondary spermatocytes, eventually maturating into flagellated spermatozoa."
                                3 -> "3. Ovarian Follicular Kinetics:\nPrimary oocytes remain arrested in prophase I of meiosis until puberty. Each menstrual cycle promotes follicular maturation leading to ovulation triggered by the mid-cycle LH surge."
                                else -> "Chapter Section ${currentPage}:\nDetailed histological diagrams, clinical correlates, pathology reviews, and high-yield board review summary notes."
                            }

                            Text(
                                text = pageContent,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                color = Color(0xFF2E3033)
                            )
                        }
                    }
                }
            }

            // 3. Bottom Page Navigation Bar: < | 1 / 28 | > (Hidden in Full Screen)
            AnimatedVisibility(
                visible = !isFullScreen,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xE61F2124))
                        .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = navBarBottom + 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Page Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (currentPage > 1) Color(0x33FFFFFF) else Color(0x11FFFFFF))
                                .clickable(enabled = currentPage > 1) {
                                    if (currentPage > 1) currentPage--
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Page",
                                tint = if (currentPage > 1) Color.White else Color.Gray
                            )
                        }

                        // Page Indicator & Slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                        ) {
                            Slider(
                                value = currentPage.toFloat(),
                                onValueChange = { currentPage = it.toInt().coerceIn(1, totalPages) },
                                valueRange = 1f..totalPages.toFloat(),
                                steps = totalPages - 2,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFEB4444),
                                    activeTrackColor = Color(0xFFEB4444),
                                    inactiveTrackColor = Color.DarkGray
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$currentPage / $totalPages",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Next Page Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (currentPage < totalPages) Color(0x33FFFFFF) else Color(0x11FFFFFF))
                                .clickable(enabled = currentPage < totalPages) {
                                    if (currentPage < totalPages) currentPage++
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Page",
                                tint = if (currentPage < totalPages) Color.White else Color.Gray
                            )
                        }
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
                    .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xEE1E2023))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
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
                        tint = Color(0xFF5A9BFF),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Exit Full Screen",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun PdfPageDiagram(
    title: String,
    page: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF3F4F6))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Diagram Illustration Schematic
            drawRoundRect(
                color = Color(0xFFE5E7EB),
                topLeft = Offset(w * 0.1f, h * 0.15f),
                size = Size(w * 0.8f, h * 0.7f),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // Central circle cell schematic
            drawCircle(
                color = Color(0xFF3872E0).copy(alpha = 0.85f),
                center = Offset(w * 0.35f, h * 0.5f),
                radius = h * 0.25f
            )
            drawCircle(
                color = Color(0xFFFFFFFF),
                center = Offset(w * 0.35f, h * 0.5f),
                radius = h * 0.12f
            )

            // Arrow to product
            drawLine(
                color = Color(0xFFEB4444),
                start = Offset(w * 0.52f, h * 0.5f),
                end = Offset(w * 0.68f, h * 0.5f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Result node
            drawCircle(
                color = Color(0xFF2EB85C),
                center = Offset(w * 0.78f, h * 0.5f),
                radius = h * 0.22f
            )
        }
    }
}
