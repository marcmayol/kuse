package com.marcm.cadencia.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marcm.cadencia.domain.model.TaskStatus
import com.marcm.cadencia.domain.model.TaskWithContext
import com.marcm.cadencia.ui.theme.Accent
import com.marcm.cadencia.ui.theme.AccentInk
import com.marcm.cadencia.ui.theme.Overdue
import com.marcm.cadencia.ui.theme.OverdueBorder
import com.marcm.cadencia.ui.theme.OverdueContainer
import com.marcm.cadencia.ui.theme.color
import com.marcm.cadencia.ui.theme.containerColor

/**
 * Fila de tarea: icono del ámbito en contenedor tintado, nombre, línea meta y círculo
 * de check a la derecha. Al marcarla, el nombre se atenúa y se tacha.
 */
@Composable
fun TaskCard(
    item: TaskWithContext,
    status: TaskStatus,
    daysOverdue: Long,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val done = status == TaskStatus.DONE
    val overdue = status == TaskStatus.OVERDUE
    val tint = item.domain.color()

    val border = if (overdue) BorderStroke(1.dp, OverdueBorder)
    else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

    val nameColor by animateColorAsState(
        if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        label = "nombre"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(start = 13.dp, top = 11.dp, bottom = 11.dp, end = 4.dp)
    ) {
        DomainIconBox(
            iconKey = item.iconKey,
            tint = if (done) tint.copy(alpha = 0.5f) else tint,
            container = item.domain.containerColor()
        )

        Column(Modifier.weight(1f)) {
            Text(
                text = item.task.name,
                style = MaterialTheme.typography.titleMedium,
                color = nameColor,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.metaLine(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (overdue && daysOverdue > 0) {
                OverdueTag(daysOverdue, Modifier.padding(top = 7.dp))
            }
        }

        CheckCircle(done = done, overdue = overdue, taskName = item.task.name, onToggle = onToggle)
    }
}

/** Etiqueta con el atraso acumulado. */
@Composable
fun OverdueTag(days: Long, modifier: Modifier = Modifier) {
    Text(
        text = overdueLabel(days),
        style = MaterialTheme.typography.labelMedium,
        color = Overdue,
        modifier = modifier
            .clip(CircleShape)
            .background(OverdueContainer)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    )
}

/**
 * Círculo de check con 48dp de área táctil (el círculo visible mide 30dp).
 * Al marcarlo hace un pequeño rebote.
 */
@Composable
fun CheckCircle(
    done: Boolean,
    overdue: Boolean,
    taskName: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (done) 1f else 0.94f,
        animationSpec = tween(160),
        label = "check"
    )
    val ring = when {
        overdue -> Overdue.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.outline
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onToggle)
            .semantics {
                contentDescription = if (done) "Desmarcar $taskName" else "Marcar $taskName como hecha"
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .scale(scale)
                .clip(CircleShape)
                .then(
                    if (done) Modifier.background(Accent)
                    else Modifier.border(2.dp, ring, CircleShape).background(Color.Transparent)
                )
        ) {
            if (done) {
                Icon(Icons.Filled.Check, null, tint = AccentInk, modifier = Modifier.size(19.dp))
            }
        }
    }
}
