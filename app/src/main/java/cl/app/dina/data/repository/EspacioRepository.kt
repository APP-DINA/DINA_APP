package cl.app.dina.data.repository

import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.NuevoEspacio
import cl.app.dina.data.remote.DinaApi
import cl.app.dina.data.remote.dto.CrearEspacioDto
import cl.app.dina.data.remote.dto.aModelo

/** Catálogo de espacios reservables. */
interface EspacioRepository {
    /** Todos los espacios activos. Lanza una excepción si no hay conexión o el backend falla. */
    suspend fun obtenerEspacios(): List<Espacio>

    /** Un espacio por id: usa el catálogo ya cargado y solo va al backend si no lo tiene. */
    suspend fun obtenerEspacio(id: Long): Espacio

    /** Solo ADMIN. Lo agrega en el backend y en el catálogo guardado (lo verá quien lo vuelva a pedir). */
    suspend fun crear(espacio: NuevoEspacio): Espacio
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

    override suspend fun crear(espacio: NuevoEspacio): Espacio {
        val creado = api.crearEspacio(
            CrearEspacioDto(
                codigo = espacio.codigo,
                nombre = espacio.nombre,
                edificio = espacio.edificio.name,
                piso = espacio.piso,
                tipo = espacio.tipo.name,
                capacidad = espacio.capacidad,
                escuela = espacio.escuela,
                proyector = espacio.proyector,
                computadores = espacio.computadores,
                climatizacion = espacio.climatizacion
            )
        ).aModelo()
        catalogo = catalogo?.plus(creado)
        return creado
    }
}
