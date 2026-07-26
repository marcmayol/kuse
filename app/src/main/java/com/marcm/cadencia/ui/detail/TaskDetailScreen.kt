package com.marcm.cadencia.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marcm.cadencia.domain.model.AnchorMode
import com.marcm.cadencia.domain.model.TaskWithContext
import com.marcm.cadencia.ui.components.DomainIconBox
import com.marcm.cadencia.ui.components.overdueLabel
import com.marcm.cadencia.ui.components.relativeFuture
import com.marcm.cadencia.ui.components.timeLabel
import com.marcm.cadencia.ui.components.weekdayDate
import com.marcm.cadencia.ui.theme.Accent
import com.marcm.cadencia.ui.theme.AccentInk
import com.marcm.cadencia.ui.theme.Late
import com.marcm.cadencia.ui.theme.Overdue
import com.marcm.cadencia.ui.theme.OverdueBorder
import com.marcm.cadencia.ui.theme.OverdueContainer
import com.marcm.cadencia.ui.theme.color
import com.marcm.cadencia.ui.theme.containerColor
import java.time.ZoneId

@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: TaskDetailViewModel = viewModel(factory = TaskDetailViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val item = state.item

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 40.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        "Borrar tarea",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sin salidas tempranas: un `return@Column` dejaría a medias los grupos que
            // Compose abre por composable y reventaría en la recomposición, que es justo
            // cuando llega la tarea y deja de ser null.
            if (item == null) {
                Text(
                    "Cargando…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                TaskDetailBody(item = item, state = state)
            }
        }

        // Barra inferior fija: editar + hecho hoy.
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item != null) {
                OutlinedButton(
                    onClick = { onEdit(item.id) },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(Icons.Filled.Edit, "Editar", modifier = Modifier.size(20.dp))
                }
                Button(
                    onClick = viewModel::toggleToday,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isDoneToday)
                            MaterialTheme.colorScheme.surface else Accent,
                        contentColor = if (state.isDoneToday)
                            MaterialTheme.colorScheme.onSurface else AccentInk
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(
                        if (state.isDoneToday) "Deshacer" else "Hecho hoy",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("¿Borrar la tarea?") },
            text = { Text("Se borrará también su historial. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text("Borrar", color = Overdue) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

/** Cuerpo del detalle una vez cargada la tarea: cabecera, métricas, ciclos e historial. */
@Composable
private fun ColumnScope.TaskDetailBody(item: TaskWithContext, state: DetailUiState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DomainIconBox(
            iconKey = item.iconKey,
            tint = item.domain.color(),
            container = item.domain.containerColor(),
            boxSize = 64.dp,
            iconSize = 32.dp,
            corner = 20.dp
        )
        Column(Modifier.weight(1f)) {
            Text(item.task.name, style = MaterialTheme.typography.headlineMedium)
            Text(
                item.metaLine(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (state.daysOverdue > 0) {
        OverdueBanner(state.daysOverdue)
    } else {
        NextDueRow(
            label = if (state.isDoneToday) {
                "Hecha hoy · vuelve a tocar ${relativeFuture(item.task.dueDate, state.today)}"
            } else {
                "Toca ${relativeFuture(item.task.dueDate, state.today)}"
            }
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard(
            value = state.streak.toString(),
            label = if (state.streak == 1) "vez seguida a tiempo" else "veces seguidas a tiempo",
            icon = {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    null,
                    tint = Accent,
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            value = state.onTimeRate?.let { "$it%" } ?: "—",
            label = "a tiempo",
            icon = { Icon(Icons.Filled.Check, null, tint = Accent, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.weight(1f)
        )
    }

    CyclesChart(state.cycles)

    AnchorNote(item.task.anchorMode, item.task.reminderTime?.let { timeLabel(it) })

    if (state.history.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "HISTORIAL",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.history.take(12).forEach { completion ->
                val date = completion.completedAt.atZone(ZoneId.systemDefault()).toLocalDate()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        weekdayDate(date),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (completion.onTime) "A tiempo" else "Con retraso",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (completion.onTime) Accent else Late
                    )
                }
            }
        }
    }
}

@Composable
private fun OverdueBanner(days: Long) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(OverdueContainer)
            .border(1.dp, OverdueBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Schedule, null, tint = Overdue, modifier = Modifier.size(22.dp))
        Column {
            Text(
                overdueLabel(days).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                color = Overdue
            )
            Text(
                "Sigue pendiente hasta que la marques.",
                style = MaterialTheme.typography.bodyMedium,
                color = Overdue.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun NextDueRow(label: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Schedule,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MetricCard(
    value: String,
    label: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        icon()
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Barras de los últimos ciclos: acento si fue a tiempo, ámbar si con retraso, gris el pendiente. */
@Composable
private fun CyclesChart(cycles: List<CycleBar>) {
    if (cycles.isEmpty()) return

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "ÚLTIMOS CICLOS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Las ranuras que aún no tienen historial se dejan marcadas: así el gráfico
            // mantiene su forma desde el primer día en lugar de dos barras enormes.
            repeat((CYCLE_SLOTS - cycles.size).coerceAtLeast(0)) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(0.12f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                )
            }
            cycles.forEach { cycle ->
                val (color, fraction) = when (cycle.outcome) {
                    CycleOutcome.ON_TIME -> Accent to 1f
                    CycleOutcome.LATE -> Late to 0.66f
                    CycleOutcome.PENDING -> MaterialTheme.colorScheme.outlineVariant to 0.3f
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(Accent, "A tiempo")
            LegendDot(Late, "Con retraso")
            LegendDot(MaterialTheme.colorScheme.outlineVariant, "En curso")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AnchorNote(anchorMode: AnchorMode, reminder: String?) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Recalcula desde: ${anchorMode.label.lowercase()}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            if (reminder != null) "Recordatorio a las $reminder" else "Sin recordatorio",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Ranuras del gráfico de ciclos; coincide con lo que calcula el ViewModel. */
private const val CYCLE_SLOTS = 6
