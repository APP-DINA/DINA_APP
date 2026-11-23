package cl.app.dina.data.remote.dto

import com.google.gson.annotations.SerializedName

/** GET /api/espacios/{id}/disponibilidad?fecha=AAAA-MM-DD */
data class DisponibilidadDto(
    @SerializedName("fecha") val fecha: String,
    @SerializedName("bloques") val bloques: List<BloqueDisponibilidadDto>
)

data class BloqueDisponibilidadDto(
    @SerializedName("bloqueId") val bloqueId: Long,
    @SerializedName("numero") val numero: Int,
    /** "10:00:00" */
    @SerializedName("horaInicio") val horaInicio: String,
    @SerializedName("horaFin") val horaFin: String,
    /** DISPONIBLE, OCUPADO o NO_DISPONIBLE */
    @SerializedName("estado") val estado: String,
    @SerializedName("solicitudesPendientes") val solicitudesPendientes: Int,
    @SerializedName("solicitadoPorMi") val solicitadoPorMi: Boolean
)

/** Cuerpo de POST /api/solicitudes */
data class CrearSolicitudDto(
    @SerializedName("espacioId") val espacioId: Long,
    /** "AAAA-MM-DD" */
    @SerializedName("fecha") val fecha: String,
    @SerializedName("bloqueIds") val bloqueIds: List<Long>,
    /** Motivo de la reserva (el backend lo llama "actividad"). */
    @SerializedName("actividad") val actividad: String,
    @SerializedName("asistentes") val asistentes: Int
)

/** Respuesta de POST /api/solicitudes (solo los campos que usa la app). */
data class SolicitudDto(
    @SerializedName("id") val id: Long,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("horaInicio") val horaInicio: String,
    @SerializedName("horaFin") val horaFin: String,
    /** PENDIENTE, APROBADA, RECHAZADA o CANCELADA */
    @SerializedName("estado") val estado: String
)
