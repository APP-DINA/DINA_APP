package cl.app.dina.data.repository

import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.data.remote.DinaApi
import cl.app.dina.data.remote.dto.EspacioDto

/** Catálogo de espacios reservables. */
interface EspacioRepository {
    /** Todos los espacios activos. Lanza una excepción si no hay conexión o el backend falla. */
    suspend fun obtenerEspacios(): List<Espacio>

    /** Un espacio por id: usa el catálogo ya cargado y solo va al backend si no lo tiene. */
    suspend fun obtenerEspacio(id: Long): Espacio
}

/**
 * Lee el catálogo de dina_backend (GET /api/espacios) y lo recuerda mientras la app está abierta:
 * así ReservaEspacio2 recibe el espacio que eligió el usuario en ReservaEspacio sin pedirlo de nuevo.
 */
class RemoteEspacioRepository(private val api: DinaApi) : EspacioRepository {

    @Volatile
    private var catalogo: List<Espacio>? = null

    override suspend fun obtenerEspacios(): List<Espacio> =
        api.espacios().map { it.aModelo() }.also { catalogo = it }

    override suspend fun obtenerEspacio(id: Long): Espacio =
        (catalogo ?: obtenerEspacios()).firstOrNull { it.id == id }
            ?: throw NoSuchElementException("El espacio ya no está disponible")
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
