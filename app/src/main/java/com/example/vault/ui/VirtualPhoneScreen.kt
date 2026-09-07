package com.example.vault.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.VibrationHelper
import com.example.vault.data.VaultRepository
import com.example.vault.data.VaultSecurityManager
import com.example.vault.util.PanicSensorManager
import com.example.vault.ui.apps.VaultAudioPlayerApp
import com.example.vault.ui.apps.VaultBrowserApp
import com.example.vault.ui.apps.VaultClockApp
import com.example.vault.ui.apps.VaultFilesApp
import com.example.vault.ui.apps.VaultGalleryApp
import com.example.vault.ui.apps.VaultNotesApp
import com.example.vault.ui.apps.VaultSettingsApp
import com.example.vault.ui.apps.VaultVideoPlayerApp
import com.example.vault.ui.lock.SetPinDialog
import com.example.vault.ui.lock.VaultLockScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VirtualPhoneScreen(
    onExitVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val repository = remember { VaultRepository(context) }
    val securityManager = remember { VaultSecurityManager(context) }

    var isLocked by remember { mutableStateOf(securityManager.isLockEnabled()) }
    var activeApp by remember { mutableStateOf<String?>(null) } // null = Home Screen
    var showSetPinDialog by remember { mutableStateOf(false) }

    var toastMessage by remember { mutableStateOf<String?>(null) }

    fun showToast(msg: String) {
        toastMessage = msg
        coroutineScope.launch {
            delay(2600)
            if (toastMessage == msg) {
                toastMessage = null
            }
        }
    }

    fun triggerPanic() {
        if (securityManager.isPanicVibrateEnabled()) {
            VibrationHelper.doubleClick(context)
        }
        if (securityManager.hasPin()) {
            isLocked = true
            securityManager.lock()
        }
        activeApp = null
        onExitVault()
    }

    // Accelerometer-based Smart Panic Triggers (Flip Face-Down & Double-Shake)
    val panicSensorManager = remember {
        PanicSensorManager(
            context = context,
            isFlipEnabled = { securityManager.isPanicFlipEnabled() },
            isShakeEnabled = { securityManager.isPanicShakeEnabled() },
            onPanicTriggered = {
                coroutineScope.launch(Dispatchers.Main) {
                    triggerPanic()
                }
            }
        )
    }

    DisposableEffect(Unit) {
        panicSensorManager.start()
        onDispose {
            panicSensorManager.stop()
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var statusBarTime by remember { mutableStateOf(timeFormat.format(Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            statusBarTime = timeFormat.format(Date())
            delay(1000)
        }
    }

    androidx.activity.compose.BackHandler {
        if (showSetPinDialog) {
            showSetPinDialog = false
        } else if (activeApp != null) {
            activeApp = null
        } else {
            if (securityManager.hasPin()) {
                isLocked = true
            }
            onExitVault()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (securityManager.hasPin()) {
                    isLocked = true
                    securityManager.lock()
                }
                activeApp = null
                onExitVault()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // Virtual Phone Status Bar (Double-tap anywhere on status bar to trigger Panic Mode)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (securityManager.isPanicDoubleTapEnabled()) {
                                triggerPanic()
                            }
                        }
                    )
                }
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
                    // Left Status: Time & Carrier
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = statusBarTime,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "VAULT 5G",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF818CF8)
                        )
                    }

                    // Center: Dynamic Island / Camera Hole
                    Box(
                        modifier = Modifier
                            .width(84.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF111827))
                            .border(0.5.dp, Color(0xFF374151), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF020617))
                                .border(0.5.dp, Color(0xFF1E293B), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                            )
                        }
                    }

                    // Right Status: Panic button, Wi-Fi, Battery, Lock button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Emergency Panic Trigger Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFDC2626))
                                .clickable {
                                    triggerPanic()
                                }
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Instant Panic Switch",
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "PANIC",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.BatteryFull,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(14.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Exit / Lock",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable {
                                    VibrationHelper.tick(context)
                                    if (securityManager.hasPin()) {
                                        isLocked = true
                                    }
                                    onExitVault()
                                }
                        )
                    }
                }

                // Phone Screen Inner Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isLocked) {
                        VaultLockScreen(
                            securityManager = securityManager,
                            onUnlocked = {
                                isLocked = false
                            },
                            onExitVault = onExitVault,
                            onConfigurePin = {
                                showSetPinDialog = true
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AnimatedContent(
                            targetState = activeApp,
                            transitionSpec = {
                                if (targetState != null) {
                                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                                            slideOutHorizontally { width -> -width } + fadeOut()
                                } else {
                                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                            slideOutHorizontally { width -> width } + fadeOut()
                                }
                            },
                            label = "phone_screen_nav"
                        ) { targetApp ->
                            when (targetApp) {
                                null -> {
                                    VirtualPhoneHomeScreen(
                                        repository = repository,
                                        securityManager = securityManager,
                                        onLaunchApp = { appId ->
                                            activeApp = appId
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "gallery" -> {
                                    VaultGalleryApp(
                                        repository = repository,
                                        onBack = { activeApp = null },
                                        showToast = ::showToast,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "video" -> {
                                    VaultVideoPlayerApp(
                                        repository = repository,
                                        onBack = { activeApp = null },
                                        showToast = ::showToast,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "audio" -> {
                                    VaultAudioPlayerApp(
                                        repository = repository,
                                        onBack = { activeApp = null },
                                        showToast = ::showToast,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "files" -> {
                                    VaultFilesApp(
                                        repository = repository,
                                        onBack = { activeApp = null },
                                        showToast = ::showToast,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "browser" -> {
                                    VaultBrowserApp(
                                        repository = repository,
                                        onBack = { activeApp = null },
                                        showToast = ::showToast,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "clock" -> {
                                    VaultClockApp(
                                        onBack = { activeApp = null },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "notes" -> {
                                    VaultNotesApp(
                                        repository = repository,
                                        onBack = { activeApp = null },
                                        showToast = ::showToast,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "settings" -> {
                                    VaultSettingsApp(
                                        repository = repository,
                                        securityManager = securityManager,
                                        onBack = { activeApp = null },
                                        onLockNow = {
                                            isLocked = true
                                            activeApp = null
                                        },
                                        onTriggerSetPin = {
                                            showSetPinDialog = true
                                        },
                                        onPanicExit = ::triggerPanic,
                                        showToast = ::showToast,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // Floating Emergency Panic Button (When inside any vault app)
                    if (!isLocked && activeApp != null && securityManager.isPanicFloatingButtonEnabled()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 60.dp, end = 16.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFDC2626).copy(alpha = 0.92f))
                                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                .clickable {
                                    triggerPanic()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Emergency Panic Switch",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "PANIC",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // In-Phone Toast Snackbar Overlay
                    val toast = toastMessage
                    if (toast != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1E293B).copy(alpha = 0.95f))
                                    .border(1.dp, Color(0xFF6366F1), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = toast,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Virtual Phone 3-Button Navigation Bar at Bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(Color.Black)
                        .padding(horizontal = 36.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    IconButton(
                        onClick = {
                            VibrationHelper.tick(context)
                            if (activeApp != null) {
                                activeApp = null
                            } else {
                                onExitVault()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Virtual Phone Back",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Home Button
                    IconButton(
                        onClick = {
                            VibrationHelper.tick(context)
                            activeApp = null
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFF94A3B8), CircleShape)
                        )
                    }

                    // Lock / Exit Button
                    IconButton(
                        onClick = {
                            VibrationHelper.tick(context)
                            if (securityManager.hasPin()) {
                                isLocked = true
                            }
                            onExitVault()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Virtual Phone",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

    // Set PIN Dialog
    if (showSetPinDialog) {
        SetPinDialog(
            onDismiss = { showSetPinDialog = false },
            onPinSet = { pin, q, a ->
                securityManager.setPin(pin)
                securityManager.setSecurityQuestion(q, a)
                showSetPinDialog = false
                showToast("Secret PIN & recovery question configured!")
            }
        )
    }
}
