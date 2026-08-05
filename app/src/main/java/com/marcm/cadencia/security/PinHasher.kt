package com.marcm.cadencia.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Derivación y verificación del PIN.
 *
 * El PIN nunca se guarda: se guarda su derivación PBKDF2 con una sal aleatoria por
 * usuario. Un PIN de 4-8 dígitos es un espacio pequeño, así que el coste alto de
 * iteraciones es lo único que encarece probarlos todos a quien copie el DataStore.
 */
object PinHasher {

    const val LONGITUD_MINIMA = 4
    const val LONGITUD_MAXIMA = 8

    private const val ALGORITMO = "PBKDF2WithHmacSHA256"
    private const val ITERACIONES = 120_000
    private const val BITS_CLAVE = 256
    private const val BYTES_SAL = 16

    fun generarSal(): String {
        val sal = ByteArray(BYTES_SAL)
        SecureRandom().nextBytes(sal)
        return Base64.getEncoder().encodeToString(sal)
    }

    fun derivar(pin: String, salBase64: String): String {
        val sal = Base64.getDecoder().decode(salBase64)
        val spec = PBEKeySpec(pin.toCharArray(), sal, ITERACIONES, BITS_CLAVE)
        val clave = SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).encoded
        spec.clearPassword()
        return Base64.getEncoder().encodeToString(clave)
    }

    /** Comparación en tiempo constante: no debe filtrar cuántos bytes coincidían. */
    fun coincide(pin: String, salBase64: String, hashBase64: String): Boolean {
        if (salBase64.isBlank() || hashBase64.isBlank()) return false
        val calculado = runCatching { derivar(pin, salBase64) }.getOrNull() ?: return false
        return MessageDigest.isEqual(
            calculado.toByteArray(Charsets.UTF_8),
            hashBase64.toByteArray(Charsets.UTF_8)
        )
    }

    fun esPinValido(pin: String): Boolean =
        pin.length in LONGITUD_MINIMA..LONGITUD_MAXIMA && pin.all { it.isDigit() }
}
