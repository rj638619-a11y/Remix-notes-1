package com.example.ui.screens.tabs

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteSummary
import com.example.ui.theme.GlassTheme

@Composable
fun ProfileTab(
    notes: List<NoteSummary>,
    onOpenMyNotes: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors

    var studentName by remember { mutableStateOf("Student") }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(studentName) }
    var alertMessage by remember { mutableStateOf<String?>(null) }

    val totalNotes = notes.size
    val pdfCount = notes.count { it.type == "pdf" }
    val htmlCount = notes.count { it.type == "html" }
    val favCount = notes.count { it.pinned }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = statusBarTop + 14.dp, bottom = navBarBottom + 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Student Profile Header Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(26.dp), ambientColor = colors.shadow, spotColor = colors.shadow)
                        .clip(RoundedCornerShape(26.dp))
                        .background(colors.card)
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(26.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .shadow(6.dp, CircleShape, ambientColor = colors.pastelBlue.copy(alpha = 0.4f))
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(colors.pastelBlue, colors.pastelLavender)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Student Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = studentName,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(colors.field)
                                        .clickable {
                                            tempName = studentName
                                            showEditNameDialog = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Name",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Keep Learning ✨",
                                fontSize = 13.sp,
                                color = colors.pastelBlue,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Active Student Plan • All files synced offline",
                                fontSize = 11.sp,
                                color = colors.textTertiary
                            )
                        }
                    }
                }
            }

            // 4 Statistics Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileStatCard(
                        count = totalNotes,
                        label = "Notes",
                        accentColor = colors.pastelBlue,
                        accentBg = colors.pastelBlueBg,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatCard(
                        count = pdfCount,
                        label = "PDFs",
                        accentColor = colors.pastelPdfRed,
                        accentBg = colors.pastelPdfRedBg,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatCard(
                        count = htmlCount,
                        label = "HTML",
                        accentColor = colors.pastelMint,
                        accentBg = colors.pastelMintBg,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatCard(
                        count = favCount,
                        label = "Favorites",
                        accentColor = colors.pastelFavYellow,
                        accentBg = colors.pastelFavYellowBg,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Study Streak & Goal Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(22.dp), ambientColor = colors.shadow)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2))
                            )
                        )
                        .border(1.dp, Color(0x66FFA726), RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFF9800)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "5 Day Study Streak 🔥",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = "You've reviewed 14 topics this week. Great work!",
                                fontSize = 12.sp,
                                color = Color(0xFF5D4037)
                            )
                        }
                    }
                }
            }

            // Navigation Links Group
            item {
                SettingsGroupCard(title = "Manage & Activity") {
                    ProfileNavRowItem(
                        icon = Icons.Default.NoteAlt,
                        iconTint = colors.pastelBlue,
                        iconBg = colors.pastelBlueBg,
                        title = "My Notes",
                        subtitle = "All text & markdown entries",
                        onClick = onOpenMyNotes
                    )
                    HorizontalDivider(color = colors.hairline, thickness = 0.5.dp)
                    ProfileNavRowItem(
                        icon = Icons.Default.History,
                        iconTint = colors.pastelLavender,
                        iconBg = colors.pastelLavenderBg,
                        title = "Recently Viewed",
                        subtitle = "Fast access to recent documents",
                        onClick = onOpenMyNotes
                    )
                    HorizontalDivider(color = colors.hairline, thickness = 0.5.dp)
                    ProfileNavRowItem(
                        icon = Icons.Default.DownloadDone,
                        iconTint = colors.pastelMint,
                        iconBg = colors.pastelMintBg,
                        title = "Offline Downloads",
                        subtitle = "Saved textbook chapters & PDFs",
                        onClick = { alertMessage = "All 33 notes and PDFs are fully available offline on your device!" }
                    )
                    HorizontalDivider(color = colors.hairline, thickness = 0.5.dp)
                    ProfileNavRowItem(
                        icon = Icons.Default.DeleteOutline,
                        iconTint = colors.danger,
                        iconBg = Color(0xFFFDEBED),
                        title = "Trash",
                        subtitle = "Recover or permanently delete",
                        onClick = onOpenTrash
                    )
                }
            }
        }
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Student Name", fontWeight = FontWeight.Bold, color = colors.text) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
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
                        if (tempName.isNotBlank()) {
                            studentName = tempName.trim()
                            showEditNameDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.pastelBlue)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = colors.card,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (alertMessage != null) {
        AlertDialog(
            onDismissRequest = { alertMessage = null },
            title = { Text("Notes", fontWeight = FontWeight.Bold, color = colors.text) },
            text = { Text(alertMessage!!, fontSize = 14.sp, color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = { alertMessage = null }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.card,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun ProfileStatCard(
    count: Int,
    label: String,
    accentColor: Color,
    accentBg: Color,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .shadow(3.dp, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.glassBorder, shape)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$count",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = colors.textSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ProfileNavRowItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = GlassTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(14.dp)
        )
    }
}
