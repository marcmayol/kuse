package com.marcm.cadencia

import com.marcm.cadencia.domain.model.AnchorMode
import com.marcm.cadencia.domain.model.Completion
import com.marcm.cadencia.domain.model.Recurrence
import com.marcm.cadencia.domain.model.Task
import com.marcm.cadencia.domain.model.TaskStatus
import com.marcm.cadencia.domain.recurrence.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Tests de la lógica de cadencia. Todas las fechas son explícitas: nada lee el reloj. */
class ScheduleTest {

    private fun task(
        recurrence: Recurrence,
        dueDate: LocalDate,
        anchorMode: AnchorMode = AnchorMode.FROM_COMPLETION,
        lastCompletedAt: LocalDate? = null
    ) = Task(
        id = 1L,
        name = "Test",
        categoryId = 1L,
        recurrence = recurrence,
        anchorMode = anchorMode,
        createdAt = dueDate,
        dueDate = dueDate,
        lastCompletedAt = lastCompletedAt
    )

    private fun completion(dueDate: LocalDate, completedOn: LocalDate) = Completion(
        taskId = 1L,
        dueDate = dueDate,
        completedAt = completedOn.atStartOfDay().toInstant(ZoneOffset.UTC),
        onTime = !completedOn.isAfter(dueDate)
    )

    // --- advance ---

    @Test
    fun `diaria avanza un dia`() {
        assertEquals(
            LocalDate.of(2026, 7, 27),
            Schedule.advance(LocalDate.of(2026, 7, 26), Recurrence.Daily)
        )
    }

    @Test
    fun `cada N dias avanza N dias`() {
        assertEquals(
            LocalDate.of(2026, 8, 25),
            Schedule.advance(LocalDate.of(2026, 7, 26), Recurrence.everyNDays(30))
        )
    }

    @Test
    fun `semanal sin dias marcados avanza N semanas`() {
        // 26-jul-2026 es domingo; cada 2 semanas cae en el domingo 9-ago.
        assertEquals(
            LocalDate.of(2026, 8, 9),
            Schedule.advance(LocalDate.of(2026, 7, 26), Recurrence(
                com.marcm.cadencia.domain.model.RecurrenceType.WEEKLY, interval = 2
            ))
        )
    }

