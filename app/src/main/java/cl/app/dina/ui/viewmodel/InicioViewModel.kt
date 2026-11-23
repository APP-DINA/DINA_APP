package cl.app.dina.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.app.dina.data.model.SolicitudResumen
import cl.app.dina.data.model.Usuario
import cl.app.dina.data.remote.aMensaje
import cl.app.dina.data.repository.SesionRepository
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

data class InicioUiState(
    /** Usuario con sesión (null solo un instante, mientras se lee el teléfono). */
    val usuario: Usuario? = null,
    /** Solicitudes PENDIENTES: las que debe revisar el ADMIN, o las propias del usuario. */
    val cargaSolicitudes: EstadoCarga = EstadoCarga.Cargando,
    val refrescando: Boolean = false,
    val solicitudes: List<SolicitudResumen> = emptyList(),
    val hoy: LocalDate = LocalDate.now()
) {
    /**
     * Solo ADMIN: las mismas [solicitudes] por día, juntando las de doble reserva.
     * Se calcula a partir de [solicitudes] (una sola fuente de verdad) y una vez por estado.
     */
    val entradas: List<EntradaRevision> by lazy {
        if (usuario?.esAdmin == true) DobleReserva.agrupar(solicitudes) else emptyList()
    }
}

/** Mensajes de una sola vez (Toast). */
sealed interface InicioEvento {
    data class Mensaje(val texto: String) : InicioEvento
}

/**
 * Inicio: encabezado con la foto de perfil (para todos) y las solicitudes pendientes:
 * - ADMIN: las de todos, para revisarlas;
 * - usuario: las propias que esperan respuesta. Cuando el admin la aprueba o rechaza,
 *   deja de estar pendiente y desaparece de la lista en la siguiente actualización.
 */
class InicioViewModel(
    sesionRepository: SesionRepository,
    private val solicitudRepository: SolicitudRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InicioUiState())
    val uiState: StateFlow<InicioUiState> = _uiState.asStateFlow()

    private val _eventos = Channel<InicioEvento>(Channel.BUFFERED)
    val eventos: Flow<InicioEvento> = _eventos.receiveAsFlow()

    private var trabajoSolicitudes: Job? = null

    init {
        // La sesión se observa: al cambiar la foto (desde el Menú) o el rol (desde el backend) el Inicio se redibuja solo.
        // La lista se pide la primera vez y cuando cambia el rol (cambia qué solicitudes se muestran).
        viewModelScope.launch {
            sesionRepository.usuario.collect { usuario ->
                val anterior = _uiState.value.usuario
                _uiState.update { it.copy(usuario = usuario) }
                if (usuario != null && (anterior == null || anterior.esAdmin != usuario.esAdmin)) {
                    cargarSolicitudes(Modo.CARGA)
                }
            }
        }
    }

    // Solicitudes pendientes
    /** Cómo se pide la lista, y qué ve el usuario mientras tanto. */
    private enum class Modo {
        /** Indicador en lugar de la lista (primera vez o "Reintentar"). */
        CARGA,
        /** Gesto de deslizar hacia abajo: indicador arriba, la lista se mantiene. */
        GESTO,
        /** Al volver a la pantalla: sin indicador (para ver si el admin ya respondió). */
        SILENCIOSO
    }

    fun reintentar() = cargarSolicitudes(Modo.CARGA)

    fun refrescar() = cargarSolicitudes(Modo.GESTO)

    /**
     * Al volver al Inicio (desde otra pestaña o desde otra app). Solo si ya hay una lista visible
     * y no hay otra carga en curso (no la interrumpe: esa carga ya trae la lista nueva).
     */
    fun actualizar() {
        val ocupado = trabajoSolicitudes?.isActive == true
        if (_uiState.value.cargaSolicitudes == EstadoCarga.Listo && !ocupado) cargarSolicitudes(Modo.SILENCIOSO)
    }

    private fun cargarSolicitudes(modo: Modo) {
        val usuario = _uiState.value.usuario ?: return
        trabajoSolicitudes?.cancel()
        trabajoSolicitudes = viewModelScope.launch {
            when (modo) {
                Modo.CARGA -> _uiState.update { it.copy(cargaSolicitudes = EstadoCarga.Cargando) }
                Modo.GESTO -> _uiState.update { it.copy(refrescando = true) }
                Modo.SILENCIOSO -> Unit
            }
            intentar {
                if (usuario.esAdmin) solicitudRepository.porRevisar() else solicitudRepository.misPendientes()
            }
                .onSuccess { lista ->
                    _uiState.update {
                        it.copy(cargaSolicitudes = EstadoCarga.Listo, solicitudes = lista, hoy = LocalDate.now())
                    }
                }
                .onFailure { e ->
                    // Con la lista visible se conserva: el gesto avisa del error, la actualización automática no
                    when (modo) {
                        Modo.CARGA -> _uiState.update { it.copy(cargaSolicitudes = EstadoCarga.Error(e.aMensaje())) }
                        Modo.GESTO -> avisar(e.aMensaje())
                        Modo.SILENCIOSO -> Unit
                    }
                }
            _uiState.update { it.copy(refrescando = false) }
        }
    }

    private suspend fun avisar(texto: String) = _eventos.send(InicioEvento.Mensaje(texto))
}
