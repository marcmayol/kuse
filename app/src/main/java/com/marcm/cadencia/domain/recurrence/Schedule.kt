package com.marcm.cadencia.domain.recurrence

import com.marcm.cadencia.domain.model.AnchorMode
import com.marcm.cadencia.domain.model.Completion
import com.marcm.cadencia.domain.model.Recurrence
import com.marcm.cadencia.domain.model.RecurrenceType
import com.marcm.cadencia.domain.model.Task
import com.marcm.cadencia.domain.model.TaskStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Cálculo de fechas como funciones puras y deterministas.
 *
 * Todas reciben la fecha de referencia ("hoy") por parámetro en lugar de leer el
 * reloj, para que sean trivialmente testeables. La capa de repositorio/UI pasa
 * `LocalDate.now()`.
 *
 * El eje del modelo es `task.dueDate`, que se persiste. Nada de esto lo recalcula a
 * partir del historial: una tarea vencida sigue con su fecha prevista en el pasado
 * hasta que alguien la complete, que es lo que da el atraso acumulado.
 */
object Schedule {

    /** Tope de iteraciones al encadenar fechas, por si una recurrencia viniera corrupta. */
    private const val GUARD = 500

    /**
     * Siguiente ocurrencia estrictamente posterior a [from] según [recurrence].
     *
     * - DAILY: día siguiente.
     * - EVERY_N_DAYS(n): [from] + n días.
     * - WEEKLY sin días marcados: [from] + n semanas.
     * - WEEKLY con días marcados: el siguiente día marcado de la misma semana; si no
     *   queda ninguno, el primer día marcado de la semana n semanas más adelante.
     * - MONTHLY(d): el día d, en este mes si aún no ha pasado, si no en el siguiente.
     *   Se recorta al último día real del mes (31 en febrero → 28 o 29).
     * - EVERY_N_MONTHS(n): [from] + n meses, recortando igual.
     */
    fun advance(from: LocalDate, recurrence: Recurrence): LocalDate {
        val n = recurrence.safeInterval.toLong()
        return when (recurrence.type) {
            RecurrenceType.DAILY -> from.plusDays(1)
            RecurrenceType.EVERY_N_DAYS -> from.plusDays(n)
            RecurrenceType.WEEKLY -> nextWeekly(from, recurrence.weekdays, n)
            RecurrenceType.MONTHLY -> nextMonthly(from, recurrence.dayOfMonth ?: from.dayOfMonth)
            RecurrenceType.EVERY_N_MONTHS -> from.plusMonths(n)
        }
    }

    private fun nextWeekly(from: LocalDate, weekdays: Set<DayOfWeek>, weeks: Long): LocalDate {
        if (weekdays.isEmpty()) return from.plusWeeks(weeks)
        val weekStart = from.with(DayOfWeek.MONDAY)
        fun daysOfWeekStarting(start: LocalDate) =
            weekdays.map { start.plusDays((it.value - 1).toLong()) }
        // Primero, lo que quede por delante en la semana en curso.
        daysOfWeekStarting(weekStart).filter { it.isAfter(from) }.minOrNull()?.let { return it }
        // Si no queda nada, saltamos a la semana objetivo y cogemos su primer día marcado.
        return daysOfWeekStarting(weekStart.plusWeeks(weeks)).min()
    }

    private fun nextMonthly(from: LocalDate, dayOfMonth: Int): LocalDate {
        val day = dayOfMonth.coerceIn(1, 31)
        val thisMonth = from.withDayOfMonth(day.coerceAtMost(from.lengthOfMonth()))
        if (thisMonth.isAfter(from)) return thisMonth
        val next = from.plusMonths(1)
        return next.withDayOfMonth(day.coerceAtMost(next.lengthOfMonth()))
    }

