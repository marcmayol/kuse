package com.marcm.cadencia.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Huella o cara como atajo del PIN. No cifra nada: el PIN sigue siendo la única
 * credencial: la biometría sólo evita teclearlo. Por eso basta con BIOMETRIC_WEAK
 * (que ya engloba a BIOMETRIC_STRONG) y no hace falta CryptoObject.
 */
object Biometria {

    private const val AUTENTICADORES = BiometricManager.Authenticators.BIOMETRIC_WEAK

    /** true si el móvil tiene sensor y el usuario ha registrado alguna biometría. */
    fun disponible(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTENTICADORES) ==
            BiometricManager.BIOMETRIC_SUCCESS

    fun pedir(
        activity: FragmentActivity,
        onExito: () -> Unit,
        onFallo: (String?) -> Unit = {},
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onExito()
                }

                override fun onAuthenticationError(codigo: Int, mensaje: CharSequence) {
                    // Cancelar a propósito no es un error que merezca mensaje.
                    val silencioso = codigo == BiometricPrompt.ERROR_USER_CANCELED ||
                        codigo == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        codigo == BiometricPrompt.ERROR_CANCELED
                    onFallo(if (silencioso) null else mensaje.toString())
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear Kuse")
            .setSubtitle("Usa tu huella o tu cara")
            .setNegativeButtonText("Usar el PIN")
            .setAllowedAuthenticators(AUTENTICADORES)
            .setConfirmationRequired(false)
            .build()

        prompt.authenticate(info)
    }
}
