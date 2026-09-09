package com.example.ui.screens.tabs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteSummary
import com.example.ui.theme.GlassTheme

import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

data class SubjectFolder(
    val name: String,
    val noteCount: Int,
    val color: Color,
    val bg: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTab(
    notes: List<NoteSummary>,
    onNoteClick: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onDeleteNote: ((String) -> Unit)? = null,
    onImportPdf: (() -> Unit)? = null,
    onImportFiles: (() -> Unit)? = null,
    onCreateNewNote: ((String?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("All") } // "All", "PDF", "HTML", "Favorites"
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var customFolders by remember { mutableStateOf(listOf<String>()) }
    var newFolderName by remember { mutableStateOf("") }

    // Predefined Subject folders with matching pastel colors
    val folderDefs = remember(notes, customFolders) {
        listOf(
            SubjectFolder("Physics", notes.count { it.category.equals("Physics", ignoreCase = true) }, colors.pastelBlue, colors.pastelBlueBg),
            SubjectFolder("Chemistry", notes.count { it.category.equals("Chemistry", ignoreCase = true) }, colors.pastelOrange, colors.pastelOrangeBg),
            SubjectFolder("Biology", notes.count { it.category.equals("Biology", ignoreCase = true) }, colors.pastelMint, colors.pastelMintBg),
            SubjectFolder("Mathematics", notes.count { it.category.equals("Mathematics", ignoreCase = true) }, colors.pastelLavender, colors.pastelLavenderBg),
            SubjectFolder("English", notes.count { it.category.equals("English", ignoreCase = true) }, colors.pastelPdfRed, colors.pastelPdfRedBg),
            SubjectFolder("My Notes", notes.count { it.category.equals("My Notes", ignoreCase = true) || it.category.isNullOrBlank() }, colors.pastelFavYellow, colors.pastelFavYellowBg)
        ) + customFolders.map { name ->
            SubjectFolder(name, notes.count { it.category.equals(name, ignoreCase = true) }, colors.pastelBlue, colors.pastelBlueBg)
        }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    AnimatedContent(
        targetState = selectedFolder,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
            } else {
                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
            }
        },
        label = "library_nav"
    ) { folder ->
        if (folder == null) {
            // Main Folder List View
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = statusBarTop + 14.dp, bottom = navBarBottom + 100.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header: "Library" and "+" Create Folder Button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Library",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            color = colors.text
                        )

                        Box {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.field)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, radius = 20.dp),
                                        onClick = { showAddMenu = true }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Content",
                                    tint = colors.text,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showAddMenu,
                                onDismissRequest = { showAddMenu = false },
                                modifier = Modifier.background(colors.glass)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Create New Note", color = colors.text) },
                                    leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null, tint = colors.pastelBlue) },
                                    onClick = {
                                        showAddMenu = false
                                        onCreateNewNote?.invoke(selectedFolder)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Import PDF Document", color = colors.text) },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = colors.pastelPdfRed) },
                                    onClick = {
                                        showAddMenu = false
                                        onImportPdf?.invoke()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Import Text / HTML File", color = colors.text) },
                                    leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null, tint = colors.pastelMint) },
                                    onClick = {
                                        showAddMenu = false
                                        onImportFiles?.invoke()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("New Subject Folder", color = colors.text) },
                                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = colors.pastelLavender) },
                                    onClick = {
                                        showAddMenu = false
                                        showCreateFolderDialog = true
                                    }
                                )
                            }
                        }
                    }
                }

                // Search Bar in Library
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = colors.shadow, spotColor = colors.shadow)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.glass)
                            .border(1.dp, colors.glassBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "Search notes, subjects...",
                                        fontSize = 14.sp,
                                        color = colors.textTertiary
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = colors.text,
                                    unfocusedTextColor = colors.text
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = colors.textTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Filter Chips: [All] [PDF] [HTML] [Favorites]
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        val filterOptions = listOf("All", "PDF", "HTML", "Favorites")
                        items(filterOptions) { opt ->
                            val isSelected = selectedTypeFilter == opt
                            val chipBg = if (isSelected) colors.pastelBlue else colors.glass
                            val chipTx = if (isSelected) Color.White else colors.textSecondary
                            val chipBorder = if (isSelected) colors.pastelBlue else colors.glassBorder

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(chipBg)
                                    .border(1.dp, chipBorder, RoundedCornerShape(16.dp))
                                    .clickable { selectedTypeFilter = opt }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = opt,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = chipTx
                                )
                            }
                        }
                    }
                }

                // Subject Folder Cards List
                val displayedFolders = folderDefs.filter {
                    searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
                }

                items(displayedFolders, key = { it.name }) { folderItem ->
                    SubjectFolderCard(
                        folder = folderItem,
                        onClick = { selectedFolder = folderItem.name }
                    )
                }
            }
        } else {
            // Folder Detail View (Inside Subject)
            val folderNotes = notes.filter { note ->
                val matchesCategory = if (folder == "My Notes") {
                    note.category.equals("My Notes", ignoreCase = true) || note.category.isNullOrBlank()
                } else {
                    note.category.equals(folder, ignoreCase = true)
                }
                val matchesQuery = searchQuery.isBlank() || note.displayTitle.contains(searchQuery, ignoreCase = true)
                val matchesFilter = when (selectedTypeFilter) {
                    "PDF" -> note.type == "pdf"
                    "HTML" -> note.type == "html"
                    "Favorites" -> note.pinned
                    else -> true
                }
                matchesCategory && matchesQuery && matchesFilter
            }

            val subjectDef = folderDefs.find { it.name == folder } ?: folderDefs.first()

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = statusBarTop + 14.dp, bottom = navBarBottom + 100.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Navigation Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.field)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, radius = 20.dp),
                                    onClick = { selectedFolder = null }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.text,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(subjectDef.bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = subjectDef.color,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = folder,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                                Text(
                                    text = "${folderNotes.size} notes available",
                                    fontSize = 13.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Box {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(colors.field)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, radius = 20.dp),
                                        onClick = { showAddMenu = true }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Content",
                                    tint = colors.text,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Filter Chips inside Folder
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        val filterOptions = listOf("All", "PDF", "HTML", "Favorites")
                        items(filterOptions) { opt ->
                            val isSelected = selectedTypeFilter == opt
                            val chipBg = if (isSelected) colors.pastelBlue else colors.glass
                            val chipTx = if (isSelected) Color.White else colors.textSecondary
                            val chipBorder = if (isSelected) colors.pastelBlue else colors.glassBorder

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(chipBg)
                                    .border(1.dp, chipBorder, RoundedCornerShape(16.dp))
                                    .clickable { selectedTypeFilter = opt }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = opt,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = chipTx
                                )
                            }
                        }
                    }
                }

                if (folderNotes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No notes in this subject yet",
                                fontSize = 14.sp,
                                color = colors.textTertiary
                            )
                        }
                    }
                } else {
                    items(folderNotes, key = { it.id }) { note ->
                        SwipeableRecentNoteWrapper(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onTogglePin = { onTogglePin(note.id) },
                            onDelete = { onDeleteNote?.invoke(note.id) }
                        )
                    }
                }
            }
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = {
                Text(
                    text = "Create Subject Folder",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
            },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("e.g. Economics, History, Literature") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.pastelBlue,
                        unfocusedBorderColor = colors.glassBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            customFolders = customFolders + newFolderName.trim()
                            newFolderName = ""
                            showCreateFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.pastelBlue)
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateFolderDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.textTertiary)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = colors.card,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun SubjectFolderCard(
    folder: SubjectFolder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    val shape = RoundedCornerShape(22.dp)

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
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Pastel Folder Icon Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(folder.bg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = folder.name,
                        tint = folder.color,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = folder.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )
                    Text(
                        text = "${folder.noteCount} notes",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.textSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Open Folder",
                tint = colors.textTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
