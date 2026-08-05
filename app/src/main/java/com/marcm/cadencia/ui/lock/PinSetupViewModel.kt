package com.marcm.cadencia.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.marcm.cadencia.security.AppLockManager
import com.marcm.cadencia.security.AppLockRepository
import com.marcm.cadencia.security.PinHasher
import com.marcm.cadencia.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PasoPin { ELEGIR, REPETIR, PISTA }

data class EstadoPinSetup(
    val paso: PasoPin = PasoPin.ELEGIR,
    val pin: String = "",
    val pista: String = "",
    val error: String? = null,
    val guardando: Boolean = false,
    val terminado: Boolean = false,
    /** Al repetir ya se sabe cuántos dígitos hay que teclear; al elegir, todavía no. */
    val longitudObjetivo: Int = 0,
)

/** Crea el PIN por primera vez o lo cambia; en ambos casos exige repetirlo. */
class PinSetupViewModel(
    private val repo: AppLockRepository,
    private val manager: AppLockManager,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoPinSetup())
    val estado: StateFlow<EstadoPinSetup> = _estado.asStateFlow()

    /** El PIN elegido en el primer paso, a la espera de que se repita. */
    private var elegido = ""

    init {
        // Al cambiar el PIN se parte de la pista que ya tenía.
        viewModelScope.launch {
            val pistaActual = repo.ajustes.first().pista
            if (pistaActual.isNotBlank()) _estado.update { it.copy(pista = pistaActual) }
        }
    }

    fun escribir(digito: Char) {
        val estado = _estado.value
        if (estado.guardando) return
        val tope = if (estado.paso == PasoPin.REPETIR) elegido.length else PinHasher.LONGITUD_MAXIMA
        if (estado.pin.length >= tope) return

        val nuevo = estado.pin + digito
        _estado.update { it.copy(pin = nuevo, error = null) }
        if (estado.paso == PasoPin.REPETIR && nuevo.length == elegido.length) comprobarRepeticion(nuevo)
    }

    fun borrar() {
        _estado.update { it.copy(pin = it.pin.dropLast(1), error = null) }
    }

    /** Sólo tiene sentido en el primer paso: la repetición se valida sola. */
    fun continuar() {
        val pin = _estado.value.pin
        if (!PinHasher.esPinValido(pin)) {
            _estado.update { it.copy(error = "El PIN debe tener entre 4 y 8 dígitos") }
            return
        }
        elegido = pin
        _estado.update {
            it.copy(
                paso = PasoPin.REPETIR,
                pin = "",
                error = null,
                longitudObjetivo = pin.length
            )
        }
    }

    private fun comprobarRepeticion(repetido: String) {
        if (repetido == elegido) {
            _estado.update { it.copy(paso = PasoPin.PISTA, pin = "", error = null) }
        } else {
            elegido = ""
            _estado.update {
                it.copy(
                    paso = PasoPin.ELEGIR,
                    pin = "",
                    error = "No coincidían. Empieza otra vez.",
                    longitudObjetivo = 0
                )
            }
        }
    }

    fun setPista(texto: String) {
        _estado.update { it.copy(pista = texto.take(60)) }
    }

    /** Guarda el PIN y deja la app desbloqueada: acaba de demostrar que es suyo. */
    fun guardar() {
        val pin = elegido
        if (!PinHasher.esPinValido(pin) || _estado.value.guardando) return
        _estado.update { it.copy(guardando = true) }
        viewModelScope.launch {
            repo.guardarPin(pin, _estado.value.pista)
            manager.desbloquear()
            elegido = ""
            _estado.update { it.copy(guardando = false, terminado = true) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                PinSetupViewModel(container.appLockRepository, container.appLockManager)
            }
        }
    }
}
