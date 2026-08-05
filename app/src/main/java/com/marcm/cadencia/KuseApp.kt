package com.marcm.cadencia

import android.app.Application
import com.marcm.actualizador.Actualizador
import com.marcm.actualizador.ActualizadorConfig
import com.marcm.cadencia.data.local.KuseDatabase
import com.marcm.cadencia.data.repository.DomainRepository
import com.marcm.cadencia.data.repository.TaskRepository
import com.marcm.cadencia.notifications.NotificationHelper
import com.marcm.cadencia.notifications.ReminderScheduler
import com.marcm.cadencia.security.AppLockManager
import com.marcm.cadencia.security.AppLockRepository
import com.marcm.cadencia.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
    val appLockRepository = AppLockRepository(app)

    /** Vive lo que vive el proceso: el bloqueo no puede depender de una Activity. */
    private val scopeApp = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val appLockManager = AppLockManager(appLockRepository, scopeApp)
}

class KuseApp : Application() {
    lateinit var container: AppContainer
        private set

    /**
     * Auto-actualización: Kuse se distribuye fuera de Play Store, así que consulta un
     * manifiesto estático en GitHub Pages (nunca la API de GitHub) y se compara por
     * versionCode entero.
     */
    val actualizador: Actualizador by lazy {
        Actualizador(
            app = this,
            config = ActualizadorConfig(
                manifiestoUrl = MANIFIESTO_URL,
                versionCodeActual = BuildConfig.VERSION_CODE,
                checkHorasPorDefecto = 24,
            ),
        )
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannel(this)
        // Comprobación periódica en segundo plano (WorkManager, solo con red).
        actualizador.programarPeriodica()
    }

    companion object {
        const val MANIFIESTO_URL = "https://marcmayol.com/kuse/updates.json"
    }
}
