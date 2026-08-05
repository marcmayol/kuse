package com.marcm.cadencia.ui.lock

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marcm.cadencia.security.Biometria
import com.marcm.cadencia.ui.theme.Accent
import com.marcm.cadencia.ui.theme.Overdue

/**
 * Puerta de la app: se dibuja encima de todo mientras el bloqueo esté echado.
 * No es un destino de navegación para que ninguna ruta pueda saltársela.
 */
@Composable
fun LockScreen(viewModel: LockViewModel = viewModel(factory = LockViewModel.Factory)) {
    val ajustes by viewModel.ajustes.collectAsStateWithLifecycle()
    val pin by viewModel.pin.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val mostrarPista by viewModel.mostrarPista.collectAsStateWithLifecycle()
    val espera by viewModel.esperaSegundos.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = remember(context) { context.buscarFragmentActivity() }
    var errorBiometria by remember { mutableStateOf<String?>(null) }
    var yaSePidioLaHuella by remember { mutableStateOf(false) }

    val cargado = ajustes != null
    val biometriaActiva = ajustes?.biometria == true

    val pedirBiometria: () -> Unit = {
        val fa = activity
        if (fa != null && biometriaActiva && Biometria.disponible(context)) {
            Biometria.pedir(
                activity = fa,
                onExito = { viewModel.desbloquearPorBiometria() },
                onFallo = { mensaje -> errorBiometria = mensaje }
            )
        }
    }

    // Al aparecer la pantalla se ofrece la huella una sola vez: si el usuario la
    // cancela, se queda el teclado sin volver a saltarle el diálogo encima.
    LaunchedEffect(biometriaActiva) {
        if (biometriaActiva && !yaSePidioLaHuella) {
            yaSePidioLaHuella = true
            pedirBiometria()
        }
    }

    // Atrás no puede llevar al contenido: manda la app al fondo, como el bloqueo del móvil.
    BackHandler { activity?.moveTaskToBack(true) }

    // Surface y no Box: al dibujarse fuera del NavHost no hereda el color de contenido
    // del tema, y los textos saldrían en negro sobre el fondo oscuro.
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            // Se dibuja encima del contenido, así que tiene que tragarse todos los
            // gestos: si no, un toque a ciegas alcanzaría los botones de debajo.
            .pointerInput(Unit) { detectTapGestures { } }
            .pointerInput(Unit) { detectDragGestures { _, _ -> } }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Text("癖", style = MaterialTheme.typography.displayMedium, color = Accent)
            Text(
                "Kuse",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )

            // El hueco va aquí: así los puntos quedan pegados al teclado, que es donde
            // están los ojos y el pulgar.
            Spacer(Modifier.weight(1f))
            Text(
                if (espera > 0) "Demasiados intentos" else "Introduce tu PIN",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(22.dp))
            PuntosPin(
                longitud = ajustes?.longitudPin ?: 4,
                escritos = pin.length,
                error = error
            )

            Spacer(Modifier.height(18.dp))
            MensajeDeAyuda(
                espera = espera,
                error = error,
                errorBiometria = errorBiometria,
                pista = ajustes?.pista?.takeIf { mostrarPista && it.isNotBlank() }
            )

            Spacer(Modifier.height(20.dp))

            TecladoPin(
                onDigito = { digito ->
                    errorBiometria = null
                    viewModel.escribir(digito)
                },
                onBorrar = viewModel::borrar,
                habilitado = cargado && espera == 0,
                onBiometria = if (biometriaActiva && Biometria.disponible(context)) {
                    { errorBiometria = null; pedirBiometria() }
                } else null
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MensajeDeAyuda(
    espera: Int,
    error: Boolean,
    errorBiometria: String?,
    pista: String?,
) {
    val texto: String?
    val color = when {
        espera > 0 || error || errorBiometria != null -> Overdue
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    texto = when {
        espera > 0 -> "Espera $espera s antes de volver a probar"
        errorBiometria != null -> errorBiometria
        error && pista != null -> "PIN incorrecto · Tu pista: $pista"
        error -> "PIN incorrecto"
        pista != null -> "Tu pista: $pista"
        else -> null
    }

    Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.TopCenter) {
        if (texto != null) {
            Text(
                texto,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun Context.buscarFragmentActivity(): FragmentActivity? {
    var actual: Context? = this
    while (actual is ContextWrapper) {
        if (actual is FragmentActivity) return actual
        actual = actual.baseContext
    }
    return null
}
