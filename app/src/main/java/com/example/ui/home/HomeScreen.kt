package com.example.ui.home

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import com.example.data.api.GeminiSearchMode
import com.example.data.model.NoteEntity
import com.example.data.model.NoteSummary
import com.example.ui.components.MainTab
import com.example.ui.components.NotesBottomBar
import com.example.ui.editor.NoteEditorScreen
import com.example.ui.screens.AnimatedTrashScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.reader.HtmlNoteReaderScreen
import com.example.ui.editor.PdfRendererView
import com.example.ui.screens.tabs.AiAssistantTab
import com.example.ui.screens.tabs.HomeTab
import com.example.ui.screens.tabs.LibraryTab
import com.example.ui.screens.tabs.ProfileTab
import com.example.ui.screens.tabs.QuickCategoryFilter
import com.example.ui.screens.tabs.SettingsTab
import com.example.ui.screens.tabs.ToolsTab
import com.example.ui.theme.GlassTheme
import com.example.ui.util.AmbientBackground
import com.example.ui.viewmodel.NotesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface ActiveScreen {
    data class Main(val tab: MainTab = MainTab.HOME) : ActiveScreen
    data class ReaderHtml(val note: NoteEntity) : ActiveScreen
    data class ReaderPdf(val note: NoteEntity) : ActiveScreen
    data class Editor(val note: NoteEntity) : ActiveScreen
    object Search : ActiveScreen
    object SettingsOverlay : ActiveScreen
    object Trash : ActiveScreen
}

