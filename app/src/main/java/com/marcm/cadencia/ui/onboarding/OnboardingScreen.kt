package com.marcm.cadencia.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marcm.cadencia.ui.components.AppLockup
import com.marcm.cadencia.ui.components.DomainIconBox
import com.marcm.cadencia.ui.components.TaskIcon
import com.marcm.cadencia.ui.components.selectableIconKeys
import com.marcm.cadencia.ui.theme.Accent
import com.marcm.cadencia.ui.theme.AccentInk
import com.marcm.cadencia.ui.theme.color
import com.marcm.cadencia.ui.theme.containerColor
import com.marcm.cadencia.ui.theme.customDomainPalette
import com.marcm.cadencia.ui.theme.parseHexColor

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 52.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    AppLockup()
                    Spacer(Modifier.height(22.dp))
                    Text(
                        "¿Qué quieres mantener al día?",
                        style = MaterialTheme.typography.displaySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Elige los ámbitos que te interesan. Cada uno trae tareas sugeridas que " +
                            "podrás cambiar o borrar después.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            items(state.options, key = { it.domain.id }) { option ->
                DomainOptionCard(option = option, onClick = { viewModel.toggle(option.domain.id) })
            }

            item {
                CustomDomainCard(onClick = { showCreate = true })
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { viewModel.finish(onDone) },
                enabled = state.anySelected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = AccentInk
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text("Empezar", style = MaterialTheme.typography.labelLarge)
            }
            TextButton(
                onClick = { viewModel.skip(onDone) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Prefiero empezar en blanco",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showCreate) {
        CreateDomainDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, color, icon ->
                viewModel.createCustomDomain(name, color, icon)
                showCreate = false
            }
        )
    }
}

@Composable
private fun DomainOptionCard(option: DomainOption, onClick: () -> Unit) {
    val tint = option.domain.color()
    val borderColor = if (option.selected) Accent else MaterialTheme.colorScheme.outlineVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(if (option.selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        DomainIconBox(
            iconKey = option.domain.iconKey,
            tint = tint,
            container = option.domain.containerColor(),
            boxSize = 48.dp,
            iconSize = 24.dp
        )
        Column(Modifier.weight(1f)) {
            Text(option.domain.name, style = MaterialTheme.typography.titleMedium)
            Text(
                option.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SelectionCheck(selected = option.selected)
    }
}

@Composable
private fun SelectionCheck(selected: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(Accent)
                else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, null, tint = AccentInk, modifier = Modifier.size(17.dp))
        }
    }
}

/** Tarjeta de "ámbito propio", con borde discontinuo. */
@Composable
private fun CustomDomainCard(onClick: () -> Unit) {
    val stroke = MaterialTheme.colorScheme.outline

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .drawBehind {
                drawRoundRect(
                    brush = SolidColor(stroke),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx())
                )
            }
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                Icons.Filled.Add,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text("Ámbito propio", style = MaterialTheme.typography.titleMedium)
            Text(
                "Crea el tuyo con su color e icono",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CreateDomainDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, colorHex: String, iconKey: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf(customDomainPalette.first()) }
    var iconKey by remember { mutableStateOf(selectableIconKeys.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Nuevo ámbito") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "COLOR",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    customDomainPalette.take(6).forEach { hex ->
                        val c = parseHexColor(hex)
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (hex == colorHex)
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }

                Text(
                    "ICONO",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    selectableIconKeys.take(6).forEach { key ->
                        val selected = key == iconKey
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) parseHexColor(colorHex).copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { iconKey = key }
                        ) {
                            TaskIcon(
                                key,
                                tint = if (selected) parseHexColor(colorHex)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 20.dp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.trim(), colorHex, iconKey) },
                enabled = name.isNotBlank()
            ) {
                Text("Crear", color = if (name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
