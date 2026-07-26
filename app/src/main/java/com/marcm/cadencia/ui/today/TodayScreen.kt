package com.marcm.cadencia.ui.today

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marcm.cadencia.KuseApp
import com.marcm.cadencia.ui.components.BannerActualizacion
import com.marcm.cadencia.ui.components.ConfettiOverlay
import com.marcm.cadencia.ui.components.DomainFilterChips
import com.marcm.cadencia.ui.components.ProgressCard
import com.marcm.cadencia.ui.components.TaskCard
import com.marcm.cadencia.ui.components.longDate
import com.marcm.cadencia.ui.theme.Overdue
import com.marcm.cadencia.ui.theme.OverdueBorder

@Composable
fun TodayScreen(
    contentPadding: PaddingValues,
    onTaskClick: (Long) -> Unit,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCelebration by remember { mutableStateOf(false) }

    // El actualizador vive en la Application: su estado sobrevive a la navegación y a
    // la recomposición, así que la pantalla solo lo observa.
    val app = LocalContext.current.applicationContext as KuseApp
    val estadoActualizacion by app.actualizador.estado.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.celebrate.collect { showCelebration = true }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 14.dp,
                bottom = contentPadding.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item("cabecera") {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                    Text(
                        text = longDate(state.date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = viewModel.greeting(),
                        style = MaterialTheme.typography.displaySmall
                    )
                }
            }

            item("actualizacion") {
                BannerActualizacion(
                    estado = estadoActualizacion,
                    onActualizar = { app.actualizador.actualizarAhora() },
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp)
                )
            }

            item("progreso") {
                ProgressCard(
                    done = state.doneCount,
                    total = state.total,
                    perDomain = state.domainProgress,
                    modifier = Modifier
                        .padding(horizontal = 18.dp)
                        .animateContentSize()
                )
            }

            if (state.domains.size > 1) {
                item("filtros") {
                    DomainFilterChips(
                        domains = state.domains,
                        selectedDomainId = state.filterDomainId,
                        onSelect = viewModel::setFilter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }

            if (state.overdue.isNotEmpty()) {
                item("atrasado-titulo") {
                    SectionTitle("ATRASADO", color = Overdue)
                }
                items(state.overdue, key = { "o-${it.id}" }) { row ->
                    TaskCard(
                        item = row.item,
                        status = row.status,
                        daysOverdue = row.daysOverdue,
                        onToggle = { viewModel.toggle(row) },
                        onClick = { onTaskClick(row.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                    )
                }
            }

            if (state.today.isNotEmpty()) {
                item("hoy-titulo") {
                    SectionTitle(if (state.overdue.isEmpty()) "HOY" else "TAMBIÉN HOY")
                }
                items(state.today, key = { "t-${it.id}" }) { row ->
                    TaskCard(
                        item = row.item,
                        status = row.status,
                        daysOverdue = row.daysOverdue,
                        onToggle = { viewModel.toggle(row) },
                        onClick = { onTaskClick(row.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                    )
                }
            }

            if (state.loaded && state.isEmpty) {
                item("vacio") { EmptyToday(hasFilter = state.filterDomainId != null) }
            }
        }

        if (showCelebration) {
            ConfettiOverlay()
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2_400)
                showCelebration = false
            }
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(start = 22.dp, top = 12.dp, bottom = 2.dp)
    )
}

@Composable
private fun EmptyToday(hasFilter: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 28.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(vertical = 34.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Filled.Schedule,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = if (hasFilter) "Nada de este ámbito hoy" else "Hoy no toca nada",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = if (hasFilter) "Prueba a quitar el filtro para ver el resto del día."
            else "Cuando algo vuelva a tocar, aparecerá aquí.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
    }
}
