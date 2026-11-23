package cl.app.dina.data.repository

import cl.app.dina.data.model.BloqueDia
import cl.app.dina.data.model.EstadoBloque
import cl.app.dina.data.model.NuevaSolicitud
import cl.app.dina.data.model.SolicitudCreada
import cl.app.dina.data.model.SolicitudResumen
import cl.app.dina.data.remote.DinaApi
import cl.app.dina.data.remote.ErrorDelBackend
import cl.app.dina.data.remote.leerErrorDelBackend
import cl.app.dina.data.remote.dto.CancelarDto
import cl.app.dina.data.remote.dto.CrearSolicitudDto
import cl.app.dina.data.remote.dto.RechazarDto
import cl.app.dina.data.remote.dto.aResumen
import cl.app.dina.data.remote.dto.hhmm
import java.time.LocalDate
import retrofit2.HttpException

/** Disponibilidad por bloques, creación de solicitudes y revisión del administrador. */
interface SolicitudRepository {
    /** Los bloques del día con su estado. Lanza una excepción si no hay conexión o el backend falla. */
    suspend fun disponibilidad(espacioId: Long, fecha: LocalDate): List<BloqueDia>

    /**
     * Crea la solicitud (queda PENDIENTE). Si el backend no la acepta lanza [ErrorDelBackend]; con
     * codigo [ErrorDelBackend.CRUCE_PROPIO] si el usuario ya tiene otra a esa hora y falta confirmar.
     */
    suspend fun solicitar(solicitud: NuevaSolicitud): SolicitudCreada

    /** Solo ADMIN: solicitudes pendientes de revisar, de la fecha más cercana a la más lejana. */
    suspend fun porRevisar(): List<SolicitudResumen>

    /** Solicitudes propias que esperan respuesta del administrador, de la más cercana a la más lejana. */
    suspend fun misPendientes(): List<SolicitudResumen>

    /** Una solicitud: la propia, o cualquiera si es ADMIN. */
    suspend fun detalle(id: Long): SolicitudResumen

    /** El usuario cancela su solicitud. */
    suspend fun cancelar(id: Long, motivo: String? = null): SolicitudResumen

    /** Solo ADMIN: cancela una reserva ya aprobada; el [motivo] le llega al usuario. */
    suspend fun cancelarComoAdmin(id: Long, motivo: String): SolicitudResumen

    /** Solo ADMIN. */
    suspend fun aprobar(id: Long): SolicitudResumen

    /** Solo ADMIN. [motivo] opcional: si se escribe, le llega al usuario en la notificación. */
    suspend fun rechazar(id: Long, motivo: String?): SolicitudResumen
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
        val r = try {
            api.crearSolicitud(
                CrearSolicitudDto(
                    espacioId = solicitud.espacioId,
                    fecha = solicitud.fecha.toString(),
                    bloqueIds = solicitud.bloqueIds,
                    actividad = solicitud.motivo,
                    asistentes = solicitud.asistentes,
                    confirmarCruce = solicitud.confirmarCruce
                )
            )
        } catch (e: HttpException) {
            throw e.leerErrorDelBackend()
        }
        return SolicitudCreada(r.id, LocalDate.parse(r.fecha), r.horaInicio.hhmm(), r.horaFin.hhmm(), r.estado)
    }

    override suspend fun porRevisar(): List<SolicitudResumen> =
        api.solicitudesAdmin(estado = PENDIENTE).map { it.aResumen() }

    override suspend fun misPendientes(): List<SolicitudResumen> =
        api.misSolicitudes()
            .filter { it.estado == PENDIENTE }
            .map { it.aResumen() }
            .sortedWith(compareBy({ it.fecha }, { it.inicio }))

    override suspend fun detalle(id: Long) = api.solicitud(id).aResumen()

    override suspend fun cancelar(id: Long, motivo: String?) =
        api.cancelarSolicitud(id, CancelarDto(motivo.limpio())).aResumen()

    override suspend fun cancelarComoAdmin(id: Long, motivo: String) =
        api.cancelarReservaComoAdmin(id, CancelarDto(motivo.limpio())).aResumen()

    override suspend fun aprobar(id: Long) = api.aprobarSolicitud(id).aResumen()

    override suspend fun rechazar(id: Long, motivo: String?) =
        api.rechazarSolicitud(id, RechazarDto(motivo.limpio())).aResumen()

    /** Texto opcional para el backend: sin espacios de sobra, o null si quedó vacío. */
    private fun String?.limpio(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        const val PENDIENTE = "PENDIENTE"
    }
}
