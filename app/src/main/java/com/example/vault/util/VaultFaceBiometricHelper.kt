package com.example.vault.util

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object VaultFaceBiometricHelper {

    enum class BiometricStatus {
        AVAILABLE,
        NOT_ENROLLED,
        NO_HARDWARE,
        HARDWARE_UNAVAILABLE,
        UNAVAILABLE
    }

    enum class BiometricHardwareType {
        FACE,
        FINGERPRINT,
        BIOMETRIC,
        NONE
    }

    /**
     * Detects specific biometric hardware present on device
     */
    fun getHardwareType(context: Context): BiometricHardwareType {
        val pm = context.packageManager
        val hasFace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.hasSystemFeature(PackageManager.FEATURE_FACE) ||
                    pm.hasSystemFeature("android.hardware.biometrics.face")
        } else {
            pm.hasSystemFeature("android.hardware.biometrics.face")
        }

        val hasFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)

        return when {
            hasFace && !hasFingerprint -> BiometricHardwareType.FACE
            hasFingerprint && !hasFace -> BiometricHardwareType.FINGERPRINT
            hasFace && hasFingerprint -> BiometricHardwareType.BIOMETRIC
            else -> BiometricHardwareType.NONE
        }
    }

    /**
     * Checks if biometric authentication can be performed
     */
    fun canAuthenticate(context: Context): BiometricStatus {
        return try {
            val biometricManager = BiometricManager.from(context)
            when (biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
                else -> BiometricStatus.UNAVAILABLE
            }
        } catch (_: Exception) {
            BiometricStatus.UNAVAILABLE
        }
    }

    /**
     * Unwraps context to find FragmentActivity
     */
    fun findFragmentActivity(context: Context): FragmentActivity? {
        var curr = context
        while (curr is ContextWrapper) {
            if (curr is FragmentActivity) {
                return curr
            }
            curr = curr.baseContext
        }
        return null
    }

    private var activePrompt: BiometricPrompt? = null

    /**
     * Cancels any active biometric prompt to prevent lifecycle-related callbacks.
     */
    fun cancelActiveAuthentication() {
        try {
            activePrompt?.cancelAuthentication()
        } catch (_: Exception) {}
        activePrompt = null
    }

    /**
     * Prompts for Biometric (Face / Fingerprint) authentication
     */
    fun authenticateBiometric(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val status = canAuthenticate(activity)
        if (status != BiometricStatus.AVAILABLE) {
            val msg = when (status) {
                BiometricStatus.NOT_ENROLLED -> "No biometric enrolled on this device"
                BiometricStatus.NO_HARDWARE -> "Biometric hardware is not available on this device"
                BiometricStatus.HARDWARE_UNAVAILABLE -> "Biometric hardware is temporarily unavailable"
                else -> "Biometric authentication is not supported on this device"
            }
            onError(BiometricPrompt.ERROR_HW_UNAVAILABLE, msg)
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val hwType = getHardwareType(activity)
        val titleText = when (hwType) {
            BiometricHardwareType.FACE -> "Face Unlock"
            BiometricHardwareType.FINGERPRINT -> "Fingerprint Unlock"
            else -> "Biometric Unlock"
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titleText)
            .setSubtitle("Authenticate to access Secret Vault")
            .setDescription("Confirm your biometric identity to unlock.")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    activePrompt = null
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    activePrompt = null
                    onError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        activePrompt = biometricPrompt

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            activePrompt = null
            onError(BiometricPrompt.ERROR_UNABLE_TO_PROCESS, e.message ?: "Authentication failed")
        }
    }

    // Retain backwards-compatible alias
    fun authenticateFace(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit = {}
    ) = authenticateBiometric(activity, onSuccess, onError, onFailed)
}
