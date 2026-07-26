package com.marcm.cadencia.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.marcm.cadencia.KuseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Acción "Marcar todas" de la notificación resumen de un ámbito: completa las tareas
 * que iban en ese aviso y retira la notificación.
 */
class CompleteDomainReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COMPLETE_DOMAIN) return
        val domainId = intent.getLongExtra(EXTRA_DOMAIN_ID, -1L)
        val taskIds = intent.getLongArrayExtra(EXTRA_TASK_IDS) ?: return
        if (domainId < 0) return

        val app = context.applicationContext as? KuseApp ?: return
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = app.container.taskRepository
                taskIds.forEach { id ->
                    repo.getTask(id)?.let { repo.complete(it.task) }
                }
                NotificationHelper.cancel(context, NotificationHelper.domainNotificationId(domainId))
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE_DOMAIN = "com.marcm.cadencia.COMPLETE_DOMAIN"
        const val EXTRA_DOMAIN_ID = "domain_id"
        const val EXTRA_TASK_IDS = "task_ids"
    }
}