    @Test
    fun `semanal con dias marcados toma el siguiente de la misma semana`() {
        // Lunes 27-jul-2026, marcada L y J -> el jueves 30.
        assertEquals(
            LocalDate.of(2026, 7, 30),
            Schedule.advance(
                LocalDate.of(2026, 7, 27),
                Recurrence.weekly(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
            )
        )
    }

    @Test
    fun `semanal con dias marcados salta a la semana siguiente cuando no queda ninguno`() {
        // Jueves 30-jul, marcada L y J -> el lunes 3-ago.
        assertEquals(
            LocalDate.of(2026, 8, 3),
            Schedule.advance(
                LocalDate.of(2026, 7, 30),
                Recurrence.weekly(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
            )
        )
    }

    @Test
    fun `semanal con intervalo salta las semanas indicadas`() {
        // Jueves 30-jul, marcada solo J, cada 3 semanas -> jueves 20-ago.
        assertEquals(
            LocalDate.of(2026, 8, 20),
            Schedule.advance(
                LocalDate.of(2026, 7, 30),
                Recurrence.weekly(DayOfWeek.THURSDAY, interval = 3)
            )
        )
    }

    @Test
    fun `mensual usa el dia del mes y salta al siguiente si ya paso`() {
        // Ancla día 5: desde el 8-jul toca el 5-ago.
        assertEquals(
            LocalDate.of(2026, 8, 5),
            Schedule.advance(LocalDate.of(2026, 7, 8), Recurrence.monthly(5))
        )
    }

    @Test
    fun `mensual se queda en el mes en curso si el dia aun no ha pasado`() {
        assertEquals(
            LocalDate.of(2026, 7, 20),
            Schedule.advance(LocalDate.of(2026, 7, 8), Recurrence.monthly(20))
        )
    }

    @Test
    fun `mensual recorta el dia 31 en los meses cortos`() {
        // Día 31 desde el 31-ene-2026 -> 28-feb (2026 no es bisiesto).
        assertEquals(
            LocalDate.of(2026, 2, 28),
            Schedule.advance(LocalDate.of(2026, 1, 31), Recurrence.monthly(31))
        )
    }

    @Test
    fun `cada N meses avanza N meses`() {
        assertEquals(
            LocalDate.of(2026, 10, 26),
            Schedule.advance(LocalDate.of(2026, 7, 26), Recurrence.everyNMonths(3))
        )
    }

    // --- anchorMode ---

    @Test
    fun `criterio de aceptacion - cada 30 dias completada con 3 de retraso cuenta desde el dia real`() {
        val due = LocalDate.of(2026, 7, 20)
        val realCompletion = LocalDate.of(2026, 7, 23) // tres días tarde
        val t = task(Recurrence.everyNDays(30), due, AnchorMode.FROM_COMPLETION)

        assertEquals(
            LocalDate.of(2026, 8, 22), // 23-jul + 30 días, no 20-jul + 30
            Schedule.nextDueAfterCompletion(t, realCompletion)
        )
    }

    @Test
    fun `mensual con FROM_COMPLETION respeta el dia del mes aunque me retrase`() {
        val t = task(Recurrence.monthly(5), LocalDate.of(2026, 7, 5), AnchorMode.FROM_COMPLETION)
        assertEquals(
            LocalDate.of(2026, 8, 5),
            Schedule.nextDueAfterCompletion(t, LocalDate.of(2026, 7, 8))
        )
    }

    @Test
    fun `FROM_DUE_DATE mantiene la rejilla del calendario`() {
        val t = task(Recurrence.everyNDays(30), LocalDate.of(2026, 7, 20), AnchorMode.FROM_DUE_DATE)
        assertEquals(
            LocalDate.of(2026, 8, 19), // 20-jul + 30, ignorando que se completó el 23
            Schedule.nextDueAfterCompletion(t, LocalDate.of(2026, 7, 23))
        )
    }

    @Test
    fun `FROM_DUE_DATE salta hacia delante hasta superar el dia del completado`() {
        // Diaria con cinco días de atraso: la siguiente no puede quedar en el pasado.
        val t = task(Recurrence.Daily, LocalDate.of(2026, 7, 20), AnchorMode.FROM_DUE_DATE)
        assertEquals(
            LocalDate.of(2026, 7, 26),
            Schedule.nextDueAfterCompletion(t, LocalDate.of(2026, 7, 25))
        )
    }

    // --- estado y atraso ---

    @Test
    fun `criterio de aceptacion - una tarea vencida sigue vencida y acumula dias`() {
        val t = task(Recurrence.Daily, LocalDate.of(2026, 7, 20))

        assertEquals(TaskStatus.OVERDUE, Schedule.status(t, LocalDate.of(2026, 7, 23)))
        assertEquals(3L, Schedule.daysOverdue(t, LocalDate.of(2026, 7, 23)))
        // Pasan tres días más sin tocarla: sigue ahí y el atraso crece.
        assertEquals(TaskStatus.OVERDUE, Schedule.status(t, LocalDate.of(2026, 7, 26)))
        assertEquals(6L, Schedule.daysOverdue(t, LocalDate.of(2026, 7, 26)))
    }

    @Test
    fun `estados de hoy, futuro y hecha`() {
        val today = LocalDate.of(2026, 7, 26)
        assertEquals(TaskStatus.DUE_TODAY, Schedule.status(task(Recurrence.Daily, today), today))
        assertEquals(
            TaskStatus.FUTURE,
            Schedule.status(task(Recurrence.Daily, today.plusDays(2)), today)
        )
        assertEquals(
            TaskStatus.DONE,
            Schedule.status(
                task(Recurrence.Daily, today.plusDays(1), lastCompletedAt = today),
                today
            )
        )
    }

    @Test
    fun `no hay atraso cuando la fecha prevista es futura`() {
        val today = LocalDate.of(2026, 7, 26)
        assertEquals(0L, Schedule.daysOverdue(task(Recurrence.Daily, today.plusDays(4)), today))
    }

    // --- ocurrencias ---

    @Test
    fun `las ocurrencias de una tarea vencida entran en el rango desde su primera fecha valida`() {
        val t = task(Recurrence.everyNDays(3), LocalDate.of(2026, 7, 20))
        val from = LocalDate.of(2026, 7, 26)
        val occurrences = Schedule.occurrencesInRange(t, from, from.plusDays(7))

        assertEquals(
            listOf(
                LocalDate.of(2026, 7, 26),
                LocalDate.of(2026, 7, 29),
                LocalDate.of(2026, 8, 1)
            ),
            occurrences
        )
    }

    // --- racha e historial ---

    @Test
    fun `la racha cuenta los completados a tiempo desde el mas reciente`() {
        val completions = listOf(
            completion(LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 22)), // tarde
            completion(LocalDate.of(2026, 7, 23), LocalDate.of(2026, 7, 23)),
            completion(LocalDate.of(2026, 7, 24), LocalDate.of(2026, 7, 24))
        )
        assertEquals(2, Schedule.currentStreak(completions))
        assertEquals(2, Schedule.longestStreak(completions))
    }

    @Test
    fun `la racha se corta con el ultimo completado tarde`() {
        val completions = listOf(
            completion(LocalDate.of(2026, 7, 23), LocalDate.of(2026, 7, 23)),
            completion(LocalDate.of(2026, 7, 24), LocalDate.of(2026, 7, 26)) // tarde
        )
        assertEquals(0, Schedule.currentStreak(completions))
        assertEquals(1, Schedule.longestStreak(completions))
    }

    @Test
    fun `porcentaje a tiempo`() {
        val completions = listOf(
            completion(LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 20)),
            completion(LocalDate.of(2026, 7, 21), LocalDate.of(2026, 7, 21)),
            completion(LocalDate.of(2026, 7, 22), LocalDate.of(2026, 7, 24)),
            completion(LocalDate.of(2026, 7, 25), LocalDate.of(2026, 7, 25))
        )
        assertEquals(75, Schedule.onTimeRate(completions))
        assertNull(Schedule.onTimeRate(emptyList()))
    }

    @Test
    fun `racha vacia sin historial`() {
        assertEquals(0, Schedule.currentStreak(emptyList()))
        assertEquals(0, Schedule.longestStreak(emptyList()))
    }
}
