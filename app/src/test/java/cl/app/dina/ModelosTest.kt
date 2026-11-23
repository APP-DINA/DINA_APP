package cl.app.dina

import cl.app.dina.data.model.EstadoBloque
import cl.app.dina.data.model.EstadoSolicitud
import cl.app.dina.data.model.ModoTema
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.data.model.TipoNotificacion
import cl.app.dina.soporte.ADMIN
import cl.app.dina.soporte.ALUMNO
import cl.app.dina.soporte.bloque
import cl.app.dina.soporte.mensaje
import cl.app.dina.soporte.solicitud
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reglas de los modelos: valores desconocidos del backend y textos derivados. */
class ModelosTest {

    @Test
    fun valoresDesconocidosDelBackendNoRompenLaApp() {
        assertEquals(TipoEspacio.OTRO, TipoEspacio.desde("PISCINA"))
        assertEquals(TipoNotificacion.OTRO, TipoNotificacion.desde("NUEVO_TIPO"))
        assertEquals(EstadoSolicitud.PENDIENTE, EstadoSolicitud.desde("RARO"))
        assertEquals(EstadoBloque.NO_DISPONIBLE, EstadoBloque.desde("RARO"))
        assertEquals(EstadoSolicitud.APROBADA, EstadoSolicitud.desde("APROBADA"))
    }

    @Test
    fun textosDerivados() {
        assertEquals("Estudiante", ALUMNO.descripcionRol)
        assertEquals("Estudiante · Administrador", ADMIN.descripcionRol)
        assertEquals(2, solicitud(inicio = "10:00", fin = "12:00").horas)
        assertEquals("Sistema", ModoTema.SISTEMA.etiqueta)
        assertTrue(mensaje(1, autorId = null).esDelSistema)
        assertFalse(mensaje(1).esDelSistema)
        assertTrue(TipoNotificacion.MENSAJE_NUEVO.abreChat)
        assertFalse(TipoNotificacion.SOLICITUD_RECHAZADA.abreChat)
    }

    @Test
    fun unBloqueSoloSeEligeSiEstaDisponibleYNoLoPediYa() {
        assertTrue(bloque(1).seleccionable)
        assertFalse(bloque(1, mio = true).seleccionable)
        assertFalse(bloque(1, EstadoBloque.OCUPADO).seleccionable)
    }
}
