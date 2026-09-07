package com.example.vault.ui.apps

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.VibrationHelper
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun VaultClockApp(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("clock") } // "clock", "stopwatch", "timer"

    // Live clock state
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
    var currentTime by remember { mutableStateOf(timeFormat.format(Date())) }
    var currentDate by remember { mutableStateOf(dateFormat.format(Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = timeFormat.format(Date())
            currentDate = dateFormat.format(Date())
            delay(500)
        }
    }

    // Stopwatch state
    var swRunning by remember { mutableStateOf(false) }
    var swElapsedMs by remember { mutableLongStateOf(0L) }
    val swLaps = remember { mutableStateListOf<Long>() }

    LaunchedEffect(swRunning) {
        var last = System.currentTimeMillis()
        while (swRunning) {
            delay(30)
            val now = System.currentTimeMillis()
            swElapsedMs += (now - last)
            last = now
        }
    }

    // Timer state
    var timerRunning by remember { mutableStateOf(false) }
    var timerRemainingSec by remember { mutableIntStateOf(300) } // default 5m
    var timerInitialSec by remember { mutableIntStateOf(300) }

    LaunchedEffect(timerRunning) {
        while (timerRunning && timerRemainingSec > 0) {
            delay(1000)
            timerRemainingSec--
            if (timerRemainingSec == 0) {
                timerRunning = false
                VibrationHelper.tick(context)
            }
        }
    }

    fun formatMs(ms: Long): String {
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        val millis = (ms % 1000) / 10
        return String.format(Locale.US, "%02d:%02d.%02d", m, s, millis)
    }

    fun formatSec(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
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
                    text = "Secret Clock",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            // Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val tabs = listOf(
                    "clock" to "Clock",
                    "stopwatch" to "Stopwatch",
                    "timer" to "Timer"
                )
                tabs.forEach { (id, label) ->
                    val isSel = selectedTab == id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) Color(0xFF6366F1) else Color.Transparent)
                            .clickable {
                                VibrationHelper.tick(context)
                                selectedTab = id
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) Color.White else Color(0xFF94A3B8),
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                "clock" -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Glowing Main Time
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                                .padding(horizontal = 28.dp, vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = currentTime,
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA5B4FC),
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentDate,
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // World time cards
                        Text(
                            text = "World Cities",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        val cities = listOf(
                            "New York" to "America/New_York",
                            "London" to "Europe/London",
                            "Tokyo" to "Asia/Tokyo",
                            "Paris" to "Europe/Paris"
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            cities.forEach { (cityName, tzId) ->
                                val tzFormat = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                                    timeZone = TimeZone.getTimeZone(tzId)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E293B))
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(cityName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(tzFormat.format(Date()), color = Color(0xFF818CF8), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                "stopwatch" -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = formatMs(swElapsedMs),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    VibrationHelper.tick(context)
                                    swRunning = !swRunning
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (swRunning) Color(0xFFEF4444) else Color(0xFF10B981)
                                ),
                                shape = CircleShape,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(
                                    imageVector = if (swRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            if (swRunning) {
                                Button(
                                    onClick = {
                                        VibrationHelper.tick(context)
                                        swLaps.add(0, swElapsedMs)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                    shape = CircleShape,
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Text("Lap", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (swElapsedMs > 0) {
                                Button(
                                    onClick = {
                                        VibrationHelper.tick(context)
                                        swElapsedMs = 0L
                                        swLaps.clear()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                    shape = CircleShape,
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Laps List
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(swLaps) { index, lapMs ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E293B))
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Lap ${swLaps.size - index}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                    Text(formatMs(lapMs), color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }

                "timer" -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = formatSec(timerRemainingSec),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (timerRemainingSec == 0) Color(0xFFEF4444) else Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Presets
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val presets = listOf(60 to "1m", 300 to "5m", 900 to "15m", 1800 to "30m")
                            presets.forEach { (sec, label) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (timerInitialSec == sec) Color(0xFF0284C7) else Color(0xFF1E293B))
                                        .clickable {
                                            VibrationHelper.tick(context)
                                            timerRunning = false
                                            timerInitialSec = sec
                                            timerRemainingSec = sec
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    VibrationHelper.tick(context)
                                    if (timerRemainingSec == 0) {
                                        timerRemainingSec = timerInitialSec
                                    }
                                    timerRunning = !timerRunning
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (timerRunning) Color(0xFFEF4444) else Color(0xFF0284C7)
                                ),
                                shape = CircleShape,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(
                                    imageVector = if (timerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    VibrationHelper.tick(context)
                                    timerRunning = false
                                    timerRemainingSec = timerInitialSec
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                shape = CircleShape,
                                modifier = Modifier.size(60.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
