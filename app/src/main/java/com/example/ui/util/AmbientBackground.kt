package com.example.ui.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun AmbientBackground(
    isDark: Boolean,
    isReduced: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isReduced) return
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        if (isDark) {
            // Dark mode soft subtle blue ambient top-center glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x183872E0), Color.Transparent),
                    center = Offset(w * 0.2f, -h * 0.05f),
                    radius = w * 0.9f
                ),
                center = Offset(w * 0.2f, -h * 0.05f),
                radius = w * 0.9f
            )

            // Dark mode subtle lavender glow on right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x129652DE), Color.Transparent),
                    center = Offset(w * 0.9f, h * 0.35f),
                    radius = w * 0.8f
                ),
                center = Offset(w * 0.9f, h * 0.35f),
                radius = w * 0.8f
            )
        } else {
            // Light mode soft white/subtle grey gradient base
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFBFBF9), Color(0xFFF2F3EF), Color(0xFFEBECE7)),
                    startY = 0f,
                    endY = h
                )
            )

            // Soft delicate blue illumination top left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x0C3872E0), Color.Transparent),
                    center = Offset(w * 0.15f, h * 0.05f),
                    radius = w * 0.7f
                ),
                center = Offset(w * 0.15f, h * 0.05f),
                radius = w * 0.7f
            )

            // Soft delicate warm illumination right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x08FF9500), Color.Transparent),
                    center = Offset(w * 0.95f, h * 0.28f),
                    radius = w * 0.85f
                ),
                center = Offset(w * 0.95f, h * 0.28f),
                radius = w * 0.85f
            )
        }
    }
}