    /**
     * Fecha prevista de la siguiente repetición tras marcar la tarea hecha en [completedOn].
     *
     * Con [AnchorMode.FROM_COMPLETION] la cuenta arranca en el día real: una tarea de
     * cada 30 días completada con tres de retraso vuelve a tocar 30 días después de hoy.
     *
     * Con [AnchorMode.FROM_DUE_DATE] se sigue la rejilla del calendario partiendo de la
     * fecha que tocaba, saltando hacia delante hasta superar el día del completado (si
     * no, una tarea diaria completada con cinco días de retraso seguiría vencida).
     */
    fun nextDueAfterCompletion(task: Task, completedOn: LocalDate): LocalDate =
        when (task.anchorMode) {
            AnchorMode.FROM_COMPLETION -> advance(completedOn, task.recurrence)
            AnchorMode.FROM_DUE_DATE -> {
                var d = advance(task.dueDate, task.recurrence)
                var guard = 0
                while (!d.isAfter(completedOn) && guard++ < GUARD) {
                    d = advance(d, task.recurrence)
                }
                d
            }
        }

    /** Estado de la tarea respecto a [today]. Comprueba primero si ya se hizo hoy. */
    fun status(task: Task, today: LocalDate): TaskStatus = when {
        task.lastCompletedAt == today -> TaskStatus.DONE
        task.dueDate.isBefore(today) -> TaskStatus.OVERDUE
        task.dueDate.isEqual(today) -> TaskStatus.DUE_TODAY
        else -> TaskStatus.FUTURE
    }

    /** Días de atraso acumulado; 0 si no está vencida. No se reinicia al cambiar de día. */
    fun daysOverdue(task: Task, today: LocalDate): Long =
        ChronoUnit.DAYS.between(task.dueDate, today).coerceAtLeast(0)

    /** Días que faltan para la próxima fecha (negativo si está vencida). */
    fun daysUntilDue(task: Task, today: LocalDate): Long =
        ChronoUnit.DAYS.between(today, task.dueDate)

    /**
     * Fechas en que la tarea toca dentro del rango [from, untilInclusive], siguiendo la
     * cadencia a partir de su fecha prevista. Si la tarea está vencida, la fecha atrasada
     * queda fuera del rango y se avanza hasta entrar en él: el atraso se muestra en su
     * propia sección de "Hoy", no repartido por el plan.
     */
    fun occurrencesInRange(
        task: Task,
        from: LocalDate,
        untilInclusive: LocalDate
    ): List<LocalDate> {
        val result = mutableListOf<LocalDate>()
        var d = task.dueDate
        var guard = 0
        while (d.isBefore(from) && guard++ < GUARD) {
            d = advance(d, task.recurrence)
        }
        while (!d.isAfter(untilInclusive) && guard++ < GUARD) {
            result.add(d)
            d = advance(d, task.recurrence)
        }
        return result
    }

    /**
     * Racha actual: completados consecutivos hechos a tiempo, contando desde el más
     * reciente hacia atrás. Se apoya en [Completion.onTime], que ya se calculó y guardó
     * al completar, así que no depende de recorrer toda la cadena.
     */
    fun currentStreak(completions: List<Completion>): Int {
        var streak = 0
        for (c in completions.sortedByDescending { it.completedAt }) {
            if (!c.onTime) break
            streak++
        }
        return streak
    }

    /** Racha más larga registrada, para la pantalla de Rachas. */
    fun longestStreak(completions: List<Completion>): Int {
        var best = 0
        var current = 0
        for (c in completions.sortedBy { it.completedAt }) {
            if (c.onTime) {
                current++
                if (current > best) best = current
            } else {
                current = 0
            }
        }
        return best
    }

    /** Porcentaje de veces completadas a tiempo, 0..100. Devuelve null si no hay historial. */
    fun onTimeRate(completions: List<Completion>): Int? {
        if (completions.isEmpty()) return null
        val onTime = completions.count { it.onTime }
        return (onTime * 100f / completions.size).toInt()
    }
}
