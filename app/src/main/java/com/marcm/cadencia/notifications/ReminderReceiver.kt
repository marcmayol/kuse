package com.marcm.cadencia.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.marcm.cadencia.KuseApp
import com.marcm.cadencia.domain.model.TaskStatus
import com.marcm.cadencia.domain.model.TaskWithContext
import com.marcm.cadencia.domain.recurrence.Schedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Se dispara una vez por hora de recordatorio. Reúne todo lo que toca o está vencido a
 * esa hora y lo agrupa por ámbito: a partir de [NotificationHelper.GROUP_THRESHOLD]
 * tareas del mismo ámbito manda un único aviso resumen; con menos, avisos sueltos.
 * Después reprograma la misma hora para mañana.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val minute = intent.getIntExtra(EXTRA_MINUTE, -1)
        if (minute < 0) return

        val app = context.applicationContext as? KuseApp ?: return
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = LocalDate.now()
                val due = app.container.taskRepository.getDueWithReminder(today)
                    .filter { it.task.reminderTime.minuteOfDay() == minute }
                    .filter {
                        val status = Schedule.status(it.task, today)
                        status == TaskStatus.DUE_TODAY || status == TaskStatus.OVERDUE
                    }

                due.groupBy { it.domain.id }.forEach { (_, items) ->
                    notifyGroup(context, items, today)
                }

                // Mientras quede alguna tarea con recordatorio a esta hora, sigue viva.
                val stillUsed = app.container.taskRepository.getActiveTasks()
                    .any { it.task.reminderTime.minuteOfDay() == minute }
                if (stillUsed) {
                    app.container.reminderScheduler.scheduleSlot(minute)
                } else {
                    app.container.reminderScheduler.cancelSlot(minute)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun notifyGroup(context: Context, items: List<TaskWithContext>, today: LocalDate) {
        val domain = items.first().domain
        if (items.size >= NotificationHelper.GROUP_THRESHOLD) {
            NotificationHelper.showDomainSummary(context, domain, items)
        } else {
            items.forEach { item ->
                val overdue = Schedule.status(item.task, today) == TaskStatus.OVERDUE
                NotificationHelper.showReminder(
                    context = context,
                    item = item,
                    overdue = overdue,
                    daysOverdue = Schedule.daysOverdue(item.task, today)
                )
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.marcm.cadencia.REMINDER_FIRE"
        const val EXTRA_MINUTE = "minute_of_day"
    }
}

/** Minuto del día de una hora de recordatorio, o -1 si la tarea no tiene. */
private fun java.time.LocalTime?.minuteOfDay(): Int =
    this?.let { it.hour * 60 + it.minute } ?: -1
