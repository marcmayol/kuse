package com.marcm.cadencia.domain.model

import com.marcm.cadencia.domain.recurrence.Schedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Tarea sugerida de un ámbito. Al activar el ámbito en el onboarding se siembran las
 * que estén marcadas; el resto quedan disponibles como sugerencia.
 *
 * @param categoryKey categoría de destino dentro del ámbito (clave, no id: los ids
 *   los resuelve el repositorio contra la base).
 * @param defaultSelected si viene marcada de serie en el onboarding.
 */
data class Preset(
    val name: String,
    val domain: BuiltInDomain,
    val categoryKey: String,
    val recurrence: Recurrence,
    val reminderTime: LocalTime? = null,
    val anchorMode: AnchorMode = AnchorMode.FROM_COMPLETION,
    val defaultSelected: Boolean = true
) {
    /**
     * Convierte la sugerencia en tarea real.
     *
     * Vence hoy, salvo que la cadencia esté atada al calendario (días concretos de la
     * semana o día fijo del mes): en ese caso arranca en la primera fecha que casa, para
     * que "aspirar los sábados" no aparezca vencida el domingo que activas el ámbito.
     */
    fun toTask(categoryId: Long, today: LocalDate = LocalDate.now()): Task {
        val calendarBound = (recurrence.type == RecurrenceType.WEEKLY && recurrence.weekdays.isNotEmpty()) ||
            recurrence.type == RecurrenceType.MONTHLY
        val firstDue =
            if (calendarBound) Schedule.advance(today.minusDays(1), recurrence) else today

        return Task(
            name = name,
            categoryId = categoryId,
            recurrence = recurrence,
            anchorMode = anchorMode,
            reminderTime = reminderTime,
            createdAt = today,
            dueDate = firstDue
        )
    }
}

object Presets {

    val all: List<Preset> = listOf(
        // --- Higiene y grooming ---
        Preset("Cepillado mañana", BuiltInDomain.HIGIENE, "dental", Recurrence.Daily, LocalTime.of(8, 0)),
        Preset("Cepillado noche", BuiltInDomain.HIGIENE, "dental", Recurrence.Daily, LocalTime.of(22, 0)),
        Preset("Hilo dental", BuiltInDomain.HIGIENE, "dental", Recurrence.Daily, LocalTime.of(22, 0)),
        Preset("Afeitado", BuiltInDomain.HIGIENE, "afeitado", Recurrence.everyNDays(3)),
        Preset("Recortar barba", BuiltInDomain.HIGIENE, "afeitado", Recurrence.everyNDays(5),
            defaultSelected = false),
        Preset("Repasar cejas", BuiltInDomain.HIGIENE, "vello_facial",
            Recurrence.weekly(interval = 2), defaultSelected = false),
        Preset("Pelos de la nariz", BuiltInDomain.HIGIENE, "vello_facial",
            Recurrence.weekly(interval = 2), defaultSelected = false),
        Preset("Limpiador + hidratante", BuiltInDomain.HIGIENE, "piel", Recurrence.Daily, LocalTime.of(8, 0)),
        Preset("Rutina de noche", BuiltInDomain.HIGIENE, "piel", Recurrence.Daily, LocalTime.of(22, 0)),
        Preset("Exfoliante", BuiltInDomain.HIGIENE, "piel",
            Recurrence.weekly(DayOfWeek.SUNDAY), defaultSelected = false),

        // --- Hogar ---
        Preset("Lavar la ropa", BuiltInDomain.HOGAR, "ropa", Recurrence.everyNDays(4)),
        Preset("Cambiar toallas", BuiltInDomain.HOGAR, "ropa", Recurrence.weekly()),
        Preset("Cambiar sábanas", BuiltInDomain.HOGAR, "dormitorio", Recurrence.weekly(interval = 2)),
        Preset("Aspirar la casa", BuiltInDomain.HOGAR, "limpieza",
            Recurrence.weekly(DayOfWeek.SATURDAY)),
        Preset("Baño a fondo", BuiltInDomain.HOGAR, "limpieza", Recurrence.weekly(interval = 2),
            defaultSelected = false),
        Preset("Sacar la basura", BuiltInDomain.HOGAR, "limpieza", Recurrence.everyNDays(2),
            reminderTime = LocalTime.of(21, 0), defaultSelected = false),
        Preset("Regar las plantas", BuiltInDomain.HOGAR, "plantas", Recurrence.everyNDays(4)),
        Preset("Filtro de la campana", BuiltInDomain.HOGAR, "limpieza",
            Recurrence.everyNMonths(3), anchorMode = AnchorMode.FROM_DUE_DATE,
            defaultSelected = false),

        // --- Ejercicio ---
        Preset("Entreno de fuerza", BuiltInDomain.EJERCICIO, "fuerza",
            Recurrence.weekly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            anchorMode = AnchorMode.FROM_DUE_DATE),
        Preset("Salir a correr", BuiltInDomain.EJERCICIO, "cardio",
            Recurrence.weekly(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY),
            anchorMode = AnchorMode.FROM_DUE_DATE, defaultSelected = false),
        Preset("Caminar 30 min", BuiltInDomain.EJERCICIO, "cardio", Recurrence.Daily,
            defaultSelected = false),
        Preset("Estiramientos", BuiltInDomain.EJERCICIO, "movilidad", Recurrence.Daily,
            reminderTime = LocalTime.of(21, 0)),

        // --- Mascotas ---
        Preset("Antiparasitario", BuiltInDomain.MASCOTAS, "salud", Recurrence.everyNDays(30)),
        Preset("Revisión veterinaria", BuiltInDomain.MASCOTAS, "salud",
            Recurrence.everyNMonths(12), anchorMode = AnchorMode.FROM_DUE_DATE,
            defaultSelected = false),
        Preset("Cepillar el pelo", BuiltInDomain.MASCOTAS, "cuidado", Recurrence.everyNDays(3)),
        Preset("Baño", BuiltInDomain.MASCOTAS, "cuidado", Recurrence.everyNDays(30),
            defaultSelected = false),
        Preset("Cortar las uñas", BuiltInDomain.MASCOTAS, "cuidado", Recurrence.everyNMonths(1),
            defaultSelected = false),
        Preset("Limpiar el arenero", BuiltInDomain.MASCOTAS, "entorno", Recurrence.Daily,
            reminderTime = LocalTime.of(20, 0)),
        Preset("Cambiar la arena", BuiltInDomain.MASCOTAS, "entorno", Recurrence.weekly(),
            defaultSelected = false),
        Preset("Lavar comederos", BuiltInDomain.MASCOTAS, "entorno", Recurrence.everyNDays(2))
    )

    fun forDomain(domain: BuiltInDomain): List<Preset> = all.filter { it.domain == domain }

    /** Cuántas sugerencias tiene el ámbito, para el resumen de la tarjeta del onboarding. */
    fun countFor(domain: BuiltInDomain): Int = forDomain(domain).size
}
