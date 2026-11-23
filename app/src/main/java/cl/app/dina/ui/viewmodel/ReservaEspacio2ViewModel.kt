package cl.app.dina.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.app.dina.data.model.BloqueDia
import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.EstadoBloque
import cl.app.dina.data.model.NuevaSolicitud
import cl.app.dina.data.model.SolicitudCreada
import cl.app.dina.data.remote.ErrorDelBackend
import cl.app.dina.data.remote.aMensaje
import cl.app.dina.data.repository.EspacioRepository
import cl.app.dina.data.repository.SolicitudRepository
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReservaEspacio2UiState(
    /** El espacio elegido en ReservaEspacio (null mientras se carga). */
    val espacio: Espacio? = null,
    val errorEspacio: String? = null,
    val calendario: Calendario = Calendario(LocalDate.now()),
    /** Carga de los bloques del día elegido. */
    val cargaBloques: EstadoCarga = EstadoCarga.Cargando,
    val bloques: List<BloqueDia> = emptyList(),
    /** Bloques elegidos: siempre seguidos y ordenados. */
    val seleccion: List<BloqueDia> = emptyList(),
    val asistentes: String = "",
    val motivo: String = "",
    val errorBloques: String? = null,
    val errorAsistentes: String? = null,
    val errorMotivo: String? = null,
    /** Error que viene del backend al solicitar. */
    val errorGeneral: String? = null,
    /** Ya tiene otra solicitud a esa hora: el mensaje del backend para el diálogo "¿Solicitar igual?". */
    val avisoCruce: String? = null,
    val enviando: Boolean = false
) {
    /** La sede no atiende ese día (domingo) o ya pasaron todos los bloques. */
    val diaSinBloques: Boolean
        get() = bloques.isNotEmpty() && bloques.all { it.estado == EstadoBloque.NO_DISPONIBLE }

    /** "10:00 a 12:00 · 2 h", o null si no hay bloques elegidos. */
    val resumenSeleccion: String?
        get() = seleccion.takeIf { it.isNotEmpty() }?.let { "${it.first().inicio} a ${it.last().fin} · ${it.size} h" }
}

/** Eventos de una sola vez (no deben repetirse al girar el teléfono). */
sealed interface ReservaEspacio2Evento {
    data class SolicitudEnviada(val solicitud: SolicitudCreada) : ReservaEspacio2Evento
}

/**
 * Formulario de reserva del espacio elegido en ReservaEspacio:
 * día (se puede cambiar), bloques de 1 hora, asistentes, motivo y "Solicitar".
 * Recibe el espacio y el día por la ruta de navegación (SavedStateHandle).
 */
