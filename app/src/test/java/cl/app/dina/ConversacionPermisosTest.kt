package cl.app.dina

import cl.app.dina.data.model.Chat
import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.EstadoSolicitud
import cl.app.dina.data.model.Solicitante
import cl.app.dina.data.model.SolicitudResumen
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.ui.viewmodel.ConversacionUiState
import cl.app.dina.ui.viewmodel.EstadoCarga
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Quién puede escribir, cancelar la reserva o cerrar el chat. */
class ConversacionPermisosTest {

    private val victor = Solicitante(2, "Victor Godoy", null, null)

    private fun estado(miId: Long, esAdmin: Boolean, abierto: Boolean = true, estado: EstadoSolicitud = EstadoSolicitud.APROBADA) =
        ConversacionUiState(
            carga = EstadoCarga.Listo,
            miId = miId,
            esAdmin = esAdmin,
            chat = Chat(
                SolicitudResumen(
                    1, Espacio(1, "S11", "Sala S11", "NORTE", "Edificio Norte", 1, TipoEspacio.SALA, 35, null, true, false, false),
                    LocalDate.now().plusDays(1), "10:00", "12:00", "Taller", 10, victor, estado, tieneChat = true
                ),
                abierto = abierto,
                ultimoMensaje = null
            )
        )

    @Test
    fun elUsuarioCancelaSuReservaSinMotivoObligatorioYNoCierraElChat() {
        val s = estado(miId = 2, esAdmin = false)
        assertTrue(s.puedeCancelarReserva)
        assertFalse(s.cancelaComoAdmin)
        assertFalse(s.puedeCerrarChat)
    }

    @Test
    fun elAdminCancelaConMotivoYCierraElChat() {
        val s = estado(miId = 1, esAdmin = true)
        assertTrue(s.puedeCancelarReserva)
        assertTrue(s.cancelaComoAdmin)
        assertTrue(s.puedeCerrarChat)
    }

    @Test
    fun canceladaSeguíaAbiertaPeroYaNoSeVuelveACancelar() {
        val s = estado(miId = 1, esAdmin = true, estado = EstadoSolicitud.CANCELADA)
        assertTrue(s.puedeEscribir)
        assertFalse(s.puedeCancelarReserva)
        assertTrue(s.puedeCerrarChat)
    }

    @Test
    fun cerradoNoPermiteNada() {
        val s = estado(miId = 1, esAdmin = true, abierto = false).copy(texto = "hola")
        assertFalse(s.puedeEscribir)
        assertFalse(s.puedeEnviar)
        assertFalse(s.puedeCancelarReserva)
        assertFalse(s.puedeCerrarChat)
    }
}
