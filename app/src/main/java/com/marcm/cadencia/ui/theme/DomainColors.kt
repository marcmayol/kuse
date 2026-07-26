package com.marcm.cadencia.ui.theme

import androidx.compose.ui.graphics.Color
import com.marcm.cadencia.domain.model.Domain

/**
 * Color de un ámbito: viene como hex desde la base (también para los ámbitos que crea
 * el usuario), así que se parsea aquí, en la capa que pinta.
 *
 * El tinte al 15% es el fondo de los contenedores de icono y de los puntos del plan.
 */
fun Domain.color(): Color = parseHexColor(colorHex)

fun Domain.containerColor(): Color = color().copy(alpha = TINT_ALPHA)

const val TINT_ALPHA = 0.15f

/** Paleta ofrecida al crear un ámbito propio, en el mismo tono que los de fábrica. */
val customDomainPalette: List<String> = listOf(
    "#6FD8C0", // verde agua
    "#F0B95E", // ámbar
    "#8FB6FF", // azul
    "#C9A6FF", // lila
    "#F58FA8", // rosa
    "#9EDB7A", // verde
    "#7FD3E8", // cian
    "#E0A87E"  // terracota
)

/**
 * Convierte "#RRGGBB" o "#AARRGGBB" en Color sin depender de android.graphics, para que
 * las previews de Compose también lo resuelvan. Si el valor viene mal, cae en el gris
 * de texto secundario en lugar de romper la pantalla.
 */
fun parseHexColor(hex: String, fallback: Color = DarkOnSurfaceVariant): Color {
    val clean = hex.removePrefix("#")
    val value = clean.toLongOrNull(16) ?: return fallback
    return when (clean.length) {
        6 -> Color(value or 0xFF000000L)
        8 -> Color(value)
        else -> fallback
    }
}
