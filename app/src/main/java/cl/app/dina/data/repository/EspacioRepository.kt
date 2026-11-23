package cl.app.dina.data.repository

import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.data.remote.DinaApi
import cl.app.dina.data.remote.dto.EspacioDto

/** Catálogo de espacios reservables. */
interface EspacioRepository {
    /** Todos los espacios activos. Lanza una excepción si no hay conexión o el backend falla. */
    suspend fun obtenerEspacios(): List<Espacio>
}

/** Lee el catálogo de dina_backend (GET /api/espacios). */
class RemoteEspacioRepository(private val api: DinaApi) : EspacioRepository {

    override suspend fun obtenerEspacios(): List<Espacio> = api.espacios().map { it.aModelo() }
}

private fun EspacioDto.aModelo() = Espacio(
    id = id,
    codigo = codigo,
    nombre = nombre,
    edificio = edificio,
    edificioNombre = edificioNombre,
    piso = piso,
    tipo = TipoEspacio.desde(tipo),
    capacidad = capacidad,
    escuela = escuela,
    proyector = proyector,
    computadores = computadores,
    climatizacion = climatizacion
)
