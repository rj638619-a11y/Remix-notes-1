package com.example.ui.animations

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.effects.glassCard

data class VaultAppDef(
    val appName: String,
    val icon: ImageVector,
    val tint: Color,
    val content: @Composable () -> Unit
)

/**
 * Custom MotionDurationScale that forces scaleFactor to 1.0f (never skip or speed up animations).
 */
val AlwaysEnableMotionDurationScale = object : MotionDurationScale {
    override val scaleFactor: Float
        get() = 1.0f
}

val LocalMotionDurationScale = staticCompositionLocalOf<MotionDurationScale> { AlwaysEnableMotionDurationScale }

/**
 * GlassNotesAnimationTheme composable wrapper that sets LocalMotionDurationScale to 1.0f.
 */
@Composable
fun GlassNotesAnimationTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalMotionDurationScale provides AlwaysEnableMotionDurationScale
    ) {
        content()
    }
}

/**
 * VaultAppLauncher - iOS-style open/close transition for vault apps with shared element bounds and color morphing.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun VaultAppLauncher(
    apps: List<VaultAppDef>,
    modifier: Modifier = Modifier
) {
    var selectedApp by remember { mutableStateOf<VaultAppDef?>(null) }

    val backgroundColor by animateColorAsState(
        targetValue = selectedApp?.tint?.copy(alpha = 0.2f) ?: Color(0xFF020617),
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 400f),
        label = "VaultBgColorMorph"
    )

    SharedTransitionLayout(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        AnimatedContent(
            targetState = selectedApp,
            transitionSpec = {
                if (targetState != null) {
                    (scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = 0.25f, stiffness = 300f)) + fadeIn(tween(200)))
                        .togetherWith(scaleOut(targetScale = 0.9f) + fadeOut(tween(200)))
                } else {
                    (scaleIn(initialScale = 0.9f) + fadeIn(tween(200)))
                        .togetherWith(scaleOut(targetScale = 0.3f, animationSpec = spring(dampingRatio = 0.25f, stiffness = 300f)) + fadeOut(tween(200)))
                }
            },
            label = "VaultAppLauncherSwitch"
        ) { activeApp ->
            if (activeApp != null) {
                // Open App Details Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { selectedApp = null },
                            modifier = Modifier.rememberBouncyClick { selectedApp = null }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        Text(
                            text = activeApp.appName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .glassCard(
                                blurRadius = 24.dp,
                                cornerRadius = 24.dp,
                                tint = Color.Black.copy(alpha = 0.35f)
                            )
                            .padding(20.dp)
                    ) {
                        activeApp.content()
                    }
                }
            } else {
                // Home Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(apps, key = { _, item -> item.appName }) { index, item ->
                        val delayMs = (index * 40L).coerceAtMost(400L)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .bouncyAppear(delayMs = delayMs)
                                .rememberBouncyClick { selectedApp = item }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .sharedVaultAppKey(
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        appName = item.appName,
                                        animatedVisibilityScope = this@AnimatedContent
                                    )
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
                                    contentDescription = item.appName,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.appName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}
