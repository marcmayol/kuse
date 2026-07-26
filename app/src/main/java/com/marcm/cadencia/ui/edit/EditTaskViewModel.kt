package com.marcm.cadencia.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.marcm.cadencia.data.repository.DomainRepository
import com.marcm.cadencia.data.repository.TaskRepository
import com.marcm.cadencia.domain.model.AnchorMode
import com.marcm.cadencia.domain.model.Domain
import com.marcm.cadencia.domain.model.Recurrence
import com.marcm.cadencia.domain.model.RecurrenceType
import com.marcm.cadencia.domain.model.Task
import com.marcm.cadencia.notifications.ReminderScheduler
import com.marcm.cadencia.ui.appContainer
import com.marcm.cadencia.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Los cuatro segmentos del selector de recurrencia. */
enum class RecurrenceTab(val label: String) {
    DAILY("Diaria"),
    EVERY_N_DAYS("Cada X"),
    WEEKLY("Semana"),
    MONTHLY("Mes")
}

data class EditUiState(
    val isNew: Boolean = true,
    val name: String = "",
    val domains: List<Domain> = emptyList(),
    val selectedDomainId: Long? = null,
    val tab: RecurrenceTab = RecurrenceTab.DAILY,
    val dayInterval: Int = 3,
    val weekInterval: Int = 1,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val monthInterval: Int = 1,
    val fixedDayOfMonth: Boolean = false,
    val dayOfMonth: Int = LocalDate.now().dayOfMonth,
    val anchorMode: AnchorMode = AnchorMode.FROM_COMPLETION,
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(8, 0),
    val loaded: Boolean = false
) {
    val canSave: Boolean get() = name.isNotBlank() && selectedDomainId != null

    /** La recurrencia que resulta de lo que hay elegido en la pantalla. */
    fun toRecurrence(): Recurrence = when (tab) {
        RecurrenceTab.DAILY -> Recurrence(RecurrenceType.DAILY)
        RecurrenceTab.EVERY_N_DAYS -> Recurrence(RecurrenceType.EVERY_N_DAYS, interval = dayInterval)
        RecurrenceTab.WEEKLY -> Recurrence(
            RecurrenceType.WEEKLY,
            interval = weekInterval,
            weekdays = weekdays
        )
        RecurrenceTab.MONTHLY ->
            if (fixedDayOfMonth) Recurrence(RecurrenceType.MONTHLY, dayOfMonth = dayOfMonth)
            else Recurrence(RecurrenceType.EVERY_N_MONTHS, interval = monthInterval)
    }

    fun previewLabel(): String = toRecurrence().label()
}

