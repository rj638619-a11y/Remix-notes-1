package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun NotesAppLogoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")
    val shimmerOffset by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = -1.2f,
            targetValue = 1.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val floatOffset by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = -4f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "float"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val squircleShape = RoundedCornerShape(size * 0.28f)

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = size * 0.16f,
                shape = squircleShape,
                ambientColor = Color(0x18000000),
                spotColor = Color(0x22000000)
            )
            .clip(squircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF3F4F1),
                        Color(0xFFE8EAE5)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xE6FFFFFF),
                        Color(0x66FFFFFF),
                        Color(0x33B0B5BD)
                    )
                ),
                shape = squircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.72f)) {
            val w = this.size.width
            val h = this.size.height
            val padX = w * 0.12f
            val padY = h * 0.08f + (floatOffset * (h / 120f))

            // Notebook body path with folded top-right corner
            val foldSize = w * 0.22f
            val cornerR = w * 0.14f

            val notePath = Path().apply {
                moveTo(padX + cornerR, padY)
                // Top edge up to fold
                lineTo(w - padX - foldSize, padY)
                // Diagonal to fold right edge
                lineTo(w - padX, padY + foldSize)
                // Right edge down
                lineTo(w - padX, h - padY - cornerR)
                // Bottom right corner
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        w - padX - 2 * cornerR, h - padY - 2 * cornerR,
                        w - padX, h - padY
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                // Bottom edge
                lineTo(padX + cornerR, h - padY)
                // Bottom left corner
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        padX, h - padY - 2 * cornerR,
                        padX + 2 * cornerR, h - padY
                    ),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                // Left edge up
                lineTo(padX, padY + cornerR)
                // Top left corner
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        padX, padY,
                        padX + 2 * cornerR, padY + 2 * cornerR
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                close()
            }

            // Notebook drop shadow
            drawPath(
                path = notePath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x10000000), Color(0x18000000)),
                    startY = padY,
                    endY = h - padY
                )
            )

            // Frosted glass notebook fill
            drawPath(
                path = notePath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xF0FFFFFF),
                        Color(0xD8F5F6F3),
                        Color(0xB8E6E9E3)
                    ),
                    start = Offset(padX, padY),
                    end = Offset(w - padX, h - padY)
                )
            )

            // Border of the notebook
            drawPath(
                path = notePath,
                color = Color(0x88FFFFFF),
                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
            )

            // Folded corner triangle flap
            val foldPath = Path().apply {
                moveTo(w - padX - foldSize, padY)
                lineTo(w - padX - foldSize, padY + foldSize - (cornerR * 0.3f))
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        w - padX - foldSize,
                        padY + foldSize - (cornerR * 0.6f),
                        w - padX - foldSize + (cornerR * 0.6f),
                        padY + foldSize
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false
                )
                lineTo(w - padX, padY + foldSize)
                close()
            }

            // Fold flap shadow & fill
            drawPath(
                path = foldPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0x22000000), Color(0x0A000000)),
                    start = Offset(w - padX - foldSize, padY + foldSize),
                    end = Offset(w - padX, padY)
                )
            )
            drawPath(
                path = foldPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xE6E8EAE4), Color(0xFFFAFAFA)),
                    start = Offset(w - padX - foldSize, padY),
                    end = Offset(w - padX, padY + foldSize)
                )
            )
            drawPath(
                path = foldPath,
                color = Color(0x88FFFFFF),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Spiral Binder Rings on Left
            val ringCount = 5
            val ringSpacing = (h - 2 * padY - 2 * cornerR) / (ringCount + 1)
            for (i in 1..ringCount) {
                val cy = padY + cornerR + (i * ringSpacing)
                val rx = padX + (w * 0.035f)
                val ringW = w * 0.09f
                val ringH = h * 0.045f

                // Pill hole
                drawRoundRect(
                    color = Color(0x22000000),
                    topLeft = Offset(rx - ringW * 0.5f, cy - ringH * 0.5f),
                    size = Size(ringW, ringH),
                    cornerRadius = CornerRadius(ringH * 0.5f, ringH * 0.5f)
                )

                // Chrome / Silver spiral ring
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF8E939B),
                            Color(0xFFFFFFFF),
                            Color(0xFF636870)
                        ),
                        startX = rx - ringW * 0.6f,
                        endX = rx + ringW * 0.6f
                    ),
                    topLeft = Offset(rx - ringW * 0.45f, cy - ringH * 0.35f),
                    size = Size(ringW * 0.9f, ringH * 0.7f),
                    cornerRadius = CornerRadius(ringH * 0.35f, ringH * 0.35f)
                )
            }

            // Subtle Neural Circuit sparkles / study lines
            val lineStartX = padX + w * 0.22f
            val lineEndX = w - padX - w * 0.15f
            val line1Y = padY + h * 0.38f
            val line2Y = padY + h * 0.52f
            val line3Y = padY + h * 0.66f

            // Soft rule lines
            drawLine(
                color = Color(0x18000000),
                start = Offset(lineStartX, line1Y),
                end = Offset(lineEndX, line1Y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0x14000000),
                start = Offset(lineStartX, line2Y),
                end = Offset(lineEndX * 0.85f, line2Y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0x10000000),
                start = Offset(lineStartX, line3Y),
                end = Offset(lineEndX * 0.65f, line3Y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Sparkle / Star on top right
            val starCenter = Offset(w - padX - w * 0.18f, padY + h * 0.32f)
            val starSize = w * 0.06f
            val starPath = Path().apply {
                moveTo(starCenter.x, starCenter.y - starSize)
                quadraticTo(starCenter.x, starCenter.y, starCenter.x + starSize, starCenter.y)
                quadraticTo(starCenter.x, starCenter.y, starCenter.x, starCenter.y + starSize)
                quadraticTo(starCenter.x, starCenter.y, starCenter.x - starSize, starCenter.y)
                quadraticTo(starCenter.x, starCenter.y, starCenter.x, starCenter.y - starSize)
                close()
            }
            drawPath(
                path = starPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF3872E0), Color(0x003872E0)),
                    center = starCenter,
                    radius = starSize * 1.5f
                )
            )

            // Glass Shimmer Sweep
            if (animated) {
                val shimmerX = w * shimmerOffset
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x33FFFFFF),
                            Color(0x66FFFFFF),
                            Color(0x33FFFFFF),
                            Color.Transparent
                        ),
                        start = Offset(shimmerX - w * 0.2f, 0f),
                        end = Offset(shimmerX + w * 0.2f, h)
                    ),
                    start = Offset(shimmerX - w * 0.2f, 0f),
                    end = Offset(shimmerX + w * 0.2f, h),
                    strokeWidth = w * 0.4f
                )
            }
        }
    }
}
