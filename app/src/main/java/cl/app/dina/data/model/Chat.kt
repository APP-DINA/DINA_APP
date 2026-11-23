package cl.app.dina.data.model

import java.time.LocalDateTime

/**
 * Chat de una solicitud aprobada (pestaña Chat). Lo ven quien la pidió y los administradores.
 * [abierto]: se puede escribir. Se cierra solo cuando termina la reserva, o cuando el admin lo cierra.
 */
data class Chat(
    val solicitud: SolicitudResumen,
    val abierto: Boolean,
    val ultimoMensaje: Mensaje?
)

/** [autor] null = aviso del sistema ("Solicitud aprobada", "Reserva cancelada"...). */
data class Mensaje(
    val id: Long,
    val texto: String,
    val autor: Solicitante?,
    val creadoEn: LocalDateTime
) {
    val esDelSistema: Boolean get() = autor == null
}

/** Un chat con sus mensajes (los nuevos, si se pidió "después de" cierto mensaje). */
data class Conversacion(val chat: Chat, val mensajes: List<Mensaje>)
