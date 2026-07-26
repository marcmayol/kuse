package com.marcm.cadencia.ui.onboarding

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
import com.marcm.cadencia.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Tarjeta de ámbito del onboarding: qué es y cuántas sugerencias trae. */
data class DomainOption(
    val domain: Domain,
    val summary: String,
    val presetCount: Int,
    val selected: Boolean
) {
    /** "Dental, afeitado, piel · 10 sugeridas" */
    val subtitle: String
        get() = if (presetCount == 0) summary else "$summary · $presetCount sugeridas"
}

data class OnboardingUiState(
    val options: List<DomainOption> = emptyList(),
    val loaded: Boolean = false
) {
    val anySelected: Boolean get() = options.any { it.selected }
}

class OnboardingViewModel(
    private val domainRepo: DomainRepository,
    private val taskRepo: TaskRepository,
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<OnboardingUiState> =
        combine(domainRepo.observeAll(), selectedIds) { domains, selected ->
            OnboardingUiState(
                options = domains.map { domain ->
                    val builtIn = BuiltInDomain.fromKey(domain.key)
                    DomainOption(
                        domain = domain,
                        summary = builtIn?.summary ?: "Ámbito propio",
                        presetCount = builtIn?.let { Presets.countFor(it) } ?: 0,
                        selected = domain.id in selected
                    )
                },
                loaded = true
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingUiState())

    fun toggle(domainId: Long) {
        selectedIds.value = selectedIds.value.let {
            if (domainId in it) it - domainId else it + domainId
        }
    }

    /** Crea un ámbito propio y lo deja marcado; sus tareas las añadirá el usuario. */
    fun createCustomDomain(name: String, colorHex: String, iconKey: String) {
        viewModelScope.launch {
            val id = domainRepo.createCustomDomain(name, colorHex, iconKey)
            // Nace activo en base; hasta terminar el onboarding sólo cuenta la marca.
            domainRepo.setActive(id, false)
            selectedIds.value = selectedIds.value + id
        }
    }

    /**
     * Cierra el onboarding: activa los ámbitos elegidos sembrando sus sugerencias por
     * defecto, deja inactivos los demás y programa los recordatorios resultantes.
     */
    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            val chosen = selectedIds.value
            domainRepo.getAll().forEach { domain ->
                if (domain.id in chosen) {
                    val presets = BuiltInDomain.fromKey(domain.key)
                        ?.let { builtIn -> Presets.forDomain(builtIn).filter { it.defaultSelected } }
                        .orEmpty()
                    domainRepo.activateWithPresets(domain.id, presets)
                } else {
                    domainRepo.setActive(domain.id, false)
                }
            }
            scheduler.syncAll(taskRepo.getActiveTasks().map { it.task })
            settings.setOnboardingDone(true)
            onDone()
        }
    }

    /** Empezar en blanco: sin ámbitos activos, se añaden luego desde Ajustes. */
    fun skip(onDone: () -> Unit) {
        viewModelScope.launch {
            settings.setOnboardingDone(true)
            onDone()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                OnboardingViewModel(
                    container.domainRepository,
                    container.taskRepository,
                    container.settingsRepository,
                    container.reminderScheduler
                )
            }
        }
    }
}
