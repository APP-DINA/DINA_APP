package cl.app.dina

import cl.app.dina.data.model.Chat
import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.EstadoSolicitud
import cl.app.dina.data.model.Solicitante
import cl.app.dina.data.model.SolicitudResumen
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.ui.viewmodel.ChatUiState
import cl.app.dina.ui.viewmodel.PestaniaChat
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** En qué pestaña de Chat cae cada chat. */
class PestaniasChatTest {

    private fun chat(abierto: Boolean, iniciado: Boolean, estado: EstadoSolicitud = EstadoSolicitud.APROBADA) = Chat(
        SolicitudResumen(
            1, Espacio(1, "S11", "Sala S11", "NORTE", "Edificio Norte", 1, TipoEspacio.SALA, 35, null, true, false, false),
            LocalDate.now().plusDays(1), "10:00", "12:00", "Taller", 10,
            Solicitante(2, "Victor Godoy", null, null), estado, tieneChat = true
        ),
        abierto = abierto,
        ultimoMensaje = null,
        iniciado = iniciado
    )

    private val estado = ChatUiState()

    @Test
    fun aceptadaSinMensajesVaAAceptados() {
        assertEquals(PestaniaChat.ACEPTADOS, estado.pestaniaDe(chat(abierto = true, iniciado = false)))
    }

    @Test
    fun conMensajesVaAAbiertos() {
        assertEquals(PestaniaChat.ABIERTOS, estado.pestaniaDe(chat(abierto = true, iniciado = true)))
    }

    @Test
    fun canceladaSinMensajesVaAAbiertosParaAclararla() {
        assertEquals(
            PestaniaChat.ABIERTOS,
            estado.pestaniaDe(chat(abierto = true, iniciado = false, estado = EstadoSolicitud.CANCELADA))
        )
    }

    @Test
    fun cerradoVaACerradosAunqueNadieHayaEscrito() {
        assertEquals(PestaniaChat.CERRADOS, estado.pestaniaDe(chat(abierto = false, iniciado = false)))
        assertEquals(PestaniaChat.CERRADOS, estado.pestaniaDe(chat(abierto = false, iniciado = true)))
    }
}
