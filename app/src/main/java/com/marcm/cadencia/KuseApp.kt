package com.marcm.cadencia

import android.app.Application
import com.marcm.cadencia.data.local.KuseDatabase
import com.marcm.cadencia.data.repository.DomainRepository
import com.marcm.cadencia.data.repository.TaskRepository
import com.marcm.cadencia.notifications.NotificationHelper
import com.marcm.cadencia.notifications.ReminderScheduler
import com.marcm.cadencia.settings.SettingsRepository

/**
 * Contenedor simple de dependencias (sin DI externo): se crea una vez en
 * [KuseApp] y las pantallas/ViewModels lo obtienen desde la Application.
 */
class AppContainer(app: Application) {
    val appContext: android.content.Context = app.applicationContext
    private val db = KuseDatabase.get(app)
    val taskRepository = TaskRepository(db.taskDao(), db.completionDao())
    val completionDao = db.completionDao()
    val domainRepository = DomainRepository(db.domainDao(), db.categoryDao(), db.taskDao())
    val settingsRepository = SettingsRepository(app)
    val reminderScheduler = ReminderScheduler(app)
}

class KuseApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannel(this)
    }
}
