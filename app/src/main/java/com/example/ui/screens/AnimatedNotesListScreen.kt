package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteEntity
import com.example.ui.animations.BouncyFloatingActionButton
import com.example.ui.animations.ElasticTabIndicator
import com.example.ui.animations.bouncyAppear
import com.example.ui.animations.rememberBouncyCardClick
import com.example.ui.animations.rememberBouncyClick
import com.example.ui.effects.glassCard
import com.example.ui.viewmodel.NotesViewModel
import com.example.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedNotesListScreen(
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier,
    onOpenNote: (String) -> Unit = {},
    onCreateNote: () -> Unit = {}
) {
    val notes by viewModel.allNotes.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    var isSearchExpanded by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(false) }

    val filteredNotes = remember(notes, searchQuery, selectedFilter) {
        notes.filter { note ->
            !note.isDeleted &&
            (searchQuery.isBlank() || note.title.contains(searchQuery, ignoreCase = true) || note.content.contains(searchQuery, ignoreCase = true)) &&
            when (selectedFilter) {
                "pinned" -> note.pinned
                "text" -> note.type == "text"
                "html" -> note.type == "html"
                else -> true
            }
        }
    }

    val categories = listOf("all", "pinned", "text", "html")

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NOTES",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 1.5.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { isSearchExpanded = !isSearchExpanded },
                        modifier = Modifier.rememberBouncyClick { isSearchExpanded = !isSearchExpanded }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = { isGridView = !isGridView },
                        modifier = Modifier.rememberBouncyClick { isGridView = !isGridView }
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridView,
                            contentDescription = "Toggle Grid/List",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // Expandable Search Bar
            AnimatedVisibility(visible = isSearchExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .padding(vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search notes...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Tabs with Elastic Indicator
            val selectedTabIndex = categories.indexOf(selectedFilter).coerceAtLeast(0)
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    ElasticTabIndicator(
                        tabPositions = tabPositions,
                        selectedIndex = selectedTabIndex,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                divider = {}
            ) {
                categories.forEachIndexed { index, cat ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { viewModel.setFilter(cat) },
                        text = {
                            Text(
                                text = cat.uppercase(),
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes List or Grid with Crossfade
            if (filteredNotes.isEmpty()) {
                EmptyNotesState()
            } else {
                Crossfade(targetState = isGridView, animationSpec = tween(300), label = "ListGridCrossfade") { grid ->
                    if (grid) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(filteredNotes, key = { _, item -> item.id }) { index, note ->
                                NoteCardWrapper(
                                    note = note,
                                    index = index,
                                    reduceTransparency = settings.reduceTransparency,
                                    onOpenNote = onOpenNote,
                                    onDeleteNote = { viewModel.moveToTrash(note.id) }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(filteredNotes, key = { _, item -> item.id }) { index, note ->
                                NoteCardWrapper(
                                    note = note,
                                    index = index,
                                    reduceTransparency = settings.reduceTransparency,
                                    onOpenNote = onOpenNote,
                                    onDeleteNote = { viewModel.moveToTrash(note.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bouncy FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            BouncyFloatingActionButton(
                onClick = onCreateNote,
                icon = Icons.Default.Add,
                contentDescription = "Create Note",
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteCardWrapper(
    note: NoteEntity,
    index: Int,
    reduceTransparency: Boolean,
    onOpenNote: (String) -> Unit,
    onDeleteNote: () -> Unit
) {
    var isDismissed by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                isDismissed = true
                onDeleteNote()
                true
            } else false
        }
    )

    AnimatedVisibility(
        visible = !isDismissed,
        exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(200))
    ) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.8f))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                }
            },
            enableDismissFromStartToEnd = false,
            content = {
                NoteCard(
                    note = note,
                    index = index,
                    reduceTransparency = reduceTransparency,
                    onClick = { onOpenNote(note.id) }
                )
            }
        )
    }
}

@Composable
fun NoteCard(
    note: NoteEntity,
    index: Int,
    reduceTransparency: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val delayMs = (index * 40L).coerceAtMost(400L)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .bouncyAppear(delayMs = delayMs)
            .rememberBouncyCardClick(onClick = onClick)
            .glassCard(
                blurRadius = 16.dp,
                cornerRadius = 20.dp,
                tint = Color.White.copy(alpha = 0.08f),
                borderColor = Color.White.copy(alpha = 0.15f),
                reduceTransparency = reduceTransparency
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.displayTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.pinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (note.isLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (note.isLocked) "••••••••••••" else note.content.take(120),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateFormatter.fmtItem(note.updatedAt),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )

                note.category?.let { category ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = category,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotesState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .bouncyAppear(delayMs = 100),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No notes yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tap the + button below to create your first note",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )
    }
}
