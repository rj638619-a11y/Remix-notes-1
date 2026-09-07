package com.example.vault.ui.apps

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.util.ImageCompressor
import com.example.vault.data.VaultRepository
import com.example.vault.model.VaultItem
import com.example.vault.model.VaultItemType
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultGalleryApp(
    repository: VaultRepository,
    onBack: () -> Unit,
    showToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allItems by repository.itemsFlow.collectAsStateWithLifecycle()
    val photos = remember(allItems) { allItems.filter { it.type == VaultItemType.PHOTO } }

    var selectedPhoto by remember { mutableStateOf<VaultItem?>(null) }
    var photoToDelete by remember { mutableStateOf<VaultItem?>(null) }
    var photoInfoToShow by remember { mutableStateOf<VaultItem?>(null) }
    var isOptimizing by remember { mutableStateOf(false) }

    val deleteConfirmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            showToast("Original photos deleted from device storage")
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                var movedCount = 0
                var hiddenCount = 0
                val pendingMediaUris = mutableListOf<Uri>()
                var singleSender: IntentSender? = null

                for (uri in uris) {
                    val res = repository.moveFileToVault(uri, VaultItemType.PHOTO)
                    if (res.item != null) {
                        movedCount++
                        if (res.wasHiddenFromMainDevice) {
                            hiddenCount++
                        } else {
                            if (res.sourceMediaUri != null) {
                                pendingMediaUris.add(res.sourceMediaUri)
                            }
                            if (res.pendingDeleteSender != null) {
                                singleSender = res.pendingDeleteSender
                            }
                        }
                    }
                }

                if (movedCount > 0) {
                    if (hiddenCount == movedCount) {
                        showToast("$movedCount photo${if (movedCount > 1) "s" else ""} moved & deleted from device")
                    } else if (pendingMediaUris.isNotEmpty()) {
                        val batchSender = repository.createBatchDeleteSender(pendingMediaUris) ?: singleSender
                        if (batchSender != null) {
                            try {
                                deleteConfirmLauncher.launch(IntentSenderRequest.Builder(batchSender).build())
                            } catch (_: Exception) {
                                showToast("$movedCount photo${if (movedCount > 1) "s" else ""} secured in vault")
                            }
                        } else {
                            showToast("$movedCount photo${if (movedCount > 1) "s" else ""} secured in vault")
                        }
                    } else if (singleSender != null) {
                        try {
                            deleteConfirmLauncher.launch(IntentSenderRequest.Builder(singleSender).build())
                        } catch (_: Exception) {
                            showToast("$movedCount photo${if (movedCount > 1) "s" else ""} secured in vault")
                        }
                    } else {
                        showToast("$movedCount photo${if (movedCount > 1) "s" else ""} secured in vault")
                    }
                } else {
                    showToast("Could not import selected photos")
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // App Top Bar
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
                    text = "Secret Gallery",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (photos.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            if (!isOptimizing) {
                                isOptimizing = true
                                coroutineScope.launch {
                                    val (count, saved) = repository.optimizeVaultImages()
                                    isOptimizing = false
                                    if (count > 0) {
                                        showToast("Compressed $count photos! Saved ${ImageCompressor.formatFileSize(saved)}")
                                    } else {
                                        showToast("All photos are already optimally compressed")
                                    }
                                }
                            }
                        }
                    ) {
                        if (isOptimizing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFF38BDF8),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Compress,
                                contentDescription = "Compress & Optimize Storage",
                                tint = Color(0xFF38BDF8)
                            )
                        }
                    }
                }
                Text(
                    text = "${photos.size} items",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            // Photos Grid or Empty State
            if (photos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF334155)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Secret Photos",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the button below to move photos from your device into the vault. They will be hidden from your main phone!",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(photos, key = { it.id }, contentType = { "photo" }) { item ->
                        val thumbFile = repository.getThumbnailForItem(item)
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .clickable { selectedPhoto = item }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(thumbFile)
                                    .size(360, 360)
                                    .allowHardware(true)
                                    .crossfade(150)
                                    .build(),
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button to Import Photos
        FloatingActionButton(
            onClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            containerColor = Color(0xFF6366F1),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Import Photo")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Move from Device", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }

    // Fullscreen Photo Viewer Dialog
    val viewing = selectedPhoto
    if (viewing != null) {
        Dialog(
            onDismissRequest = { selectedPhoto = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val file = repository.getFileForItem(viewing)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Main Photo
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(file)
                        .crossfade(true)
                        .build(),
                    contentDescription = viewing.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedPhoto = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Text(
                        text = viewing.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { photoInfoToShow = viewing }) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                    }
                }

                // Bottom Action Bar: Export, Share, Delete
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Export back to device
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val success = repository.exportItemToDevice(viewing)
                                if (success) {
                                    showToast("Photo restored to device Pictures folder")
                                } else {
                                    showToast("Failed to restore photo")
                                }
                            }
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = Color.White)
                            Text("Restore", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    // Share
                    IconButton(
                        onClick = {
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = viewing.mimeType.ifBlank { "image/*" }
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Secret Photo"))
                            } catch (_: Exception) {
                                showToast("Could not share photo")
                            }
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            Text("Share", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    // Delete
                    IconButton(
                        onClick = {
                            photoToDelete = viewing
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF87171))
                            Text("Delete", color = Color(0xFFF87171), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    val del = photoToDelete
    if (del != null) {
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            title = { Text("Delete Photo?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete this photo from the vault. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteItem(del)
                            if (selectedPhoto?.id == del.id) selectedPhoto = null
                            photoToDelete = null
                            showToast("Photo deleted permanently")
                        }
                    }
                ) {
                    Text("Delete", color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Info Dialog
    val info = photoInfoToShow
    if (info != null) {
        val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
        AlertDialog(
            onDismissRequest = { photoInfoToShow = null },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            title = { Text("Photo Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Name: ${info.name}", fontSize = 13.sp)
                    Text("Size: ${ImageCompressor.formatFileSize(info.sizeBytes)}", fontSize = 13.sp)
                    Text("Date: ${dateFormat.format(Date(info.dateAdded))}", fontSize = 13.sp)
                    Text("Status: Isolated in Secret Vault", fontSize = 13.sp, color = Color(0xFF34D399))
                    Text("Optimization: Smart Compressed (Fast & Lightweight)", fontSize = 12.sp, color = Color(0xFF38BDF8))
                }
            },
            confirmButton = {
                TextButton(onClick = { photoInfoToShow = null }) {
                    Text("Close", color = Color(0xFF818CF8))
                }
            }
        )
    }
}
