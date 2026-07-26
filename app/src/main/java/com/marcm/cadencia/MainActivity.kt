package com.marcm.cadencia

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marcm.cadencia.settings.ThemeMode
import com.marcm.cadencia.ui.navigation.KuseNavHost
import com.marcm.cadencia.ui.navigation.Routes
import com.marcm.cadencia.ui.theme.KuseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settings = (application as KuseApp).container.settingsRepository

        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.DARK)
            val onboardingDone by settings.onboardingDone
                .collectAsStateWithLifecycle<Boolean?>(initialValue = null)

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
                if (done == null) {
                    // Estado inicial mientras se lee la preferencia.
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                } else {
                    KuseNavHost(
                        startDestination = if (done) Routes.TODAY else Routes.ONBOARDING
                    )
                }
            }
        }
    }
}
