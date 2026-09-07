package com.example.vault.ui.lock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.VibrationHelper
import com.example.vault.data.VaultSecurityManager
import com.example.vault.util.VaultFaceBiometricHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun VaultLockScreen(
    securityManager: VaultSecurityManager,
    onUnlocked: () -> Unit,
    onExitVault: () -> Unit,
    onConfigurePin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fragmentActivity = remember(context) { VaultFaceBiometricHelper.findFragmentActivity(context) }

    var enteredPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }

    val biometricStatus = remember(context) { VaultFaceBiometricHelper.canAuthenticate(context) }
    val isBiometricAvailable = biometricStatus == VaultFaceBiometricHelper.BiometricStatus.AVAILABLE
    val hwType = remember(context) { VaultFaceBiometricHelper.getHardwareType(context) }
    val bioLabel = when (hwType) {
        VaultFaceBiometricHelper.BiometricHardwareType.FACE -> "Face Unlock"
        VaultFaceBiometricHelper.BiometricHardwareType.FINGERPRINT -> "Fingerprint Unlock"
        else -> "Biometric Unlock"
    }
    val bioIcon = if (hwType == VaultFaceBiometricHelper.BiometricHardwareType.FACE) {
        Icons.Default.Face
    } else {
        Icons.Default.Fingerprint
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    var currentTime by remember { mutableStateOf(timeFormat.format(Date())) }
    var currentDate by remember { mutableStateOf(dateFormat.format(Date())) }

    fun triggerBiometricUnlock(manualTap: Boolean = false) {
        if (!securityManager.isFaceUnlockEnabled()) return

        val currentStatus = VaultFaceBiometricHelper.canAuthenticate(context)
        if (currentStatus != VaultFaceBiometricHelper.BiometricStatus.AVAILABLE) {
            if (manualTap) {
                VibrationHelper.tick(context)
                errorText = when (currentStatus) {
                    VaultFaceBiometricHelper.BiometricStatus.NOT_ENROLLED -> "No biometric registered in device settings"
                    VaultFaceBiometricHelper.BiometricStatus.NO_HARDWARE -> "Biometric sensor not available on this device"
                    VaultFaceBiometricHelper.BiometricStatus.HARDWARE_UNAVAILABLE -> "Biometric sensor temporarily unavailable"
                    else -> "Biometric unlock not supported on this device"
                }
            }
            return
        }

        val activity = fragmentActivity ?: return
        VaultFaceBiometricHelper.authenticateBiometric(
            activity = activity,
            onSuccess = {
                VibrationHelper.click(context)
                onUnlocked()
            },
            onError = { errorCode, errString ->
                if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != androidx.biometric.BiometricPrompt.ERROR_CANCELED) {
                    if (manualTap) {
                        errorText = errString.toString()
                    }
                }
            },
            onFailed = {
                VibrationHelper.tick(context)
                errorText = "Biometric not recognized. Use PIN."
            }
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = timeFormat.format(Date())
            currentDate = dateFormat.format(Date())
            delay(1000)
        }
    }

    // Auto-launch Biometric Unlock only if hardware is available and enrolled
    LaunchedEffect(Unit) {
        if (securityManager.isFaceUnlockEnabled() && isBiometricAvailable) {
            delay(400)
            triggerBiometricUnlock(manualTap = false)
        }
    }

    fun triggerShake() {
        VibrationHelper.tick(context)
        coroutineScope.launch {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(24f, tween(50))
            shakeOffset.animateTo(-24f, tween(50))
            shakeOffset.animateTo(16f, tween(50))
            shakeOffset.animateTo(-16f, tween(50))
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    fun handleDigitPress(digit: String) {
        if (enteredPin.length < 8) {
            VibrationHelper.tick(context)
            val newPin = enteredPin + digit
            enteredPin = newPin
            errorText = null

            // If 4 digits reached, check PIN
            if (newPin.length >= 4) {
                if (securityManager.verifyPin(newPin)) {
                    onUnlocked()
                } else if (newPin.length >= 6) {
                    // Fail after 6
                    errorText = "Incorrect PIN"
                    triggerShake()
                    coroutineScope.launch {
                        delay(350)
                        enteredPin = ""
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        if (enteredPin.isNotEmpty()) {
            VibrationHelper.tick(context)
            enteredPin = enteredPin.dropLast(1)
            errorText = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF090D16),
                        Color(0xFF020617)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Lock Icon & Time
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                        .border(1.dp, Color(0xFF334155), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentTime,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraLight,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )

                Text(
                    text = currentDate,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Secret Vault",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFA5B4FC)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // PIN indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                        .padding(vertical = 4.dp)
                ) {
                    val dotCount = 4
                    for (i in 0 until dotCount) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) Color(0xFF818CF8) else Color(0xFF334155)
                                )
                                .border(
                                    1.dp,
                                    if (isFilled) Color(0xFFA5B4FC) else Color(0xFF475569),
                                    CircleShape
                                )
                        )
                    }
                }

                if (securityManager.isFaceUnlockEnabled() && (isBiometricAvailable || biometricStatus == VaultFaceBiometricHelper.BiometricStatus.NOT_ENROLLED)) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .clickable {
                                triggerBiometricUnlock(manualTap = true)
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = bioIcon,
                            contentDescription = bioLabel,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = bioLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFBAE6FD)
                        )
                    }
                }

                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF87171),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Numeric Keypad
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("bio", "0", "del")
                )

                rows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "del" -> {
                                    KeypadButton(
                                        onClick = { handleBackspace() }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Backspace",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                "bio" -> {
                                    if (securityManager.isFaceUnlockEnabled() &&
                                        (isBiometricAvailable || biometricStatus == VaultFaceBiometricHelper.BiometricStatus.NOT_ENROLLED)
                                    ) {
                                        KeypadButton(
                                            onClick = {
                                                triggerBiometricUnlock(manualTap = true)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = bioIcon,
                                                contentDescription = bioLabel,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(68.dp))
                                    }
                                }
                                else -> {
                                    KeypadButton(
                                        onClick = { handleDigitPress(key) }
                                    ) {
                                        Text(
                                            text = key,
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Actions: Forgot PIN & Exit Vault
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onExitVault) {
                    Text(
                        text = "Exit to Notes",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (securityManager.hasSecurityQuestion()) {
                    TextButton(onClick = { showForgotDialog = true }) {
                        Text(
                            text = "Forgot PIN?",
                            color = Color(0xFF818CF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    TextButton(onClick = onConfigurePin) {
                        Text(
                            text = "Reset PIN",
                            color = Color(0xFF818CF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Forgot PIN Dialog
    if (showForgotDialog) {
        var answerInput by remember { mutableStateOf("") }
        var answerError by remember { mutableStateOf<String?>(null) }
        val question = securityManager.getSecurityQuestion()

        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            title = { Text("Security Recovery", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Answer your secret question to reset the Vault PIN.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = question,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA5B4FC)
                    )
                    OutlinedTextField(
                        value = answerInput,
                        onValueChange = { answerInput = it; answerError = null },
                        label = { Text("Your Answer") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF818CF8),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (answerError != null) {
                        Text(
                            text = answerError ?: "",
                            color = Color(0xFFF87171),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (securityManager.verifySecurityAnswer(answerInput)) {
                            showForgotDialog = false
                            onConfigurePin()
                        } else {
                            answerError = "Incorrect answer. Try again."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Verify & Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
private fun KeypadButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E293B).copy(alpha = 0.75f))
            .border(1.dp, Color(0xFF334155), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, radius = 34.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
