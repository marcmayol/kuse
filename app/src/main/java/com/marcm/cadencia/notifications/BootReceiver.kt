package com.marcm.cadencia.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.marcm.cadencia.KuseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Reprograma todas las horas de recordatorio tras reiniciar o actualizar la app. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val app = context.applicationContext as? KuseApp ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = app.container.taskRepository.getActiveTasks().map { it.task }
                app.container.reminderScheduler.syncAll(tasks)
            } finally {
                pending.finish()
            }
        }
    }
}
