package com.example.ui.screens

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.animations.rememberBouncyClick
import com.example.ui.animations.rememberShakeController
import com.example.ui.animations.triggerTickHaptic
import com.example.ui.effects.glassCard
import kotlinx.coroutines.launch

@Composable
fun AnimatedVaultLockScreen(
    onUnlocked: () -> Unit,
    correctPin: String = "1234",
    hapticsEnabled: Boolean = true,
    reduceTransparency: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shakeController = rememberShakeController()

    var pinDraft by remember { mutableStateOf("") }
    var showBiometricMode by remember { mutableStateOf(false) }

    // Check PIN entry
    LaunchedEffect(pinDraft) {
        if (pinDraft.length == 4) {
            if (pinDraft == correctPin) {
                if (hapticsEnabled) triggerTickHaptic(context)
                onUnlocked()
            } else {
                if (hapticsEnabled) triggerTickHaptic(context)
                shakeController.shake()
                pinDraft = ""
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
    ) {
        // Blurred Background Wallpaper Effect (40.dp blur)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (!reduceTransparency && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val blurPx = 40.dp.toPx()
                        renderEffect = RenderEffect.createBlurEffect(
                            blurPx,
                            blurPx,
                            Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                }
                .background(
                    Color(0xFF1E1B4B).copy(alpha = 0.8f)
                )
        )

        // Dark Overlay at 0.3 alpha
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )

        // Centered Content Switcher (Biometric vs PIN Pad)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = showBiometricMode,
                transitionSpec = {
                    (fadeIn(tween(250)) + scaleIn(initialScale = 0.9f)).togetherWith(
                        fadeOut(tween(200)) + scaleOut(targetScale = 0.9f)
                    )
                },
                label = "VaultLockContentSwitch"
            ) { isBiometric ->
                if (isBiometric) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(
                                blurRadius = 28.dp,
                                cornerRadius = 32.dp,
                                tint = Color.White.copy(alpha = 0.08f),
                                borderColor = Color.White.copy(alpha = 0.18f),
                                reduceTransparency = reduceTransparency
                            )
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Lock",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier
                                .size(80.dp)
                                .rememberBouncyClick {
                                    if (hapticsEnabled) triggerTickHaptic(context)
                                    onUnlocked()
                                }
                        )

                        Text(
                            text = "Biometric Authentication",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "Touch sensor to access Secret Vault",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Use PIN Code Instead",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3B82F6),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showBiometricMode = false }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    // PIN Pad Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationX = shakeController.translationX.value
                            }
                            .glassCard(
                                blurRadius = 28.dp,
                                cornerRadius = 32.dp,
                                tint = Color.White.copy(alpha = 0.08f),
                                borderColor = Color.White.copy(alpha = 0.18f),
                                reduceTransparency = reduceTransparency
                            )
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "ENTER VAULT PIN",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Animated PIN Dots with spring scaling
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(4) { index ->
                                val isFilled = index < pinDraft.length
                                val dotScale by animateFloatAsState(
                                    targetValue = if (isFilled) 1.3f else 1.0f,
                                    animationSpec = spring(dampingRatio = 0.35f, stiffness = 500f),
                                    label = "PinDotScale_$index"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .graphicsLayer {
                                            scaleX = dotScale
                                            scaleY = dotScale
                                        }
                                        .clip(CircleShape)
                                        .background(
                                            if (isFilled) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.25f)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // 0-9 Number Pad Grid
                        val numberRows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("bio", "0", "del")
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            numberRows.forEach { row ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    row.forEach { digit ->
                                        Box(
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .rememberBouncyClick {
                                                    if (hapticsEnabled) triggerTickHaptic(context)
                                                    when (digit) {
                                                        "del" -> if (pinDraft.isNotEmpty()) pinDraft = pinDraft.dropLast(1)
                                                        "bio" -> showBiometricMode = true
                                                        else -> if (pinDraft.length < 4) pinDraft += digit
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            when (digit) {
                                                "del" -> Icon(
                                                    imageVector = Icons.Default.Backspace,
                                                    contentDescription = "Delete",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                "bio" -> Icon(
                                                    imageVector = Icons.Default.Fingerprint,
                                                    contentDescription = "Biometrics",
                                                    tint = Color(0xFF3B82F6),
                                                    modifier = Modifier.size(26.dp)
                                                )
                                                else -> Text(
                                                    text = digit,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
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
        }
    }
}
