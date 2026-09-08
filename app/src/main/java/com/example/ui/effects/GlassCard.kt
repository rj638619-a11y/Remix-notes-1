package com.example.ui.effects

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 1. Modifier.glassCard - Applies clip, hardware blur (API 31+), translucent tint & subtle glass border.
 */
fun Modifier.glassCard(
    blurRadius: Dp = 24.dp,
    cornerRadius: Dp = 20.dp,
    tint: Color = Color.White.copy(alpha = 0.08f),
    borderColor: Color = Color.White.copy(alpha = 0.12f),
    reduceTransparency: Boolean = false
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .graphicsLayer {
        if (!reduceTransparency && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0.dp) {
            val blurPx = blurRadius.toPx()
            renderEffect = RenderEffect.createBlurEffect(
                blurPx,
                blurPx,
                Shader.TileMode.CLAMP
            ).asComposeRenderEffect()
        }
    }
    .background(
        if (reduceTransparency) tint.copy(alpha = 0.95f) else tint
    )
    .border(
        width = 1.dp,
        color = borderColor,
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * 2. BlurredScrim - Full-screen overlay that animates blur radius and alpha with dark overlay. Clickable to dismiss.
 */
@Composable
fun BlurredScrim(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    maxBlur: Dp = 32.dp,
    reduceTransparency: Boolean = false
) {
    val animatedBlur by animateDpAsState(
        targetValue = if (visible) maxBlur else 0.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "ScrimBlur"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1.0f else 0f,
        animationSpec = tween(300),
        label = "ScrimAlpha"
    )

    if (animatedAlpha > 0.01f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = animatedAlpha
                    if (!reduceTransparency && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && animatedBlur > 0.dp) {
                        val blurPx = animatedBlur.toPx()
                        renderEffect = RenderEffect.createBlurEffect(
                            blurPx,
                            blurPx,
                            Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                }
                .background(Color.Black.copy(alpha = 0.4f * animatedAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }
}

/**
 * 3. GlassBottomSheet - Bottom sheet with glassCard background, BlurredScrim behind it, slides up with spring(0.3, 400), scaleIn from 0.95.
 */
@Composable
fun GlassBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    reduceTransparency: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        BlurredScrim(
            visible = visible,
            onDismiss = onDismiss,
            reduceTransparency = reduceTransparency
        )

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.3f, stiffness = 400f),
                initialOffsetY = { fullHeight -> fullHeight }
            ) + scaleIn(
                initialScale = 0.95f,
                animationSpec = spring(dampingRatio = 0.3f, stiffness = 400f)
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                animationSpec = tween(250),
                targetOffsetY = { fullHeight -> fullHeight }
            ) + scaleOut(
                targetScale = 0.95f,
                animationSpec = tween(250)
            ) + fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .glassCard(
                        blurRadius = 32.dp,
                        cornerRadius = 28.dp,
                        tint = Color(0xFF1E293B).copy(alpha = 0.85f),
                        borderColor = Color.White.copy(alpha = 0.2f),
                        reduceTransparency = reduceTransparency
                    )
                    .padding(24.dp)
            ) {
                content()
            }
        }
    }
}
