package com.marcm.cadencia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marcm.cadencia.R

/**
 * Pinta el icono asociado a una `iconKey` del diseño (nombres de Material Symbols).
 * "dentistry" usa un vector propio porque no existe en Material Icons.
 */
@Composable
fun TaskIcon(iconKey: String, tint: Color, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    val m = modifier.size(size)
    if (iconKey == "dentistry") {
        Icon(painterResource(R.drawable.ic_tooth), null, tint = tint, modifier = m)
    } else {
        Icon(iconVectorFor(iconKey), null, tint = tint, modifier = m)
    }
}

/**
 * Icono dentro de su contenedor tintado, tal como aparece en las filas de tarea y en
 * las tarjetas de ámbito. El contenedor usa el color del ámbito al 15%.
 */
@Composable
fun DomainIconBox(
    iconKey: String,
    tint: Color,
    container: Color,
    boxSize: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    corner: Dp = 15.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(boxSize)
            .clip(RoundedCornerShape(corner))
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        TaskIcon(iconKey, tint, iconSize)
    }
}

private fun iconVectorFor(iconKey: String): ImageVector = when (iconKey) {
    // Ámbitos
    "self_care" -> Icons.Filled.Spa
    "home" -> Icons.Filled.Home
    "fitness_center" -> Icons.Filled.FitnessCenter
    "pets" -> Icons.Filled.Pets
    // Categorías
    "content_cut" -> Icons.Filled.ContentCut
    "face" -> Icons.Filled.Face
    "water_drop" -> Icons.Filled.WaterDrop
    "local_laundry_service" -> Icons.Filled.LocalLaundryService
    "bed" -> Icons.Filled.Bed
    "cleaning_services" -> Icons.Filled.CleaningServices
    "potted_plant" -> Icons.Filled.LocalFlorist
    "directions_run" -> Icons.Filled.DirectionsRun
    "self_improvement" -> Icons.Filled.SelfImprovement
    "vaccines" -> Icons.Filled.Vaccines
    "mop" -> Icons.Filled.Grass
    // Extras para ámbitos propios
    "restaurant" -> Icons.Filled.Restaurant
    "work" -> Icons.Filled.Work
    "menu_book" -> Icons.Filled.MenuBook
    "music_note" -> Icons.Filled.MusicNote
    else -> Icons.Filled.Check
}

/** Iconos ofrecidos al crear un ámbito propio o al personalizar una tarea. */
val selectableIconKeys: List<String> = listOf(
    "self_care", "home", "fitness_center", "pets",
    "cleaning_services", "potted_plant", "restaurant", "work",
    "menu_book", "music_note", "water_drop", "vaccines"
)
