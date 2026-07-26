package com.marcm.cadencia.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Registro histórico: una fila por cada vez que una tarea se marca como hecha.
 *
 * Guarda la fecha que estaba prevista además de la real, para poder decir si se hizo
 * a tiempo sin recalcular la cadena entera hacia atrás.
 *
 * @param dueDate fecha en la que tocaba, tal como estaba en la tarea al completarla.
 * @param onTime true si se completó en o antes de [dueDate].
 */
data class Completion(
    val id: Long = 0L,
    val taskId: Long,
    val dueDate: LocalDate,
    val completedAt: Instant,
    val onTime: Boolean
)
