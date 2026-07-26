package com.marcm.cadencia.domain.model

import java.time.DayOfWeek

/**
 * Tipo de recurrencia.
 *
 * - [DAILY]: todos los días.
 * - [EVERY_N_DAYS]: cada `interval` días.
 * - [WEEKLY]: cada `interval` semanas. Si hay `weekdays`, en esos días de la semana;
 *   si no, el mismo día de la semana que la última vez.
 * - [MONTHLY]: el día `dayOfMonth` de cada mes (se recorta al último día si el mes es corto).
 * - [EVERY_N_MONTHS]: cada `interval` meses, manteniendo el día ancla.
 */
enum class RecurrenceType {
    DAILY,
    EVERY_N_DAYS,
    WEEKLY,
    MONTHLY,
    EVERY_N_MONTHS;

    companion object {
        fun fromName(name: String): RecurrenceType =
            entries.firstOrNull { it.name == name } ?: DAILY
    }
}

/**
 * Cadencia de una tarea. Los campos que no aplican al [type] se ignoran, de forma que
 * cambiar de tipo en la pantalla de edición no pierde lo que ya había configurado.
 *
 * @param interval número de días/semanas/meses; siempre >= 1.
 * @param weekdays sólo para [RecurrenceType.WEEKLY].
 * @param dayOfMonth sólo para [RecurrenceType.MONTHLY]; 1..31.
 */
data class Recurrence(
    val type: RecurrenceType,
    val interval: Int = 1,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val dayOfMonth: Int? = null
) {
    /** Intervalo saneado: nunca menor que 1, pase lo que pase en la UI o en la base. */
    val safeInterval: Int get() = interval.coerceAtLeast(1)

    /** Etiqueta corta para la línea meta de las tarjetas ("Cada 3 días", "L·X·V"). */
    fun label(): String = when (type) {
        RecurrenceType.DAILY -> "Diaria"
        RecurrenceType.EVERY_N_DAYS ->
            if (safeInterval == 1) "Diaria" else "Cada $safeInterval días"
        RecurrenceType.WEEKLY -> when {
            // Un único día se lee mejor con su nombre ("Sábados") que con la inicial.
            weekdays.size == 1 -> {
                val day = weekdays.first().pluralLabel()
                if (safeInterval == 1) day.replaceFirstChar { it.uppercase() }
                else "$day, cada $safeInterval sem."
            }
            weekdays.isNotEmpty() -> {
                val days = weekdays.sortedBy { it.value }.joinToString("·") { it.shortLabel() }
                if (safeInterval == 1) days else "$days · cada $safeInterval sem."
            }
            safeInterval == 1 -> "Semanal"
            else -> "Cada $safeInterval semanas"
        }
        RecurrenceType.MONTHLY ->
            dayOfMonth?.let { "El día $it de cada mes" } ?: "Mensual"
        RecurrenceType.EVERY_N_MONTHS ->
            if (safeInterval == 1) "Mensual" else "Cada $safeInterval meses"
    }

    companion object {
        val Daily = Recurrence(RecurrenceType.DAILY)

        fun everyNDays(n: Int) = Recurrence(RecurrenceType.EVERY_N_DAYS, interval = n)

        fun weekly(vararg days: DayOfWeek, interval: Int = 1) =
            Recurrence(RecurrenceType.WEEKLY, interval = interval, weekdays = days.toSet())

        fun monthly(dayOfMonth: Int) =
            Recurrence(RecurrenceType.MONTHLY, dayOfMonth = dayOfMonth)

        fun everyNMonths(n: Int) = Recurrence(RecurrenceType.EVERY_N_MONTHS, interval = n)
    }
}

/** Nombre en plural del día ("sábados"), para las recurrencias de un solo día. */
fun DayOfWeek.pluralLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "lunes"
    DayOfWeek.TUESDAY -> "martes"
    DayOfWeek.WEDNESDAY -> "miércoles"
    DayOfWeek.THURSDAY -> "jueves"
    DayOfWeek.FRIDAY -> "viernes"
    DayOfWeek.SATURDAY -> "sábados"
    DayOfWeek.SUNDAY -> "domingos"
}

/** Inicial del día de la semana en español, para las etiquetas de recurrencia. */
fun DayOfWeek.shortLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "L"
    DayOfWeek.TUESDAY -> "M"
    DayOfWeek.WEDNESDAY -> "X"
    DayOfWeek.THURSDAY -> "J"
    DayOfWeek.FRIDAY -> "V"
    DayOfWeek.SATURDAY -> "S"
    DayOfWeek.SUNDAY -> "D"
}

/**
 * Desde dónde se cuenta la siguiente repetición.
 *
 * - [FROM_COMPLETION]: desde el día real en que se marcó hecha. Es lo natural para
 *   hábitos de mantenimiento (si me afeito dos días tarde, los tres días cuentan
 *   desde hoy, no desde la fecha que tocaba).
 * - [FROM_DUE_DATE]: desde la fecha prevista, para lo que va atado al calendario
 *   (el recibo del día 1, la basura de los martes).
 */
enum class AnchorMode(val label: String, val description: String) {
    FROM_COMPLETION("El día que la marco hecha", "La cuenta empieza de nuevo cuando la completo"),
    FROM_DUE_DATE("La fecha prevista", "Mantiene el calendario aunque me retrase");

    companion object {
        fun fromName(name: String): AnchorMode =
            entries.firstOrNull { it.name == name } ?: FROM_COMPLETION
    }
}
