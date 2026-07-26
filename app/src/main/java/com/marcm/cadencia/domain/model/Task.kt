package com.marcm.cadencia.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Modelo de dominio de una tarea recurrente.
 *
 * Es independiente de Room: la entidad de persistencia
 * ([com.marcm.cadencia.data.local.TaskEntity]) se mapea a/desde este modelo.
 *
 * [dueDate] se persiste en lugar de derivarse del último completado: es lo que permite
 * que una tarea vencida siga vencida con su atraso acumulado, y lo que hace posible
 * [AnchorMode.FROM_DUE_DATE], donde el calendario avanza aunque no se complete nada.
 *
 * @param iconKey icono propio; si es null se hereda el de la categoría o el del ámbito.
 * @param reminderTime hora local del recordatorio, o null si no hay.
 * @param lastCompletedAt fecha de la última vez que se marcó hecha, o null si nunca.
 */
data class Task(
    val id: Long = 0L,
    val name: String,
    val categoryId: Long,
    val iconKey: String? = null,
    val recurrence: Recurrence,
    val anchorMode: AnchorMode = AnchorMode.FROM_COMPLETION,
    val reminderTime: LocalTime? = null,
    val createdAt: LocalDate = LocalDate.now(),
    val dueDate: LocalDate = LocalDate.now(),
    val lastCompletedAt: LocalDate? = null,
    val isActive: Boolean = true
)

/**
 * Una tarea con el contexto que necesita la UI para pintarla: su categoría y su ámbito.
 * Lo devuelven las consultas con JOIN del DAO, para no resolver el ámbito fila a fila.
 */
data class TaskWithContext(
    val task: Task,
    val category: Category,
    val domain: Domain
) {
    val id: Long get() = task.id

    /** Icono efectivo: el de la tarea, si no el de la categoría, si no el del ámbito. */
    val iconKey: String get() = task.iconKey ?: category.iconKey ?: domain.iconKey

    /** Línea meta de las tarjetas: "Hogar · Cada 2 semanas". */
    fun metaLine(): String = "${domain.name} · ${task.recurrence.label()}"
}
