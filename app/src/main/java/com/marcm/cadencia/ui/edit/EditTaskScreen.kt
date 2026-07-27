package com.marcm.cadencia.ui.edit

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marcm.cadencia.domain.model.AnchorMode
import com.marcm.cadencia.domain.model.Domain
import com.marcm.cadencia.ui.components.DomainIconBox
import com.marcm.cadencia.ui.onboarding.CreateDomainDialog
import com.marcm.cadencia.ui.components.timeLabel
import com.marcm.cadencia.ui.components.weekdayInitial
import com.marcm.cadencia.ui.theme.Accent
import com.marcm.cadencia.ui.theme.AccentInk
import com.marcm.cadencia.ui.theme.color
import com.marcm.cadencia.ui.theme.containerColor
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun EditTaskScreen(
    onClose: () -> Unit,
    viewModel: EditTaskViewModel = viewModel(factory = EditTaskViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCreateDomain by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 40.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Cerrar") }
                Spacer(Modifier.width(4.dp))
                Text(
                    if (state.isNew) "Nueva tarea" else "Editar tarea",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Nombre") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            )

            SectionLabel("ÁMBITO")
            DomainGrid(
                domains = state.domains,
                selectedId = state.selectedDomainId,
                onSelect = viewModel::setDomain,
                onCreateDomain = { showCreateDomain = true }
            )

            SectionLabel("CADA CUÁNTO")
            RecurrenceTabs(selected = state.tab, onSelect = viewModel::setTab)

            when (state.tab) {
                RecurrenceTab.DAILY -> Unit

                RecurrenceTab.EVERY_N_DAYS -> Stepper(
                    label = "Cada",
                    value = "${state.dayInterval} días",
                    onMinus = { viewModel.stepDayInterval(-1) },
                    onPlus = { viewModel.stepDayInterval(1) }
                )

                RecurrenceTab.WEEKLY -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Stepper(
                        label = "Cada",
                        value = if (state.weekInterval == 1) "1 semana" else "${state.weekInterval} semanas",
                        onMinus = { viewModel.stepWeekInterval(-1) },
                        onPlus = { viewModel.stepWeekInterval(1) }
                    )
                    WeekdayPicker(state.weekdays, viewModel::toggleWeekday)
                }

                RecurrenceTab.MONTHLY -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (state.fixedDayOfMonth) {
                        Stepper(
                            label = "El día",
                            value = "${state.dayOfMonth} de cada mes",
                            onMinus = { viewModel.stepDayOfMonth(-1) },
                            onPlus = { viewModel.stepDayOfMonth(1) }
                        )
                    } else {
                        Stepper(
                            label = "Cada",
                            value = if (state.monthInterval == 1) "1 mes" else "${state.monthInterval} meses",
                            onMinus = { viewModel.stepMonthInterval(-1) },
                            onPlus = { viewModel.stepMonthInterval(1) }
                        )
                    }
                    ToggleRow(
                        title = "Fijar el día del mes",
                        subtitle = "Siempre el mismo día, aunque me retrase",
                        checked = state.fixedDayOfMonth,
                        onCheckedChange = viewModel::setFixedDayOfMonth
                    )
                }
            }

            SectionLabel("RECALCULAR DESDE")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnchorMode.entries.forEach { mode ->
                    AnchorOption(
                        mode = mode,
                        selected = state.anchorMode == mode,
                        onClick = { viewModel.setAnchor(mode) }
                    )
                }
            }

            SectionLabel("RECORDATORIO")
            ToggleRow(
                title = "Avisarme",
                subtitle = if (state.reminderEnabled) "A las ${timeLabel(state.reminderTime)}"
                else "Sin notificación",
                checked = state.reminderEnabled,
                onCheckedChange = viewModel::setReminderEnabled,
                onClick = if (state.reminderEnabled) {
                    {
                        TimePickerDialog(
                            context,
                            { _, hour, minute -> viewModel.setReminderTime(LocalTime.of(hour, minute)) },
                            state.reminderTime.hour,
                            state.reminderTime.minute,
                            true
                        ).show()
                    }
                } else null
            )

            Text(
                "Resumen: ${state.previewLabel()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Button(
                onClick = { viewModel.save(onClose) },
                enabled = state.canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = AccentInk
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    if (state.isNew) "Crear tarea" else "Guardar",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    if (showCreateDomain) {
        CreateDomainDialog(
            onDismiss = { showCreateDomain = false },
            onCreate = { name, color, icon ->
                viewModel.createCustomDomain(name, color, icon)
                showCreateDomain = false
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Rejilla 2×2 de ámbitos (crece en filas de dos si hay más), con una celda final para
 * crear un ámbito propio. Se listan todos, activos o no: elegir uno apagado lo enciende
 * al guardar. El `null` de la última posición es esa celda de "ámbito propio".
 */
@Composable
private fun DomainGrid(
    domains: List<Domain>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onCreateDomain: () -> Unit
) {
    val cells: List<Domain?> = domains + null

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cells.chunked(2).forEach { row ->
            // Altura mínima intrínseca: las dos celdas de la fila igualan a la más alta,
            // que los nombres largos ocupan dos líneas.
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(IntrinsicSize.Min)
            ) {
                row.forEach { domain ->
                    if (domain == null) {
                        NewDomainCell(
                            onClick = onCreateDomain,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    } else {
                        DomainCell(
                            domain = domain,
                            selected = domain.id == selectedId,
                            onClick = { onSelect(domain.id) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Celda de "ámbito propio": mismo formato que las demás, con borde discontinuo. */
@Composable
private fun NewDomainCell(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val stroke = MaterialTheme.colorScheme.outline

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .drawBehind {
                drawRoundRect(
                    brush = SolidColor(stroke),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    ),
                    cornerRadius = CornerRadius(18.dp.toPx())
                )
            }
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                Icons.Filled.Add,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            "Ámbito propio",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
        )
    }
}

@Composable
private fun DomainCell(
    domain: Domain,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) Accent else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        DomainIconBox(
            iconKey = domain.iconKey,
            tint = domain.color(),
            container = domain.containerColor(),
            boxSize = 34.dp,
            iconSize = 18.dp,
            corner = 12.dp
        )
        Text(
            domain.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2
        )
    }
}

@Composable
private fun RecurrenceTabs(selected: RecurrenceTab, onSelect: (RecurrenceTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RecurrenceTab.entries.forEach { tab ->
            val active = tab == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (active) Accent else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) AccentInk else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
        StepperButton(Icons.Filled.Remove, "Menos", onMinus)
        Spacer(Modifier.width(8.dp))
        StepperButton(Icons.Filled.Add, "Más", onPlus)
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        Icon(icon, description, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun WeekdayPicker(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        DayOfWeek.entries.forEach { day ->
            val active = day in selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(if (active) Accent else MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        if (active) Accent else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape
                    )
                    .clickable { onToggle(day) }
            ) {
                Text(
                    weekdayInitial(day),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) AccentInk else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AnchorOption(mode: AnchorMode, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) Accent else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(mode.label, style = MaterialTheme.typography.titleMedium)
            Text(
                mode.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(Icons.Filled.Check, null, tint = Accent, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentInk,
                checkedTrackColor = Accent
            )
        )
    }
}
