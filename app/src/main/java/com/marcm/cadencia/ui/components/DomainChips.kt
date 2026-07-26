package com.marcm.cadencia.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.marcm.cadencia.domain.model.Domain
import com.marcm.cadencia.ui.theme.Accent
import com.marcm.cadencia.ui.theme.AccentInk
import com.marcm.cadencia.ui.theme.color

/**
 * Fila de filtros: "Todo" más un chip por ámbito activo. El chip seleccionado va en el
 * rojo de marca (estado activo); el color del ámbito aparece como punto, nunca como
 * fondo del chip.
 */
@Composable
fun DomainFilterChips(
    domains: List<Domain>,
    selectedDomainId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp)
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = contentPadding
    ) {
        item {
            FilterChip(
                label = "Todo",
                selected = selectedDomainId == null,
                dotColor = null,
                onClick = { onSelect(null) }
            )
        }
        items(domains, key = { it.id }) { domain ->
            FilterChip(
                label = domain.name,
                selected = selectedDomainId == domain.id,
                dotColor = domain.color(),
                onClick = { onSelect(domain.id) }
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    dotColor: androidx.compose.ui.graphics.Color?,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        if (selected) Accent else MaterialTheme.colorScheme.surface,
        label = "chip-bg"
    )
    val content by animateColorAsState(
        if (selected) AccentInk else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chip-fg"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .then(
                if (selected) Modifier
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        if (dotColor != null) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (selected) AccentInk.copy(alpha = 0.55f) else dotColor)
            )
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = content)
    }
}
