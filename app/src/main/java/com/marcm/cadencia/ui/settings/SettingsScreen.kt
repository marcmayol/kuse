package com.marcm.cadencia.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marcm.actualizador.EstadoActualizacion
import com.marcm.actualizador.Modo
import com.marcm.actualizador.TipoError
import com.marcm.cadencia.BuildConfig
import com.marcm.cadencia.KuseApp
import com.marcm.cadencia.domain.model.Domain
import com.marcm.cadencia.security.AjustesBloqueo
import com.marcm.cadencia.security.Biometria
import com.marcm.cadencia.security.Gracia
import com.marcm.cadencia.settings.ThemeMode
import com.marcm.cadencia.ui.components.DomainIconBox
import com.marcm.cadencia.ui.onboarding.CreateDomainDialog
import com.marcm.cadencia.ui.theme.Accent
import com.marcm.cadencia.ui.theme.AccentInk
import com.marcm.cadencia.ui.theme.color
import com.marcm.cadencia.ui.theme.Overdue
import com.marcm.cadencia.ui.theme.containerColor
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onConfigurarPin: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val domains by viewModel.domains.collectAsStateWithLifecycle()
    val bloqueo by viewModel.bloqueo.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCreate by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 18.dp, end = 18.dp,
                top = contentPadding.calculateTopPadding() + 14.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            )
    ) {
        Text("Ajustes", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(start = 4.dp))

        SectionTitle("ÁMBITOS")
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            domains.forEach { domain ->
                DomainRow(
                    domain = domain,
                    onToggle = { viewModel.toggleDomain(domain) },
                    onDelete = if (domain.isBuiltIn) null else ({ viewModel.deleteDomain(domain) })
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCreate = true }
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    null,
                    tint = Accent,
                    modifier = Modifier.size(22.dp)
                )
                Text("Nuevo ámbito", style = MaterialTheme.typography.titleMedium, color = Accent)
            }
        }
        Text(
            "Al activar un ámbito por primera vez se añaden sus tareas sugeridas. " +
                "Desactivarlo sólo lo esconde: no se borra nada.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 10.dp)
        )

        SectionTitle("APARIENCIA")
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            ThemeOption("Según el sistema", Icons.Filled.PhoneAndroid, theme == ThemeMode.SYSTEM) {
                viewModel.setTheme(ThemeMode.SYSTEM)
            }
            ThemeOption("Oscuro", Icons.Filled.DarkMode, theme == ThemeMode.DARK) {
                viewModel.setTheme(ThemeMode.DARK)
            }
            ThemeOption("Claro", Icons.Filled.LightMode, theme == ThemeMode.LIGHT) {
                viewModel.setTheme(ThemeMode.LIGHT)
            }
        }

        SectionTitle("NOTIFICACIONES")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    }
                }
                .padding(16.dp)
        ) {
            Icon(
                Icons.Filled.Notifications,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "Permisos de notificación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Abre los ajustes del sistema para esta app",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SectionTitle("SEGURIDAD")
        SeccionSeguridad(
            bloqueo = bloqueo,
            onActivar = onConfigurarPin,
            onCambiarPin = onConfigurarPin,
            onDesactivar = viewModel::desactivarBloqueo,
            onBiometria = viewModel::setBiometria,
            onGracia = viewModel::setGracia,
            onPista = viewModel::setPista
        )

        SectionTitle("ACTUALIZACIONES")
        SeccionActualizaciones()

        SectionTitle("ACERCA DE")
        Text(
            "Kuse · versión ${BuildConfig.VERSION_NAME}\n癖 — lo que haces sin pensarlo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
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

/**
 * Bloqueo de la app. El PIN se crea en su propia pantalla; aquí sólo se enciende,
 * se apaga y se afinan la biometría, la gracia y la pista.
 */
@Composable
private fun SeccionSeguridad(
    bloqueo: AjustesBloqueo,
    onActivar: () -> Unit,
    onCambiarPin: () -> Unit,
    onDesactivar: () -> Unit,
    onBiometria: (Boolean) -> Unit,
    onGracia: (Gracia) -> Unit,
    onPista: (String) -> Unit,
) {
    val context = LocalContext.current
    val hayBiometria = remember { Biometria.disponible(context) }
    var confirmarQuitar by remember { mutableStateOf(false) }
    var editandoPista by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(
                Icons.Filled.Lock,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "Bloquear la app",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Pide un PIN para abrir Kuse",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = bloqueo.activo,
                onCheckedChange = { activar ->
                    if (activar) onActivar() else confirmarQuitar = true
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentInk,
                    checkedTrackColor = Accent
                )
            )
        }

        if (bloqueo.activo) {
            FilaSeguridad(
                icono = Icons.Filled.Password,
                titulo = "Cambiar el PIN",
                subtitulo = "Tiene ${bloqueo.longitudPin} dígitos",
                onClick = onCambiarPin
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Icon(
                    Icons.Filled.Fingerprint,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        "Huella o cara",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (hayBiometria) "Atajo para no teclear el PIN"
                        else "Este móvil no tiene biometría registrada",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = bloqueo.biometria && hayBiometria,
                    enabled = hayBiometria,
                    onCheckedChange = onBiometria,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentInk,
                        checkedTrackColor = Accent
                    )
                )
            }

            FilaSeguridad(
                icono = Icons.AutoMirrored.Filled.HelpOutline,
                titulo = "Pista de recuperación",
                subtitulo = bloqueo.pista.ifBlank { "Sin pista" },
                onClick = { editandoPista = true }
            )
        }
    }

    if (bloqueo.activo) {
        Text(
            "CUÁNDO VOLVER A PEDIRLO",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Gracia.entries.forEach { gracia ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGracia(gracia) }
                        .padding(16.dp)
                ) {
                    Text(
                        gracia.etiqueta,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (bloqueo.gracia == gracia) {
                        Icon(Icons.Filled.Check, null, tint = Accent, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
        Text(
            "Al abrir la app siempre pide el PIN. La espera sólo cuenta cuando sales " +
                "y vuelves.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 10.dp)
        )
    }

    if (confirmarQuitar) {
        AlertDialog(
            onDismissRequest = { confirmarQuitar = false },
            title = { Text("¿Quitar el bloqueo?") },
            text = {
                Text(
                    "Se borrará el PIN y Kuse volverá a abrirse sin pedir nada."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarQuitar = false
                    onDesactivar()
                }) { Text("Quitar", color = Overdue) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarQuitar = false }) { Text("Cancelar") }
            }
        )
    }

    if (editandoPista) {
        var texto by remember { mutableStateOf(bloqueo.pista) }
        AlertDialog(
            onDismissRequest = { editandoPista = false },
            title = { Text("Pista de recuperación") },
            text = {
                Column {
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it.take(60) },
                        label = { Text("Pista (opcional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp)
                    )
                    Text(
                        "Se ve sin desbloquear la app: no escribas aquí el PIN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onPista(texto)
                    editandoPista = false
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { editandoPista = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun FilaSeguridad(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Icon(
            icono,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(
                subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Único punto donde la comprobación de actualizaciones es manual, y el único sitio
 * donde se informa de errores o del "estás al día": aquí el usuario ha preguntado.
 */
@Composable
private fun SeccionActualizaciones() {
    val app = LocalContext.current.applicationContext as KuseApp
    val actualizador = app.actualizador
    val scope = rememberCoroutineScope()
    val estado by actualizador.estado.collectAsStateWithLifecycle()
    var buscarAuto by remember { mutableStateOf(actualizador.buscarAutomaticamente) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                Icons.Filled.Refresh,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "Buscar actualizaciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Comprueba de vez en cuando si hay una versión nueva",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = buscarAuto,
                onCheckedChange = {
                    buscarAuto = it
                    actualizador.buscarAutomaticamente = it
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentInk,
                    checkedTrackColor = Accent
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            OutlinedButton(
                onClick = { scope.launch { actualizador.comprobar(Modo.MANUAL) } },
                enabled = estado != EstadoActualizacion.Comprobando,
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Buscar ahora")
            }
            Text(
                text = mensajeDe(estado),
                style = MaterialTheme.typography.bodyMedium,
                color = if (estado is EstadoActualizacion.Error) Overdue
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Traducción del estado a la frase que ve el usuario en Ajustes. */
private fun mensajeDe(estado: EstadoActualizacion): String = when (estado) {
    EstadoActualizacion.Comprobando -> "Comprobando…"
    EstadoActualizacion.AlDia -> "Estás al día"
    is EstadoActualizacion.Disponible -> "Hay una versión nueva: ${estado.info.versionName}"
    is EstadoActualizacion.Descargando -> "Descargando… ${estado.porcentaje} %"
    EstadoActualizacion.Verificando -> "Comprobando el archivo…"
    EstadoActualizacion.PidiendoPermiso -> "Falta el permiso para instalar"
    EstadoActualizacion.Instalando -> "Instalando…"
    is EstadoActualizacion.Error -> when (estado.tipo) {
        TipoError.SIN_RED -> "Sin conexión"
        TipoError.HTTP -> "El servidor no responde"
        TipoError.MANIFIESTO -> "No se pudo leer la información de versiones"
        TipoError.DESCARGA -> "No se pudo descargar la actualización"
        TipoError.HASH -> "El archivo descargado no era válido"
        TipoError.INSTALACION -> estado.mensaje ?: "No se pudo instalar"
    }
    EstadoActualizacion.Inactivo -> ""
}

@Composable
private fun DomainRow(domain: Domain, onToggle: () -> Unit, onDelete: (() -> Unit)?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        DomainIconBox(
            iconKey = domain.iconKey,
            tint = domain.color(),
            container = domain.containerColor(),
            boxSize = 38.dp,
            iconSize = 19.dp,
            corner = 13.dp
        )
        Text(
            domain.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    "Borrar ámbito",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        Switch(
            checked = domain.isActive,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentInk,
                checkedTrackColor = Accent
            )
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 26.dp, bottom = 10.dp)
    )
}

@Composable
private fun ThemeOption(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        if (selected) Icon(Icons.Filled.Check, null, tint = Accent, modifier = Modifier.size(22.dp))
    }
}
