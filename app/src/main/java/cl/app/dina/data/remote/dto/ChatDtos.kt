package cl.app.dina.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Un mensaje del chat. [autor] null = aviso del sistema ("Solicitud aprobada"...). */
data class MensajeDto(
    @SerializedName("id") val id: Long,
    @SerializedName("texto") val texto: String,
    @SerializedName("autor") val autor: UsuarioDto?,
    /** "2026-11-23T10:15:30.123" */
    @SerializedName("creadoEn") val creadoEn: String
)

/** GET /api/chats: un chat por solicitud aprobada. */
data class ChatDto(
    @SerializedName("solicitud") val solicitud: SolicitudDto,
    /** ABIERTO o CERRADO */
    @SerializedName("estado") val estado: String,
    @SerializedName("ultimoMensaje") val ultimoMensaje: MensajeDto?,
    /** Alguien (usuario o admin) ya escribió; si no, solo hay avisos del sistema. */
    @SerializedName("conversacionIniciada") val conversacionIniciada: Boolean
)

/** GET /api/chats/{id}?despuesDe=N */
data class ChatDetalleDto(
    @SerializedName("chat") val chat: ChatDto,
    @SerializedName("mensajes") val mensajes: List<MensajeDto>
)

data class EnviarMensajeDto(
    @SerializedName("texto") val texto: String
)
