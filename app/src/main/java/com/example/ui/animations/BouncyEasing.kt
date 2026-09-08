package com.example.ui.animations

import androidx.compose.animation.core.CubicBezierEasing

/**
 * Custom CubicBezierEasing curves for hyper-responsive, organic bouncy animations.
 */
object BouncyEasing {
    /** Smooth overshoot curve (0.34, 1.56, 0.64, 1.0) */
    val SmoothOvershoot = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

    /** Gentle bounce curve (0.22, 1.2, 0.36, 1.0) */
    val GentleBounce = CubicBezierEasing(0.22f, 1.2f, 0.36f, 1f)

    /** Strong, dramatic bounce curve (0.16, 1.8, 0.3, 1.0) */
    val StrongBounce = CubicBezierEasing(0.16f, 1.8f, 0.3f, 1f)

    /** Elastic settle curve (0.5, 1.5, 0.5, 1.0) */
    val ElasticSettle = CubicBezierEasing(0.5f, 1.5f, 0.5f, 1f)

    /** Snappy, high-frequency bounce curve (0.3, 1.1, 0.4, 1.0) */
    val SnappyBounce = CubicBezierEasing(0.3f, 1.1f, 0.4f, 1f)
}
