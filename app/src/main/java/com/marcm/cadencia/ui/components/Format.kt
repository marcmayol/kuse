package com.marcm.cadencia.ui.components

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private val ES = Locale("es", "ES")

fun weekdayShort(day: DayOfWeek): String =
    day.getDisplayName(TextStyle.SHORT, ES).replaceFirstChar { it.uppercase(ES) }

fun weekdayFull(day: DayOfWeek): String =
    day.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.uppercase(ES) }

/** Inicial del día para la tira de 7 días del Plan. */
fun weekdayInitial(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "L"
    DayOfWeek.TUESDAY -> "M"
    DayOfWeek.WEDNESDAY -> "X"
    DayOfWeek.THURSDAY -> "J"
    DayOfWeek.FRIDAY -> "V"
    DayOfWeek.SATURDAY -> "S"
    DayOfWeek.SUNDAY -> "D"
}

/** "Miércoles, 18 de junio" (cabecera de Hoy). */
fun longDate(date: LocalDate): String {
    val dow = date.dayOfWeek.getDisplayName(TextStyle.FULL, ES)
        .replaceFirstChar { it.uppercase(ES) }
    val month = date.month.getDisplayName(TextStyle.FULL, ES)
    return "$dow, ${date.dayOfMonth} de $month"
}

/** "18 jun" (forma compacta). */
fun shortDate(date: LocalDate): String {
    val month = date.month.getDisplayName(TextStyle.SHORT, ES).lowercase(ES).take(3)
    return "${date.dayOfMonth} $month"
}

/** "Domingo 15 jun" (fila de historial). */
fun weekdayDate(date: LocalDate): String = "${weekdayFull(date.dayOfWeek)} ${shortDate(date)}"

/** Cabecera de día en el Plan: "Hoy", "Mañana", "Sábado 1 ago". */
fun dayHeader(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Hoy"
    today.plusDays(1) -> "Mañana"
    else -> "${weekdayFull(date.dayOfWeek)} ${shortDate(date)}"
}

/** "hace 3 días", "ayer", "hoy". */
fun relativePast(date: LocalDate, today: LocalDate): String {
    val days = today.toEpochDay() - date.toEpochDay()
    return when {
        days <= 0L -> "hoy"
        days == 1L -> "ayer"
        else -> "hace $days días"
    }
}

/** "en 3 días", "mañana", "hoy". */
fun relativeFuture(date: LocalDate, today: LocalDate): String {
    val days = date.toEpochDay() - today.toEpochDay()
    return when {
        days <= 0L -> "hoy"
        days == 1L -> "mañana"
        else -> "en $days días"
    }
}

/** Etiqueta del atraso acumulado: "1 día de atraso", "6 días de atraso". */
fun overdueLabel(days: Long): String =
    if (days == 1L) "1 día de atraso" else "$days días de atraso"

/** "8:00" para la hora del recordatorio. */
fun timeLabel(time: LocalTime): String = "%d:%02d".format(time.hour, time.minute)

/** Saludo de la cabecera según la hora del día. */
fun greeting(time: LocalTime): String = when (time.hour) {
    in 5..12 -> "Buenos días"
    in 13..20 -> "Buenas tardes"
    else -> "Buenas noches"
}
