package com.example.ui.animations

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TabPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 1. BouncyContentTransition - Wraps AnimatedContent with spring slide up + scale out exit.
 */
@Composable
fun <T> BouncyContentTransition(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            val enterTransition = slideInVertically(
                animationSpec = spring(dampingRatio = 0.34f, stiffness = 400f),
                initialOffsetY = { fullHeight -> (fullHeight * 0.15f).toInt() }
            ) + fadeIn(animationSpec = tween(200))

            val exitTransition = slideOutVertically(
                animationSpec = tween(300),
                targetOffsetY = { fullHeight -> (-fullHeight * 0.1f).toInt() }
            ) + fadeOut(animationSpec = tween(200)) + scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(300)
            )

            (enterTransition togetherWith exitTransition).using(
                SizeTransform(clip = false)
            )
        },
        label = "BouncyContentTransition"
    ) { state ->
        content(state)
    }
}

/**
 * 2. BouncyScreenEnter - AnimatedVisibility with bouncy scaleIn and slide/fade enter.
 */
@Composable
fun BouncyScreenEnter(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = scaleIn(
            initialScale = 0.85f,
            animationSpec = spring(dampingRatio = 0.3f, stiffness = 500f)
        ) + fadeIn(animationSpec = tween(200)),
        exit = scaleOut(
            targetScale = 0.9f,
            animationSpec = tween(200)
        ) + fadeOut(animationSpec = tween(200))
    ) {
        content()
    }
}

/**
 * 3. ElasticTabIndicator - Custom tab underline indicator that springs between tab positions.
 */
@Composable
fun ElasticTabIndicator(
    tabPositions: List<TabPosition>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 3.dp,
    cornerRadius: Dp = 2.dp
) {
    if (tabPositions.isEmpty() || selectedIndex !in tabPositions.indices) return

    val currentTab = tabPositions[selectedIndex]

    val indicatorOffset by animateDpAsState(
        targetValue = currentTab.left,
        animationSpec = spring(
            dampingRatio = 0.2f,
            stiffness = 350f
        ),
        label = "ElasticTabOffset"
    )

    val indicatorWidth by animateDpAsState(
        targetValue = currentTab.width,
        animationSpec = spring(
            dampingRatio = 0.2f,
            stiffness = 350f
        ),
        label = "ElasticTabWidth"
    )

    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.BottomStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(indicatorWidth)
                .height(height)
                .clip(RoundedCornerShape(cornerRadius))
                .background(color)
        )
    }
}
