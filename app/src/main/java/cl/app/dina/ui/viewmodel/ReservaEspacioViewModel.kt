package cl.app.dina.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.data.remote.aMensaje
import cl.app.dina.data.repository.EspacioRepository
import java.text.Normalizer
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Cómo va la carga del catálogo. */
sealed interface EstadoCarga {
    data object Cargando : EstadoCarga
    data class Error(val mensaje: String) : EstadoCarga
    data object Listo : EstadoCarga
}

/** Filtro de escuela: una escuela concreta, o los espacios de uso general. */
sealed interface FiltroEscuela {
    /** Nombre del enum Escuela de ui/theme ("INFORMATICA"…), o null para uso general. */
    val codigo: String?

    data class De(override val codigo: String) : FiltroEscuela
    data object UsoGeneral : FiltroEscuela {
        override val codigo: String? = null
    }
}

/** Lo que el usuario eligió para acotar la lista. */
data class Filtros(
    val busqueda: String = "",
    val tipo: TipoEspacio? = null,
    val escuela: FiltroEscuela? = null
) {
    val activos: Boolean get() = busqueda.isNotBlank() || tipo != null || escuela != null
}

/**
 * Día elegido para la reserva y sus reglas: desde [hoy] hasta [fechaMaxima].
 * Todas las reglas de fechas de la pantalla viven aquí, en un solo lugar.
 */
data class Calendario(val hoy: LocalDate, val fecha: LocalDate = hoy) {
    val fechaMaxima: LocalDate get() = hoy.plusDays(DIAS_ANTICIPACION)

    /** El carrusel parte hoy; si se eligió un día fuera de esta semana, parte en ese día. */
    private val inicioCarrusel: LocalDate
        get() = if (fecha.isAfter(hoy.plusDays(DIAS_CARRUSEL - 1))) fecha else hoy

    val diasCarrusel: List<LocalDate> get() = (0 until DIAS_CARRUSEL).map { inicioCarrusel.plusDays(it) }

    /** true cuando el carrusel no muestra hoy: la pantalla ofrece el atajo "Hoy". */
    val lejosDeHoy: Boolean get() = inicioCarrusel != hoy

    /** ¿Se puede reservar ese día? (de hoy a la fecha máxima) */
    fun permite(dia: LocalDate): Boolean = dia in hoy..fechaMaxima

    /** Elige un día, siempre dentro del rango permitido. */
    fun elegir(dia: LocalDate): Calendario = copy(fecha = dia.coerceIn(hoy, fechaMaxima))

    /** Si la app quedó abierta de un día para otro, mueve "hoy" y no deja la fecha en el pasado. */
    fun alDia(ahora: LocalDate = LocalDate.now()): Calendario =
        if (ahora == hoy) this else Calendario(ahora).elegir(fecha)

    companion object {
        /** Días del carrusel (una semana). */
        const val DIAS_CARRUSEL = 7L

        /** Igual que la regla de dina_backend (dina.solicitudes.max-dias-anticipacion). */
        const val DIAS_ANTICIPACION = 30L
    }
}

/** Espacios de un edificio, para mostrarlos agrupados con un título. */
data class GrupoEdificio(val edificioNombre: String, val espacios: List<Espacio>)

data class ReservaEspacioUiState(
    val carga: EstadoCarga = EstadoCarga.Cargando,
    /** true mientras se recarga con la lista ya visible (gesto de deslizar hacia abajo). */
    val refrescando: Boolean = false,
    val filtros: Filtros = Filtros(),
    /** Opciones de los menús: solo las que existen en el catálogo. */
    val tiposDisponibles: List<TipoEspacio> = emptyList(),
    val escuelasDisponibles: List<FiltroEscuela> = emptyList(),
    val calendario: Calendario = Calendario(LocalDate.now()),
    /** Resultado de aplicar los filtros, agrupado por edificio. */
    val grupos: List<GrupoEdificio> = emptyList()
)

/**
 * Catálogo de espacios con buscador, filtros por tipo y escuela, y día de la reserva.
 * El catálogo se pide una vez al backend; buscar y filtrar se hace en el teléfono.
 */
class ReservaEspacioViewModel(private val espacioRepository: EspacioRepository) : ViewModel() {

    /** Estado interno: lo mismo que ve la UI, más el catálogo completo ya preparado. */
    private data class Estado(
        val carga: EstadoCarga = EstadoCarga.Cargando,
        val refrescando: Boolean = false,
        val catalogo: Catalogo? = null,
        val filtros: Filtros = Filtros(),
        val calendario: Calendario = Calendario(LocalDate.now())
    )

    private val estado = MutableStateFlow(Estado())

