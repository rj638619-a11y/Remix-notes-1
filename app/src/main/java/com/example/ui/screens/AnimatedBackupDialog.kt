package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.animations.BouncyEasing
import com.example.ui.animations.BouncyHapticButton
import com.example.ui.effects.BlurredScrim
import com.example.ui.effects.glassCard
import com.example.ui.viewmodel.NotesViewModel
import kotlinx.coroutines.delay

enum class BackupState {
    IDLE, PROCESSING, COMPLETE
}

@Composable
fun AnimatedBackupDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()

    var backupState by remember { mutableStateOf(BackupState.IDLE) }
    var rawProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(backupState) {
        if (backupState == BackupState.PROCESSING) {
            viewModel.exportBackupJson { }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 300),
        label = "BackupProgress"
    )

    val checkmarkScale by animateFloatAsState(
        targetValue = if (backupState == BackupState.COMPLETE) 1.0f else 0f,
        animationSpec = tween(durationMillis = 400, easing = BouncyEasing.StrongBounce),
        label = "CheckmarkScale"
    )

    Box(modifier = modifier.fillMaxSize()) {
        BlurredScrim(
            visible = visible,
            onDismiss = onDismiss,
            reduceTransparency = settings.reduceTransparency
        )

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            enter = scaleIn(
                initialScale = 0.85f,
                animationSpec = spring(dampingRatio = 0.3f, stiffness = 500f)
            ) + fadeIn(tween(200)),
            exit = scaleOut(
                targetScale = 0.85f,
                animationSpec = tween(200)
            ) + fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(
                        blurRadius = 32.dp,
                        cornerRadius = 28.dp,
                        tint = Color(0xFF0F172A).copy(alpha = 0.9f),
                        borderColor = Color.White.copy(alpha = 0.2f),
                        reduceTransparency = settings.reduceTransparency
                    )
                    .padding(28.dp)
            ) {
                Crossfade(targetState = backupState, label = "BackupDialogCrossfade") { state ->
                    when (state) {
                        BackupState.IDLE -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Backup & Restore",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Export your encrypted notes and vault database or import a saved backup.",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    BouncyHapticButton(
                                        text = "Export",
                                        onClick = {
                                            backupState = BackupState.PROCESSING
                                            rawProgress = 0f
                                        },
                                        hapticsEnabled = settings.hapticsEnabled,
                                        containerColor = Color(0xFF3B82F6),
                                        contentColor = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    BouncyHapticButton(
                                        text = "Import",
                                        onClick = {
                                            backupState = BackupState.PROCESSING
                                            rawProgress = 0f
                                        },
                                        hapticsEnabled = settings.hapticsEnabled,
                                        containerColor = Color.White.copy(alpha = 0.15f),
                                        contentColor = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        BackupState.PROCESSING -> {
                            LaunchedEffect(Unit) {
                                for (i in 1..10) {
                                    delay(150)
                                    rawProgress = i / 10f
                                }
                                backupState = BackupState.COMPLETE
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Syncing Backup...",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                // Custom Progress Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.1f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animatedProgress)
                                            .height(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF3B82F6))
                                    )
                                }

                                Text(
                                    text = "${(animatedProgress * 100).toInt()}% Complete",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        BackupState.COMPLETE -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .graphicsLayer {
                                            scaleX = checkmarkScale
                                            scaleY = checkmarkScale
                                        }
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Success",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Text(
                                    text = "Backup Successful!",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                BouncyHapticButton(
                                    text = "Done",
                                    onClick = {
                                        backupState = BackupState.IDLE
                                        onDismiss()
                                    },
                                    hapticsEnabled = settings.hapticsEnabled,
                                    containerColor = Color(0xFF10B981),
                                    contentColor = Color.White,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
