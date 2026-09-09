package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassTheme
import com.example.util.VibrationHelper

enum class MainTab(
    val title: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
) {
    HOME("Home", Icons.Outlined.Home, Icons.Filled.Home),
    LIBRARY("Library", Icons.Outlined.Folder, Icons.Filled.Folder),
    AI("AI", Icons.Outlined.AutoAwesome, Icons.Filled.AutoAwesome),
    TOOLS("Tools", Icons.Outlined.Widgets, Icons.Filled.Widgets),
    PROFILE("Profile", Icons.Outlined.Person, Icons.Filled.Person)
}

@Composable
fun NotesBottomBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    val density = LocalDensity.current
    val context = LocalContext.current
    val shape = RoundedCornerShape(28.dp)

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    val tabIndex = currentTab.ordinal
    val totalTabs = MainTab.entries.size

    val targetFraction = tabIndex.toFloat() / totalTabs.toFloat()
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(
            dampingRatio = 0.65f, // Snappy & tactile
            stiffness = Spring.StiffnessMediumLow // Luxurious visual travel speed
        ),
        label = "liquid_dock_slide"
    )

    val scaleState by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "dock_scale"
    )

    fun selectTabFromX(x: Float) {
        if (containerSize.width > 0) {
            val clampedX = x.coerceIn(0f, containerSize.width.toFloat())
            val fraction = clampedX / containerSize.width.toFloat()
            val index = (fraction * totalTabs).toInt().coerceIn(0, totalTabs - 1)
            val selected = MainTab.entries[index]
            if (selected != currentTab) {
                VibrationHelper.tick(context)
                onTabSelected(selected)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .scale(scaleState)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = colors.shadow,
                spotColor = colors.shadow
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.glass,
                        colors.glass.copy(alpha = colors.glass.alpha * 0.96f)
                    )
                )
            )
            .border(1.5.dp, colors.glassBorder, shape)
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        VibrationHelper.tick(context)
                        selectTabFromX(offset.x)
                    },
                    onDragEnd = {
                        isDragging = false
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        selectTabFromX(change.position.x)
                    }
                )
            }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Floating Liquid Glass Indicator Pill
        if (containerSize.width > 0) {
            val tabWidthPx = containerSize.width.toFloat() / totalTabs.toFloat()
            val tabWidthDp = with(density) { tabWidthPx.toDp() }
            val baseOffsetXDp = with(density) { (animatedFraction * containerSize.width.toFloat()).toDp() }

            val diff = targetFraction - animatedFraction
            val stretchFactor = (kotlin.math.abs(diff) * 1.5f).coerceAtMost(0.35f)
            val extraWidthDp = tabWidthDp * stretchFactor
            val finalWidthDp = tabWidthDp + extraWidthDp

            // If moving forward, anchor stays left and right edge stretches.
            // If moving backward, anchor shifts left by extraWidth to stretch leftwards.
            val compensatedOffsetXDp = if (diff >= 0f) {
                baseOffsetXDp
            } else {
                baseOffsetXDp - extraWidthDp
            }

            Box(
                modifier = Modifier
                    .offset(x = compensatedOffsetXDp)
                    .width(finalWidthDp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                colors.pastelBlueBg.copy(alpha = if (isDragging) 0.35f else 0.22f),
                                colors.pastelBlue.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                colors.pastelBlue.copy(alpha = 0.6f),
                                colors.pastelBlue.copy(alpha = 0.2f),
                                colors.pastelBlue.copy(alpha = 0.6f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
            )
        }

        // Tab Icons & Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = currentTab == tab

                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) colors.pastelBlue else colors.textTertiary,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "tab_tint"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 28.dp),
                            onClick = {
                                VibrationHelper.tick(context)
                                onTabSelected(tab)
                            }
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.title,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = tab.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = iconTint
                        )
                    }
                }
            }
        }
    }
}
