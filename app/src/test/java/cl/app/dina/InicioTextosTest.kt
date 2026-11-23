package cl.app.dina

import cl.app.dina.ui.components.fechaEvento
import cl.app.dina.ui.components.iniciales
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class InicioTextosTest {

    @Test
    fun inicialesDelPrimerYUltimoNombre() {
        assertEquals("VG", iniciales("Victor Godoy"))
        assertEquals("EG", iniciales("Exequiel Guzman"))
        assertEquals("EP", iniciales("  Estudiante de   Prueba "))
        assertEquals("A", iniciales("Admin"))
        assertEquals("", iniciales(""))
    }

    @Test
    fun fechaDelEventoConHoyYManiana() {
        val lunes = LocalDate.of(2026, 11, 23)
        assertEquals("Hoy · Lun. 23 de noviembre", fechaEvento(lunes, lunes))
        assertEquals("Mañana · Mar. 24 de noviembre", fechaEvento(lunes.plusDays(1), lunes))
        assertEquals("Mié. 25 de noviembre", fechaEvento(lunes.plusDays(2), lunes))
    }
}