class ReservaEspacio2ViewModel(
    savedStateHandle: SavedStateHandle,
    private val espacioRepository: EspacioRepository,
    private val solicitudRepository: SolicitudRepository
) : ViewModel() {

    private val espacioId: Long = checkNotNull(savedStateHandle[ARG_ESPACIO_ID])

    private val _uiState = MutableStateFlow(
        ReservaEspacio2UiState(
            calendario = Calendario(LocalDate.now()).elegir(
                savedStateHandle.get<String>(ARG_FECHA)?.let(LocalDate::parse) ?: LocalDate.now()
            )
        )
    )
    val uiState: StateFlow<ReservaEspacio2UiState> = _uiState.asStateFlow()

    private val _eventos = Channel<ReservaEspacio2Evento>(Channel.BUFFERED)
    val eventos: Flow<ReservaEspacio2Evento> = _eventos.receiveAsFlow()

    private var trabajoBloques: Job? = null

    init {
        viewModelScope.launch {
            intentar { espacioRepository.obtenerEspacio(espacioId) }
                .onSuccess { espacio -> _uiState.update { it.copy(espacio = espacio) } }
                .onFailure { e -> _uiState.update { it.copy(errorEspacio = e.aMensaje()) } }
        }
        cargarBloques()
    }

    // Día y bloques
    /** Cambiar el día borra la selección y trae los bloques de ese día. */
    fun seleccionarFecha(dia: LocalDate) {
        _uiState.update {
            it.copy(calendario = it.calendario.alDia().elegir(dia), seleccion = emptyList(), errorBloques = null)
        }
        cargarBloques()
    }

    fun onBloqueClick(bloque: BloqueDia) = _uiState.update {
        val nueva = SeleccionBloques.tocar(it.seleccion, bloque)
        if (nueva == null) it.copy(errorBloques = "Puedes juntar como máximo $MAX_BLOQUES bloques ($MAX_BLOQUES horas)")
        else it.copy(seleccion = nueva, errorBloques = null, errorGeneral = null)
    }

    fun reintentarBloques() = cargarBloques()

    /** [conservarSeleccion]: tras un error al solicitar, la selección se mantiene si sigue libre. */
    private fun cargarBloques(conservarSeleccion: Boolean = false) {
        trabajoBloques?.cancel()
        trabajoBloques = viewModelScope.launch {
            _uiState.update { it.copy(cargaBloques = EstadoCarga.Cargando) }
            intentar { solicitudRepository.disponibilidad(espacioId, _uiState.value.calendario.fecha) }
                .onSuccess { bloques ->
                    val libres = bloques.filter { it.seleccionable }.map { it.id }.toSet()
                    _uiState.update { s ->
                        val sigueLibre = conservarSeleccion && s.seleccion.all { it.id in libres }
                        s.copy(
                            cargaBloques = EstadoCarga.Listo,
                            bloques = bloques,
                            seleccion = if (sigueLibre) s.seleccion else emptyList()
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(cargaBloques = EstadoCarga.Error(e.aMensaje())) } }
        }
    }

    // Asistentes y motivo
    fun onAsistentesChange(texto: String) =
        _uiState.update { it.copy(asistentes = texto.filter(Char::isDigit).take(3), errorAsistentes = null) }

    fun onMotivoChange(texto: String) =
        _uiState.update { it.copy(motivo = texto.take(MAX_MOTIVO), errorMotivo = null, errorGeneral = null) }

    // Solicitar
    /** El usuario aceptó pedir aunque ya tiene otra solicitud a esa hora. */
    fun solicitarDeTodasFormas() {
        _uiState.update { it.copy(avisoCruce = null) }
        solicitar(confirmarCruce = true)
    }

    fun cancelarCruce() = _uiState.update { it.copy(avisoCruce = null) }

    fun solicitar() = solicitar(confirmarCruce = false)

    private fun solicitar(confirmarCruce: Boolean) {
        val s = _uiState.value
        val espacio = s.espacio ?: return
        if (s.enviando) return

        // 1. Validar en el teléfono (mismas reglas que el backend)
        val asistentes = s.asistentes.toIntOrNull()?.takeIf { it in 1..espacio.capacidad }
        val errorBloques = "Elige al menos un bloque".takeIf { s.seleccion.isEmpty() }
        val errorAsistentes = "Indica entre 1 y ${espacio.capacidad} personas".takeIf { asistentes == null }
        val errorMotivo = "Escribe el motivo de la reserva".takeIf { s.motivo.isBlank() }
        if (errorBloques != null || errorAsistentes != null || asistentes == null || errorMotivo != null) {
            _uiState.update {
                it.copy(errorBloques = errorBloques, errorAsistentes = errorAsistentes, errorMotivo = errorMotivo)
            }
            return
        }

        // 2. Enviar al backend
        _uiState.update { it.copy(enviando = true, errorGeneral = null) }
        val nueva = NuevaSolicitud(
            espacioId = espacio.id,
            fecha = s.calendario.fecha,
            bloqueIds = s.seleccion.map { it.id },
            motivo = s.motivo.trim(),
            asistentes = asistentes,
            confirmarCruce = confirmarCruce
        )
        viewModelScope.launch {
            intentar { solicitudRepository.solicitar(nueva) }
                .onSuccess { creada ->
                    _uiState.update { it.copy(enviando = false) }
                    _eventos.send(ReservaEspacio2Evento.SolicitudEnviada(creada))
                }
                .onFailure { e ->
                    if (e is ErrorDelBackend && e.codigo == ErrorDelBackend.CRUCE_PROPIO) {
                        // No es un error: se pregunta si quiere pedir igual
                        _uiState.update { it.copy(enviando = false, avisoCruce = e.message) }
                    } else {
                        // Ej.: "El espacio ya está reservado en ese horario": se actualizan los bloques
                        _uiState.update { it.copy(enviando = false, errorGeneral = e.aMensaje()) }
                        cargarBloques(conservarSeleccion = true)
                    }
                }
        }
    }

    companion object {
        /** Argumentos de la ruta de navegación. */
        const val ARG_ESPACIO_ID = "espacioId"
        const val ARG_FECHA = "fecha"

        /** Igual que el backend (dina.solicitudes.max-bloques) y el largo de "actividad". */
        const val MAX_BLOQUES = 4
        const val MAX_MOTIVO = 250
    }
}

/**
 * Regla para elegir bloques seguidos tocándolos de a uno:
 * - tocar un bloque libre junto a la selección la alarga (hasta [ReservaEspacio2ViewModel.MAX_BLOQUES]);
 * - tocar un extremo de la selección lo quita;
 * - tocar un bloque suelto, o uno del medio, empieza una selección nueva con ese bloque.
 */
object SeleccionBloques {

    /** Nueva selección, o null si se pasaría del máximo. Los bloques no seleccionables se ignoran. */
    fun tocar(seleccion: List<BloqueDia>, bloque: BloqueDia): List<BloqueDia>? {
        if (!bloque.seleccionable) return seleccion
        if (seleccion.isEmpty()) return listOf(bloque)

        val primero = seleccion.first()
        val ultimo = seleccion.last()
        return when {
            bloque.id == primero.id -> seleccion.drop(1)
            bloque.id == ultimo.id -> seleccion.dropLast(1)
            seleccion.any { it.id == bloque.id } -> listOf(bloque)
            bloque.numero == primero.numero - 1 || bloque.numero == ultimo.numero + 1 ->
                if (seleccion.size >= ReservaEspacio2ViewModel.MAX_BLOQUES) null
                else (seleccion + bloque).sortedBy { it.numero }
            else -> listOf(bloque)
        }
    }
}