@Composable
fun HomeScreen(
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    val coroutineScope = rememberCoroutineScope()

    var showSplash by remember { mutableStateOf(true) }
    var currentScreen by remember { mutableStateOf<ActiveScreen>(ActiveScreen.Main(MainTab.HOME)) }
    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    val pagerState = rememberPagerState(initialPage = currentTab.ordinal) { MainTab.entries.size }
    var homeFilter by remember { mutableStateOf(QuickCategoryFilter.ALL) }
    var aiInitialPrompt by remember { mutableStateOf<String?>(null) }

    // Sync pager -> currentTab when user swipes
    LaunchedEffect(pagerState.currentPage) {
        val tab = MainTab.entries[pagerState.currentPage]
        if (currentTab != tab) {
            currentTab = tab
        }
    }

    // Sync currentTab -> pager when tab changes programmatically
    LaunchedEffect(currentTab) {
        if (pagerState.currentPage != currentTab.ordinal) {
            pagerState.animateScrollToPage(currentTab.ordinal)
        }
    }

    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    val allNoteSummaries by viewModel.allNoteSummaries.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val chatSessions by viewModel.chatSessions.collectAsStateWithLifecycle()
    val activeChatSession by viewModel.activeChatSession.collectAsStateWithLifecycle()
    val geminiState by viewModel.geminiState.collectAsStateWithLifecycle()
    val chatSelectedModel by viewModel.chatSelectedModel.collectAsStateWithLifecycle()
    val chatDetailedAnswers by viewModel.chatDetailedAnswers.collectAsStateWithLifecycle()

    val noteSummaries: List<NoteSummary> = remember(allNotes, allNoteSummaries) {
        if (allNoteSummaries.isNotEmpty()) allNoteSummaries else allNotes.map { it.toSummary() }
    }

    fun openNote(noteId: String) {
        val note = allNotes.find { it.id == noteId } ?: return
        when (note.type.lowercase()) {
            "html" -> currentScreen = ActiveScreen.ReaderHtml(note)
            "pdf" -> currentScreen = ActiveScreen.ReaderPdf(note)
            else -> currentScreen = ActiveScreen.Editor(note)
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importPdfUris(uris)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFileUris(uris)
        }
    }

    fun createNewNote(category: String? = null) {
        viewModel.createNote(type = "text") { newId ->
            coroutineScope.launch {
                delay(120)
                if (!category.isNullOrBlank()) {
                    val n = allNotes.find { it.id == newId }
                    if (n != null) {
                        viewModel.updateNote(n.copy(category = category))
                    }
                }
                val newNote = allNotes.find { it.id == newId } ?: NoteEntity(
                    id = newId,
                    title = "Untitled Study Note",
                    content = "",
                    category = category ?: "My Notes"
                )
                currentScreen = ActiveScreen.Editor(newNote)
            }
        }
    }

    if (showSplash) {
        SplashScreen(
            onFinished = { showSplash = false }
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.bg)
        ) {
            AmbientBackground(isDark = colors.isDark)

            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    when {
                        targetState is ActiveScreen.ReaderHtml || targetState is ActiveScreen.ReaderPdf || targetState is ActiveScreen.Editor || targetState is ActiveScreen.Search || targetState is ActiveScreen.SettingsOverlay || targetState is ActiveScreen.Trash -> {
                            (slideInVertically { it / 3 } + fadeIn()).togetherWith(slideOutVertically { -it / 3 } + fadeOut())
                        }
                        else -> {
                            fadeIn().togetherWith(fadeOut())
                        }
                    }
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    is ActiveScreen.Main -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                userScrollEnabled = false,
                                pageSpacing = 0.dp
                            ) { page ->
                                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            val scale = lerp(0.96f, 1.0f, 1f - pageOffset.coerceIn(0f, 1f))
                                            val alpha = lerp(0.6f, 1.0f, 1f - pageOffset.coerceIn(0f, 1f))
                                            scaleX = scale
                                            scaleY = scale
                                            this.alpha = alpha
                                        }
                                ) {
                                    when (MainTab.entries[page]) {
                                         MainTab.HOME -> {
                                            HomeTab(
                                                notes = noteSummaries,
                                                selectedFilter = homeFilter,
                                                onFilterSelected = { homeFilter = it },
                                                onNoteClick = { noteId -> openNote(noteId) },
                                                onTogglePin = { noteId -> viewModel.togglePin(noteId) },
                                                onDeleteNote = { noteId -> viewModel.deleteNote(noteId) },
                                                onOpenSearch = { currentScreen = ActiveScreen.Search },
                                                onOpenSettings = { currentScreen = ActiveScreen.SettingsOverlay },
                                                onOpenCreateNote = { createNewNote() },
                                                onSeeAllClick = { currentTab = MainTab.LIBRARY },
                                                onToggleTheme = {
                                                    val nextTheme = if (colors.isDark) "light" else "dark"
                                                    viewModel.setTheme(nextTheme)
                                                }
                                            )
                                        }

                                        MainTab.LIBRARY -> {
                                            LibraryTab(
                                                notes = noteSummaries,
                                                onNoteClick = { noteId -> openNote(noteId) },
                                                onTogglePin = { noteId -> viewModel.togglePin(noteId) },
                                                onDeleteNote = { noteId -> viewModel.deleteNote(noteId) },
                                                onImportPdf = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                                                onImportFiles = { filePickerLauncher.launch(arrayOf("text/*", "text/html", "*/*")) },
                                                onCreateNewNote = { category -> createNewNote(category) }
                                            )
                                        }

                                        MainTab.AI -> {
                                            AiAssistantTab(
                                                chatSessions = chatSessions,
                                                activeChatSession = activeChatSession,
                                                geminiState = geminiState,
                                                geminiApiKey = settings.geminiApiKey,
                                                chatSelectedModel = chatSelectedModel,
                                                chatDetailedAnswers = chatDetailedAnswers,
                                                onSendChatPrompt = { prompt -> viewModel.sendChatPrompt(prompt) },
                                                onStartNewChat = { viewModel.startNewChatSession() },
                                                onSelectChatSession = { id -> viewModel.setActiveChatSession(id) },
                                                onDeleteChatSession = { id -> viewModel.deleteChatSession(id) },
                                                onClearAllChats = { viewModel.clearAllChatSessions() },
                                                onSetGeminiApiKey = { key -> viewModel.setGeminiApiKey(key) },
                                                onSetChatDetailedAnswers = { detailed -> viewModel.setChatDetailedAnswers(detailed) },
                                                onSetChatSelectedModel = { model -> viewModel.setChatSelectedModel(model) },
                                                initialPrompt = aiInitialPrompt
                                            )
                                        }

                                        MainTab.TOOLS -> {
                                            ToolsTab(
                                                notes = noteSummaries,
                                                onOpenPdfReader = {
                                                    val pdfNote = allNotes.find { it.type == "pdf" } ?: allNotes.firstOrNull()
                                                    if (pdfNote != null) currentScreen = ActiveScreen.ReaderPdf(pdfNote)
                                                },
                                                onOpenHtmlViewer = {
                                                    val htmlNote = allNotes.find { it.type == "html" } ?: allNotes.firstOrNull()
                                                    if (htmlNote != null) currentScreen = ActiveScreen.ReaderHtml(htmlNote)
                                                },
                                                onOpenNoteEditor = { createNewNote() },
                                                onOpenBookmarks = {
                                                    currentTab = MainTab.LIBRARY
                                                },
                                                onOpenNote = { noteId -> openNote(noteId) },
                                                onAddNote = { title, content, type, category ->
                                                    viewModel.addNoteDirect(title, content, type, category)
                                                }
                                            )
                                        }

                                        MainTab.PROFILE -> {
                                            ProfileTab(
                                                notes = noteSummaries,
                                                onOpenMyNotes = { currentTab = MainTab.LIBRARY },
                                                onOpenFavorites = {
                                                    currentTab = MainTab.HOME
                                                    homeFilter = QuickCategoryFilter.FAVORITES
                                                },
                                                onOpenTrash = {
                                                    currentScreen = ActiveScreen.Trash
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

                            // Persistent Frosted Glass Bottom Navigation Bar
                            NotesBottomBar(
                                currentTab = currentTab,
                                onTabSelected = { tab ->
                                    coroutineScope.launch {
                                        currentTab = tab
                                        aiInitialPrompt = null
                                        pagerState.animateScrollToPage(tab.ordinal)
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(start = 20.dp, end = 20.dp, bottom = navBarBottom + 12.dp)
                            )
                        }
                    }

                    is ActiveScreen.ReaderHtml -> {
                        BackHandler { currentScreen = ActiveScreen.Main(currentTab) }
                        HtmlNoteReaderScreen(
                            note = screen.note,
                            onBack = { currentScreen = ActiveScreen.Main(currentTab) },
                            onOpenAiSummary = { content ->
                                aiInitialPrompt = "Please provide an executive study summary and bulleted key takeaways for:\n$content"
                                currentTab = MainTab.AI
                                currentScreen = ActiveScreen.Main(MainTab.AI)
                            },
                            onToggleBookmark = { noteId -> viewModel.togglePin(noteId) },
                            onDelete = {
                                viewModel.deleteNote(screen.note.id)
                                currentScreen = ActiveScreen.Main(currentTab)
                            }
                        )
                    }

                    is ActiveScreen.ReaderPdf -> {
                        BackHandler { currentScreen = ActiveScreen.Main(currentTab) }
                        PdfRendererView(
                            note = screen.note,
                            onBack = { currentScreen = ActiveScreen.Main(currentTab) },
                            onSaveTitle = { title ->
                                viewModel.saveNote(screen.note.id, title, screen.note.content)
                            },
                            onAskGemini = { prompt, _ ->
                                aiInitialPrompt = prompt
                                currentTab = MainTab.AI
                                currentScreen = ActiveScreen.Main(MainTab.AI)
                            }
                        )
                    }

                    is ActiveScreen.Editor -> {
                        BackHandler { currentScreen = ActiveScreen.Main(currentTab) }
                        NoteEditorScreen(
                            note = screen.note,
                            fontSize = settings.readingFontSize,
                            onBack = { currentScreen = ActiveScreen.Main(currentTab) },
                            onSave = { title, content ->
                                viewModel.saveNote(screen.note.id, title, content)
                            },
                            onOpenMore = { },
                            onAskGemini = { prompt, _ ->
                                aiInitialPrompt = prompt
                                currentTab = MainTab.AI
                                currentScreen = ActiveScreen.Main(MainTab.AI)
                            }
                        )
                    }

                    is ActiveScreen.Search -> {
                        BackHandler { currentScreen = ActiveScreen.Main(currentTab) }
                        SearchScreen(
                            notes = noteSummaries,
                            onNoteClick = { noteId -> openNote(noteId) },
                            onTogglePin = { noteId -> viewModel.togglePin(noteId) },
                            onBack = { currentScreen = ActiveScreen.Main(currentTab) }
                        )
                    }

                    is ActiveScreen.SettingsOverlay -> {
                        BackHandler { currentScreen = ActiveScreen.Main(currentTab) }
                        SettingsTab(
                            currentThemeSetting = settings.theme,
                            onThemeSettingChange = { viewModel.setTheme(it) },
                            onExportBackup = { viewModel.exportBackupJson { } },
                            onImportBackup = { },
                            onShowAboutSplash = { showSplash = true },
                            onBack = { currentScreen = ActiveScreen.Main(currentTab) },
                            onSyncFullDevice = { viewModel.syncFullDevice() },
                            geminiApiKey = settings.geminiApiKey,
                            onUpdateApiKey = { key -> viewModel.setGeminiApiKey(key) }
                        )
                    }

                    is ActiveScreen.Trash -> {
                        BackHandler { currentScreen = ActiveScreen.Main(currentTab) }
                        AnimatedTrashScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = ActiveScreen.Main(currentTab) }
                        )
                    }
                }
            }
        }
    }
}
