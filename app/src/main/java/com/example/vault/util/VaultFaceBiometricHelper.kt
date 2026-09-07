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

    /**
     * Checks if the device has face biometric hardware support
     */
    fun hasFaceHardware(context: Context): Boolean {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.hasSystemFeature(PackageManager.FEATURE_FACE) ||
                    pm.hasSystemFeature("android.hardware.biometrics.face")
        } else {
            pm.hasSystemFeature("android.hardware.biometrics.face")
        }
    }

    /**
     * Checks if biometric authentication can be performed
     */
    fun canAuthenticate(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            else -> BiometricStatus.UNAVAILABLE
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

    /**
     * Prompts for Face Unlock Biometric authentication
     */
    fun authenticateFace(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Face Unlock")
            .setSubtitle("Authenticate your face to open Secret Vault")
            .setDescription("Look at the camera for instant face verification.")
            .setNegativeButtonText("Use Vault PIN")
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
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }

    enum class BiometricStatus {
        AVAILABLE,
        NOT_ENROLLED,
        NO_HARDWARE,
        HARDWARE_UNAVAILABLE,
        UNAVAILABLE
    }
}
