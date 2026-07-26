package com.marcm.cadencia.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.marcm.cadencia.data.repository.DomainRepository
import com.marcm.cadencia.data.repository.TaskRepository
import com.marcm.cadencia.domain.model.BuiltInDomain
import com.marcm.cadencia.domain.model.Domain
import com.marcm.cadencia.domain.model.Presets
import com.marcm.cadencia.notifications.ReminderScheduler
import com.marcm.cadencia.settings.SettingsRepository
import com.marcm.cadencia.settings.ThemeMode
import com.marcm.cadencia.ui.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Un ámbito en la lista de ajustes, con cuántas tareas tiene ya creadas. */
data class DomainSetting(val domain: Domain, val taskCount: Int)

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val domainRepo: DomainRepository,
    private val taskRepo: TaskRepository,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> =
        settings.themeMode.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ThemeMode.DARK
        )

    val domains: StateFlow<List<Domain>> =
        domainRepo.observeAll().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    /**
     * Activa o desactiva un ámbito. La primera vez que se activa uno de fábrica que no
     * tiene ninguna tarea, se siembran sus sugerencias; después ya sólo se muestra u
     * oculta, sin volver a crear nada.
     */
    fun toggleDomain(domain: Domain) {
        viewModelScope.launch {
            if (domain.isActive) {
                domainRepo.setActive(domain.id, false)
            } else {
                // activateWithPresets ignora las sugerencias si el ámbito ya tiene tareas.
                val presets = BuiltInDomain.fromKey(domain.key)
                    ?.let { builtIn -> Presets.forDomain(builtIn).filter { it.defaultSelected } }
                    .orEmpty()
                domainRepo.activateWithPresets(domain.id, presets)
            }
            scheduler.syncAll(taskRepo.getActiveTasks().map { it.task })
        }
    }

    fun createCustomDomain(name: String, colorHex: String, iconKey: String) {
        viewModelScope.launch { domainRepo.createCustomDomain(name, colorHex, iconKey) }
    }

    fun deleteDomain(domain: Domain) {
        viewModelScope.launch {
            domainRepo.deleteCustom(domain)
            scheduler.syncAll(taskRepo.getActiveTasks().map { it.task })
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                SettingsViewModel(
                    container.settingsRepository,
                    container.domainRepository,
                    container.taskRepository,
                    container.reminderScheduler
                )
            }
        }
    }
}
