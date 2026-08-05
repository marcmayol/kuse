package com.marcm.cadencia

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.marcm.actualizador.Modo
import com.marcm.cadencia.settings.ThemeMode
import com.marcm.cadencia.ui.lock.LockScreen
import com.marcm.cadencia.ui.navigation.KuseNavHost
import com.marcm.cadencia.ui.navigation.Routes
import com.marcm.cadencia.ui.theme.KuseTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * FragmentActivity (y no ComponentActivity a secas) porque BiometricPrompt lo exige
 * para poder mostrarse; el resto de la app no nota la diferencia.
 */
class MainActivity : FragmentActivity() {

    private val appLock get() = (application as KuseApp).container.appLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settings = (application as KuseApp).container.settingsRepository

        // Comprobación de actualizaciones al abrir: con retardo para no competir con
        // el arranque, y muda ante cualquier fallo (modo automático).
        lifecycleScope.launch {
            delay(4_000)
            (application as KuseApp).actualizador.comprobar(Modo.AUTOMATICO)
        }

        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.DARK)
            val onboardingDone by settings.onboardingDone
                .collectAsStateWithLifecycle<Boolean?>(initialValue = null)
            val bloqueado by appLock.bloqueado.collectAsStateWithLifecycle()

            // Pide permiso de notificaciones una vez en Android 13+.
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* el resultado lo respeta NotificationHelper al publicar */ }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            KuseTheme(themeMode = themeMode) {
                val done = onboardingDone
                val cerrado = bloqueado
                if (done == null || cerrado == null) {
                    // Estado inicial mientras se leen las preferencias. Mientras no se
                    // sepa si hay bloqueo, no se dibuja nada del contenido.
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                } else {
                    KuseNavHost(
                        startDestination = if (done) Routes.TODAY else Routes.ONBOARDING
                    )
                    // Encima de todo: ninguna ruta ni deep link puede esquivarla.
                    if (cerrado) LockScreen()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appLock.alVolverAlFrente()
    }

    override fun onStop() {
        super.onStop()
        // Girar la pantalla recrea la Activity, pero eso no es salir de la app.
        if (!isChangingConfigurations) appLock.alIrAlFondo()
    }

    override fun onResume() {
        super.onResume()
        // Reanuda el flujo si el usuario acaba de conceder el permiso de instalación,
        // o deshace el "Instalando…" cuando la instalación se delegó al sistema.
        (application as KuseApp).actualizador.onPermisoQuizaConcedido()
    }
}
