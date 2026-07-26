package com.marcm.cadencia.ui.streaks

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marcm.cadencia.ui.components.DomainIconBox
import com.marcm.cadencia.ui.theme.Accent
import com.marcm.cadencia.ui.theme.color
import com.marcm.cadencia.ui.theme.containerColor

@Composable
fun StreaksScreen(
    contentPadding: PaddingValues,
    onTaskClick: (Long) -> Unit,
    viewModel: StreaksViewModel = viewModel(factory = StreaksViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 14.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item("titulo") {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                Text("Rachas", style = MaterialTheme.typography.displaySmall)
                Text(
                    "Tu constancia, ámbito a ámbito",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        state.domains.forEach { domainStreak ->
            item("d-${domainStreak.domain.id}") {
                DomainStreakCard(
                    streak = domainStreak,
                    onTaskClick = onTaskClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                )
            }
        }

        if (state.loaded && state.domains.isEmpty()) {
            item("vacio") {
                Text(
                    "Aún no hay nada que medir. Activa un ámbito y empieza a marcar tareas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun DomainStreakCard(
    streak: DomainStreak,
    onTaskClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val color = streak.domain.color()

    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DomainIconBox(
                iconKey = streak.domain.iconKey,
                tint = color,
                container = streak.domain.containerColor(),
                boxSize = 40.dp,
                iconSize = 20.dp,
                corner = 14.dp
            )
            Column(Modifier.weight(1f)) {
                Text(streak.domain.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    streak.onTimeRate?.let { "$it% a tiempo" } ?: "Sin historial todavía",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    null,
                    tint = Accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(streak.bestStreak.toString(), style = MaterialTheme.typography.titleLarge)
            }
        }

        streak.tasks.forEach { taskStreak ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTaskClick(taskStreak.item.id) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = if (taskStreak.streak > 0) 1f else 0.35f))
                )
                Text(
                    taskStreak.item.task.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    when {
                        taskStreak.streak > 0 -> "×${taskStreak.streak}"
                        taskStreak.onTimeRate != null -> "—"
                        else -> "nueva"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (taskStreak.streak > 0) Accent
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
