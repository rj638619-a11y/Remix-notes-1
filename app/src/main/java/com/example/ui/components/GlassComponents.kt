package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassTheme
import com.example.ui.theme.LocalThemeTransition
import com.example.util.VibrationHelper
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    val colors = GlassTheme.colors
    val isReduced = colors.isReduced

    val bgModifier = if (isReduced) {
        Modifier.background(colors.card, shape)
    } else {
        Modifier.background(
            Brush.verticalGradient(
                listOf(
                    colors.glass,
                    colors.glass.copy(alpha = (colors.glass.alpha * 0.95f))
                )
            ),
            shape
        )
    }

    Box(
        modifier = modifier
            .shadow(if (isReduced) 0.dp else elevation, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
            .then(bgModifier)
            .border(1.dp, colors.glassBorder, shape)
    ) {
        content()
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    iconSize: Dp = 19.dp,
    tint: Color = GlassTheme.colors.text
) {
    val colors = GlassTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.field)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = size / 2),
                onClick = onClick
            )
            .padding((size - iconSize) / 2),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

@Composable
fun ThemeToggleIconButton(
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenVault: (() -> Unit)? = null,
    size: Dp = 38.dp,
    iconSize: Dp = 19.dp
) {
    val colors = GlassTheme.colors
    val context = LocalContext.current
    val view = LocalView.current
    val themeTransition = LocalThemeTransition.current

    var buttonCenter by remember { mutableStateOf(Offset.Unspecified) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isDark) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "theme_rotation"
    )

    val currentOnOpenVault by rememberUpdatedState(onOpenVault)
    val currentOnClick by rememberUpdatedState(onClick)

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                val rootPos = coords.positionInRoot()
                val sz = coords.size
                val center = Offset(rootPos.x + sz.width / 2f, rootPos.y + sz.height / 2f)
                buttonCenter = center
                themeTransition.recordOrigin(center)
            }
            .clip(CircleShape)
            .background(colors.field)
            .pointerInput(Unit) {
                val requiredHoldMs = 3000L
                coroutineScope {
                    while (true) {
                        awaitPointerEventScope {
                            awaitFirstDown(requireUnconsumed = false)
                            val downTime = System.currentTimeMillis()
                            isHolding = true
                            holdProgress = 0f
                            var triggeredVault = false

                            val timerJob = launch {
                                val startTime = System.currentTimeMillis()
                                while (isActive) {
                                    val elapsed = System.currentTimeMillis() - startTime
                                    holdProgress = (elapsed.toFloat() / requiredHoldMs).coerceIn(0f, 1f)
                                    if (elapsed >= requiredHoldMs) {
                                        triggeredVault = true
                                        VibrationHelper.vibratePattern(context, longArrayOf(0, 100, 60, 250))
                                        currentOnOpenVault?.invoke()
                                        break
                                    }
                                    delay(16L)
                                }
                            }

                            val up = waitForUpOrCancellation()
                            timerJob.cancel()
                            val duration = System.currentTimeMillis() - downTime
                            isHolding = false
                            holdProgress = 0f

                            if (up != null && !triggeredVault) {
                                if (duration < 600L) {
                                    VibrationHelper.click(context)
                                    val origin = if (buttonCenter.isSpecified) buttonCenter else Offset(800f, 150f)
                                    themeTransition.prepareTransition(origin, view, colors.bg)
                                    currentOnClick()
                                }
                            }
                        }
                    }
                }
            }
            .padding((size - iconSize) / 2),
        contentAlignment = Alignment.Center
    ) {
        if (isHolding && holdProgress > 0.04f) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val strokeW = 2.5.dp.toPx()
                drawArc(
                    color = Color(0xFFFFB74D).copy(alpha = 0.9f),
                    startAngle = -90f,
                    sweepAngle = holdProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }
        }

        Crossfade(
            targetState = isDark,
            animationSpec = tween(240, easing = FastOutSlowInEasing),
            modifier = Modifier.graphicsLayer {
                rotationZ = rotation
                if (isHolding) {
                    val scale = 1f + (holdProgress * 0.12f)
                    scaleX = scale
                    scaleY = scale
                }
            },
            label = "theme_crossfade"
        ) { dark ->
            Icon(
                imageVector = if (dark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (dark) "Switch to Light Mode" else "Switch to Dark Mode",
                tint = if (dark) Color(0xFFFFB74D) else colors.text,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun HighlightedText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default,
    highlightColor: Color = GlassTheme.colors.mark,
    textColor: Color = GlassTheme.colors.text,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val annotatedString = remember(text, query, highlightColor, textColor) {
        if (query.isBlank() || !text.contains(query, ignoreCase = true)) {
            AnnotatedString(text)
        } else {
            buildAnnotatedString {
                var currentIndex = 0
                val lowerText = text.lowercase()
                val lowerQuery = query.trim().lowercase()

                while (currentIndex < text.length) {
                    val matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
                    if (matchIndex == -1) {
                        append(text.substring(currentIndex))
                        break
                    }

                    if (matchIndex > currentIndex) {
                        append(text.substring(currentIndex, matchIndex))
                    }

                    val endMatch = matchIndex + lowerQuery.length
                    pushStyle(SpanStyle(background = highlightColor))
                    append(text.substring(matchIndex, endMatch))
                    pop()

                    currentIndex = endMatch
                }
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = style,
        color = textColor,
        maxLines = maxLines,
        overflow = overflow
    )
}