    val uiState: StateFlow<ReservaEspacioUiState> = estado
        .map { e ->
            ReservaEspacioUiState(
                carga = e.carga,
                refrescando = e.refrescando,
                filtros = e.filtros,
                tiposDisponibles = e.catalogo?.tipos.orEmpty(),
                escuelasDisponibles = e.catalogo?.escuelas.orEmpty(),
                calendario = e.calendario.alDia(),
                grupos = e.catalogo?.filtrar(e.filtros).orEmpty()
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReservaEspacioUiState())

    private var trabajoCarga: Job? = null

    init {
        cargar(refresco = false)
    }

    // Filtros
    fun onBusquedaChange(texto: String) = cambiarFiltros { copy(busqueda = texto) }

    /** null = "Todos". */
    fun seleccionarTipo(tipo: TipoEspacio?) = cambiarFiltros { copy(tipo = tipo) }

    /** null = "Todas". */
    fun seleccionarEscuela(escuela: FiltroEscuela?) = cambiarFiltros { copy(escuela = escuela) }

    /** Limpia búsqueda y filtros; la fecha elegida se mantiene. */
    fun limpiarFiltros() = cambiarFiltros { Filtros() }

    private fun cambiarFiltros(cambio: Filtros.() -> Filtros) =
        estado.update { it.copy(filtros = it.filtros.cambio()) }

    // Fecha
    fun seleccionarFecha(dia: LocalDate) =
        estado.update { it.copy(calendario = it.calendario.alDia().elegir(dia)) }

    // Carga
    fun reintentar() = cargar(refresco = false)

    fun refrescar() = cargar(refresco = true)

    private fun cargar(refresco: Boolean) {
        trabajoCarga?.cancel()
        trabajoCarga = viewModelScope.launch {
            // Con lista visible y gesto de refrescar, la lista se mantiene; si no, se muestra el indicador
            estado.update {
                if (refresco && it.catalogo != null) it.copy(refrescando = true)
                else it.copy(carga = EstadoCarga.Cargando)
            }
            try {
                val catalogo = Catalogo(espacioRepository.obtenerEspacios())
                estado.update { it.copy(carga = EstadoCarga.Listo, catalogo = catalogo) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Si ya había lista, se conserva; si no, se muestra el error
                estado.update {
                    it.copy(carga = if (it.catalogo != null) EstadoCarga.Listo else EstadoCarga.Error(e.aMensaje()))
                }
            } finally {
                estado.update { it.copy(refrescando = false) }
            }
        }
    }
}

/**
 * Catálogo preparado UNA vez al llegar del backend: ordenado, con su texto de búsqueda
 * normalizado y con las opciones de los filtros. Al escribir o filtrar solo se recorre la lista.
 */
private class Catalogo(espacios: List<Espacio>) {

    private val ordenados = espacios.sortedWith(ORDEN)

    /** "s11 sala s11" — código y nombre en minúsculas y sin tildes, por espacio. */
    private val textoBusqueda = ordenados.associate { it.id to normalizar("${it.codigo} ${it.nombre}") }

    val tipos: List<TipoEspacio> = TipoEspacio.entries.filter { t -> ordenados.any { it.tipo == t } }

    val escuelas: List<FiltroEscuela> =
        ordenados.mapNotNull { it.escuela }.distinct().sorted().map { FiltroEscuela.De(it) } +
            listOfNotNull(FiltroEscuela.UsoGeneral.takeIf { ordenados.any { it.escuela == null } })

    fun filtrar(filtros: Filtros): List<GrupoEdificio> {
        val consulta = normalizar(filtros.busqueda)
        return ordenados
            .filter { espacio ->
                (consulta.isEmpty() || textoBusqueda.getValue(espacio.id).contains(consulta)) &&
                    (filtros.tipo == null || espacio.tipo == filtros.tipo) &&
                    // UsoGeneral tiene código null: coincide con los espacios sin escuela
                    (filtros.escuela == null || espacio.escuela == filtros.escuela.codigo)
            }
            .groupBy { it.edificioNombre }
            .map { (nombre, lista) -> GrupoEdificio(nombre, lista) }
    }

    private companion object {
        /** Orden de los edificios como en el mapa de la sede. */
        val ORDEN_EDIFICIOS = listOf("NORTE", "CENTRAL", "SUR", "CTI")

        /** Edificio → piso → código en orden natural (LC2 antes que LC10). */
        val ORDEN: Comparator<Espacio> = compareBy<Espacio>(
            { ORDEN_EDIFICIOS.indexOf(it.edificio).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE },
            { it.piso },
            { it.codigo.takeWhile { c -> !c.isDigit() } },
            { it.codigo.dropWhile { c -> !c.isDigit() }.takeWhile { c -> c.isDigit() }.toIntOrNull() ?: 0 },
            { it.codigo }
        )

        val MARCAS = Regex("\\p{Mn}+")

        /** Minúsculas y sin tildes: "diseno" encuentra "Diseño", "s11" encuentra "S11". */
        fun normalizar(texto: String): String =
            Normalizer.normalize(texto.trim().lowercase(), Normalizer.Form.NFD).replace(MARCAS, "")
    }
}
