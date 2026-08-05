package com.marcm.cadencia.security

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.lockDataStore by preferencesDataStore(name = "app_lock")

/** Cuánto puede estar la app en segundo plano antes de volver a pedir el PIN. */
enum class Gracia(val ms: Long, val etiqueta: String) {
    INMEDIATA(0L, "Siempre que salgo"),
    UN_MINUTO(60_000L, "Si paso 1 minuto fuera"),
    CINCO_MINUTOS(5 * 60_000L, "Si paso 5 minutos fuera"),
    QUINCE_MINUTOS(15 * 60_000L, "Si paso 15 minutos fuera");

    companion object {
        fun desdeNombre(nombre: String?): Gracia =
            entries.firstOrNull { it.name == nombre } ?: UN_MINUTO
    }
}

/** Lo que la interfaz necesita saber del bloqueo (nunca incluye el hash ni la sal). */
data class AjustesBloqueo(
    val activo: Boolean = false,
    val biometria: Boolean = false,
    val gracia: Gracia = Gracia.UN_MINUTO,
    val longitudPin: Int = PinHasher.LONGITUD_MINIMA,
    val pista: String = "",
)

/**
 * Ajustes del bloqueo, en un DataStore propio ("app_lock") separado del de la app:
 * así el secreto vive en un archivo distinto y desactivar el bloqueo no toca nada más.
 */
class AppLockRepository(private val context: Context) {

    private val claveActivo = booleanPreferencesKey("lock_enabled")
    private val claveBiometria = booleanPreferencesKey("biometric_enabled")
    private val claveGracia = stringPreferencesKey("lock_grace")
    private val claveHash = stringPreferencesKey("pin_hash")
    private val claveSal = stringPreferencesKey("pin_salt")
    private val claveLongitud = intPreferencesKey("pin_length")
    private val clavePista = stringPreferencesKey("pin_hint")

    val ajustes: Flow<AjustesBloqueo> = context.lockDataStore.data.map { prefs ->
        val hayPin = !prefs[claveHash].isNullOrBlank()
        AjustesBloqueo(
            // Sin PIN guardado el bloqueo no puede estar activo: evita dejar fuera al
            // usuario si la escritura del PIN se quedó a medias.
            activo = (prefs[claveActivo] ?: false) && hayPin,
            biometria = prefs[claveBiometria] ?: false,
            gracia = Gracia.desdeNombre(prefs[claveGracia]),
            longitudPin = prefs[claveLongitud] ?: PinHasher.LONGITUD_MINIMA,
            pista = prefs[clavePista].orEmpty(),
        )
    }

    /** Guarda el PIN (derivado) y deja el bloqueo activo. Sirve también para cambiarlo. */
    suspend fun guardarPin(pin: String, pista: String) {
        require(PinHasher.esPinValido(pin)) { "PIN inválido" }
        val sal = PinHasher.generarSal()
        val hash = withContext(Dispatchers.Default) { PinHasher.derivar(pin, sal) }
        context.lockDataStore.edit { prefs ->
            prefs[claveSal] = sal
            prefs[claveHash] = hash
            prefs[claveLongitud] = pin.length
            prefs[clavePista] = pista.trim()
            prefs[claveActivo] = true
        }
    }

    suspend fun comprobarPin(pin: String): Boolean {
        val prefs = context.lockDataStore.data.first()
        val sal = prefs[claveSal] ?: return false
        val hash = prefs[claveHash] ?: return false
        return withContext(Dispatchers.Default) { PinHasher.coincide(pin, sal, hash) }
    }

    /** Desactiva el bloqueo y borra el secreto: no queda rastro del PIN anterior. */
    suspend fun desactivar() {
        context.lockDataStore.edit { prefs ->
            prefs.remove(claveHash)
            prefs.remove(claveSal)
            prefs.remove(claveLongitud)
            prefs.remove(clavePista)
            prefs[claveActivo] = false
            prefs[claveBiometria] = false
        }
    }

    suspend fun setBiometria(activa: Boolean) {
        context.lockDataStore.edit { it[claveBiometria] = activa }
    }

    suspend fun setGracia(gracia: Gracia) {
        context.lockDataStore.edit { it[claveGracia] = gracia.name }
    }

    suspend fun setPista(pista: String) {
        context.lockDataStore.edit { it[clavePista] = pista.trim() }
    }
}
