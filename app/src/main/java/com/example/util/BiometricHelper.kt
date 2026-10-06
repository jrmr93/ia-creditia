package com.example.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal

object BiometricHelper {

    fun isBiometricHardwareAvailable(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val biometricManager = context.getSystemService(BiometricManager::class.java)
            val res = biometricManager?.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            res != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE && res != BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        } else {
            false
        }
    }

    fun showNativeBiometricPrompt(
        activity: Activity,
        title: String = "Creditia - Acceso Seguro",
        subtitle: String = "Usa tu huella para acceder a la app",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onUsePin: () -> Unit
    ): CancellationSignal? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val cancellationSignal = CancellationSignal()
                val prompt = BiometricPrompt.Builder(activity)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setDescription("Autenticación biométrica nativa del sistema operativo")
                    .setNegativeButton("Ingresar con PIN", activity.mainExecutor) { _, _ ->
                        onUsePin()
                    }
                    .build()

                prompt.authenticate(
                    cancellationSignal,
                    activity.mainExecutor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            // Error code 10 = BIOMETRIC_ERROR_USER_CANCELED, 13 = BIOMETRIC_ERROR_NEGATIVE_BUTTON, 5 = BIOMETRIC_ERROR_CANCELED
                            if (errorCode == 10 || errorCode == 13 || errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED || errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED) {
                                onUsePin()
                            } else {
                                onError(errString?.toString() ?: "Biometría no disponible")
                            }
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            onError("Huella no reconocida. Intenta de nuevo o usa tu PIN.")
                        }
                    }
                )
                cancellationSignal
            } catch (e: Exception) {
                onError(e.message ?: "Sensor no disponible")
                onUsePin()
                null
            }
        } else {
            onUsePin()
            null
        }
    }
}

