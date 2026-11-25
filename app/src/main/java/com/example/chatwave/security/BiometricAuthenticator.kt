package com.example.chatwave.security

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationResult
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.Signature

class BiometricAuthenticator(private val context: Context) {
  enum class Availability {
    AVAILABLE,
    NONE_ENROLLED,
    NO_HARDWARE,
    LOCKED_OUT,
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED,
    UNKNOWN
  }

  fun strongAvailability(): Availability {
    val status = BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    return when (status) {
      BiometricManager.BIOMETRIC_SUCCESS -> Availability.AVAILABLE
      BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NONE_ENROLLED
      BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Availability.NO_HARDWARE
      BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Availability.LOCKED_OUT
      BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> Availability.SECURITY_UPDATE_REQUIRED
      BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> Availability.UNSUPPORTED
      else -> Availability.UNKNOWN
    }
  }

  fun isStrongBiometricAvailable(): Boolean = strongAvailability() == Availability.AVAILABLE

  fun authenticate(
    activity: FragmentActivity,
    title: String = "Unlock ChatWave",
    subtitle: String = "Use your device biometric",
    crypto: Signature? = null,
    onSuccess: (AuthenticationResult) -> Unit,
    onError: (Int, String) -> Unit,
    onPinFallback: () -> Unit
  ) {
    if (strongAvailability() != Availability.AVAILABLE) {
      onPinFallback()
      return
    }
    val prompt = BiometricPrompt(
      activity,
      ContextCompat.getMainExecutor(activity),
      object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: AuthenticationResult) = onSuccess(result)
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError(errorCode, errString.toString())
        override fun onAuthenticationFailed() = Unit
      }
    )
    val builder = BiometricPrompt.PromptInfo.Builder()
      .setTitle(title)
      .setSubtitle(subtitle)
      .setConfirmationRequired(true)
      .setNegativeButtonText("Use PIN")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    }
    val info = builder.build()
    if (crypto != null) prompt.authenticate(info, BiometricPrompt.CryptoObject(crypto)) else prompt.authenticate(info)
  }

  fun enrollmentIntent(): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      return Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
        putExtra(
          Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
          BiometricManager.Authenticators.BIOMETRIC_STRONG
        )
      }
    }
    @Suppress("DEPRECATION")
    return Intent(Settings.ACTION_FINGERPRINT_ENROLL)
  }
}
