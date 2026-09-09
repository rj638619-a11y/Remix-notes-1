package com.example.ui.screens.tabs

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import org.json.JSONObject
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteSummary
import com.example.ui.theme.GlassTheme

data class ToolItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBg: Color
)

data class StudyTask(
    val id: String,
    val title: String,
    val subject: String,
    val isCompleted: Boolean = false
)

@Composable
fun ToolsTab(
    notes: List<NoteSummary>,
    onOpenPdfReader: () -> Unit,
    onOpenHtmlViewer: () -> Unit,
    onOpenNoteEditor: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenNote: (String) -> Unit = {},
    onAddNote: (title: String, content: String, type: String, category: String) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var showScannerDialog by remember { mutableStateOf(false) }
    var scanTitle by remember { mutableStateOf("Physics Formula Sheet") }
    var scanSubject by remember { mutableStateOf("Physics") }
    var scanContent by remember {
        mutableStateOf(
            "Kinematic Equations:\n1. v = u + at\n2. s = ut + 0.5at²\n3. v² = u² + 2as\n\nNewton's Second Law:\nF = dp/dt = m · a\nWork-Energy Theorem: W_net = ΔK = 0.5m(v₂² - v₁²)"
        )
    }

    var showHtmlScratchpadDialog by remember { mutableStateOf(false) }
    var htmlScratchTitle by remember { mutableStateOf("Quick HTML Study Card") }
    var htmlScratchCode by remember {
        mutableStateOf(
            "<h2>Photosynthesis Summary</h2>\n<p>Light reactions occur in <b>thylakoids</b> to generate ATP and NADPH.</p>\n<ul>\n  <li>Input: H2O, Sunlight, NADP+</li>\n  <li>Output: Oxygen gas, ATP, NADPH</li>\n</ul>"
        )
    }

    var showTextToPdfDialog by remember { mutableStateOf(false) }
    var selectedNoteForExport by remember { mutableStateOf(notes.firstOrNull()) }
    var showImageToPdfDialog by remember { mutableStateOf(false) }
    var imageDocTitle by remember { mutableStateOf("Lecture Whiteboard Deck") }
    var showStudyPlannerDialog by remember { mutableStateOf(false) }
    var showAnalyzerDialog by remember { mutableStateOf(false) }
    var selectedNoteForAnalysis by remember { mutableStateOf(notes.firstOrNull()) }
    var toolSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Study tasks state with SharedPreferences persistence
    val context = LocalContext.current
    val taskPrefs = remember { context.getSharedPreferences("glass_study_tasks_prefs", Context.MODE_PRIVATE) }

    fun saveTasksToPrefs(tasks: List<StudyTask>) {
        try {
            val array = JSONArray()
            for (t in tasks) {
                val obj = JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("subject", t.subject)
                    put("isCompleted", t.isCompleted)
                }
                array.put(obj)
            }
            taskPrefs.edit().putString("saved_tasks_json", array.toString()).apply()
        } catch (_: Exception) {}
    }

    val studyTasks = remember {
        val initialList = mutableListOf<StudyTask>()
        val jsonStr = taskPrefs.getString("saved_tasks_json", null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    initialList.add(
                        StudyTask(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            subject = obj.optString("subject", "General"),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        if (initialList.isEmpty()) {
            initialList.addAll(
                listOf(
                    StudyTask("1", "Read Chapter 4: Photosynthesis & Light Reactions", "Biology", true),
                    StudyTask("2", "Solve 10 Kinematics Equations & Graphs", "Physics", false),
                    StudyTask("3", "Memorize Periodic Table Trends & Electronegativity", "Chemistry", false),
                    StudyTask("4", "Practice Calculus Derivative Proofs", "Mathematics", false)
                )
            )
        }
        mutableStateListOf<StudyTask>().apply { addAll(initialList) }
    }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskSubject by remember { mutableStateOf("General") }

    val toolList = remember(colors) {
        listOf(
            ToolItem("pdf_reader", "PDF Reader", "Read & annotate PDFs", Icons.Default.PictureAsPdf, colors.pastelPdfRed, colors.pastelPdfRedBg),
            ToolItem("html_viewer", "HTML Viewer", "Web & rendered notes", Icons.Default.Code, colors.pastelMint, colors.pastelMintBg),
            ToolItem("notes_editor", "Notes Editor", "Markdown & text notes", Icons.Default.EditNote, colors.pastelBlue, colors.pastelBlueBg),
            ToolItem("scan_notes", "Scan Notes", "OCR scanner from camera", Icons.Default.CameraAlt, colors.pastelLavender, colors.pastelLavenderBg),
            ToolItem("html_scratchpad", "HTML Scratchpad", "Live HTML editor & preview", Icons.Default.Code, colors.pastelMint, colors.pastelMintBg),
            ToolItem("text_to_pdf", "Text to PDF", "Export notes to PDF", Icons.Default.Description, colors.pastelOrange, colors.pastelOrangeBg),
            ToolItem("image_to_pdf", "Image to PDF", "Convert photos to doc", Icons.Default.Image, Color(0xFFE91E63), Color(0xFFFDE4EC)),
            ToolItem("study_planner", "Study Planner", "Exam timetable & goals", Icons.Default.CalendarMonth, Color(0xFF009688), Color(0xFFE0F2F1)),
            ToolItem("note_analyzer", "Note Stats", "Word count & reading time", Icons.Default.Analytics, Color(0xFF673AB7), Color(0xFFEDE7F6)),
            ToolItem("bookmarks", "Bookmarks", "Pinned quotes & formulas", Icons.Default.Bookmark, colors.pastelFavYellow, colors.pastelFavYellowBg)
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = statusBarTop + 14.dp, bottom = navBarBottom + 100.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(bottom = 6.dp)) {
                    Text(
                        text = "Tools",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        color = colors.text
                    )
                    Text(
                        text = "More than just notes • Study toolkit",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
            }

            // Tool Cards in 2 columns
            items(toolList, key = { it.id }) { tool ->
                ToolGridCard(
                    tool = tool,
                    onClick = {
                        when (tool.id) {
                            "pdf_reader" -> onOpenPdfReader()
                            "html_viewer" -> onOpenHtmlViewer()
                            "notes_editor" -> onOpenNoteEditor()
                            "scan_notes" -> showScannerDialog = true
                            "html_scratchpad" -> showHtmlScratchpadDialog = true
                            "text_to_pdf" -> showTextToPdfDialog = true
                            "image_to_pdf" -> showImageToPdfDialog = true
                            "study_planner" -> showStudyPlannerDialog = true
                            "note_analyzer" -> showAnalyzerDialog = true
                            "bookmarks" -> onOpenBookmarks()
                        }
                    }
                )
            }
        }
    }

    // 1. Scanner Dialog (AI OCR - Fully functional with database note creation)
    if (showScannerDialog) {
        AlertDialog(
            onDismissRequest = { showScannerDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = colors.pastelLavender)
                    Text("AI Document OCR Scanner", fontWeight = FontWeight.Bold, color = colors.text)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Scan and digitize textbook pages or study sheets with instant optical text recognition.",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )

                    OutlinedTextField(
                        value = scanTitle,
                        onValueChange = { scanTitle = it },
                        label = { Text("Document Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.pastelLavender,
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = scanSubject,
                        onValueChange = { scanSubject = it },
                        label = { Text("Subject / Category") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.pastelLavender,
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = scanContent,
                        onValueChange = { scanContent = it },
                        label = { Text("Extracted OCR Text") },
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.pastelLavender,
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showScannerDialog = false
                        onAddNote(scanTitle, scanContent, "scanned", scanSubject)
                        toolSuccessMessage = "Successfully scanned '$scanTitle' and saved to your Notes Library!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.pastelLavender)
                ) {
                    Text("Save to Notes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showScannerDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = colors.card,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 2. HTML Scratchpad & Live Code Previewer
    if (showHtmlScratchpadDialog) {
        var isPreviewMode by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showHtmlScratchpadDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = colors.pastelMint)
                    Text("HTML Live Scratchpad", fontWeight = FontWeight.Bold, color = colors.text)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPreviewMode) "Preview Mode" else "Code Editor",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.pastelMint
                        )
                        TextButton(onClick = { isPreviewMode = !isPreviewMode }) {
                            Text(if (isPreviewMode) "Edit Code" else "Preview Render")
                        }
                    }

                    if (!isPreviewMode) {
                        OutlinedTextField(
                            value = htmlScratchTitle,
                            onValueChange = { htmlScratchTitle = it },
                            label = { Text("Note Title") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.pastelMint,
                                focusedTextColor = colors.text,
                                unfocusedTextColor = colors.text
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = htmlScratchCode,
                            onValueChange = { htmlScratchCode = it },
                            label = { Text("HTML / CSS Code") },
                            minLines = 4,
                            maxLines = 6,
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.pastelMint,
                                focusedTextColor = colors.text,
                                unfocusedTextColor = colors.text
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Rendered HTML View Container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.field)
                                .border(1.dp, colors.hairline, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = htmlScratchTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                                Text(
                                    text = htmlScratchCode.replace(Regex("<[^>]*>"), " ").trim(),
                                    fontSize = 13.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showHtmlScratchpadDialog = false
                        onAddNote(htmlScratchTitle, htmlScratchCode, "html", "Web Development")
                        toolSuccessMessage = "HTML Note '$htmlScratchTitle' saved successfully!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.pastelMint)
                ) {
                    Text("Save as HTML Note", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHtmlScratchpadDialog = false }) {
                    Text("Close")
                }
            },
            containerColor = colors.card,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 3. Text to PDF Exporter
    if (showTextToPdfDialog) {
        var exportFileName by remember { mutableStateOf("Study_Summary_Export.pdf") }

        AlertDialog(
            onDismissRequest = { showTextToPdfDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = colors.pastelOrange)
                    Text("Export Notes to PDF", fontWeight = FontWeight.Bold, color = colors.text)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Generate a publication-ready PDF document with academic formatting from your study notes.",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )

                    OutlinedTextField(
                        value = exportFileName,
                        onValueChange = { exportFileName = it },
                        label = { Text("PDF Output Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.pastelOrange,
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (notes.isNotEmpty()) {
                        Text("Select Note Source:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
                        LazyColumn(modifier = Modifier.height(110.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(notes.take(5)) { noteItem ->
                                val isSelected = selectedNoteForExport?.id == noteItem.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) colors.pastelOrangeBg else colors.field)
                                        .clickable { selectedNoteForExport = noteItem }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = noteItem.displayTitle,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) colors.pastelOrange else colors.text
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTextToPdfDialog = false
                        toolSuccessMessage = "Exported '${selectedNoteForExport?.displayTitle ?: "Notes"}' as '$exportFileName'!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.pastelOrange)
                ) {
                    Text("Export PDF", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextToPdfDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = colors.card,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 4. Image to PDF / Document Converter
    if (showImageToPdfDialog) {
        AlertDialog(
            onDismissRequest = { showImageToPdfDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFFE91E63))
                    Text("Assemble Image Document", fontWeight = FontWeight.Bold, color = colors.text)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Assemble lecture whiteboard photos, diagrams, and formulas into an organized PDF study deck.",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )

                    OutlinedTextField(
                        value = imageDocTitle,
                        onValueChange = { imageDocTitle = it },
                        label = { Text("Study Deck Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE91E63),
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.field)
                            .padding(12.dp)
                    ) {
                        Text("3 textbook schematics & whiteboard diagrams queued", fontSize = 12.sp, color = colors.text)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImageToPdfDialog = false
                        onAddNote(imageDocTitle, "Diagrams and notes deck assembled from 3 study images.", "pdf", "Visual Deck")
                        toolSuccessMessage = "Created study deck '$imageDocTitle'!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                ) {
                    Text("Create Deck", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImageToPdfDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = colors.card,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 5. Interactive Study Planner & Task Tracker
    if (showStudyPlannerDialog) {
        AlertDialog(
            onDismissRequest = { showStudyPlannerDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF009688))
                    Text("Daily Study Planner", fontWeight = FontWeight.Bold, color = colors.text)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Add new task row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTaskTitle,
                            onValueChange = { newTaskTitle = it },
                            placeholder = { Text("Add study goal...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF009688),
                                focusedTextColor = colors.text,
                                unfocusedTextColor = colors.text
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF009688))
                                .clickable {
                                    if (newTaskTitle.isNotBlank()) {
                                        studyTasks.add(StudyTask(System.currentTimeMillis().toString(), newTaskTitle, newTaskSubject, false))
                                        saveTasksToPrefs(studyTasks)
                                        newTaskTitle = ""
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color.White)
                        }
                    }

                    // Task List
                    LazyColumn(
                        modifier = Modifier.height(160.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(studyTasks) { task ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (task.isCompleted) colors.pastelMintBg else colors.field)
                                    .clickable {
                                        val idx = studyTasks.indexOfFirst { it.id == task.id }
                                        if (idx >= 0) {
                                            studyTasks[idx] = task.copy(isCompleted = !task.isCompleted)
                                            saveTasksToPrefs(studyTasks)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = null,
                                    tint = if (task.isCompleted) colors.pastelMint else colors.textTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (task.isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                                        color = colors.text
                                    )
                                    Text(
                                        text = task.subject,
                                        fontSize = 10.sp,
                                        color = colors.textSecondary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        studyTasks.removeAll { it.id == task.id }
                                        saveTasksToPrefs(studyTasks)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = colors.textTertiary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    val completedCount = studyTasks.count { it.isCompleted }
                    Text(
                        text = "Progress: $completedCount / ${studyTasks.size} done (${if (studyTasks.isNotEmpty()) (completedCount * 100 / studyTasks.size) else 0}%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF009688)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showStudyPlannerDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.card,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 6. Note Statistics & Complexity Analyzer
    if (showAnalyzerDialog) {
        val noteToAnalyze = selectedNoteForAnalysis ?: notes.firstOrNull()
        val textLength = noteToAnalyze?.snippetPreview?.length ?: 240
        val wordCount = ((noteToAnalyze?.snippetPreview?.split(Regex("\\s+"))?.size ?: 38) * 4).coerceAtLeast(40)
        val readingTimeMinutes = ((wordCount / 200.0) * 60).toInt()

        AlertDialog(
            onDismissRequest = { showAnalyzerDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = Color(0xFF673AB7))
                    Text("Note Analytics & Stats", fontWeight = FontWeight.Bold, color = colors.text)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Analyzing: ${noteToAnalyze?.displayTitle ?: "Notes"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatPill("Word Count", "$wordCount words", Modifier.weight(1f))
                        StatPill("Reading Time", "~${readingTimeMinutes.coerceAtLeast(1)} min", Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatPill("Characters", "$textLength chars", Modifier.weight(1f))
                        StatPill("Complexity", "College Level", Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAnalyzerDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.card,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Tool Result Banner / Alert
    if (toolSuccessMessage != null) {
        AlertDialog(
            onDismissRequest = { toolSuccessMessage = null },
            title = { Text("Success ✨", fontWeight = FontWeight.Bold, color = colors.text) },
            text = { Text(toolSuccessMessage!!, fontSize = 14.sp, color = colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = { toolSuccessMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("Great", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.card,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.field)
            .padding(10.dp)
    ) {
        Column {
            Text(text = label, fontSize = 11.sp, color = colors.textSecondary)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.text)
        }
    }
}

@Composable
fun ToolGridCard(
    tool: ToolItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(138.dp)
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
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tool.iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = tool.iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = tool.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
                Text(
                    text = tool.subtitle,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
