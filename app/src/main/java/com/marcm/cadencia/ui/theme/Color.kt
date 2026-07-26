package com.marcm.cadencia.ui.theme

import androidx.compose.ui.graphics.Color

// --- Marca ---
// El rojo se reserva para acciones, progreso y estado activo. Los ámbitos se
// distinguen por su propio color (DomainColors.kt), nunca por el rojo.
val Accent = Color(0xFFD64C4C)
val AccentInk = Color(0xFF101316) // tinta sobre el rojo
val AccentSoft = Color(0x26D64C4C) // ~15%, indicadores y contenedores
val AccentDark = Color(0xFFB83A3A) // legible sobre fondo claro

// --- Superficies modo oscuro (la base del sistema) ---
val DarkScreen = Color(0xFF101316)
val DarkSurface = Color(0xFF1A1E20) // tarjeta
val DarkSurfaceLow = Color(0xFF16191B) // superficie hundida
val DarkNavBar = Color(0xFF16191B)
val DarkBorder = Color(0xFF2A2F31)
val DarkOutline = Color(0xFF3C4245)

// --- Texto modo oscuro ---
val DarkOnSurface = Color(0xFFE6E8E8)
val DarkOnSurfaceVariant = Color(0xFF8A9290)

// --- Atrasado / error ---
val Overdue = Color(0xFFFF8F88)
val OverdueContainer = Color(0x1FFF8F88) // ~12%
val OverdueBorder = Color(0x4DFF8F88)

// --- Retraso (barras del historial: hecho, pero tarde) ---
val Late = Color(0xFFF0B95E)

// --- Superficies modo claro ---
val LightScreen = Color(0xFFF4F2EC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceLow = Color(0xFFECEAE3)
val LightBorder = Color(0xFFD9D6CD)
val LightOnSurface = Color(0xFF23262A)
val LightOnSurfaceVariant = Color(0xFF6B6F73)
