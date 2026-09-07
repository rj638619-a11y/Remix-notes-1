package com.example.vault.ui.apps

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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vault.data.VaultRepository
import com.example.vault.data.VaultSecurityManager
import kotlinx.coroutines.launch

@Composable
fun VaultSettingsApp(
    repository: VaultRepository,
    securityManager: VaultSecurityManager,
    onBack: () -> Unit,
    onLockNow: () -> Unit,
    onTriggerSetPin: () -> Unit,
    showToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val allItems by repository.itemsFlow.collectAsStateWithLifecycle()
    val stats = remember(allItems) { repository.getStats() }

    var biometricEnabled by remember { mutableStateOf(securityManager.isBiometricEnabled()) }
    var currentWallpaper by remember { mutableStateOf(securityManager.getWallpaper()) }

    var showQuestionDialog by remember { mutableStateOf(false) }
    var showExportConfirm by remember { mutableStateOf(false) }
    var showWipeConfirm by remember { mutableStateOf(false) }

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

                    // Face Unlock toggle (Face Only, No Fingerprint)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Face, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Face Unlock Only", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Biometric access via face scan only (no fingerprint)", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = {
                                biometricEnabled = it
                                securityManager.setFaceUnlockEnabled(it)
                                showToast(if (it) "Face Unlock enabled" else "Face Unlock disabled")
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
