package com.example.ui.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeminiClient
import com.example.ui.components.NotesAppLogoIcon
import com.example.ui.theme.GlassTheme

@Composable
fun SettingsTab(
    currentThemeSetting: String, // "auto", "light", "dark", "matcha", "lavender", "sepia", "ocean"
    onThemeSettingChange: (String) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onShowAboutSplash: () -> Unit,
    onBack: (() -> Unit)? = null,
    onSyncFullDevice: (() -> Unit)? = null,
    geminiApiKey: String = "",
    onUpdateApiKey: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val clipboardManager = LocalClipboardManager.current

    var autoSaveEnabled by remember { mutableStateOf(true) }
    var showThumbnails by remember { mutableStateOf(true) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showReadingModeDialog by remember { mutableStateOf(false) }
    var selectedReadingMode by remember { mutableStateOf("Standard") }
    var showInfoAlert by remember { mutableStateOf<String?>(null) }
    var showExportSuccess by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyDraft by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }
    var isKeyVisible by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = statusBarTop + 14.dp, bottom = navBarBottom + 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with optional integrated back navigation
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (onBack != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .shadow(2.dp, CircleShape, ambientColor = colors.shadow, spotColor = colors.shadow)
                                .clip(CircleShape)
                                .background(colors.card)
                                .border(1.dp, colors.glassBorder, CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, radius = 20.dp),
                                    onClick = onBack
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.text,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Settings",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            color = colors.text
                        )
                        Text(
                            text = "Preferences & App Configurations",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            // Section 1: Display & Reading
            item {
                SettingsGroupCard(title = "Appearance & Theme") {
                    SettingsRowItem(
                        icon = Icons.Default.DarkMode,
                        iconTint = colors.pastelLavender,
                        iconBg = colors.pastelLavenderBg,
                        title = "App Theme",
                        value = when (currentThemeSetting.lowercase()) {
                            "light" -> "Soft White (Light) ☀️"
                            "dark" -> "Obsidian (Dark) 🌙"
                            "matcha" -> "Matcha Mint 🍵"
                            "lavender" -> "Lilac Lavender 🪻"
                            "sepia" -> "Sepia Paper 📜"
                            "ocean" -> "Midnight Ocean 🌊"
                            else -> "System Default ⚙️"
                        },
                        onClick = { showAppearanceDialog = true }
                    )
                    HorizontalDivider(color = colors.hairline, thickness = 0.5.dp)
                    SettingsRowItem(
                        icon = Icons.Default.MenuBook,
                        iconTint = colors.pastelOrange,
                        iconBg = colors.pastelOrangeBg,
                        title = "Default Reader Theme",
                        value = selectedReadingMode,
                        onClick = { showReadingModeDialog = true }
                    )
                }
            }

            // Section 1.5: Google Gemini AI & Manual API Key
            item {
                SettingsGroupCard(title = "Google Gemini AI") {
                    val isKeySet = geminiApiKey.isNotBlank() && GeminiClient.isValidGeminiApiKey(geminiApiKey)
                    val displayKeyText = if (geminiApiKey.isNotBlank()) {
                        "${geminiApiKey.take(6)}••••••••"
                    } else {
                        "Not Set (Tap to add key)"
                    }

                    SettingsRowItem(
                        icon = Icons.Default.Key,
                        iconTint = if (isKeySet) Color(0xFF10B981) else colors.pastelOrange,
                        iconBg = if (isKeySet) Color(0xFF10B981).copy(alpha = 0.15f) else colors.pastelOrangeBg,
                        title = "Manual Gemini API Key",
                        value = displayKeyText,
                        onClick = {
                            apiKeyDraft = geminiApiKey
                            showApiKeyDialog = true
                        }
                    )
                }
            }

            // Section 2: Editor & Notes Preferences
            item {
                SettingsGroupCard(title = "Preferences") {
                    SettingsSwitchItem(
                        icon = Icons.Default.Save,
                        iconTint = colors.pastelMint,
                        iconBg = colors.pastelMintBg,
                        title = "Auto Save Notes",
                        subtitle = "Saves edits instantaneously as you type",
                        checked = autoSaveEnabled,
                        onCheckedChange = { autoSaveEnabled = it }
                    )
                    HorizontalDivider(color = colors.hairline, thickness = 0.5.dp)
                    SettingsSwitchItem(
                        icon = Icons.Default.PhotoSizeSelectActual,
                        iconTint = colors.pastelBlue,
                        iconBg = colors.pastelBlueBg,
                        title = "Show File Thumbnails",
                        subtitle = "Display visual diagram badges on cards",
                        checked = showThumbnails,
                        onCheckedChange = { showThumbnails = it }
                    )
                    HorizontalDivider(color = colors.hairline, thickness = 0.5.dp)
                    SettingsRowItem(
                        icon = Icons.Default.Language,
                        iconTint = colors.pastelFavYellow,
                        iconBg = colors.pastelFavYellowBg,
                        title = "Language",
                        value = "English (US)",
                        onClick = { showInfoAlert = "English is selected." }
                    )
                }
            }

            // Section 3: Backup & Device Sync
            item {
                SettingsGroupCard(title = "Device Sync & Backup") {
                    SettingsRowItem(
                        icon = Icons.Default.Sync,
                        iconTint = colors.pastelBlue,
                        iconBg = colors.pastelBlueBg,
                        title = "Sync Full Device",
                        value = "Scan & Import",
                        onClick = {
                            if (onSyncFullDevice != null) {
                                onSyncFullDevice()
                            } else {
                                showInfoAlert = "Scanning device for PDF, HTML, and Markdown study notes..."
                            }
                        }
                    )
                    HorizontalDivider(color = colors.hairline, thickness = 0.5.dp)
                    SettingsRowItem(
                        icon = Icons.Default.FileUpload,
                        iconTint = colors.pastelMint,
                        iconBg = colors.pastelMintBg,
                        title = "Backup & Export All Notes",
                        value = "Export JSON",
                        onClick = onExportBackup
                    )
                    HorizontalDivider(color = colors.hairline, thickness = 0.5.dp)
                    SettingsRowItem(
                        icon = Icons.Default.FileDownload,
                        iconTint = colors.pastelOrange,
                        iconBg = colors.pastelOrangeBg,
                        title = "Restore Backup",
                        value = "Import JSON",
                        onClick = onImportBackup
                    )
                }
            }

            // Section 4: About & Feedback
            item {
                SettingsGroupCard(title = "Support & About") {
                    SettingsRowItem(
                        icon = Icons.Default.StarRate,
                        iconTint = colors.pastelFavYellow,
                        iconBg = colors.pastelFavYellowBg,
                        title = "Rate Notes App",
                        value = "★★★★★",
                        onClick = { showInfoAlert = "Thank you for rating Notes 5 stars! 🌟" }
                    )
                    HorizontalDivider(color = colors.hairline, thickness = 0.5.dp)
                    SettingsRowItem(
                        icon = Icons.Default.Share,
                        iconTint = colors.pastelMint,
                        iconBg = colors.pastelMintBg,
                        title = "Share with Friends",
                        value = "",
                        onClick = { showInfoAlert = "Share Notes with fellow students to study together!" }
                    )
                    HorizontalDivider(color = colors.hairline, thickness = 0.5.dp)
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        iconTint = colors.pastelBlue,
                        iconBg = colors.pastelBlueBg,
                        title = "About Notes",
                        value = "v2.0 • Build Clean",
                        onClick = onShowAboutSplash
                    )
                }
            }

            // Footer Brand
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NotesAppLogoIcon(size = 48.dp, animated = false)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notes • Read • Organize • Learn • Grow",
                        fontSize = 12.sp,
                        color = colors.textTertiary
                    )
                }
            }
        }
    }

    // Appearance Dialog with 7 theme presets
    if (showAppearanceDialog) {
        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            title = { Text("Choose App Theme", fontWeight = FontWeight.Bold, color = colors.text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "auto" to "System Default ⚙️",
                        "light" to "Minimalist Soft White (Light) ☀️",
                        "dark" to "Obsidian Charcoal (Dark) 🌙",
                        "matcha" to "Zen Matcha Sage 🍵",
                        "lavender" to "Dreamy Lilac Lavender 🪻",
                        "sepia" to "Vintage Sepia Paper 📜",
                        "ocean" to "Deep Midnight Ocean 🌊"
                    ).forEach { (key, name) ->
                        val isSelected = currentThemeSetting.equals(key, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) colors.chipOnBg else Color.Transparent)
                                .clickable {
                                    onThemeSettingChange(key)
                                    showAppearanceDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = name,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.accent else colors.text
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppearanceDialog = false }) {
                    Text("Close")
                }
            },
            containerColor = colors.card,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Reading Mode Dialog
    if (showReadingModeDialog) {
        AlertDialog(
            onDismissRequest = { showReadingModeDialog = false },
            title = { Text("Reader Paper Style", fontWeight = FontWeight.Bold, color = colors.text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Standard Clean", "Sepia Parchment", "Soft Cream", "High Contrast Dark").forEach { mode ->
                        val isSelected = selectedReadingMode == mode
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) colors.chipOnBg else Color.Transparent)
                                .clickable {
                                    selectedReadingMode = mode
                                    showReadingModeDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = mode,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.accent else colors.text
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReadingModeDialog = false }) {
                    Text("Close")
                }
            },
            containerColor = colors.card,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showExportSuccess) {
        AlertDialog(
            onDismissRequest = { showExportSuccess = false },
            title = { Text("Backup Created ✨", fontWeight = FontWeight.Bold, color = colors.text) },
            text = { Text("All your study notes, categories, and bookmarks have been compiled into 'Notes_Backup_Complete.json'.", fontSize = 14.sp, color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = { showExportSuccess = false }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.card,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showInfoAlert != null) {
        AlertDialog(
            onDismissRequest = { showInfoAlert = null },
            title = { Text("Notes", fontWeight = FontWeight.Bold, color = colors.text) },
            text = { Text(showInfoAlert!!, fontSize = 14.sp, color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = { showInfoAlert = null }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = colors.card,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Manual API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = colors.pastelLavender
                    )
                    Text(
                        text = "Manual Gemini API Key",
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your personal Google Gemini API key to activate AI features. Your key is stored locally on your device and is NOT embedded in the app.",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )

                    OutlinedTextField(
                        value = apiKeyDraft,
                        onValueChange = { apiKeyDraft = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Visibility",
                                    tint = colors.textSecondary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.pastelLavender,
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    apiKeyDraft = clipText.trim()
                                }
                            }
                        ) {
                            Text("Paste Clipboard", fontSize = 12.sp)
                        }

                        if (apiKeyDraft.isNotBlank()) {
                            TextButton(
                                onClick = { apiKeyDraft = "" }
                            ) {
                                Text("Clear Key", fontSize = 12.sp, color = colors.pastelPdfRed)
                            }
                        }
                    }

                    // Test Connection Button
                    var isTestingConnection by remember { mutableStateOf(false) }
                    var testConnectionResult by remember { mutableStateOf<String?>(null) }
                    val coroutineScope = rememberCoroutineScope()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    isTestingConnection = true
                                    testConnectionResult = null
                                    val keyToTest = apiKeyDraft.trim()
                                    if (!GeminiClient.isValidGeminiApiKey(keyToTest)) {
                                        testConnectionResult = "❌ Invalid API key format"
                                        isTestingConnection = false
                                        return@launch
                                    }
                                    val models = GeminiClient.fetchLiveModels(keyToTest)
                                    if (models.isNotEmpty()) {
                                        testConnectionResult = "✅ Connected! Models: ${models.take(2).joinToString()}"
                                    } else {
                                        testConnectionResult = "❌ Connection failed. Verify key or network."
                                    }
                                    isTestingConnection = false
                                }
                            },
                            enabled = !isTestingConnection
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.pastelLavender
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Testing...", fontSize = 12.sp)
                            } else {
                                Text("Test Connection", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    testConnectionResult?.let { result ->
                        Text(
                            text = result,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (result.startsWith("✅")) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showApiKeyDialog = false
                        onUpdateApiKey(apiKeyDraft)
                        showInfoAlert = if (apiKeyDraft.isBlank()) "API key cleared." else "Manual Gemini API key saved!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.pastelLavender)
                ) {
                    Text("Save API Key", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = colors.card,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun SettingsGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
    val colors = GlassTheme.colors
    val shape = RoundedCornerShape(22.dp)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
                .clip(shape)
                .background(colors.card)
                .border(1.dp, colors.glassBorder, shape)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    value: String = "",
    onClick: () -> Unit
) {
    val colors = GlassTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.text
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (value.isNotBlank()) {
                Text(
                    text = value,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = GlassTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.text
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.pastelMint,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colors.field
            )
        )
    }
}
