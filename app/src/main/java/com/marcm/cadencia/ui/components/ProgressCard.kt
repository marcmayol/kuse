package com.marcm.cadencia.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marcm.cadencia.domain.model.Domain
import com.marcm.cadencia.ui.theme.Accent
import com.marcm.cadencia.ui.theme.color

/** Progreso de un ámbito dentro del día. */
data class DomainProgress(val domain: Domain, val done: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total
    val pct: Int get() = if (total == 0) 0 else (done * 100) / total
}

/**
 * Tarjeta de progreso del día: anillo con el porcentaje dentro y, debajo, una mini-barra
 * por ámbito con su color. Todo lo que muestra corresponde a lo que hay filtrado en ese
 * momento, no al total del día.
 */
@Composable
fun ProgressCard(
    done: Int,
    total: Int,
    perDomain: List<DomainProgress>,
    modifier: Modifier = Modifier
) {
    val pct = if (total == 0) 0 else (done * 100) / total

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ProgressRing(pct)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = if (total == 0) "Nada pendiente" else "$done de $total hechas",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = when {
                        total == 0 -> "Hoy no toca nada"
                        done == total -> "Día completo"
                        else -> "Sigue así"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (perDomain.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                perDomain.forEach { dp ->
                    DomainBar(dp, Modifier.weight(1f))
                }
            }
        }
    }
}

/** Anillo de progreso con el porcentaje dentro. El barrido se anima al marcar tareas. */
@Composable
fun ProgressRing(
    pct: Int,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    strokeWidth: Dp = 9.dp
) {
    val fraction by animateFloatAsState(
        targetValue = (pct / 100f).coerceIn(0f, 1f),
        animationSpec = tween(520),
        label = "anillo"
    )
    val track = MaterialTheme.colorScheme.outlineVariant

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2
            val arcSize = androidx.compose.ui.geometry.Size(
                this.size.width - strokeWidth.toPx(),
                this.size.height - strokeWidth.toPx()
            )
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = stroke
            )
            if (fraction > 0f) {
                drawArc(
                    color = Accent,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = arcSize,
                    style = stroke
                )
            }
        }
        Text(
            text = "$pct%",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Mini-barra de un ámbito: su nombre, su porcentaje y la barra en su color. */
@Composable
private fun DomainBar(progress: DomainProgress, modifier: Modifier = Modifier) {
    val fraction by animateFloatAsState(
        targetValue = progress.fraction,
        animationSpec = tween(520),
        label = "barra-${progress.domain.key}"
    )
    val color = progress.domain.color()

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = progress.domain.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Text(
                text = "${progress.pct}%",
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(if (fraction > 0f) color else Color.Transparent)
            )
        }
    }
}
