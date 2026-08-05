package com.marcm.cadencia

import com.marcm.cadencia.security.Gracia
import com.marcm.cadencia.security.PinHasher
import com.marcm.cadencia.security.PoliticaBloqueo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Las dos piezas del bloqueo que, si fallan, o dejan entrar a quien no debe o dejan
 * fuera al dueño. El resto (interfaz, DataStore) no se prueba aquí.
 */
class AppLockTest {

    // --- PIN ---

    @Test
    fun `el pin correcto coincide`() {
        val sal = PinHasher.generarSal()
        val hash = PinHasher.derivar("1234", sal)
        assertTrue(PinHasher.coincide("1234", sal, hash))
    }

    @Test
    fun `un pin distinto no coincide`() {
        val sal = PinHasher.generarSal()
        val hash = PinHasher.derivar("1234", sal)
        assertFalse(PinHasher.coincide("1235", sal, hash))
        assertFalse(PinHasher.coincide("12345", sal, hash))
        assertFalse(PinHasher.coincide("", sal, hash))
    }

    @Test
    fun `la misma clave con otra sal da otro hash`() {
        val hashA = PinHasher.derivar("1234", PinHasher.generarSal())
        val hashB = PinHasher.derivar("1234", PinHasher.generarSal())
        assertNotEquals(hashA, hashB)
    }

    @Test
    fun `sin sal ni hash guardados nada coincide`() {
        assertFalse(PinHasher.coincide("1234", "", ""))
    }

    @Test
    fun `solo se aceptan pines de 4 a 8 digitos`() {
        assertTrue(PinHasher.esPinValido("1234"))
        assertTrue(PinHasher.esPinValido("12345678"))
        assertFalse(PinHasher.esPinValido("123"))
        assertFalse(PinHasher.esPinValido("123456789"))
        assertFalse(PinHasher.esPinValido("12a4"))
        assertFalse(PinHasher.esPinValido(""))
    }

    // --- Cuándo volver a pedirlo ---

    @Test
    fun `con gracia inmediata siempre se bloquea al volver`() {
        assertTrue(PoliticaBloqueo.debeBloquear(1_000L, 1_000L, Gracia.INMEDIATA.ms))
    }

    @Test
    fun `dentro de la gracia no se bloquea`() {
        val gracia = Gracia.UN_MINUTO.ms
        assertFalse(PoliticaBloqueo.debeBloquear(0L, 59_000L, gracia))
    }

    @Test
    fun `al cumplirse la gracia se bloquea`() {
        val gracia = Gracia.UN_MINUTO.ms
        assertTrue(PoliticaBloqueo.debeBloquear(0L, 60_000L, gracia))
        assertTrue(PoliticaBloqueo.debeBloquear(0L, 10 * 60_000L, gracia))
    }

    @Test
    fun `una gracia desconocida cae en un minuto`() {
        assertEquals(Gracia.UN_MINUTO, Gracia.desdeNombre(null))
        assertEquals(Gracia.UN_MINUTO, Gracia.desdeNombre("LO_QUE_SEA"))
        assertEquals(Gracia.QUINCE_MINUTOS, Gracia.desdeNombre("QUINCE_MINUTOS"))
    }
}
