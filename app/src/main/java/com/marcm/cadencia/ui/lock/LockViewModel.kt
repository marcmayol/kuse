package com.marcm.cadencia.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.marcm.cadencia.security.AjustesBloqueo
import com.marcm.cadencia.security.AppLockManager
import com.marcm.cadencia.security.AppLockRepository
import com.marcm.cadencia.security.FALLOS_PARA_LA_PISTA
import com.marcm.cadencia.ui.appContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de la pantalla de desbloqueo. */
class LockViewModel(
    private val repo: AppLockRepository,
    private val manager: AppLockManager,
) : ViewModel() {

    /**
     * null hasta que se lee el DataStore. Importa: si se diera por buena la longitud
     * por defecto, un PIN de 6 dígitos se validaría al cuarto y contaría un fallo que
     * el usuario no ha cometido.
     */
    val ajustes: StateFlow<AjustesBloqueo?> =
        repo.ajustes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _pin = MutableStateFlow("")
    val pin: StateFlow<String> = _pin.asStateFlow()

    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error.asStateFlow()

    private val _mostrarPista = MutableStateFlow(false)
    val mostrarPista: StateFlow<Boolean> = _mostrarPista.asStateFlow()

    private val _esperaSegundos = MutableStateFlow(manager.segundosDeEspera())
    val esperaSegundos: StateFlow<Int> = _esperaSegundos.asStateFlow()

    private val _comprobando = MutableStateFlow(false)
    val comprobando: StateFlow<Boolean> = _comprobando.asStateFlow()

    private var cuentaAtras: Job? = null

    init {
        if (manager.fallosSeguidos >= FALLOS_PARA_LA_PISTA) _mostrarPista.value = true
        if (manager.segundosDeEspera() > 0) arrancarCuentaAtras()
    }

    fun escribir(digito: Char) {
        val longitud = ajustes.value?.longitudPin ?: return
        if (_comprobando.value || _esperaSegundos.value > 0) return
        _error.value = false
        if (_pin.value.length >= longitud) return
        _pin.value += digito
        if (_pin.value.length == longitud) comprobar()
    }

    fun borrar() {
        if (_comprobando.value) return
        _error.value = false
        _pin.value = _pin.value.dropLast(1)
    }

    /** La huella o la cara valen por el PIN: sólo se aceptan si están activadas. */
    fun desbloquearPorBiometria() {
        if (ajustes.value?.biometria != true) return
        manager.desbloquear()
    }

    private fun comprobar() {
        val intento = _pin.value
        _comprobando.value = true
        viewModelScope.launch {
            val correcto = repo.comprobarPin(intento)
            _comprobando.value = false
            if (correcto) {
                _pin.value = ""
                manager.desbloquear()
            } else {
                val fallos = manager.registrarFallo()
                _pin.value = ""
                _error.value = true
                if (fallos >= FALLOS_PARA_LA_PISTA) _mostrarPista.value = true
                if (manager.segundosDeEspera() > 0) arrancarCuentaAtras()
            }
        }
    }

    private fun arrancarCuentaAtras() {
        cuentaAtras?.cancel()
        cuentaAtras = viewModelScope.launch {
            while (true) {
                val restante = manager.segundosDeEspera()
                _esperaSegundos.value = restante
                if (restante <= 0) break
                delay(1_000)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                LockViewModel(container.appLockRepository, container.appLockManager)
            }
        }
    }
}
