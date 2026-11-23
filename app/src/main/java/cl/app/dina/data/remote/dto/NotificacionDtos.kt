package cl.app.dina.data.remote.dto

import com.google.gson.annotations.SerializedName

/** GET /api/notificaciones */
data class NotificacionDto(
    @SerializedName("id") val id: Long,
    /** NUEVA_SOLICITUD, SOLICITUD_APROBADA, MENSAJE_NUEVO... */
    @SerializedName("tipo") val tipo: String,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("mensaje") val mensaje: String,
    @SerializedName("solicitudId") val solicitudId: Long?,
    @SerializedName("leida") val leida: Boolean,
    /** "2026-11-23T10:15:30.123" */
    @SerializedName("creadaEn") val creadaEn: String
)

/** GET /api/notificaciones/no-leidas */
data class ConteoDto(
    @SerializedName("noLeidas") val noLeidas: Long
)
