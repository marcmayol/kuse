package com.marcm.cadencia.ui.all

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marcm.cadencia.domain.model.Category
import com.marcm.cadencia.domain.model.TaskStatus
import com.marcm.cadencia.ui.components.DomainFilterChips
import com.marcm.cadencia.ui.components.DomainIconBox
import com.marcm.cadencia.ui.components.shortDate
import com.marcm.cadencia.ui.theme.Accent
import com.marcm.cadencia.ui.theme.Overdue
import com.marcm.cadencia.ui.theme.color
import com.marcm.cadencia.ui.theme.containerColor

/**
 * Inventario de todas las tareas activas, agrupadas por ámbito y con filtro propio.
 * A diferencia de Hoy y del Plan, aquí no manda el calendario: se ve todo lo que hay
 * montado aunque no toque en semanas. Sólo se consulta; para marcar hecho se entra al
 * detalle.
 */
@Composable
fun AllTasksScreen(
    contentPadding: PaddingValues,
    onTaskClick: (Long) -> Unit,
    viewModel: AllTasksViewModel = viewModel(factory = AllTasksViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 14.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item("titulo") {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                Text("Todas", style = MaterialTheme.typography.displaySmall)
                Text(
                    subtitle(state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.domains.size > 1) {
            item("filtros") {
                DomainFilterChips(
                    domains = state.domains,
                    selectedDomainId = state.filterDomainId,
                    onSelect = viewModel::setFilter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 2.dp)
                )
            }
        }

        state.groups.forEach { group ->
            // Con un ámbito filtrado, su chip ya dice de qué va la lista: la cabecera de
            // grupo sólo repetiría el nombre.
            if (state.filterDomainId == null) {
                item("h-${group.domain.id}") {
                    GroupHeader(
                        name = group.domain.name,
                        count = group.rows.size,
                        color = group.domain.color()
                    )
                }
            }
            items(group.rows.size) { index ->
                val row = group.rows[index]
                AllTaskRowCard(
                    row = row,
                    onClick = { onTaskClick(row.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 3.dp)
                )
            }
        }

        if (state.loaded && state.isEmpty) {
            item("vacio") {
                Text(
                    if (state.filterDomainId != null) "Este ámbito no tiene tareas todavía."
                    else "Aún no hay tareas. Créalas desde Hoy con el botón +.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp)
                )
            }
        }
    }
}

private fun subtitle(state: AllTasksUiState): String = when {
    !state.loaded || state.visibleCount == 0 -> "Todo lo que tienes montado"
    state.overdueCount > 0 ->
        "${state.visibleCount} ${tareas(state.visibleCount)} · ${state.overdueCount} con atraso"
    else -> "${state.visibleCount} ${tareas(state.visibleCount)}, ninguna con atraso"
}

private fun tareas(count: Int): String = if (count == 1) "tarea" else "tareas"

/** Cabecera de grupo: punto del color del ámbito, nombre y recuento. */
@Composable
private fun GroupHeader(name: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 4.dp)
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            name.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun AllTaskRowCard(
    row: AllTaskRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = row.item

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(13.dp)
    ) {
        DomainIconBox(
            iconKey = item.iconKey,
            tint = item.domain.color(),
            container = item.domain.containerColor(),
            boxSize = 38.dp,
            iconSize = 19.dp,
            corner = 13.dp
        )
        Column(Modifier.weight(1f)) {
            Text(
                item.task.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                // Dentro de su ámbito, repetirlo sobra: manda la categoría, si no es la
                // genérica, y la cadencia.
                if (item.category.key == Category.GENERAL_KEY) item.task.recurrence.label()
                else "${item.category.name} · ${item.task.recurrence.label()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DueLabel(row)
    }
}

/** Etiqueta de la derecha: cuándo toca, o cuánto atraso lleva. */
@Composable
private fun DueLabel(row: AllTaskRow) {
    val (text, color) = when (row.status) {
        TaskStatus.OVERDUE -> "+${row.daysOverdue} d" to Overdue
        TaskStatus.DUE_TODAY -> "Hoy" to Accent
        TaskStatus.DONE -> "Hecha" to MaterialTheme.colorScheme.outline
        TaskStatus.FUTURE -> futureLabel(row) to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1
    )
}

/** Cerca, en días; lejos, la fecha: "mañana", "en 5 días", "14 sep". */
private fun futureLabel(row: AllTaskRow): String = when {
    row.daysUntilDue == 1L -> "Mañana"
    row.daysUntilDue <= 7L -> "En ${row.daysUntilDue} d"
    else -> shortDate(row.item.task.dueDate)
}
