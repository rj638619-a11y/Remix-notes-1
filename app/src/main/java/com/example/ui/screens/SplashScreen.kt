package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NotesAppLogoIcon
import com.example.ui.theme.GlassTheme
import com.example.ui.util.AmbientBackground
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    var progress by remember { mutableFloatStateOf(0f) }
    var visible by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
        label = "splash_progress"
    )

    LaunchedEffect(Unit) {
        visible = true
        progress = 1f
        delay(2200L)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished
            ),
        contentAlignment = Alignment.Center
    ) {
        AmbientBackground(isDark = colors.isDark)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(initialScale = 0.85f, animationSpec = tween(700, easing = FastOutSlowInEasing)) +
                        fadeIn(animationSpec = tween(700))
            ) {
                NotesAppLogoIcon(size = 132.dp, animated = true)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Spaced Title "N O T E S"
            Text(
                text = "N  O  T  E  S",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = colors.text,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Your Learning Companion",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Read • Organize • Learn • Grow",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textTertiary,
                letterSpacing = 0.3.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Sleek Progress Bar
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.field)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    colors.pastelBlue,
                                    colors.pastelLavender,
                                    colors.pastelMint
                                )
                            )
                        )
                )
            }
        }
    }
}
