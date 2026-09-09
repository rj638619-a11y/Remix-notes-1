package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteSummary
import com.example.ui.components.HighlightedText
import com.example.ui.screens.tabs.RecentNoteCard
import com.example.ui.theme.GlassTheme
import com.example.ui.util.AmbientBackground

@Composable
fun SearchScreen(
    notes: List<NoteSummary>,
    onNoteClick: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Notes", "PDF", "HTML"
    val focusRequester = remember { FocusRequester() }

    val recentSearches = listOf("Photosynthesis", "Human Reproduction", "Chemical Bonding", "Kinematics", "Ecology")

    val searchResults = remember(notes, query, selectedFilter) {
        if (query.isBlank()) {
            emptyList()
        } else {
            notes.filter { note ->
                val matchesQuery = note.displayTitle.contains(query, ignoreCase = true) ||
                        note.snippetPreview.contains(query, ignoreCase = true) ||
                        (note.category?.contains(query, ignoreCase = true) ?: false)
                val matchesFilter = when (selectedFilter) {
                    "PDF" -> note.type == "pdf"
                    "HTML" -> note.type == "html"
                    "Notes" -> note.type == "text"
                    else -> true
                }
                matchesQuery && matchesFilter
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        AmbientBackground(isDark = colors.isDark)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 18.dp, top = statusBarTop + 10.dp, bottom = navBarBottom + 10.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
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
                        tint = colors.text,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Search Input Field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = colors.shadow, spotColor = colors.shadow)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.card)
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Search notes, topics, PDFs...", fontSize = 14.sp, color = colors.textTertiary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = colors.text,
                                unfocusedTextColor = colors.text
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                        )
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = colors.textTertiary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                val filters = listOf("All", "Notes", "PDF", "HTML")
                items(filters) { f ->
                    val isSelected = selectedFilter == f
                    val chipBg = if (isSelected) colors.pastelBlue else colors.glass
                    val chipTx = if (isSelected) Color.White else colors.textSecondary

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(chipBg)
                            .border(1.dp, if (isSelected) colors.pastelBlue else colors.glassBorder, RoundedCornerShape(16.dp))
                            .clickable { selectedFilter = f }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = f,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = chipTx
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Recent Searches or Search Results
            if (query.isBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Recent Searches",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(recentSearches) { searchItem ->
                            Box(
                                modifier = Modifier
                                    .shadow(2.dp, RoundedCornerShape(14.dp), ambientColor = colors.shadow)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.card)
                                    .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))
                                    .clickable { query = searchItem }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = colors.textTertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = searchItem,
                                        fontSize = 13.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "${searchResults.size} results found for \"$query\"",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    if (searchResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No notes matched your query",
                                    fontSize = 14.sp,
                                    color = colors.textTertiary
                                )
                            }
                        }
                    } else {
                        items(searchResults, key = { it.id }) { note ->
                            RecentNoteCard(
                                note = note,
                                onClick = { onNoteClick(note.id) },
                                onTogglePin = { onTogglePin(note.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
