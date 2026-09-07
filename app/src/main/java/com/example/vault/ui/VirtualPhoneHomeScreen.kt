package com.example.vault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.VibrationHelper
import com.example.vault.data.VaultRepository
import com.example.vault.data.VaultSecurityManager
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VirtualAppDef(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val iconBgGradient: List<Color>,
    val badgeCount: Int = 0
)

@Composable
fun VirtualPhoneHomeScreen(
    repository: VaultRepository,
    securityManager: VaultSecurityManager,
    onLaunchApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allItems by repository.itemsFlow.collectAsStateWithLifecycle()
    val stats = remember(allItems) { repository.getStats() }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    var currentTime by remember { mutableStateOf(timeFormat.format(Date())) }
    var currentDate by remember { mutableStateOf(dateFormat.format(Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = timeFormat.format(Date())
            currentDate = dateFormat.format(Date())
            delay(1000)
        }
    }

    val wpId = securityManager.getWallpaper()
    val wp = VaultSecurityManager.WALLPAPERS.firstOrNull { it.id == wpId }
        ?: VaultSecurityManager.WALLPAPERS.first()

    val apps = listOf(
        VirtualAppDef(
            id = "gallery",
            name = "Gallery",
            icon = Icons.Default.PhotoLibrary,
            iconBgGradient = listOf(Color(0xFF6366F1), Color(0xFF4F46E5)),
            badgeCount = stats.photoCount
        ),
        VirtualAppDef(
            id = "video",
            name = "Videos",
            icon = Icons.Default.Movie,
            iconBgGradient = listOf(Color(0xFF0284C7), Color(0xFF0369A1)),
            badgeCount = stats.videoCount
        ),
        VirtualAppDef(
            id = "audio",
            name = "Audio Player",
            icon = Icons.Default.Audiotrack,
            iconBgGradient = listOf(Color(0xFFA855F7), Color(0xFF7E22CE)),
            badgeCount = stats.audioCount
        ),
        VirtualAppDef(
            id = "files",
            name = "Files",
            icon = Icons.Default.Folder,
            iconBgGradient = listOf(Color(0xFF10B981), Color(0xFF059669)),
            badgeCount = stats.docCount
        ),
        VirtualAppDef(
            id = "browser",
            name = "Private Web",
            icon = Icons.Default.Public,
            iconBgGradient = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
        ),
        VirtualAppDef(
            id = "clock",
            name = "Clock",
            icon = Icons.Default.Schedule,
            iconBgGradient = listOf(Color(0xFFF97316), Color(0xFFC2410C))
        ),
        VirtualAppDef(
            id = "notes",
            name = "Secret Notes",
            icon = Icons.Default.NoteAlt,
            iconBgGradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
            badgeCount = stats.noteCount
        ),
        VirtualAppDef(
            id = "settings",
            name = "Vault Settings",
            icon = Icons.Default.Settings,
            iconBgGradient = listOf(Color(0xFF64748B), Color(0xFF475569))
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(wp.colorStart),
                        Color(wp.colorMid),
                        Color(0xFF050508)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Area: Clock Widget & Google Search Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Clock Widget Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.45f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(22.dp))
                        .clickable {
                            VibrationHelper.tick(context)
                            onLaunchApp("clock")
                        }
                        .padding(vertical = 16.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentTime,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraLight,
                            color = Color.White,
                            letterSpacing = (-1).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentDate,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(wp.accentColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Web Search Bar Widget
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                        .clickable {
                            VibrationHelper.tick(context)
                            onLaunchApp("browser")
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFFA5B4FC),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Search private web...",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Encrypted",
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Middle Grid of Apps
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                items(apps, key = { it.id }) { app ->
                    AppGridIconItem(
                        app = app,
                        onClick = {
                            VibrationHelper.tick(context)
                            onLaunchApp(app.id)
                        }
                    )
                }
            }

            // Bottom Dock
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dockApps = listOf(
                        apps.first { it.id == "browser" },
                        apps.first { it.id == "audio" },
                        apps.first { it.id == "gallery" },
                        apps.first { it.id == "settings" }
                    )
                    dockApps.forEach { app ->
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(app.iconBgGradient))
                                .clickable {
                                    VibrationHelper.tick(context)
                                    onLaunchApp(app.id)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = app.icon,
                                contentDescription = app.name,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppGridIconItem(
    app: VirtualAppDef,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(app.iconBgGradient))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = app.icon,
                    contentDescription = app.name,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Notification Badge if has items
            if (app.badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (app.badgeCount > 99) "99+" else app.badgeCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = app.name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
