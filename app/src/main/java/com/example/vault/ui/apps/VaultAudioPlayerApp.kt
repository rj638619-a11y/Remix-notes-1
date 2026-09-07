package com.example.vault.ui.apps

import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vault.data.VaultRepository
import com.example.vault.model.VaultItem
import com.example.vault.model.VaultItemType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultAudioPlayerApp(
    repository: VaultRepository,
    onBack: () -> Unit,
    showToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allItems by repository.itemsFlow.collectAsStateWithLifecycle()
    val audios = remember(allItems) { allItems.filter { it.type == VaultItemType.AUDIO } }

    var currentTrack by remember { mutableStateOf<VaultItem?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLooping by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(0) }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSec by remember { mutableIntStateOf(0) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var tempAudioFile by remember { mutableStateOf<File?>(null) }

    var itemToDelete by remember { mutableStateOf<VaultItem?>(null) }

    fun stopAndReleasePlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        isPlaying = false
    }

    DisposableEffect(Unit) {
        onDispose {
            stopAndReleasePlayer()
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (_: Exception) {}
        }
    }

    // Playback progress ticker
    LaunchedEffect(isPlaying, mediaPlayer) {
        while (isPlaying && mediaPlayer != null) {
            try {
                currentPosMs = mediaPlayer?.currentPosition ?: 0
                totalDurationMs = mediaPlayer?.duration ?: 0
            } catch (_: Exception) {}
            delay(300)
        }
    }

    // Recording duration ticker
    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1000)
            recordingDurationSec++
        }
    }

    fun playTrack(item: VaultItem) {
        stopAndReleasePlayer()
        currentTrack = item
        val file = repository.getFileForItem(item)
        if (!file.exists()) {
            showToast("Audio file not found")
            return
        }

        try {
            val mp = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                this.isLooping = isLooping
                start()
                setOnCompletionListener {
                    if (!isLooping) {
                        isPlaying = false
                        currentPosMs = 0
                    }
                }
            }
            mediaPlayer = mp
            totalDurationMs = mp.duration
            currentPosMs = 0
            isPlaying = true
        } catch (e: Exception) {
            showToast("Could not play audio track")
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        try {
            if (mp.isPlaying) {
                mp.pause()
                isPlaying = false
            } else {
                mp.start()
                isPlaying = true
            }
        } catch (_: Exception) {}
    }

    fun formatDuration(ms: Int): String {
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                var count = 0
                var hidden = 0
                for (uri in uris) {
                    val res = repository.moveFileToVault(uri, VaultItemType.AUDIO)
                    if (res.item != null) {
                        count++
                        if (res.wasHiddenFromMainDevice) hidden++
                    }
                }
                if (count > 0) {
                    if (hidden == count) {
                        showToast("$count audio file${if (count > 1) "s" else ""} moved & hidden from device")
                    } else {
                        showToast("$count audio file${if (count > 1) "s" else ""} secured in vault")
                    }
                } else {
                    showToast("Could not import selected audio")
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
                IconButton(onClick = {
                    stopAndReleasePlayer()
                    onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Secret Audio Player",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${audios.size} tracks",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            // Audio Tracks List
            if (audios.isEmpty()) {
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
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Secret Audio",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Move private music or voice notes from your device into the vault, or record a secret memo directly.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(audios, key = { it.id }) { item ->
                        val isThisActive = currentTrack?.id == item.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isThisActive) Color(0xFF2E1065) else Color(0xFF1E293B))
                                .border(
                                    1.dp,
                                    if (isThisActive) Color(0xFFA855F7) else Color(0xFF334155),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    if (isThisActive) togglePlayPause()
                                    else playTrack(item)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isThisActive) Color(0xFFA855F7) else Color(0xFF334155)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isThisActive && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
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
                                Text(
                                    text = repository.getStats().formatBytes(item.sizeBytes),
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            IconButton(onClick = { itemToDelete = item }) {
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

            // Now Playing Bottom Panel (if track selected)
            val track = currentTrack
            if (track != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "${formatDuration(currentPosMs)} / ${formatDuration(totalDurationMs)}",
                                color = Color(0xFFA5B4FC),
                                fontSize = 12.sp
                            )
                        }

                        // Equalizer animation
                        if (isPlaying) {
                            AnimatedEqualizer()
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                isLooping = !isLooping
                                mediaPlayer?.isLooping = isLooping
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "Loop",
                                    tint = if (isLooping) Color(0xFFA855F7) else Color(0xFF64748B)
                                )
                            }

                            IconButton(onClick = { togglePlayPause() }) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    if (totalDurationMs > 0) {
                        Slider(
                            value = currentPosMs.toFloat(),
                            onValueChange = { newPos ->
                                currentPosMs = newPos.toInt()
                                mediaPlayer?.seekTo(newPos.toInt())
                            },
                            valueRange = 0f..totalDurationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFA855F7),
                                activeTrackColor = Color(0xFFA855F7),
                                inactiveTrackColor = Color(0xFF475569)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                        )
                    }
                }
            }
        }

        // Action Buttons Row (Import Audio & Record Voice Note)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .padding(bottom = if (currentTrack != null) 90.dp else 0.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Record Voice Note Button
            FloatingActionButton(
                onClick = {
                    if (!isRecording) {
                        // Start recording
                        try {
                            val temp = File(context.cacheDir, "vault_rec_${System.currentTimeMillis()}.m4a")
                            tempAudioFile = temp
                            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                MediaRecorder(context)
                            } else {
                                @Suppress("DEPRECATION")
                                MediaRecorder()
                            }
                            recorder.apply {
                                setAudioSource(MediaRecorder.AudioSource.MIC)
                                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                setOutputFile(temp.absolutePath)
                                prepare()
                                start()
                            }
                            mediaRecorder = recorder
                            recordingDurationSec = 0
                            isRecording = true
                            showToast("Recording secret voice memo...")
                        } catch (e: Exception) {
                            showToast("Microphone permission needed or recording error")
                        }
                    } else {
                        // Stop recording and save
                        try {
                            mediaRecorder?.stop()
                            mediaRecorder?.release()
                            mediaRecorder = null
                            isRecording = false

                            val temp = tempAudioFile
                            if (temp != null && temp.exists()) {
                                coroutineScope.launch {
                                    val nowStr = SimpleDateFormat("MMM_d_HHmm", Locale.getDefault()).format(Date())
                                    repository.saveRecordedAudio(temp, "Voice_Memo_$nowStr")
                                    showToast("Voice memo saved to secret vault!")
                                }
                            }
                        } catch (e: Exception) {
                            showToast("Error saving recording")
                        }
                    }
                },
                containerColor = if (isRecording) Color(0xFFEF4444) else Color(0xFF475569),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Record"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRecording) "${formatDuration(recordingDurationSec * 1000)}" else "Record",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Move Audio from Device
            FloatingActionButton(
                onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                containerColor = Color(0xFF9333EA),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Audiotrack, contentDescription = "Import Audio")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Move from Device", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    // Delete dialog
    val toDel = itemToDelete
    if (toDel != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            title = { Text("Delete Audio?", fontWeight = FontWeight.Bold) },
            text = { Text("Permanently delete this audio from the secret vault?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            if (currentTrack?.id == toDel.id) {
                                stopAndReleasePlayer()
                                currentTrack = null
                            }
                            repository.deleteItem(toDel)
                            itemToDelete = null
                            showToast("Audio track deleted")
                        }
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
private fun AnimatedEqualizer() {
    val transition = rememberInfiniteTransition(label = "eq")
    val h1 by transition.animateFloat(
        initialValue = 4f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by transition.animateFloat(
        initialValue = 16f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(250, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by transition.animateFloat(
        initialValue = 8f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .height(22.dp)
            .padding(horizontal = 8.dp)
    ) {
        Box(modifier = Modifier.width(3.dp).height(h1.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFA855F7)))
        Box(modifier = Modifier.width(3.dp).height(h2.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF38BDF8)))
        Box(modifier = Modifier.width(3.dp).height(h3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFA855F7)))
    }
}
