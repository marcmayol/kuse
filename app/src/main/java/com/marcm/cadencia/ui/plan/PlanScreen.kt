package com.marcm.cadencia.ui.plan

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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marcm.cadencia.domain.model.TaskWithContext
import com.marcm.cadencia.ui.components.DomainIconBox
import com.marcm.cadencia.ui.components.dayHeader
import com.marcm.cadencia.ui.components.weekdayInitial
import com.marcm.cadencia.ui.theme.Accent
import com.marcm.cadencia.ui.theme.AccentInk
import com.marcm.cadencia.ui.theme.color
import com.marcm.cadencia.ui.theme.containerColor

@Composable
fun PlanScreen(
    contentPadding: PaddingValues,
    onTaskClick: (Long) -> Unit,
    viewModel: PlanViewModel = viewModel(factory = PlanViewModel.Factory)
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
                Text("Plan", style = MaterialTheme.typography.displaySmall)
                Text(
                    "Lo que viene en los próximos 14 días",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item("tira") {
            WeekStrip(state.strip, Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
        }

        if (state.dailyCount > 0) {
            item("diarias") {
                DailyCollapsedRow(
                    count = state.dailyCount,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                )
            }
        }

        state.days.forEach { day ->
            item("h-${day.date}") {
                Text(
                    dayHeader(day.date, state.today).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 22.dp, top = 16.dp, bottom = 4.dp)
                )
            }
            items(day.tasks.size) { index ->
                val item = day.tasks[index]
                PlanRow(
                    item = item,
                    onClick = { onTaskClick(item.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 3.dp)
                )
            }
        }

        if (state.loaded && state.days.isEmpty()) {
            item("vacio") {
                Text(
                    if (state.dailyCount > 0) "Sólo tienes rutinas diarias por delante."
                    else "No hay nada programado en las próximas dos semanas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp)
                )
            }
        }
    }
}

/** Tira de 7 días con el número, la inicial y un punto por ámbito con algo ese día. */
@Composable
private fun WeekStrip(days: List<StripDay>, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        days.forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (day.isToday) Accent else MaterialTheme.colorScheme.surface
                    )
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    weekdayInitial(day.date.dayOfWeek),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (day.isToday) AccentInk.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (day.isToday) AccentInk else MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.height(6.dp)
                ) {
                    day.domains.take(4).forEach { domain ->
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (day.isToday) AccentInk.copy(alpha = 0.55f) else domain.color()
                                )
                        )
                    }
                }
            }
        }
    }
}

/** Fila discontinua que colapsa las tareas diarias. */
@Composable
private fun DailyCollapsedRow(count: Int, modifier: Modifier = Modifier) {
    val stroke = MaterialTheme.colorScheme.outline

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                drawRoundRect(
                    brush = SolidColor(stroke),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                )
            }
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Text(
            "+ $count ${if (count == 1) "diaria de rutina" else "diarias de rutina"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlanRow(item: TaskWithContext, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
                item.metaLine(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
