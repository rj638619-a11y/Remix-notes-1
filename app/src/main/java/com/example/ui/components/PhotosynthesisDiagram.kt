package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassTheme

@Composable
fun PhotosynthesisDiagram(
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "diagram_flow")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .shadow(6.dp, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFF2FBF4),
                        Color(0xFFE5F7EB)
                    )
                )
            )
            .border(1.dp, Color(0x662EB85C), shape)
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(236.dp)) {
            val w = size.width
            val h = size.height

            // 1. Sun on Top-Left
            val sunCenter = Offset(w * 0.18f, h * 0.22f)
            val sunRadius = w * 0.08f * pulse

            // Sun Rays
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFD54F).copy(alpha = 0.4f), Color.Transparent),
                    center = sunCenter,
                    radius = sunRadius * 1.8f
                ),
                center = sunCenter,
                radius = sunRadius * 1.8f
            )

            // Sun Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFE082), Color(0xFFFFB300)),
                    center = sunCenter,
                    radius = sunRadius
                ),
                center = sunCenter,
                radius = sunRadius
            )

            // Sunlight Beam to Leaf
            val leafCenter = Offset(w * 0.52f, h * 0.54f)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFC107).copy(alpha = 0.8f), Color(0x00FFC107)),
                    start = sunCenter,
                    end = leafCenter
                ),
                start = Offset(sunCenter.x + sunRadius * 0.6f, sunCenter.y + sunRadius * 0.6f),
                end = Offset(leafCenter.x - w * 0.1f, leafCenter.y - h * 0.1f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 2. Center Green Leaf
            val leafW = w * 0.36f
            val leafH = h * 0.42f
            val leafPath = Path().apply {
                moveTo(leafCenter.x - leafW * 0.5f, leafCenter.y)
                cubicTo(
                    leafCenter.x - leafW * 0.2f, leafCenter.y - leafH * 0.5f,
                    leafCenter.x + leafW * 0.3f, leafCenter.y - leafH * 0.45f,
                    leafCenter.x + leafW * 0.5f, leafCenter.y - leafH * 0.1f
                )
                cubicTo(
                    leafCenter.x + leafW * 0.3f, leafCenter.y + leafH * 0.5f,
                    leafCenter.x - leafW * 0.2f, leafCenter.y + leafH * 0.45f,
                    leafCenter.x - leafW * 0.5f, leafCenter.y
                )
                close()
            }

            // Leaf Shadow
            drawPath(
                path = leafPath,
                color = Color(0x18000000)
            )

            // Leaf Fill Gradient
            drawPath(
                path = leafPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF4CAF50), Color(0xFF2E7D32), Color(0xFF1B5E20)),
                    start = Offset(leafCenter.x - leafW * 0.5f, leafCenter.y - leafH * 0.5f),
                    end = Offset(leafCenter.x + leafW * 0.5f, leafCenter.y + leafH * 0.5f)
                )
            )

            // Leaf Main Vein
            val veinPath = Path().apply {
                moveTo(leafCenter.x - leafW * 0.46f, leafCenter.y)
                quadraticTo(
                    leafCenter.x, leafCenter.y - leafH * 0.05f,
                    leafCenter.x + leafW * 0.48f, leafCenter.y - leafH * 0.08f
                )
            }
            drawPath(
                path = veinPath,
                color = Color(0x66A5D6A7),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Inputs & Outputs Labels & Arrows
            // CO2 Input (Left arrow in)
            val co2Pos = Offset(w * 0.16f, h * 0.65f)
            drawRoundRect(
                color = Color(0xFF3872E0),
                topLeft = Offset(co2Pos.x - 28.dp.toPx(), co2Pos.y - 14.dp.toPx()),
                size = Size(56.dp.toPx(), 28.dp.toPx()),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
            )
            drawLine(
                color = Color(0xFF3872E0),
                start = Offset(co2Pos.x + 30.dp.toPx(), co2Pos.y),
                end = Offset(leafCenter.x - leafW * 0.35f, leafCenter.y + leafH * 0.1f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // H2O Water Input (Bottom up)
            val h2oPos = Offset(w * 0.5f, h * 0.9f)
            drawRoundRect(
                color = Color(0xFF0288D1),
                topLeft = Offset(h2oPos.x - 26.dp.toPx(), h2oPos.y - 12.dp.toPx()),
                size = Size(52.dp.toPx(), 24.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )
            drawLine(
                color = Color(0xFF0288D1),
                start = Offset(h2oPos.x, h2oPos.y - 14.dp.toPx()),
                end = Offset(leafCenter.x, leafCenter.y + leafH * 0.35f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // O2 Output (Top Right arrow out)
            val o2Pos = Offset(w * 0.84f, h * 0.28f)
            drawRoundRect(
                color = Color(0xFF2EB85C),
                topLeft = Offset(o2Pos.x - 24.dp.toPx(), o2Pos.y - 14.dp.toPx()),
                size = Size(48.dp.toPx(), 28.dp.toPx()),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
            )
            drawLine(
                color = Color(0xFF2EB85C),
                start = Offset(leafCenter.x + leafW * 0.3f, leafCenter.y - leafH * 0.2f),
                end = Offset(o2Pos.x - 26.dp.toPx(), o2Pos.y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Glucose Output (Bottom Right)
            val sugarPos = Offset(w * 0.82f, h * 0.72f)
            drawRoundRect(
                color = Color(0xFFFF8B19),
                topLeft = Offset(sugarPos.x - 38.dp.toPx(), sugarPos.y - 14.dp.toPx()),
                size = Size(76.dp.toPx(), 28.dp.toPx()),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
            )
            drawLine(
                color = Color(0xFFFF8B19),
                start = Offset(leafCenter.x + leafW * 0.35f, leafCenter.y + leafH * 0.1f),
                end = Offset(sugarPos.x - 40.dp.toPx(), sugarPos.y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Draw text labels with native canvas for crisp typography
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 12.dp.toPx()
                color = android.graphics.Color.WHITE
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            drawContext.canvas.nativeCanvas.apply {
                drawText("CO₂", co2Pos.x, co2Pos.y + 4.dp.toPx(), paint)
                drawText("H₂O", h2oPos.x, h2oPos.y + 4.dp.toPx(), paint)
                drawText("O₂", o2Pos.x, o2Pos.y + 4.dp.toPx(), paint)
                drawText("Glucose", sugarPos.x, sugarPos.y + 4.dp.toPx(), paint)

                val sunPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 10.dp.toPx()
                    color = android.graphics.Color.DKGRAY
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                drawText("Sunlight", sunCenter.x, sunCenter.y + sunRadius + 14.dp.toPx(), sunPaint)
            }
        }
    }
}
