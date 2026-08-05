package com.marcm.cadencia.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marcm.cadencia.security.PinHasher
import com.marcm.cadencia.ui.theme.Accent
import com.marcm.cadencia.ui.theme.AccentInk
import com.marcm.cadencia.ui.theme.Overdue

/** Crear o cambiar el PIN: elegirlo, repetirlo y dejar una pista. */
@Composable
fun PinSetupScreen(
    onListo: () -> Unit,
    onCancelar: () -> Unit,
    viewModel: PinSetupViewModel = viewModel(factory = PinSetupViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.terminado) {
        if (estado.terminado) onListo()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancelar) { Icon(Icons.Filled.Close, "Cancelar") }
                Spacer(Modifier.width(4.dp))
                Text("Bloqueo de la app", style = MaterialTheme.typography.titleLarge)
            }

            Spacer(Modifier.height(28.dp))
            Text(
                when (estado.paso) {
                    PasoPin.ELEGIR -> "Elige un PIN"
                    PasoPin.REPETIR -> "Repítelo"
                    PasoPin.PISTA -> "Deja una pista"
                },
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                when (estado.paso) {
                    PasoPin.ELEGIR -> "Entre ${PinHasher.LONGITUD_MINIMA} y " +
                        "${PinHasher.LONGITUD_MAXIMA} dígitos"
                    PasoPin.REPETIR -> "Para asegurarnos de que no hay erratas"
                    PasoPin.PISTA -> "Se te enseñará tras varios intentos fallidos. " +
                        "No sirve para entrar: si olvidas el PIN, la única salida es " +
                        "reinstalar la app y perder los datos."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(Modifier.height(26.dp))

            if (estado.paso == PasoPin.PISTA) {
                PasoDeLaPista(
                    pista = estado.pista,
                    guardando = estado.guardando,
                    onPista = viewModel::setPista,
                    onGuardar = viewModel::guardar
                )
            } else {
                PasoDelPin(
                    pin = estado.pin,
                    longitudObjetivo = estado.longitudObjetivo,
                    error = estado.error,
                    puedeContinuar = estado.paso == PasoPin.ELEGIR &&
                        PinHasher.esPinValido(estado.pin),
                    mostrarBotonContinuar = estado.paso == PasoPin.ELEGIR,
                    onDigito = viewModel::escribir,
                    onBorrar = viewModel::borrar,
                    onContinuar = viewModel::continuar
                )
            }
        }
    }
}

@Composable
private fun PasoDelPin(
    pin: String,
    longitudObjetivo: Int,
    error: String?,
    puedeContinuar: Boolean,
    mostrarBotonContinuar: Boolean,
    onDigito: (Char) -> Unit,
    onBorrar: () -> Unit,
    onContinuar: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Al elegir, los puntos crecen con lo escrito porque la longitud aún no está
        // fijada; al repetir ya se sabe cuántos dígitos faltan y se muestran todos.
        PuntosPin(
            longitud = if (longitudObjetivo > 0) longitudObjetivo
            else maxOf(pin.length, PinHasher.LONGITUD_MINIMA),
            escritos = pin.length,
            error = error != null
        )

        Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
            if (error != null) {
                Text(
                    error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Overdue,
                    textAlign = TextAlign.Center
                )
            }
        }

        TecladoPin(onDigito = onDigito, onBorrar = onBorrar)

        Spacer(Modifier.height(20.dp))
        if (mostrarBotonContinuar) {
            Button(
                onClick = onContinuar,
                enabled = puedeContinuar,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = AccentInk
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Continuar")
            }
        }
    }
}

@Composable
private fun PasoDeLaPista(
    pista: String,
    guardando: Boolean,
    onPista: (String) -> Unit,
    onGuardar: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = pista,
            onValueChange = onPista,
            label = { Text("Pista (opcional)") },
            placeholder = { Text("El año de siempre") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "No escribas aquí el PIN: la pista se ve sin desbloquear la app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onGuardar,
            enabled = !guardando,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = AccentInk
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(if (guardando) "Guardando…" else "Activar el bloqueo")
        }
        OutlinedButton(
            onClick = { onPista(""); onGuardar() },
            enabled = !guardando,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sin pista")
        }
    }
}
