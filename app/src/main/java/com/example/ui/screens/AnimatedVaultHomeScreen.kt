package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.animations.bouncyAppear
import com.example.ui.animations.rememberBouncyClick
import com.example.ui.effects.glassCard
import com.example.ui.viewmodel.NotesViewModel
import kotlin.math.cos
import kotlin.math.sin

data class VaultAppItem(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val tint: Color
)

@Composable
fun AnimatedVaultHomeScreen(
    viewModel: NotesViewModel,
    onBackToNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeApp by remember { mutableStateOf<VaultAppItem?>(null) }

    val apps = remember {
        listOf(
            VaultAppItem("browser", "Browser", Icons.Default.Language, Color(0xFF3B82F6)),
            VaultAppItem("clock", "Clock", Icons.Default.Schedule, Color(0xFF10B981)),
            VaultAppItem("files", "Files", Icons.Default.Folder, Color(0xFFF59E0B)),
            VaultAppItem("gallery", "Gallery", Icons.Default.Image, Color(0xFFEC4899)),
            VaultAppItem("notes", "Notes", Icons.Default.Description, Color(0xFF8B5CF6)),
            VaultAppItem("settings", "Settings", Icons.Default.Settings, Color(0xFF64748B)),
            VaultAppItem("audio", "Audio", Icons.Default.AudioFile, Color(0xFF06B6D4)),
            VaultAppItem("video", "Video", Icons.Default.VideoLibrary, Color(0xFFEF4444))
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
    ) {
        AnimatedContent(
            targetState = activeApp,
            transitionSpec = {
                if (targetState != null) {
                    // App Open Transition (scaleIn + fadeIn)
                    (scaleIn(initialScale = 0.2f, animationSpec = spring(0.3f, 400f)) + fadeIn(tween(250))).togetherWith(
                        scaleOut(targetScale = 0.9f) + fadeOut(tween(200))
                    )
                } else {
                    // App Exit Transition
                    (scaleIn(initialScale = 0.9f) + fadeIn(tween(200))).togetherWith(
                        scaleOut(targetScale = 0.2f, animationSpec = spring(0.3f, 400f)) + fadeOut(tween(250))
                    )
                }
            },
            label = "VaultAppTransition"
        ) { app ->
            if (app != null) {
                // Opened App Container
                VaultAppContainer(
                    app = app,
                    onClose = { activeApp = null }
                )
            } else {
                // Home Screen Grid View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Clock Widget at top
                    AnimatedClockWidget()

                    Spacer(modifier = Modifier.height(32.dp))

                    // 4-Column Grid of Apps
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(apps, key = { _, item -> item.id }) { index, item ->
                            val delayMs = (index * 50L).coerceAtMost(400L)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .bouncyAppear(delayMs = delayMs)
                                    .rememberBouncyClick { activeApp = item }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .glassCard(
                                            blurRadius = 16.dp,
                                            cornerRadius = 18.dp,
                                            tint = item.tint.copy(alpha = 0.25f),
                                            borderColor = item.tint.copy(alpha = 0.4f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.name,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    // Bottom Dock
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(
                                blurRadius = 24.dp,
                                cornerRadius = 28.dp,
                                tint = Color.White.copy(alpha = 0.1f),
                                borderColor = Color.White.copy(alpha = 0.2f)
                            )
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            apps.take(4).forEach { dockApp ->
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(dockApp.tint.copy(alpha = 0.3f))
                                        .rememberBouncyClick { activeApp = dockApp },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = dockApp.icon,
                                        contentDescription = dockApp.name,
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
    }
}

@Composable
fun AnimatedClockWidget() {
    val transition = rememberInfiniteTransition(label = "ClockSecondHand")
    val secondRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SecondHandRotation"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .glassCard(
                blurRadius = 24.dp,
                cornerRadius = 60.dp,
                tint = Color.White.copy(alpha = 0.08f),
                borderColor = Color.White.copy(alpha = 0.2f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(100.dp)) {
            val centerPx = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f

            // Hour mark ticks
            repeat(12) { i ->
                val angleRad = Math.toRadians((i * 30).toDouble())
                val startX = centerPx.x + (radius - 8.dp.toPx()) * sin(angleRad).toFloat()
                val startY = centerPx.y - (radius - 8.dp.toPx()) * cos(angleRad).toFloat()
                val endX = centerPx.x + radius * sin(angleRad).toFloat()
                val endY = centerPx.y - radius * cos(angleRad).toFloat()
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Hour Hand
            val hourRad = Math.toRadians(300.0)
            drawLine(
                color = Color.White,
                start = centerPx,
                end = Offset(
                    centerPx.x + (radius * 0.45f) * sin(hourRad).toFloat(),
                    centerPx.y - (radius * 0.45f) * cos(hourRad).toFloat()
                ),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Minute Hand
            val minuteRad = Math.toRadians(120.0)
            drawLine(
                color = Color.White,
                start = centerPx,
                end = Offset(
                    centerPx.x + (radius * 0.65f) * sin(minuteRad).toFloat(),
                    centerPx.y - (radius * 0.65f) * cos(minuteRad).toFloat()
                ),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Second Hand
            val secondRad = Math.toRadians(secondRotation.toDouble())
            drawLine(
                color = Color(0xFFEF4444),
                start = centerPx,
                end = Offset(
                    centerPx.x + (radius * 0.8f) * sin(secondRad).toFloat(),
                    centerPx.y - (radius * 0.8f) * cos(secondRad).toFloat()
                ),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun VaultAppContainer(
    app: VaultAppItem,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(app.tint.copy(alpha = 0.15f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.rememberBouncyClick { onClose() }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Close App",
                    tint = Color.White
                )
            }

            Text(
                text = app.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .glassCard(
                    blurRadius = 24.dp,
                    cornerRadius = 24.dp,
                    tint = Color.Black.copy(alpha = 0.4f)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = app.icon,
                    contentDescription = null,
                    tint = app.tint,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${app.name} App",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Encrypted Vault Mini-App Content",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}
