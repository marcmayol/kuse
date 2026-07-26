package com.marcm.cadencia.domain.model

/**
 * Estado derivado de una tarea respecto a una fecha de referencia ("hoy").
 * Se calcula en domain/recurrence/Recurrence.kt; no se persiste.
 */
enum class TaskStatus {
    /** Su fecha prevista ya pasó y no se ha hecho. */
    OVERDUE,

    /** Le toca hoy y aún no se ha hecho. */
    DUE_TODAY,

    /** Ya se ha hecho hoy. */
    DONE,

    /** Su próxima fecha es futura. */
    FUTURE
}
