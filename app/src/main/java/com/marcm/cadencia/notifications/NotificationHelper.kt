package com.marcm.cadencia.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.marcm.cadencia.MainActivity
import com.marcm.cadencia.R
import com.marcm.cadencia.domain.model.Domain
import com.marcm.cadencia.domain.model.TaskWithContext

object NotificationHelper {

    const val CHANNEL_ID = "reminders"

    /** Id fija para la notificación de enhorabuena (no colisiona con ids de tareas). */
    private const val CELEBRATION_ID = 990_001

    /** Las notificaciones resumen usan un rango propio para no chocar con las de tarea. */
    private const val DOMAIN_ID_BASE = 900_000

    /** A partir de cuántas tareas del mismo ámbito se agrupa en un solo aviso. */
    const val GROUP_THRESHOLD = 3

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.reminder_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun domainNotificationId(domainId: Long): Int = (DOMAIN_ID_BASE + domainId).toInt()

    /** Aviso de una tarea concreta. */
    fun showReminder(context: Context, item: TaskWithContext, overdue: Boolean, daysOverdue: Long) {
        if (!canNotify(context)) return

        val title = if (overdue) "Se te ha pasado: ${item.task.name}" else "Toca: ${item.task.name}"
        val text = if (overdue) {
            "${item.domain.name} · ${daysOverdue} ${if (daysOverdue == 1L) "día" else "días"} de atraso"
        } else {
            item.metaLine()
        }

        val notification = baseBuilder(context, item.id.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(item.id.toInt(), notification)
        }
    }

    /**
     * Aviso agrupado de un ámbito: "Hogar · 3 tareas hoy", con la lista dentro y una
     * acción para marcarlas todas de una vez.
     */
    fun showDomainSummary(context: Context, domain: Domain, items: List<TaskWithContext>) {
        if (!canNotify(context) || items.isEmpty()) return

        val id = domainNotificationId(domain.id)
        val title = "${domain.name} · ${items.size} tareas hoy"
        val lines = items.map { it.task.name }

        val markAll = PendingIntent.getBroadcast(
            context, id,
            Intent(context, CompleteDomainReceiver::class.java).apply {
                action = CompleteDomainReceiver.ACTION_COMPLETE_DOMAIN
                putExtra(CompleteDomainReceiver.EXTRA_DOMAIN_ID, domain.id)
                putExtra(CompleteDomainReceiver.EXTRA_TASK_IDS, items.map { it.id }.toLongArray())
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
        lines.forEach { style.addLine(it) }

        val notification = baseBuilder(context, id)
            .setContentTitle(title)
            .setContentText(lines.joinToString(" · "))
            .setStyle(style)
            .addAction(R.drawable.ic_notification, "Marcar todas", markAll)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
            .onFailure { android.util.Log.e("KuseReminder", "no se pudo publicar", it) }
    }

    /** Notificación de enhorabuena al completar todas las tareas del día. */
    fun showCelebration(context: Context) {
        if (!canNotify(context)) return

        val text = "Has completado todas tus tareas de hoy. ✨"
        val notification = baseBuilder(context, CELEBRATION_ID)
            .setContentTitle("¡Enhorabuena! 🎉")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(CELEBRATION_ID, notification) }
    }

    fun cancel(context: Context, id: Int) {
        runCatching { NotificationManagerCompat.from(context).cancel(id) }
    }

    private fun baseBuilder(context: Context, requestCode: Int): NotificationCompat.Builder {
        val contentIntent = PendingIntent.getActivity(
            context, requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
    }

    /** Sin permiso POST_NOTIFICATIONS (Android 13+) no se publica nada. */
    private fun canNotify(context: Context): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
