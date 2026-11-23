package cl.app.dina.data.model

import java.time.LocalDateTime

/** Aviso de la pestaña Avisos. [solicitudId]: la solicitud de la que habla (para abrirla). */
data class Notificacion(
    val id: Long,
    val tipo: TipoNotificacion,
    val titulo: String,
    val mensaje: String,
    val solicitudId: Long?,
    val leida: Boolean,
    val creadaEn: LocalDateTime
)

/**
 * Tipos de aviso (mismos nombres que el backend). [abreChat]: al tocarlo se abre el chat de la
 * solicitud; si no, su detalle (p. ej. una rechazada no tiene chat; una nueva se revisa en el detalle).
 */
enum class TipoNotificacion(val abreChat: Boolean) {
    /** Admin: llegó una solicitud por revisar. */
    NUEVA_SOLICITUD(abreChat = false),
    SOLICITUD_APROBADA(abreChat = true),
    SOLICITUD_RECHAZADA(abreChat = false),
    /** El admin canceló una reserva ya aprobada. */
    RESERVA_CANCELADA(abreChat = true),
    /** Admin: el usuario canceló su reserva ya aprobada. */
    CANCELADA_POR_USUARIO(abreChat = true),
    MENSAJE_NUEVO(abreChat = true),
    /** "Tu reserva es hoy". */
    RECORDATORIO_RESERVA(abreChat = false),
    /** Un tipo nuevo del backend que la app aún no conoce. */
    OTRO(abreChat = false);

    companion object {
        fun desde(valor: String): TipoNotificacion = entries.firstOrNull { it.name == valor } ?: OTRO
    }
}
