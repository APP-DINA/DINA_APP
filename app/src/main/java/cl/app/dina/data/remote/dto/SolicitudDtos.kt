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
    @SerializedName("asistentes") val asistentes: Int,
    @SerializedName("confirmarCruce") val confirmarCruce: Boolean
)

/** Una solicitud (respuesta de POST /api/solicitudes y de GET /api/admin/solicitudes). */
data class SolicitudDto(
    @SerializedName("id") val id: Long,
    @SerializedName("espacio") val espacio: EspacioDto,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("horaInicio") val horaInicio: String,
    @SerializedName("horaFin") val horaFin: String,
    /** Motivo de la reserva. */
    @SerializedName("actividad") val actividad: String,
    @SerializedName("asistentes") val asistentes: Int,
    /** PENDIENTE, APROBADA, RECHAZADA o CANCELADA */
    @SerializedName("estado") val estado: String,
    /** Lo que escribió el administrador al aprobar o rechazar (puede venir vacío). */
    @SerializedName("comentarioAdmin") val comentarioAdmin: String?,
    @SerializedName("solicitante") val solicitante: UsuarioDto,
    /** Se aprobó alguna vez: tiene chat (aunque después se haya cancelado). */
    @SerializedName("tieneChat") val tieneChat: Boolean,
    /** Cuándo se pidió: "2026-11-23T10:15:30.123" */
    @SerializedName("creadaEn") val creadaEn: String?
)

/** Cuerpo de PATCH .../cancelar: opcional para el usuario, obligatorio para el admin. */
data class CancelarDto(
    @SerializedName("motivo") val motivo: String?
)

/** Cuerpo de PATCH /api/admin/solicitudes/{id}/rechazar (el motivo es opcional). */
data class RechazarDto(
    @SerializedName("motivo") val motivo: String?
)
