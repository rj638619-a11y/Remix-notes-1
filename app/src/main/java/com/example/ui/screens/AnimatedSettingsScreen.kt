package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.animations.BouncyHapticButton
import com.example.ui.animations.SpringSwitch
import com.example.ui.animations.rememberBouncyClick
import com.example.ui.effects.GlassBottomSheet
import com.example.ui.effects.glassCard
import com.example.ui.viewmodel.NotesViewModel

@Composable
fun AnimatedSettingsScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.syncFullDevice()
        } else {
            android.widget.Toast.makeText(
                context,
                "Storage permission is required to sync local files.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    val hasStoragePermission = if (Build.VERSION.SDK_INT >= 33) {
        // Android 13+ does not support standard READ_EXTERNAL_STORAGE runtime permission
        true
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    var showClearDataDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var isThemeSectionExpanded by remember { mutableStateOf(false) }
    var isSecuritySectionExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.rememberBouncyClick { onBack() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "SETTINGS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Appearance & Theme Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .glassCard(
                        blurRadius = 20.dp,
                        cornerRadius = 24.dp,
                        tint = Color.White.copy(alpha = 0.08f),
                        borderColor = Color.White.copy(alpha = 0.15f),
                        reduceTransparency = settings.reduceTransparency
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isThemeSectionExpanded = !isThemeSectionExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Appearance & Theme", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }

                AnimatedVisibility(
                    visible = isThemeSectionExpanded,
                    enter = expandVertically(tween(250)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Text("Theme Swatch Preview", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Crossfade(targetState = settings.theme, label = "ThemeSwatchCrossfade") { mode ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                listOf("auto", "dark", "light").forEach { themeMode ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (themeMode == "dark") Color(0xFF0F172A) else if (themeMode == "light") Color(0xFFF8FAFC) else Color(0xFF3B82F6)
                                            )
                                            .rememberBouncyClick { viewModel.setTheme(themeMode) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = themeMode.uppercase(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (themeMode == "light") Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reduce Transparency", fontSize = 14.sp)
                            SpringSwitch(
                                checked = settings.reduceTransparency,
                                onCheckedChange = { viewModel.setReduceTransparency(it) },
                                hapticsEnabled = settings.hapticsEnabled
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Security & Haptics Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .glassCard(
                        blurRadius = 20.dp,
                        cornerRadius = 24.dp,
                        tint = Color.White.copy(alpha = 0.08f),
                        borderColor = Color.White.copy(alpha = 0.15f),
                        reduceTransparency = settings.reduceTransparency
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSecuritySectionExpanded = !isSecuritySectionExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Security & Feedback", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }

                AnimatedVisibility(
                    visible = isSecuritySectionExpanded,
                    enter = expandVertically(tween(250)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                ) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Vibration, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Haptic Feedback", fontSize = 14.sp)
                            }
                            SpringSwitch(
                                checked = settings.hapticsEnabled,
                                onCheckedChange = { viewModel.setHapticsEnabled(it) },
                                hapticsEnabled = settings.hapticsEnabled
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Biometric Vault Lock", fontSize = 14.sp)
                            }
                            SpringSwitch(
                                checked = settings.biometricLockEnabled,
                                onCheckedChange = { viewModel.setBiometricLock(it) },
                                hapticsEnabled = settings.hapticsEnabled
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Sync & Cloud
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .glassCard(
                        blurRadius = 20.dp,
                        cornerRadius = 24.dp,
                        tint = Color.White.copy(alpha = 0.08f),
                        borderColor = Color.White.copy(alpha = 0.15f),
                        reduceTransparency = settings.reduceTransparency
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF3B82F6))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Auto Cloud Sync", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    SpringSwitch(
                        checked = settings.autoSync,
                        onCheckedChange = { viewModel.setAutoSync(it) },
                        hapticsEnabled = settings.hapticsEnabled
                    )
                }

                var apiKeyInput by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Gemini API Key", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            viewModel.setGeminiApiKey(it)
                        },
                        placeholder = { Text("Paste AI Studio API Key") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BouncyHapticButton(
                        text = "Sync Device Files",
                        onClick = {
                            if (hasStoragePermission) {
                                viewModel.syncFullDevice()
                            } else {
                                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        },
                        hapticsEnabled = settings.hapticsEnabled,
                        containerColor = Color(0xFF3B82F6).copy(alpha = 0.2f),
                        contentColor = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    )

                    BouncyHapticButton(
                        text = "Backup / Restore",
                        onClick = { showBackupDialog = true },
                        hapticsEnabled = settings.hapticsEnabled,
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Destructive Action: Wipe Data
            BouncyHapticButton(
                text = "Wipe All Data & Notes",
                onClick = { showClearDataDialog = true },
                hapticsEnabled = settings.hapticsEnabled,
                containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                contentColor = Color(0xFFEF4444),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Clear All Data Bottom Sheet Dialog
        GlassBottomSheet(
            visible = showClearDataDialog,
            onDismiss = { showClearDataDialog = false },
            reduceTransparency = settings.reduceTransparency
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(48.dp))
                Text("Confirm Wipe All Data?", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "This action cannot be undone. All notes, vault files, and AI chat logs will be permanently deleted.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BouncyHapticButton(
                        text = "Cancel",
                        onClick = { showClearDataDialog = false },
                        hapticsEnabled = settings.hapticsEnabled,
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    BouncyHapticButton(
                        text = "Wipe Everything",
                        onClick = {
                            viewModel.deleteAllNotes()
                            showClearDataDialog = false
                            onBack()
                        },
                        hapticsEnabled = settings.hapticsEnabled,
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        AnimatedBackupDialog(
            visible = showBackupDialog,
            onDismiss = { showBackupDialog = false },
            viewModel = viewModel
        )
    }
}
