package com.marcm.cadencia.security

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Decide si toca volver a pedir el PIN. Aparte para poder probarla sin Android:
 * es la única regla del bloqueo que puede dejar al usuario dentro por error.
 */
object PoliticaBloqueo {
    fun debeBloquear(fueraDesdeMs: Long, ahoraMs: Long, graciaMs: Long): Boolean =
        ahoraMs - fueraDesdeMs >= graciaMs
}

/**
 * Estado vivo del bloqueo. Vive en el AppContainer (no en un ViewModel) porque debe
 * sobrevivir a que se recree la Activity: girar la pantalla no es salir de la app.
 *
 * El reloj es [SystemClock.elapsedRealtime], que no retrocede ni se puede adelantar
 * cambiando la hora del sistema.
 */
class AppLockManager(
    private val repo: AppLockRepository,
    scope: CoroutineScope,
    private val reloj: () -> Long = SystemClock::elapsedRealtime,
) {

    private val _desbloqueado = MutableStateFlow(false)

    /** null mientras no se sabe: la pantalla espera antes de enseñar nada. */
    val bloqueado: StateFlow<Boolean?> =
        combine(repo.ajustes.map { it.activo }, _desbloqueado) { activo, desbloqueado ->
            activo && !desbloqueado
        }.stateIn(scope, SharingStarted.Eagerly, null)

    private var fueraDesde: Long? = null
    private var graciaMs: Long = Gracia.UN_MINUTO.ms

    private var fallos = 0
    private var esperaHasta = 0L

    init {
        scope.launch {
            repo.ajustes.collect { graciaMs = it.gracia.ms }
        }
    }

    fun desbloquear() {
        _desbloqueado.value = true
        fueraDesde = null
        fallos = 0
        esperaHasta = 0L
    }

    fun bloquear() {
        _desbloqueado.value = false
    }

    /**
     * La app pasa a segundo plano: se apunta el momento. Si no hay gracia se echa el
     * cerrojo ya, para que ni la vista de apps recientes enseñe el contenido.
     */
    fun alIrAlFondo() {
        if (!_desbloqueado.value) return
        if (graciaMs <= 0L) bloquear() else fueraDesde = reloj()
    }

    /** La app vuelve al frente: se bloquea si estuvo fuera más que la gracia elegida. */
    fun alVolverAlFrente() {
        val salida = fueraDesde ?: return
        fueraDesde = null
        if (PoliticaBloqueo.debeBloquear(salida, reloj(), graciaMs)) bloquear()
    }

    /** Devuelve cuántos fallos seguidos lleva. Cada 5, impone una espera de 30 s. */
    fun registrarFallo(): Int {
        fallos++
        if (fallos % INTENTOS_ANTES_DE_ESPERAR == 0) esperaHasta = reloj() + ESPERA_MS
        return fallos
    }

    val fallosSeguidos: Int get() = fallos

    /** Segundos que faltan antes de poder volver a probar (0 si se puede probar ya). */
    fun segundosDeEspera(): Int {
        val restante = esperaHasta - reloj()
        return if (restante > 0) ((restante + 999) / 1000).toInt() else 0
    }

    private companion object {
        const val INTENTOS_ANTES_DE_ESPERAR = 5
        const val ESPERA_MS = 30_000L
    }
}

/** A partir de tantos fallos se le enseña la pista al usuario. */
const val FALLOS_PARA_LA_PISTA = 3