class EditTaskViewModel(
    private val taskRepo: TaskRepository,
    private val domainRepo: DomainRepository,
    private val scheduler: ReminderScheduler,
    private val taskId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditUiState(isNew = taskId <= 0L))
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    /** Se conserva para no mover la fecha prevista ni el historial al editar. */
    private var editing: Task? = null

    init {
        viewModelScope.launch {
            val domains = domainRepo.getActive().ifEmpty { domainRepo.getAll() }
            val existing = if (taskId > 0L) taskRepo.getTask(taskId) else null
            editing = existing?.task

            _uiState.update { state ->
                if (existing == null) {
                    state.copy(
                        domains = domains,
                        selectedDomainId = domains.firstOrNull()?.id,
                        loaded = true
                    )
                } else {
                    val r = existing.task.recurrence
                    state.copy(
                        isNew = false,
                        name = existing.task.name,
                        domains = domains,
                        selectedDomainId = existing.domain.id,
                        tab = when (r.type) {
                            RecurrenceType.DAILY -> RecurrenceTab.DAILY
                            RecurrenceType.EVERY_N_DAYS -> RecurrenceTab.EVERY_N_DAYS
                            RecurrenceType.WEEKLY -> RecurrenceTab.WEEKLY
                            RecurrenceType.MONTHLY, RecurrenceType.EVERY_N_MONTHS -> RecurrenceTab.MONTHLY
                        },
                        dayInterval = if (r.type == RecurrenceType.EVERY_N_DAYS) r.safeInterval else state.dayInterval,
                        weekInterval = if (r.type == RecurrenceType.WEEKLY) r.safeInterval else state.weekInterval,
                        weekdays = r.weekdays,
                        monthInterval = if (r.type == RecurrenceType.EVERY_N_MONTHS) r.safeInterval else state.monthInterval,
                        fixedDayOfMonth = r.type == RecurrenceType.MONTHLY,
                        dayOfMonth = r.dayOfMonth ?: existing.task.dueDate.dayOfMonth,
                        anchorMode = existing.task.anchorMode,
                        reminderEnabled = existing.task.reminderTime != null,
                        reminderTime = existing.task.reminderTime ?: state.reminderTime,
                        loaded = true
                    )
                }
            }
        }
    }

    fun setName(value: String) = _uiState.update { it.copy(name = value) }
    fun setDomain(id: Long) = _uiState.update { it.copy(selectedDomainId = id) }
    fun setTab(tab: RecurrenceTab) = _uiState.update { it.copy(tab = tab) }
    fun setAnchor(mode: AnchorMode) = _uiState.update { it.copy(anchorMode = mode) }
    fun setReminderEnabled(enabled: Boolean) = _uiState.update { it.copy(reminderEnabled = enabled) }
    fun setReminderTime(time: LocalTime) = _uiState.update { it.copy(reminderTime = time) }
    fun setFixedDayOfMonth(fixed: Boolean) = _uiState.update { it.copy(fixedDayOfMonth = fixed) }

    fun stepDayInterval(delta: Int) =
        _uiState.update { it.copy(dayInterval = (it.dayInterval + delta).coerceIn(2, 365)) }

    fun stepWeekInterval(delta: Int) =
        _uiState.update { it.copy(weekInterval = (it.weekInterval + delta).coerceIn(1, 12)) }

    fun stepMonthInterval(delta: Int) =
        _uiState.update { it.copy(monthInterval = (it.monthInterval + delta).coerceIn(1, 24)) }

    fun stepDayOfMonth(delta: Int) =
        _uiState.update { it.copy(dayOfMonth = (it.dayOfMonth + delta).coerceIn(1, 31)) }

    fun toggleWeekday(day: DayOfWeek) = _uiState.update {
        it.copy(weekdays = if (day in it.weekdays) it.weekdays - day else it.weekdays + day)
    }

    /**
     * Guarda la tarea. Una tarea nueva vence hoy; al editar una existente se respeta su
     * fecha prevista, salvo que haya cambiado de cadencia de forma que ya no tenga
     * sentido (eso lo resuelve el propio ciclo al completarla).
     */
    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val domainId = state.selectedDomainId ?: return
        if (!state.canSave) return

        viewModelScope.launch {
            val current = editing
            val categoryId = resolveCategoryId(domainId, current)
            val today = LocalDate.now()

            val task = Task(
                id = current?.id ?: 0L,
                name = state.name.trim(),
                categoryId = categoryId,
                iconKey = current?.iconKey,
                recurrence = state.toRecurrence(),
                anchorMode = state.anchorMode,
                reminderTime = if (state.reminderEnabled) state.reminderTime else null,
                createdAt = current?.createdAt ?: today,
                dueDate = current?.dueDate ?: today,
                lastCompletedAt = current?.lastCompletedAt,
                isActive = current?.isActive ?: true
            )

            val id = taskRepo.upsert(task)
            scheduler.schedule(task.copy(id = id))
            onSaved()
        }
    }

    /**
     * Categoría destino: si la tarea sigue en su ámbito, conserva la suya; si se ha
     * movido de ámbito, cae en la categoría "General" del nuevo.
     */
    private suspend fun resolveCategoryId(domainId: Long, current: Task?): Long {
        if (current != null) {
            val categories = domainRepo.categoriesOf(domainId)
            categories.firstOrNull { it.id == current.categoryId }?.let { return it.id }
        }
        return domainRepo.generalCategoryId(domainId)
            ?: domainRepo.categoriesOf(domainId).first().id
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                val handle: SavedStateHandle = createSavedStateHandle()
                val id = handle.get<String>(Routes.ARG_TASK_ID)?.toLongOrNull() ?: -1L
                EditTaskViewModel(
                    container.taskRepository,
                    container.domainRepository,
                    container.reminderScheduler,
                    id
                )
            }
        }
    }
}
