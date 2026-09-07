package com.example.vault.data

import android.content.Context
import android.content.SharedPreferences
import com.example.util.HashUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class VaultSecurityManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("secret_vault_security_prefs", Context.MODE_PRIVATE)

    private val _isLockedFlow = MutableStateFlow(hasPin())
    val isLockedFlow = _isLockedFlow.asStateFlow()

    fun hasPin(): Boolean {
        return prefs.getString(KEY_PIN_HASH, "").isNullOrEmpty().not()
    }

    fun isLockEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOCK_ENABLED, true) && hasPin()
    }

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
    }

    fun verifyPin(enteredPin: String): Boolean {
        if (!hasPin()) return true
        val savedHash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        val enteredHash = HashUtil.sha256(enteredPin.trim())
        val match = savedHash == enteredHash
        if (match) {
            _isLockedFlow.value = false
        }
        return match
    }

    fun setPin(newPin: String) {
        val hash = HashUtil.sha256(newPin.trim())
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putBoolean(KEY_LOCK_ENABLED, true)
            .apply()
    }

    fun removePin() {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_LOCK_ENABLED, false)
            .apply()
        _isLockedFlow.value = false
    }

    fun isFaceUnlockEnabled(): Boolean {
        return prefs.getBoolean(KEY_FACE_UNLOCK_ENABLED, prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true))
    }

    fun setFaceUnlockEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_FACE_UNLOCK_ENABLED, enabled)
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .apply()
    }

    fun isBiometricEnabled(): Boolean {
        return isFaceUnlockEnabled()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        setFaceUnlockEnabled(enabled)
    }

    fun getSecurityQuestion(): String {
        return prefs.getString(KEY_SEC_QUESTION, "What is your secret passphrase?") ?: "What is your secret passphrase?"
    }

    fun hasSecurityQuestion(): Boolean {
        return prefs.getString(KEY_SEC_ANSWER_HASH, "").isNullOrEmpty().not()
    }

    fun setSecurityQuestion(question: String, answer: String) {
        val ansHash = HashUtil.sha256(answer.trim().lowercase())
        prefs.edit()
            .putString(KEY_SEC_QUESTION, question.trim())
            .putString(KEY_SEC_ANSWER_HASH, ansHash)
            .apply()
    }

    fun verifySecurityAnswer(answer: String): Boolean {
        val savedHash = prefs.getString(KEY_SEC_ANSWER_HASH, "") ?: return false
        val enteredHash = HashUtil.sha256(answer.trim().lowercase())
        return savedHash == enteredHash
    }

    fun getWallpaper(): String {
        return prefs.getString(KEY_WALLPAPER, "midnight_glass") ?: "midnight_glass"
    }

    fun setWallpaper(wallpaperId: String) {
        prefs.edit().putString(KEY_WALLPAPER, wallpaperId).apply()
    }

    fun unlockDirectly() {
        _isLockedFlow.value = false
    }

    fun lock() {
        if (hasPin()) {
            _isLockedFlow.value = true
        }
    }

    // Panic Mode Settings
    fun isPanicFlipEnabled(): Boolean = prefs.getBoolean(KEY_PANIC_FLIP, true)
    fun setPanicFlipEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_PANIC_FLIP, enabled).apply()

    fun isPanicShakeEnabled(): Boolean = prefs.getBoolean(KEY_PANIC_SHAKE, true)
    fun setPanicShakeEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_PANIC_SHAKE, enabled).apply()

    fun isPanicDoubleTapEnabled(): Boolean = prefs.getBoolean(KEY_PANIC_DOUBLE_TAP, true)
    fun setPanicDoubleTapEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_PANIC_DOUBLE_TAP, enabled).apply()

    fun isPanicFloatingButtonEnabled(): Boolean = prefs.getBoolean(KEY_PANIC_FLOATING_BTN, true)
    fun setPanicFloatingButtonEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_PANIC_FLOATING_BTN, enabled).apply()

    fun isPanicVibrateEnabled(): Boolean = prefs.getBoolean(KEY_PANIC_VIBRATE, true)
    fun setPanicVibrateEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_PANIC_VIBRATE, enabled).apply()

    companion object {
        private const val KEY_PIN_HASH = "vault_pin_hash"
        private const val KEY_LOCK_ENABLED = "vault_lock_enabled"
        private const val KEY_FACE_UNLOCK_ENABLED = "vault_face_unlock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "vault_biometric_enabled"
        private const val KEY_SEC_QUESTION = "vault_sec_question"
        private const val KEY_SEC_ANSWER_HASH = "vault_sec_answer_hash"
        private const val KEY_WALLPAPER = "vault_wallpaper"
        private const val KEY_PANIC_FLIP = "vault_panic_flip"
        private const val KEY_PANIC_SHAKE = "vault_panic_shake"
        private const val KEY_PANIC_DOUBLE_TAP = "vault_panic_double_tap"
        private const val KEY_PANIC_FLOATING_BTN = "vault_panic_floating_btn"
        private const val KEY_PANIC_VIBRATE = "vault_panic_vibrate"

        val WALLPAPERS = listOf(
            WallpaperOption("midnight_glass", "Midnight Glass", 0xFF0D1117, 0xFF161B22, 0xFF58A6FF),
            WallpaperOption("aurora_neon", "Aurora Neon", 0xFF0B192C, 0xFF1E3E62, 0xFF00FFCC),
            WallpaperOption("cyberpunk", "Cyberpunk", 0xFF1A0B2E, 0xFF3D155F, 0xFFFF007F),
            WallpaperOption("desert_sunset", "Desert Sunset", 0xFF2A1B0E, 0xFF5C3317, 0xFFFF9F43),
            WallpaperOption("deep_space", "Deep Space", 0xFF05050A, 0xFF110E2A, 0xFF9D4EDD)
        )
    }
}

data class WallpaperOption(
    val id: String,
    val name: String,
    val colorStart: Long,
    val colorMid: Long,
    val accentColor: Long
)
