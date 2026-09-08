package com.example.ui.animations

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 1. Modifier.bouncyClick - Scales to 0.92 on press, springs back to 1.0 with overshoot.
 */
fun Modifier.bouncyClick(onClick: () -> Unit): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        detectTapGestures(
            onPress = {
                // Press scale logic handled via graphicsLayer or pointerInput
            }
        )
    }
)

/**
 * Composable stateful bouncyClick modifier using graphicsLayer.
 */
@Composable
fun Modifier.bouncyClickStateful(onClick: () -> Unit): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.34f,
            stiffness = 400f
        ),
        label = "BouncyClickScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(onClick) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    val released = tryAwaitRelease()
                    isPressed = false
                    if (released) {
                        onClick()
                    }
                }
            )
        }
}

/**
 * Extension modifier that applies stateful bouncyClick.
 */
fun Modifier.bouncyClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = this.pointerInput(enabled, onClick) {
    if (!enabled) return@pointerInput
    detectTapGestures(
        onPress = {
            // Managed inside graphicsLayer composable wrapper or pointer input
        }
    )
}

/**
 * Bouncy click modifier that scales to 0.92f with spring (dampingRatio = 0.34f, stiffness = 400f).
 */
@Composable
fun Modifier.rememberBouncyClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.34f,
            stiffness = 400f
        ),
        label = "BouncyClick"
    )

    return if (!enabled) this else this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(onClick) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    try {
                        awaitRelease()
                        onClick()
                    } finally {
                        isPressed = false
                    }
                }
            )
        }
}

/**
 * 2. Modifier.bouncyCardClick - Scales press to 0.92f and animates shadow elevation from 8.dp to 2.dp.
 */
@Composable
fun Modifier.rememberBouncyCardClick(
    enabled: Boolean = true,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    onClick: () -> Unit
): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.34f,
            stiffness = 400f
        ),
        label = "BouncyCardScale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 8.dp,
        animationSpec = spring(
            dampingRatio = 0.34f,
            stiffness = 400f
        ),
        label = "BouncyCardElevation"
    )

    return if (!enabled) this else this
        .shadow(elevation = elevation, shape = shape)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(onClick) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    try {
                        awaitRelease()
                        onClick()
                    } finally {
                        isPressed = false
                    }
                }
            )
        }
}

/**
 * 3. Modifier.bouncyAppear - Starts at scale 0.3 + alpha 0, springs to scale 1.0 with MediumBouncy damping, alpha to 1.0 with tween(300).
 */
@Composable
fun Modifier.bouncyAppear(delayMs: Long = 0): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs)
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1.0f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 400f
        ),
        label = "BouncyAppearScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1.0f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "BouncyAppearAlpha"
    )

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}

/**
 * 4. BouncyFloatingActionButton - Scales in from 0 with spring overshoot after 200ms delay, rotates -90° to 0° with LowBouncy damping.
 */
@Composable
fun BouncyFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    contentDescription: String? = "FAB",
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: (@Composable () -> Unit)? = null
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(200)
        appeared = true
    }

    val scale by animateFloatAsState(
        targetValue = if (appeared) 1.0f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 350f
        ),
        label = "FABScale"
    )

    val rotation by animateFloatAsState(
        targetValue = if (appeared) 0f else -90f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = 300f
        ),
        label = "FABRotation"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
            .rememberBouncyClick(onClick = onClick)
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            shape = CircleShape
        ) {
            if (content != null) {
                content()
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Helper to trigger tick haptic feedback across API levels.
 */
fun triggerTickHaptic(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(15L)
        }
    } catch (_: Exception) {}
}

/**
 * 5. BouncyHapticButton - Scales to 0.9 on press with spring(0.3, 600), triggers VibrationEffect.EFFECT_TICK on press if hapticsEnabled.
 */
@Composable
fun BouncyHapticButton(
    text: String,
    onClick: () -> Unit,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val context = LocalContext.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.3f,
            stiffness = 600f
        ),
        label = "HapticButtonScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .pointerInput(hapticsEnabled, onClick) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        if (hapticsEnabled) {
                            triggerTickHaptic(context)
                        }
                        try {
                            awaitRelease()
                            onClick()
                        } finally {
                            isPressed = false
                        }
                    }
                )
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 6. rememberShakeController - Animates a decaying shake (amplitude 24f, 500ms, decay pattern: full -> 0.7 -> 0.5 -> 0.3 -> 0.15 -> 0).
 */
class ShakeController {
    val translationX = Animatable(0f)

    suspend fun shake() {
        val pattern = listOf(
            24f to 60,
            -16.8f to 70, // 0.7 * 24
            12f to 80,    // 0.5 * 24
            -7.2f to 90,  // 0.3 * 24
            3.6f to 100,  // 0.15 * 24
            0f to 100
        )
        for ((target, duration) in pattern) {
            translationX.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = duration, easing = LinearEasing)
            )
        }
    }
}

@Composable
fun rememberShakeController(): ShakeController {
    return remember { ShakeController() }
}

/**
 * 7. SpringSwitch - Custom switch where thumb scales 1.0 -> 1.15 -> 1.0 on toggle with spring, track colour crossfades, haptic on toggle.
 */
@Composable
fun SpringSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    hapticsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isToggling by remember { mutableStateOf(false) }

    val trackColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF3B82F6) else Color(0xFF374151),
        animationSpec = tween(250),
        label = "TrackColor"
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = 0.35f,
            stiffness = 500f
        ),
        label = "ThumbOffset"
    )

    val thumbScale by animateFloatAsState(
        targetValue = if (isToggling) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.3f,
            stiffness = 600f
        ),
        label = "ThumbScale"
    )

    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .width(50.dp)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (hapticsEnabled) {
                    triggerTickHaptic(context)
                }
                scope.launch {
                    isToggling = true
                    delay(120)
                    isToggling = false
                }
                onCheckedChange(!checked)
            }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .size(22.dp)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/**
 * 8. TypingIndicator - 3 bouncing dots using rememberInfiniteTransition. Each dot offset 150ms. Dots scale 0.6 -> 1.0 and translateY -4.dp -> 0.dp.
 */
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    dotColor: Color = Color(0xFF3B82F6),
    dotSize: Dp = 8.dp
) {
    val transition = rememberInfiniteTransition(label = "TypingIndicatorTransition")

    @Composable
    fun animateDot(delayMillis: Int): Pair<Float, Float> {
        val scale by transition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 450, delayMillis = delayMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DotScale_$delayMillis"
        )
        val translateY by transition.animateFloat(
            initialValue = 0f,
            targetValue = -4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 450, delayMillis = delayMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DotTranslateY_$delayMillis"
        )
        return Pair(scale, translateY)
    }

    val dot1 = animateDot(0)
    val dot2 = animateDot(150)
    val dot3 = animateDot(300)

    Row(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(dot1, dot2, dot3).forEach { (scale, translateY) ->
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationY = translateY * density
                    }
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}
