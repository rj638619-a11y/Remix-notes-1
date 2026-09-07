package com.example.vault.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Sensor-based emergency Panic triggers for the secret vault:
 * 1. Flip phone face-down (places phone screen flat down on a desk/bed) -> fast panic exit!
 * 2. Rapid double shake -> fast panic exit!
 */
class PanicSensorManager(
    context: Context,
    private val isFlipEnabled: () -> Boolean,
    private val isShakeEnabled: () -> Boolean,
    private val onPanicTriggered: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isListening = false

    // Flip face-down state
    private var faceDownStartTime = 0L
    private val FACE_DOWN_HOLD_MS = 140L

    // Shake state
    private var lastShakeTime = 0L
    private var shakeCount = 0
    private var lastTriggerTime = 0L

    fun start() {
        if (isListening) return
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            isListening = true
        }
    }

    fun stop() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < 1800L) return // Debounce triggers

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // 1. Flip Face Down Detection
        // When screen is facing downwards: Z axis acceleration is negative (< -6.8 m/s^2)
        if (isFlipEnabled()) {
            if (z < -6.8f && abs(x) < 6.0f && abs(y) < 6.0f) {
                if (faceDownStartTime == 0L) {
                    faceDownStartTime = now
                } else if (now - faceDownStartTime >= FACE_DOWN_HOLD_MS) {
                    lastTriggerTime = now
                    faceDownStartTime = 0L
                    onPanicTriggered()
                    return
                }
            } else {
                faceDownStartTime = 0L
            }
        }

        // 2. Double-Shake Detection
        // Sudden violent acceleration vector above gravity
        if (isShakeEnabled()) {
            val netAccel = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
            if (netAccel > 11.5f) {
                if (now - lastShakeTime < 750L) {
                    shakeCount++
                    if (shakeCount >= 2) {
                        lastTriggerTime = now
                        shakeCount = 0
                        onPanicTriggered()
                        return
                    }
                } else {
                    shakeCount = 1
                }
                lastShakeTime = now
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
