package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.data.model.NoteEntity
import com.example.util.DateFormatter

class GlassNotesWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetManager.getWidgetData(context)

        provideContent {
            WidgetContent(context = context, data = data)
        }
    }

    @Composable
    private fun WidgetContent(context: Context, data: WidgetData) {
        val size = LocalSize.current
        val width = size.width
        val height = size.height

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val createNoteIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ACTION, MainActivity.ACTION_CREATE)
        }

        val widgetConfigIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ACTION, MainActivity.ACTION_WIDGET_CONFIG)
        }

        val searchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ACTION, MainActivity.ACTION_SEARCH)
        }

        // Color palette for ultra-polished frosted dark glassmorphism
        val outerBg = Color(0xF212151C)
        val cardBg = Color(0x2EFFFFFF)
        val cardBorder = Color(0x38FFFFFF)
        val pillActiveBg = Color(0x3DF2B90C)
        val pillActiveText = Color(0xFFFACC15)
        val pillInactiveBg = Color(0x1AFFFFFF)
        val pillInactiveText = Color(0xFF94A3B8)
        val textPrimary = Color(0xFFF1F5F9)
        val textSecondary = Color(0xFFCBD5E1)
        val textTertiary = Color(0xFF7E8B9B)
        val amberAccent = Color(0xFFF2B90C)
        val dividerColor = Color(0x1FFFFFFF)

        val activeNote = data.activeNote

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(outerBg))
                .cornerRadius(26.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Case 1: Ultra-compact horizontal strip (e.g. 2x1 cell or height < 115.dp)
            if (height < 115.dp) {
                CompactStripLayout(
                    data = data,
                    activeNote = activeNote,
                    openAppIntent = openAppIntent,
                    createNoteIntent = createNoteIntent,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    amberAccent = amberAccent,
                    cardBg = cardBg
                )
            } else if (width < 205.dp) {
                // Case 2: Narrow / Small vertical layout (e.g. 2x2 or 2x3 cell)
                CompactVerticalLayout(
                    data = data,
                    activeNote = activeNote,
                    openAppIntent = openAppIntent,
                    createNoteIntent = createNoteIntent,
                    widgetConfigIntent = widgetConfigIntent,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    textTertiary = textTertiary,
                    amberAccent = amberAccent,
                    cardBg = cardBg,
                    dividerColor = dividerColor
                )
            } else {
                // Case 3: Standard & Large interactive widget (3x2, 3x3, 4x3, 4x4, etc.)
                StandardWidgetLayout(
                    data = data,
                    activeNote = activeNote,
                    openAppIntent = openAppIntent,
                    createNoteIntent = createNoteIntent,
                    widgetConfigIntent = widgetConfigIntent,
                    searchIntent = searchIntent,
                    widgetHeight = height,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    textTertiary = textTertiary,
                    amberAccent = amberAccent,
                    cardBg = cardBg,
                    pillActiveBg = pillActiveBg,
                    pillActiveText = pillActiveText,
                    pillInactiveBg = pillInactiveBg,
                    pillInactiveText = pillInactiveText,
                    dividerColor = dividerColor
                )
            }
        }
    }

    @Composable
    private fun CompactStripLayout(
        data: WidgetData,
        activeNote: NoteEntity?,
        openAppIntent: Intent,
        createNoteIntent: Intent,
        textPrimary: Color,
        textSecondary: Color,
        amberAccent: Color,
        cardBg: Color
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Amber Pin Icon Badge
            Box(
                modifier = GlanceModifier
                    .size(34.dp)
                    .background(ColorProvider(Color(0x33F2B90C)))
                    .cornerRadius(10.dp)
                    .clickable(actionStartActivity(openAppIntent)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📌", style = TextStyle(fontSize = 15.sp))
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Note title & details
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(
                        if (activeNote != null) {
                            actionStartActivity(
                                Intent(openAppIntent).apply {
                                    putExtra(MainActivity.EXTRA_OPEN_NOTE_ID, activeNote.id)
                                }
                            )
                        } else {
                            actionStartActivity(openAppIntent)
                        }
                    )
            ) {
                Text(
                    text = activeNote?.displayTitle ?: "Glass Notes",
                    style = TextStyle(
                        color = ColorProvider(textPrimary),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(1.dp))
                Text(
                    text = if (activeNote != null) {
                        if (activeNote.snippet.isNotBlank()) activeNote.snippet else "Tap to open note"
                    } else "No notes selected",
                    style = TextStyle(
                        color = ColorProvider(textSecondary),
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = GlanceModifier.width(6.dp))

            // Prev note button
            if (data.notes.size > 1) {
                Box(
                    modifier = GlanceModifier
                        .size(28.dp)
                        .background(ColorProvider(Color(0x22FFFFFF)))
                        .cornerRadius(14.dp)
                        .clickable(actionRunCallback<CycleNoteAction>(actionParametersOf(CycleNoteAction.DirectionKey to -1))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "‹",
                        style = TextStyle(color = ColorProvider(textPrimary), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = GlanceModifier.width(4.dp))

                // Next note button
                Box(
                    modifier = GlanceModifier
                        .size(28.dp)
                        .background(ColorProvider(Color(0x22FFFFFF)))
                        .cornerRadius(14.dp)
                        .clickable(actionRunCallback<CycleNoteAction>(actionParametersOf(CycleNoteAction.DirectionKey to 1))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "›",
                        style = TextStyle(color = ColorProvider(textPrimary), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = GlanceModifier.width(4.dp))
            }

            // Quick Create Note Button
            Box(
                modifier = GlanceModifier
                    .size(28.dp)
                    .background(ColorProvider(amberAccent))
                    .cornerRadius(14.dp)
                    .clickable(actionStartActivity(createNoteIntent)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF1E1500)),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }

    @Composable
    private fun CompactVerticalLayout(
        data: WidgetData,
        activeNote: NoteEntity?,
        openAppIntent: Intent,
        createNoteIntent: Intent,
        widgetConfigIntent: Intent,
        textPrimary: Color,
        textSecondary: Color,
        textTertiary: Color,
        amberAccent: Color,
        cardBg: Color,
        dividerColor: Color
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionStartActivity(openAppIntent)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(22.dp)
                            .background(ColorProvider(Color(0x33F2B90C)))
                            .cornerRadius(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📌", style = TextStyle(fontSize = 11.sp))
                    }
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = "Glass Notes",
                        style = TextStyle(
                            color = ColorProvider(textPrimary),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Box(
                    modifier = GlanceModifier
                        .size(24.dp)
                        .background(ColorProvider(amberAccent))
                        .cornerRadius(12.dp)
                        .clickable(actionStartActivity(createNoteIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF1A1202)),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Divider
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ColorProvider(dividerColor))
            ) {}

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Main Note Card
            if (activeNote == null) {
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth()
                        .clickable(actionStartActivity(widgetConfigIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No note chosen",
                            style = TextStyle(
                                color = ColorProvider(textSecondary),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(3.dp))
                        Text(
                            text = "Tap to pick a note",
                            style = TextStyle(
                                color = ColorProvider(amberAccent),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            } else {
                val openNoteIntent = Intent(openAppIntent).apply {
                    putExtra(MainActivity.EXTRA_OPEN_NOTE_ID, activeNote.id)
                }

                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth()
                        .background(ColorProvider(cardBg))
                        .cornerRadius(16.dp)
                        .clickable(actionStartActivity(openNoteIntent))
                        .padding(9.dp)
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Text(
                            text = activeNote.displayTitle,
                            style = TextStyle(
                                color = ColorProvider(textPrimary),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 2
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = if (activeNote.snippet.isNotBlank()) activeNote.snippet else "Tap to view full note",
                            style = TextStyle(
                                color = ColorProvider(textSecondary),
                                fontSize = 11.sp
                            ),
                            maxLines = 3
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Footer controls: Prev / Indicator / Next
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (data.notes.size > 1) {
                    Box(
                        modifier = GlanceModifier
                            .size(24.dp)
                            .background(ColorProvider(Color(0x22FFFFFF)))
                            .cornerRadius(12.dp)
                            .clickable(actionRunCallback<CycleNoteAction>(actionParametersOf(CycleNoteAction.DirectionKey to -1))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "‹",
                            style = TextStyle(color = ColorProvider(textPrimary), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    Text(
                        text = "${data.currentIndex + 1}/${data.notes.size}",
                        style = TextStyle(
                            color = ColorProvider(textTertiary),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    Box(
                        modifier = GlanceModifier
                            .size(24.dp)
                            .background(ColorProvider(Color(0x22FFFFFF)))
                            .cornerRadius(12.dp)
                            .clickable(actionRunCallback<CycleNoteAction>(actionParametersOf(CycleNoteAction.DirectionKey to 1))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "›",
                            style = TextStyle(color = ColorProvider(textPrimary), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(Color(0x1FFFFFFF)))
                            .cornerRadius(10.dp)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .clickable(actionStartActivity(widgetConfigIntent))
                    ) {
                        Text(
                            text = "Pick Note",
                            style = TextStyle(
                                color = ColorProvider(textSecondary),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun StandardWidgetLayout(
        data: WidgetData,
        activeNote: NoteEntity?,
        openAppIntent: Intent,
        createNoteIntent: Intent,
        widgetConfigIntent: Intent,
        searchIntent: Intent,
        widgetHeight: androidx.compose.ui.unit.Dp,
        textPrimary: Color,
        textSecondary: Color,
        textTertiary: Color,
        amberAccent: Color,
        cardBg: Color,
        pillActiveBg: Color,
        pillActiveText: Color,
        pillInactiveBg: Color,
        pillInactiveText: Color,
        dividerColor: Color
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Top Bar: Brand, Interactive Tabs, Add Note Button
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo & Name
                Row(
                    modifier = GlanceModifier.clickable(actionStartActivity(openAppIntent)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(26.dp)
                            .background(ColorProvider(Color(0x35F2B90C)))
                            .cornerRadius(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📌", style = TextStyle(fontSize = 13.sp))
                    }
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = "Notes",
                        style = TextStyle(
                            color = ColorProvider(textPrimary),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Interactive Mode Switcher Tabs
                Row(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ModeTabItem(
                        label = "Selected",
                        mode = WidgetManager.MODE_SELECTED,
                        currentMode = data.mode,
                        activeBg = pillActiveBg,
                        activeText = pillActiveText,
                        inactiveBg = pillInactiveBg,
                        inactiveText = pillInactiveText
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    ModeTabItem(
                        label = "Pinned",
                        mode = WidgetManager.MODE_PINNED,
                        currentMode = data.mode,
                        activeBg = pillActiveBg,
                        activeText = pillActiveText,
                        inactiveBg = pillInactiveBg,
                        inactiveText = pillInactiveText
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    ModeTabItem(
                        label = "Recent",
                        mode = WidgetManager.MODE_RECENT,
                        currentMode = data.mode,
                        activeBg = pillActiveBg,
                        activeText = pillActiveText,
                        inactiveBg = pillInactiveBg,
                        inactiveText = pillInactiveText
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    ModeTabItem(
                        label = "Tasks",
                        mode = WidgetManager.MODE_TODOS,
                        currentMode = data.mode,
                        activeBg = pillActiveBg,
                        activeText = pillActiveText,
                        inactiveBg = pillInactiveBg,
                        inactiveText = pillInactiveText
                    )
                }

                Spacer(modifier = GlanceModifier.width(6.dp))

                // Quick Add Button
                Box(
                    modifier = GlanceModifier
                        .size(28.dp)
                        .background(ColorProvider(amberAccent))
                        .cornerRadius(14.dp)
                        .clickable(actionStartActivity(createNoteIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF1E1500)),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(7.dp))

            // Sub-control bar: Note counter, Prev/Next buttons, Pin toggle, and Config shortcut
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Cycle controls
                if (data.notes.size > 1) {
                    Row(
                        modifier = GlanceModifier
                            .background(ColorProvider(Color(0x1FFFFFFF)))
                            .cornerRadius(12.dp)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .size(22.dp)
                                .clickable(actionRunCallback<CycleNoteAction>(actionParametersOf(CycleNoteAction.DirectionKey to -1))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "◀",
                                style = TextStyle(color = ColorProvider(textPrimary), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(4.dp))

                        Text(
                            text = "${data.currentIndex + 1} of ${data.notes.size}",
                            style = TextStyle(
                                color = ColorProvider(textSecondary),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Spacer(modifier = GlanceModifier.width(4.dp))

                        Box(
                            modifier = GlanceModifier
                                .size(22.dp)
                                .clickable(actionRunCallback<CycleNoteAction>(actionParametersOf(CycleNoteAction.DirectionKey to 1))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "▶",
                                style = TextStyle(color = ColorProvider(textPrimary), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(Color(0x1AFFFFFF)))
                            .cornerRadius(10.dp)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when (data.mode) {
                                WidgetManager.MODE_SELECTED -> "Active Note"
                                WidgetManager.MODE_PINNED -> "Pinned Note"
                                WidgetManager.MODE_RECENT -> "Latest Note"
                                else -> "Task Note"
                            },
                            style = TextStyle(
                                color = ColorProvider(textTertiary),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Interactive Pin Toggle
                if (activeNote != null) {
                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(if (activeNote.pinned) Color(0x3DF2B90C) else Color(0x18FFFFFF)))
                            .cornerRadius(10.dp)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                            .clickable(actionRunCallback<TogglePinAction>(actionParametersOf(TogglePinAction.NoteIdKey to activeNote.id))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (activeNote.pinned) "📌 Pinned" else "Pin Note",
                            style = TextStyle(
                                color = ColorProvider(if (activeNote.pinned) amberAccent else textTertiary),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(6.dp))
                }

                // Choose Note Button (opens note selector in app)
                Box(
                    modifier = GlanceModifier
                        .background(ColorProvider(Color(0x22FFFFFF)))
                        .cornerRadius(10.dp)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .clickable(actionStartActivity(widgetConfigIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📋 Select",
                        style = TextStyle(
                            color = ColorProvider(textSecondary),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Main Featured Note Card
            if (activeNote == null) {
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth()
                        .background(ColorProvider(cardBg))
                        .cornerRadius(18.dp)
                        .clickable(actionStartActivity(widgetConfigIntent))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No notes found in this category",
                            style = TextStyle(
                                color = ColorProvider(textSecondary),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "Tap to choose a note from Glass Notes",
                            style = TextStyle(
                                color = ColorProvider(amberAccent),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            } else {
                val openNoteIntent = Intent(openAppIntent).apply {
                    putExtra(MainActivity.EXTRA_OPEN_NOTE_ID, activeNote.id)
                }

                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth()
                        .background(ColorProvider(cardBg))
                        .cornerRadius(18.dp)
                        .clickable(actionStartActivity(openNoteIntent))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        // Title & Category Row
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeNote.displayTitle,
                                style = TextStyle(
                                    color = ColorProvider(textPrimary),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight()
                            )

                            if (!activeNote.category.isNullOrBlank()) {
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                Box(
                                    modifier = GlanceModifier
                                        .background(ColorProvider(Color(0x3560A5FA)))
                                        .cornerRadius(6.dp)
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "#${activeNote.category}",
                                        style = TextStyle(
                                            color = ColorProvider(Color(0xFF93C5FD)),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            if (activeNote.type == "html") {
                                Spacer(modifier = GlanceModifier.width(4.dp))
                                Box(
                                    modifier = GlanceModifier
                                        .background(ColorProvider(Color(0x35F2B90C)))
                                        .cornerRadius(6.dp)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "HTML",
                                        style = TextStyle(
                                            color = ColorProvider(amberAccent),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(5.dp))

                        // If note has interactive todo items, show interactive checkboxes!
                        if (data.todoItems.isNotEmpty()) {
                            Column(modifier = GlanceModifier.fillMaxWidth()) {
                                val displayTodos = data.todoItems.take(if (widgetHeight >= 240.dp) 5 else 3)
                                for (todo in displayTodos) {
                                    InteractiveTodoRow(
                                        noteId = activeNote.id,
                                        todo = todo,
                                        textPrimary = textPrimary,
                                        textTertiary = textTertiary,
                                        amberAccent = amberAccent
                                    )
                                    Spacer(modifier = GlanceModifier.height(3.dp))
                                }

                                if (data.todoItems.size > displayTodos.size) {
                                    Text(
                                        text = "+${data.todoItems.size - displayTodos.size} more tasks…",
                                        style = TextStyle(
                                            color = ColorProvider(textTertiary),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        } else {
                            // Standard note snippet / body preview
                            Text(
                                text = if (activeNote.snippet.isNotBlank()) activeNote.snippet else "No additional note content. Tap to edit.",
                                style = TextStyle(
                                    color = ColorProvider(textSecondary),
                                    fontSize = 12.sp
                                ),
                                maxLines = if (widgetHeight >= 240.dp) 5 else 3
                            )
                        }
                    }
                }
            }

            // Bottom Quick Action Bar when widget height is spacious (height >= 210.dp)
            if (widgetHeight >= 210.dp) {
                Spacer(modifier = GlanceModifier.height(6.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Search
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(ColorProvider(Color(0x1AFFFFFF)))
                            .cornerRadius(12.dp)
                            .clickable(actionStartActivity(searchIntent))
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔍 Search",
                            style = TextStyle(
                                color = ColorProvider(textSecondary),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // Select Note from App
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(ColorProvider(Color(0x1AFFFFFF)))
                            .cornerRadius(12.dp)
                            .clickable(actionStartActivity(widgetConfigIntent))
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📌 Set Note",
                            style = TextStyle(
                                color = ColorProvider(textSecondary),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // Open Note / Open App
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(ColorProvider(Color(0x28FFFFFF)))
                            .cornerRadius(12.dp)
                            .clickable(
                                if (activeNote != null) {
                                    actionStartActivity(
                                        Intent(openAppIntent).apply {
                                            putExtra(MainActivity.EXTRA_OPEN_NOTE_ID, activeNote.id)
                                        }
                                    )
                                } else {
                                    actionStartActivity(openAppIntent)
                                }
                            )
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Open Note ↗",
                            style = TextStyle(
                                color = ColorProvider(amberAccent),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ModeTabItem(
        label: String,
        mode: String,
        currentMode: String,
        activeBg: Color,
        activeText: Color,
        inactiveBg: Color,
        inactiveText: Color
    ) {
        val isSelected = currentMode == mode
        Box(
            modifier = GlanceModifier
                .background(ColorProvider(if (isSelected) activeBg else inactiveBg))
                .cornerRadius(10.dp)
                .padding(horizontal = 7.dp, vertical = 3.dp)
                .clickable(actionRunCallback<SwitchModeAction>(actionParametersOf(SwitchModeAction.ModeKey to mode))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = ColorProvider(if (isSelected) activeText else inactiveText),
                    fontSize = 10.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            )
        }
    }

    @Composable
    private fun InteractiveTodoRow(
        noteId: String,
        todo: WidgetTodoItem,
        textPrimary: Color,
        textTertiary: Color,
        amberAccent: Color
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(
                    actionRunCallback<ToggleTodoAction>(
                        actionParametersOf(
                            ToggleTodoAction.NoteIdKey to noteId,
                            ToggleTodoAction.LineIndexKey to todo.lineIndex
                        )
                    )
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Interactive Checkbox Box
            Box(
                modifier = GlanceModifier
                    .size(18.dp)
                    .background(ColorProvider(if (todo.isDone) Color(0x3510B981) else Color(0x28FFFFFF)))
                    .cornerRadius(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (todo.isDone) "✓" else "",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF34D399)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(7.dp))

            Text(
                text = todo.text,
                style = TextStyle(
                    color = ColorProvider(if (todo.isDone) textTertiary else textPrimary),
                    fontSize = 11.5.sp,
                    textDecoration = if (todo.isDone) TextDecoration.LineThrough else TextDecoration.None
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
        }
    }
}

// -------------------------------------------------------------------------
// Action Callbacks for Interactive Home Screen Controls
// -------------------------------------------------------------------------

class ToggleTodoAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val noteId = parameters[NoteIdKey] ?: return
        val lineIndex = parameters[LineIndexKey] ?: return
        WidgetManager.toggleTodoItem(context, noteId, lineIndex)
        GlassNotesWidget().update(context, glanceId)
    }

    companion object {
        val NoteIdKey = ActionParameters.Key<String>("note_id")
        val LineIndexKey = ActionParameters.Key<Int>("line_index")
    }
}

class CycleNoteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val direction = parameters[DirectionKey] ?: 1
        WidgetManager.cycleNote(context, direction)
        GlassNotesWidget().update(context, glanceId)
    }

    companion object {
        val DirectionKey = ActionParameters.Key<Int>("direction")
    }
}

class SwitchModeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val mode = parameters[ModeKey] ?: WidgetManager.MODE_SELECTED
        WidgetManager.setWidgetMode(context, mode)
        GlassNotesWidget().update(context, glanceId)
    }

    companion object {
        val ModeKey = ActionParameters.Key<String>("mode")
    }
}

class TogglePinAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val noteId = parameters[NoteIdKey] ?: return
        WidgetManager.togglePinNote(context, noteId)
        GlassNotesWidget().update(context, glanceId)
    }

    companion object {
        val NoteIdKey = ActionParameters.Key<String>("note_id")
    }
}
