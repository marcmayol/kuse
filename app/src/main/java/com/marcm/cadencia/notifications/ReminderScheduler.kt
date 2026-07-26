package com.marcm.cadencia.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.marcm.cadencia.domain.model.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Programa los recordatorios con AlarmManager.
 *
 * Una alarma por **hora de recordatorio**, no por tarea: si cinco tareas avisan a las
 * 8:00, salta una sola alarma y [ReminderReceiver] decide qué notificar y cómo
 * agruparlo. Así el móvil no encadena cinco avisos seguidos.
 *
 * En Android 12+ se intenta alarma exacta si hay permiso; si no, una ventana inexacta.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    private val prefs by lazy {
        context.getSharedPreferences("reminder_slots", Context.MODE_PRIVATE)
    }

    /** Asegura la alarma de la hora de esta tarea. */
    fun schedule(task: Task) {
        val time = task.reminderTime
        if (!task.isActive || time == null) return
        scheduleSlot(time.hour * 60 + time.minute)
    }

    /**
     * No hace nada a propósito: las alarmas son por hora y pueden estar compartidas con
     * otras tareas. Cuando a esa hora ya no queda nada, [ReminderReceiver] deja de
     * reprogramarla.
     */
    fun cancel(task: Task) = Unit

    /**
     * Deja programadas exactamente las horas que necesitan las tareas dadas y cancela
     * las que sobran. Es lo que se llama tras el onboarding, al cambiar de ámbitos o al
     * reiniciar el dispositivo.
     */
    fun syncAll(tasks: List<Task>) {
        val wanted = tasks
            .mapNotNull { task ->
                task.reminderTime?.takeIf { task.isActive }?.let { it.hour * 60 + it.minute }
            }
            .toSet()

        (storedSlots() - wanted).forEach { cancelSlot(it) }
        wanted.forEach { scheduleSlot(it) }
    }

    /** Programa (o reprograma) una hora concreta. La usa también el receptor al disparar. */
    fun scheduleSlot(minuteOfDay: Int) {
        val am = alarmManager ?: return
        val zone = ZoneId.systemDefault()
        val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)

        var next = LocalDateTime.of(LocalDate.now(), time)
        if (!next.isAfter(LocalDateTime.now())) next = next.plusDays(1)
        val triggerAt = next.atZone(zone).toInstant().toEpochMilli()

        val pi = pendingIntent(minuteOfDay)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 60 * 60 * 1000L, pi)
        }
        storeSlots(storedSlots() + minuteOfDay)
    }

    fun cancelSlot(minuteOfDay: Int) {
        alarmManager?.cancel(pendingIntent(minuteOfDay))
        storeSlots(storedSlots() - minuteOfDay)
    }

    private fun pendingIntent(minuteOfDay: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            putExtra(ReminderReceiver.EXTRA_MINUTE, minuteOfDay)
        }
        return PendingIntent.getBroadcast(
            context, REQUEST_BASE + minuteOfDay, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun storedSlots(): Set<Int> =
        prefs.getStringSet(KEY_SLOTS, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()

    private fun storeSlots(slots: Set<Int>) {
        prefs.edit().putStringSet(KEY_SLOTS, slots.map { it.toString() }.toSet()).apply()
    }

    private companion object {
        const val KEY_SLOTS = "slots"

        /** Separa los requestCode de las alarmas de los de las notificaciones. */
        const val REQUEST_BASE = 10_000
    }
}
