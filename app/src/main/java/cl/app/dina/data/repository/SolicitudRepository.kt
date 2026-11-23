package cl.app.dina.data.repository

import cl.app.dina.data.model.BloqueDia
import cl.app.dina.data.model.EstadoBloque
import cl.app.dina.data.model.NuevaSolicitud
import cl.app.dina.data.model.SolicitudCreada
import cl.app.dina.data.remote.DinaApi
import cl.app.dina.data.remote.dto.CrearSolicitudDto
import java.time.LocalDate

/** Disponibilidad por bloques y creación de solicitudes. */
interface SolicitudRepository {
    /** Los bloques del día con su estado. Lanza una excepción si no hay conexión o el backend falla. */
    suspend fun disponibilidad(espacioId: Long, fecha: LocalDate): List<BloqueDia>

    /** Crea la solicitud (queda PENDIENTE). Lanza HttpException con el mensaje del backend si no se acepta. */
    suspend fun solicitar(solicitud: NuevaSolicitud): SolicitudCreada
}

class RemoteSolicitudRepository(private val api: DinaApi) : SolicitudRepository {

    override suspend fun disponibilidad(espacioId: Long, fecha: LocalDate): List<BloqueDia> =
        api.disponibilidad(espacioId, fecha.toString()).bloques.map {
            BloqueDia(
                id = it.bloqueId,
                numero = it.numero,
                inicio = it.horaInicio.hhmm(),
                fin = it.horaFin.hhmm(),
                estado = EstadoBloque.desde(it.estado),
                pendientes = it.solicitudesPendientes,
                mio = it.solicitadoPorMi
            )
        }

    override suspend fun solicitar(solicitud: NuevaSolicitud): SolicitudCreada {
        val r = api.crearSolicitud(
            CrearSolicitudDto(
                espacioId = solicitud.espacioId,
                fecha = solicitud.fecha.toString(),
                bloqueIds = solicitud.bloqueIds,
                actividad = solicitud.motivo,
                asistentes = solicitud.asistentes
            )
        )
        return SolicitudCreada(r.id, LocalDate.parse(r.fecha), r.horaInicio.hhmm(), r.horaFin.hhmm(), r.estado)
    }
}

/** "10:00:00" → "10:00" */
private fun String.hhmm(): String = take(5)
