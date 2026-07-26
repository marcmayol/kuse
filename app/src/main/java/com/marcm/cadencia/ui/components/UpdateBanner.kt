package com.marcm.cadencia.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.marcm.actualizador.EstadoActualizacion
import com.marcm.cadencia.ui.theme.Accent
import com.marcm.cadencia.ui.theme.AccentInk
import com.marcm.cadencia.ui.theme.AccentSoft

/**
 * Banner no bloqueante de actualización, bajo la cabecera de Hoy.
 *
 * Solo habla cuando hay algo que hacer o algo en marcha: versión nueva, descarga,
 * verificación o instalación. Los errores y el "estás al día" no aparecen aquí —
 * eso vive en Ajustes, donde el usuario ha pedido la comprobación a mano.
 */
@Composable
fun BannerActualizacion(
    estado: EstadoActualizacion,
    onActualizar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = estado is EstadoActualizacion.Disponible ||
        estado is EstadoActualizacion.Descargando ||
        estado is EstadoActualizacion.Verificando ||
        estado is EstadoActualizacion.PidiendoPermiso ||
        estado is EstadoActualizacion.Instalando

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, AccentSoft, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (estado) {
                is EstadoActualizacion.Disponible -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppLogo(size = 30.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Kuse ${estado.info.versionName} disponible",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (estado.info.notas.isNotBlank()) {
                            Text(
                                estado.info.notas,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = onActualizar,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = AccentInk
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Actualizar", style = MaterialTheme.typography.labelLarge)
                    }
                }

                is EstadoActualizacion.Descargando -> Progreso(
                    titulo = "Descargando la actualización",
                    detalle = "${estado.porcentaje} %",
                    fraccion = estado.porcentaje / 100f
                )

                EstadoActualizacion.Verificando -> Progreso(
                    titulo = "Comprobando el archivo",
                    detalle = "Verificando que la descarga es íntegra",
                    fraccion = null
                )

                EstadoActualizacion.PidiendoPermiso -> Progreso(
                    titulo = "Falta un permiso",
                    detalle = "Autoriza a Kuse a instalar aplicaciones y volvemos aquí",
                    fraccion = null
                )

                EstadoActualizacion.Instalando -> Progreso(
                    titulo = "Instalando",
                    detalle = "Kuse se reiniciará al terminar",
                    fraccion = null
                )

                else -> Unit
            }
        }
    }
}

@Composable
private fun Progreso(titulo: String, detalle: String, fraccion: Float?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Text(
            detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (fraccion != null) {
            LinearProgressIndicator(
                progress = { fraccion.coerceIn(0f, 1f) },
                color = Accent,
                trackColor = AccentSoft,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )
        } else {
            LinearProgressIndicator(
                color = Accent,
                trackColor = AccentSoft,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )
        }
    }
}
