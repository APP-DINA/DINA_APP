package cl.app.dina

import cl.app.dina.ui.viewmodel.Calendario
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarioTest {

    private val hoy = LocalDate.of(2026, 11, 23)
    private val calendario = Calendario(hoy)

    @Test
    fun carruselEsUnaSemanaDesdeHoy() {
        assertEquals((0L..6L).map { hoy.plusDays(it) }, calendario.diasCarrusel)
        assertFalse(calendario.lejosDeHoy)
    }

    @Test
    fun soloPermiteDeHoyAFechaMaxima() {
        assertFalse(calendario.permite(hoy.minusDays(1)))
        assertTrue(calendario.permite(hoy))
        assertTrue(calendario.permite(hoy.plusDays(30)))
        assertFalse(calendario.permite(hoy.plusDays(31)))
        // Elegir fuera del rango lo ajusta al borde
        assertEquals(hoy, calendario.elegir(hoy.minusDays(5)).fecha)
        assertEquals(hoy.plusDays(30), calendario.elegir(hoy.plusDays(90)).fecha)
    }

    @Test
    fun diaLejanoMueveElCarruselYOfreceVolverAHoy() {
        val lejano = calendario.elegir(hoy.plusDays(17))
        assertEquals(hoy.plusDays(17), lejano.diasCarrusel.first())
        assertTrue(lejano.lejosDeHoy)
        // Un día dentro de esta semana no mueve el carrusel
        assertEquals(hoy, calendario.elegir(hoy.plusDays(6)).diasCarrusel.first())
    }

    @Test
    fun alDiaNoDejaLaFechaEnElPasado() {
        val elegido = calendario.elegir(hoy.plusDays(1))
        val manana = elegido.alDia(ahora = hoy.plusDays(3))
        assertEquals(hoy.plusDays(3), manana.hoy)
        assertEquals(hoy.plusDays(3), manana.fecha)
    }
}
