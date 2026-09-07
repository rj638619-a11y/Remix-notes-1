package com.example.vault.ui.apps

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vault.data.VaultRepository
import com.example.vault.model.VaultItem
import com.example.vault.model.VaultItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultFilesApp(
    repository: VaultRepository,
    onBack: () -> Unit,
    showToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allItems by repository.itemsFlow.collectAsStateWithLifecycle()
    val files = remember(allItems) { allItems.filter { it.type == VaultItemType.DOCUMENT } }

    var selectedFileItem by remember { mutableStateOf<VaultItem?>(null) }
    var fileContentPreview by remember { mutableStateOf<String?>(null) }
    var fileToDelete by remember { mutableStateOf<VaultItem?>(null) }

    val deleteConfirmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            showToast("Original files deleted from device storage")
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                var count = 0
                var hidden = 0
                val pendingMediaUris = mutableListOf<Uri>()
                var singleSender: IntentSender? = null

                for (uri in uris) {
                    val res = repository.moveFileToVault(uri, VaultItemType.DOCUMENT)
                    if (res.item != null) {
                        count++
                        if (res.wasHiddenFromMainDevice) {
                            hidden++
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

                if (count > 0) {
                    if (hidden == count) {
                        showToast("$count file${if (count > 1) "s" else ""} moved & deleted from device")
                    } else if (pendingMediaUris.isNotEmpty()) {
                        val batchSender = repository.createBatchDeleteSender(pendingMediaUris) ?: singleSender
                        if (batchSender != null) {
                            try {
                                deleteConfirmLauncher.launch(IntentSenderRequest.Builder(batchSender).build())
                                showToast("$count file${if (count > 1) "s" else ""} secured in vault")
                            } catch (_: Exception) {
                                showToast("$count file${if (count > 1) "s" else ""} secured in vault")
                            }
                        } else {
                            showToast("$count file${if (count > 1) "s" else ""} secured in vault")
                        }
                    } else if (singleSender != null) {
                        try {
                            deleteConfirmLauncher.launch(IntentSenderRequest.Builder(singleSender).build())
                            showToast("$count file${if (count > 1) "s" else ""} secured in vault")
                        } catch (_: Exception) {
                            showToast("$count file${if (count > 1) "s" else ""} secured in vault")
                        }
                    } else {
                        showToast("$count file${if (count > 1) "s" else ""} secured in vault")
                    }
                } else {
                    showToast("Could not import selected files")
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
                    text = "Secret Documents",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${files.size} files",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            if (files.isEmpty()) {
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
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Secret Documents",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Move confidential PDFs, contracts, text files, or archives from your device to keep them strictly isolated.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(files, key = { it.id }) { item ->
                        val ext = item.name.substringAfterLast('.', "").lowercase()
                        val icon = when (ext) {
                            "pdf" -> Icons.Default.PictureAsPdf
                            "zip", "rar", "7z", "tar", "gz" -> Icons.Default.FolderZip
                            "json", "xml", "kt", "js", "html", "css" -> Icons.Default.Code
                            else -> Icons.Default.Description
                        }
                        val tint = when (ext) {
                            "pdf" -> Color(0xFFF87171)
                            "zip", "rar", "7z" -> Color(0xFFFBBF24)
                            "json", "xml", "kt", "js", "html", "css" -> Color(0xFF60A5FA)
                            else -> Color(0xFF34D399)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                                .clickable {
                                    selectedFileItem = item
                                    coroutineScope.launch {
                                        val realFile = repository.getFileForItem(item)
                                        if (realFile.exists() && realFile.length() < 200_000) {
                                            try {
                                                val text = withContext(Dispatchers.IO) { realFile.readText() }
                                                fileContentPreview = text
                                            } catch (_: Exception) {
                                                fileContentPreview = null
                                            }
                                        } else {
                                            fileContentPreview = null
                                        }
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tint.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = repository.getStats().formatBytes(item.sizeBytes),
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text("•", fontSize = 12.sp, color = Color(0xFF64748B))
                                    Text(
                                        text = dateFormat.format(Date(item.dateAdded)),
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            IconButton(onClick = { fileToDelete = item }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Import Button
        FloatingActionButton(
            onClick = {
                filePickerLauncher.launch(
                    arrayOf(
                        "application/*",
                        "text/*",
                        "application/pdf",
                        "application/zip"
                    )
                )
            },
            containerColor = Color(0xFF10B981),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = "Import Files")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Move from Device", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    // File Preview / Actions Dialog
    val viewingItem = selectedFileItem
    if (viewingItem != null) {
        val realFile = repository.getFileForItem(viewingItem)
        Dialog(
            onDismissRequest = {
                selectedFileItem = null
                fileContentPreview = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewingItem.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            selectedFileItem = null
                            fileContentPreview = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    // Content Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        if (fileContentPreview != null) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Text(
                                    text = fileContentPreview ?: "",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Binary / Protected Document",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Size: ${repository.getStats().formatBytes(viewingItem.sizeBytes)}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Stored exclusively in your isolated secret vault.",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Bottom actions: Restore to device & Share
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    val ok = repository.exportItemToDevice(viewingItem)
                                    if (ok) showToast("File restored to device Downloads folder")
                                    else showToast("Failed to restore file")
                                }
                            }
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color(0xFF60A5FA))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore to Device", color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        realFile
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = viewingItem.mimeType.ifBlank { "*/*" }
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Document"))
                                } catch (_: Exception) {
                                    showToast("Could not share document")
                                }
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Delete Dialog
    val fDel = fileToDelete
    if (fDel != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            title = { Text("Delete Document?", fontWeight = FontWeight.Bold) },
            text = { Text("Permanently delete ${fDel.name} from secret vault?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteItem(fDel)
                            if (selectedFileItem?.id == fDel.id) {
                                selectedFileItem = null
                                fileContentPreview = null
                            }
                            fileToDelete = null
                            showToast("Document deleted permanently")
                        }
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}
