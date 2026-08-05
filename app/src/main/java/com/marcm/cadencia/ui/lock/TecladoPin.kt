package com.marcm.cadencia.ui.lock

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcm.cadencia.ui.theme.Accent
import com.marcm.cadencia.ui.theme.Overdue

/** Los puntos que marcan cuántos dígitos llevas escritos. */
@Composable
fun PuntosPin(
    longitud: Int,
    escritos: Int,
    error: Boolean = false,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(longitud) { indice ->
            val lleno = indice < escritos
            val tamano by animateDpAsState(if (lleno) 15.dp else 12.dp, label = "punto")
            // Un punto vacío nunca se rellena, ni en rojo: si se rellenara, un fallo
            // parecería dejar el PIN escrito cuando en realidad está en blanco.
            val relleno = when {
                !lleno -> Color.Transparent
                error -> Overdue
                else -> Accent
            }
            val borde = when {
                lleno -> null
                error -> Overdue
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box(
                Modifier
                    .size(tamano)
                    .clip(CircleShape)
                    .background(relleno)
                    .then(
                        if (borde == null) Modifier
                        else Modifier.border(1.5.dp, borde, CircleShape)
                    )
            )
        }
    }
}

/**
 * Teclado numérico propio: no se usa el del sistema para no arrastrar teclados de
 * terceros ni sugerencias sobre lo que se escribe aquí.
 */
@Composable
fun TecladoPin(
    onDigito: (Char) -> Unit,
    onBorrar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    onBiometria: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf("123", "456", "789").forEach { fila ->
            FilaTeclas {
                fila.forEach { digito ->
                    Tecla(habilitado = habilitado, onClick = { onDigito(digito) }) {
                        Text(
                            digito.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        FilaTeclas {
            if (onBiometria != null) {
                Tecla(habilitado = habilitado, fondo = false, onClick = onBiometria) {
                    Icon(
                        Icons.Filled.Fingerprint,
                        "Desbloquear con huella o cara",
                        tint = Accent,
                        modifier = Modifier.size(30.dp)
                    )
                }
            } else {
                Box(Modifier.size(72.dp))
            }
            Tecla(habilitado = habilitado, onClick = { onDigito('0') }) {
                Text(
                    "0",
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Tecla(habilitado = habilitado, fondo = false, onClick = onBorrar) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    "Borrar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun FilaTeclas(contenido: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) { contenido() }
}

@Composable
private fun Tecla(
    habilitado: Boolean,
    onClick: () -> Unit,
    fondo: Boolean = true,
    contenido: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                if (fondo) MaterialTheme.colorScheme.surface else Color.Transparent
            )
            .clickable(enabled = habilitado, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { contenido() }
}
