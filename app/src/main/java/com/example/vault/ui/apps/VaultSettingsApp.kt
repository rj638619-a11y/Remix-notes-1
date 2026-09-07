package com.example.vault.ui.apps

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.ImageCompressor
import com.example.vault.data.VaultRepository
import com.example.vault.data.VaultSecurityManager
import com.example.vault.util.VaultFaceBiometricHelper
import kotlinx.coroutines.launch

@Composable
fun VaultSettingsApp(
    repository: VaultRepository,
    securityManager: VaultSecurityManager,
    onBack: () -> Unit,
    onLockNow: () -> Unit,
    onTriggerSetPin: () -> Unit,
    showToast: (String) -> Unit,
    onPanicExit: () -> Unit = onLockNow,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allItems by repository.itemsFlow.collectAsStateWithLifecycle()
    val stats = remember(allItems) { repository.getStats() }

    val hwType = remember(context) { VaultFaceBiometricHelper.getHardwareType(context) }
    val bioTitle = when (hwType) {
        VaultFaceBiometricHelper.BiometricHardwareType.FACE -> "Face Unlock"
        VaultFaceBiometricHelper.BiometricHardwareType.FINGERPRINT -> "Fingerprint Unlock"
        else -> "Biometric Unlock"
    }
    val bioSubtitle = when (hwType) {
        VaultFaceBiometricHelper.BiometricHardwareType.FACE -> "Authenticate vault with secure face recognition"
        VaultFaceBiometricHelper.BiometricHardwareType.FINGERPRINT -> "Authenticate vault with fingerprint sensor"
        else -> "Authenticate vault with biometric hardware"
    }
    val bioIcon = if (hwType == VaultFaceBiometricHelper.BiometricHardwareType.FACE) Icons.Default.Face else Icons.Default.Fingerprint

    // Real system memory and storage info
    val memoryInfo = remember {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(mi)
        mi
    }
    val storageInfo = remember {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            val availBytes = stat.availableBlocksLong * stat.blockSizeLong
            Pair(availBytes, totalBytes)
        } catch (_: Exception) {
            Pair(0L, 0L)
        }
    }

    var biometricEnabled by remember { mutableStateOf(securityManager.isBiometricEnabled()) }
    var currentWallpaper by remember { mutableStateOf(securityManager.getWallpaper()) }

    var panicFlip by remember { mutableStateOf(securityManager.isPanicFlipEnabled()) }
    var panicShake by remember { mutableStateOf(securityManager.isPanicShakeEnabled()) }
    var panicDoubleTap by remember { mutableStateOf(securityManager.isPanicDoubleTapEnabled()) }
    var panicFloatingBtn by remember { mutableStateOf(securityManager.isPanicFloatingButtonEnabled()) }
    var panicVibrate by remember { mutableStateOf(securityManager.isPanicVibrateEnabled()) }

    var showQuestionDialog by remember { mutableStateOf(false) }
    var showExportConfirm by remember { mutableStateOf(false) }
    var showWipeConfirm by remember { mutableStateOf(false) }
    var isCompressing by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Vault Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Section 1: Security & Lock
                Text(
                    text = "SECURITY & LOCK",
                    color = Color(0xFF818CF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // PIN status & Change
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTriggerSetPin() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (securityManager.hasPin()) "Change Secret PIN" else "Set Secret PIN",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (securityManager.hasPin()) "Vault is protected with PIN" else "No PIN set yet",
                                    color = if (securityManager.hasPin()) Color(0xFF34D399) else Color(0xFFFBBF24),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Text(
                            text = if (securityManager.hasPin()) "Modify" else "Set",
                            color = Color(0xFF818CF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Biometric Unlock toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(bioIcon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(bioTitle, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(bioSubtitle, color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = {
                                biometricEnabled = it
                                securityManager.setBiometricEnabled(it)
                                showToast(if (it) "$bioTitle enabled" else "$bioTitle disabled")
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0284C7)
                            )
                        )
                    }

                    // Recovery Question
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showQuestionDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Recovery Question", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Reset PIN if forgotten", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        }
                        Text("Configure", color = Color(0xFFFBBF24), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Lock Now button
                    Button(
                        onClick = onLockNow,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lock Vault Immediately", fontWeight = FontWeight.Bold)
                    }
                }

                // Section: Smart Panic Mode
                Text(
                    text = "SMART PANIC MODE (EMERGENCY SWITCH)",
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "Emergency Fast Escape",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Instantly hide the vault and return to normal Notes if someone approaches",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // 1. Flip Phone Face-Down Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Flip Face-Down", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Placing phone face down on desk/bed instantly locks & exits", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Switch(
                            checked = panicFlip,
                            onCheckedChange = {
                                panicFlip = it
                                securityManager.setPanicFlipEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEF4444)
                            )
                        )
                    }

                    // 2. Shake to Panic Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Double-Shake Device", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Two quick shakes of the phone instantly triggers panic exit", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Switch(
                            checked = panicShake,
                            onCheckedChange = {
                                panicShake = it
                                securityManager.setPanicShakeEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEF4444)
                            )
                        )
                    }

                    // 3. Status Bar Double-Tap Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Double-Tap Status Bar", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Double tap the top status bar / dynamic island to exit", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Switch(
                            checked = panicDoubleTap,
                            onCheckedChange = {
                                panicDoubleTap = it
                                securityManager.setPanicDoubleTapEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEF4444)
                            )
                        )
                    }

                    // 4. Floating Emergency Panic Button Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Floating Panic Button", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Display an emergency red panic pill in vault apps", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Switch(
                            checked = panicFloatingBtn,
                            onCheckedChange = {
                                panicFloatingBtn = it
                                securityManager.setPanicFloatingButtonEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEF4444)
                            )
                        )
                    }

                    // 5. Vibrate on Panic Exit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Haptic Feedback", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Subtle vibration confirmation when panic mode activates", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Switch(
                            checked = panicVibrate,
                            onCheckedChange = {
                                panicVibrate = it
                                securityManager.setPanicVibrateEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEF4444)
                            )
                        )
                    }

                    // Test Panic Mode Button
                    Button(
                        onClick = {
                            showToast("Panic triggered! Switching to Notes...")
                            onPanicExit()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Smart Panic Switch", fontWeight = FontWeight.Bold)
                    }
                }

                // Section 2: Virtual Phone Wallpaper
                Text(
                    text = "VIRTUAL PHONE WALLPAPER",
                    color = Color(0xFF818CF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(VaultSecurityManager.WALLPAPERS) { wp ->
                        val isSelected = currentWallpaper == wp.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    currentWallpaper = wp.id
                                    securityManager.setWallpaper(wp.id)
                                    showToast("Wallpaper updated")
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 72.dp, height = 110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(wp.colorStart),
                                                Color(wp.colorMid)
                                            )
                                        )
                                    )
                                    .border(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) Color(wp.accentColor) else Color(0xFF334155),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = Color(wp.accentColor),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = wp.name,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Section 3: Storage Breakdown
                Text(
                    text = "ISOLATED STORAGE & BACKUP",
                    color = Color(0xFF818CF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Vault Storage", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(stats.formatBytes(stats.totalBytes), color = Color(0xFF818CF8), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    // Stat badges
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("📷 ${stats.photoCount} photos", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text(stats.formatBytes(stats.photoBytes), color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🎬 ${stats.videoCount} videos", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text(stats.formatBytes(stats.videoBytes), color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🎵 ${stats.audioCount} audio files", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text(stats.formatBytes(stats.audioBytes), color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("📁 ${stats.docCount} documents", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text(stats.formatBytes(stats.docBytes), color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("📝 ${stats.noteCount} secret notes", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text("Encrypted", color = Color(0xFF34D399), fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Compress & Optimize Photos button
                    Button(
                        onClick = {
                            if (!isCompressing) {
                                isCompressing = true
                                coroutineScope.launch {
                                    val (count, saved) = repository.optimizeVaultImages()
                                    isCompressing = false
                                    if (count > 0) {
                                        showToast("Compressed $count photos! Saved ${ImageCompressor.formatFileSize(saved)}")
                                    } else {
                                        showToast("Vault storage is already fully compressed and optimized")
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isCompressing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compressing Photos...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Compress, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compress & Optimize Photos", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Restore All button
                    Button(
                        onClick = { showExportConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore All Files to Main Device", fontWeight = FontWeight.Bold)
                    }

                    // Wipe Vault button
                    Button(
                        onClick = { showWipeConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFCA5A5))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wipe All Secret Vault Data", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold)
                    }
                }

                // Section 4: Real Device & Vault System Info
                Text(
                    text = "DEVICE & VAULT SYSTEM INFO",
                    color = Color(0xFF818CF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val deviceName = remember {
                        val mfr = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
                        val model = Build.MODEL
                        if (model.startsWith(mfr, ignoreCase = true)) model else "$mfr $model"
                    }
                    val androidVer = remember { "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})" }
                    val secPatch = remember {
                        try {
                            Build.VERSION.SECURITY_PATCH
                        } catch (_: Exception) {
                            "Up to date"
                        }
                    }
                    val abi = remember { Build.SUPPORTED_ABIS.firstOrNull() ?: Build.HARDWARE }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Device Model", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text(deviceName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("OS Version", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text(androidVer, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Security Patch", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text(secPatch, color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("CPU Architecture", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text(abi, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Normal)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Device RAM", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        val availRam = stats.formatBytes(memoryInfo?.availMem ?: 0L)
                        val totalRam = stats.formatBytes(memoryInfo?.totalMem ?: 0L)
                        Text("$availRam free / $totalRam", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Internal Storage", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        val availStorage = stats.formatBytes(storageInfo.first)
                        val totalStorage = stats.formatBytes(storageInfo.second)
                        Text("$availStorage free / $totalStorage", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Biometric Hardware", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        val isBioReady = VaultFaceBiometricHelper.canAuthenticate(context) == VaultFaceBiometricHelper.BiometricStatus.AVAILABLE
                        Text(
                            text = if (isBioReady) "$bioTitle (Ready)" else "$bioTitle (Available)",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vault Sandbox", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text("AES-256 GCM Encrypted", color = Color(0xFF818CF8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vault Hold Trigger", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text("3 Seconds", color = Color(0xFFFBBF24), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Security Question Dialog
    if (showQuestionDialog) {
        var qText by remember { mutableStateOf(securityManager.getSecurityQuestion()) }
        var aText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showQuestionDialog = false },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            title = { Text("Configure Recovery Question", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Used to recover access if you ever forget your vault PIN.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    OutlinedTextField(
                        value = qText,
                        onValueChange = { qText = it },
                        label = { Text("Question") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF818CF8),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = aText,
                        onValueChange = { aText = it },
                        label = { Text("Answer") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF818CF8),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (aText.trim().isNotEmpty()) {
                            securityManager.setSecurityQuestion(qText, aText)
                            showQuestionDialog = false
                            showToast("Recovery question saved")
                        } else {
                            showToast("Please enter an answer")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuestionDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Export Confirm Dialog
    if (showExportConfirm) {
        AlertDialog(
            onDismissRequest = { showExportConfirm = false },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            title = { Text("Restore All Files?", fontWeight = FontWeight.Bold) },
            text = { Text("This will copy all hidden photos, videos, audio, and documents back to your main device folders so they are visible again outside the vault.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExportConfirm = false
                        coroutineScope.launch {
                            val count = repository.exportAllToDevice()
                            showToast("Restored $count files to main device")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportConfirm = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Wipe Confirm Dialog
    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            title = { Text("Permanently Wipe Secret Vault?", fontWeight = FontWeight.Bold, color = Color(0xFFF87171)) },
            text = { Text("This will PERMANENTLY ERASE all secret photos, videos, recordings, notes, and documents in the vault. This action CANNOT be undone!") },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeConfirm = false
                        coroutineScope.launch {
                            repository.wipeVault()
                            showToast("Secret Vault data wiped clean")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Erase Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}
