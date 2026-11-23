package cl.app.dina.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.app.dina.data.model.EstadoSolicitud
import cl.app.dina.data.model.SolicitudResumen
import cl.app.dina.data.remote.aMensaje
import cl.app.dina.data.repository.SesionRepository
import cl.app.dina.data.repository.SolicitudRepository
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lo que se puede hacer con una solicitud. Cada acción se confirma antes en un diálogo. */
enum class AccionSolicitud {
    /** Usuario: retira su propia solicitud. */
    CANCELAR,
    /** Admin. */
    APROBAR,
    /** Admin (con motivo opcional). */
    RECHAZAR
}

data class SolicitudUiState(
    val carga: EstadoCarga = EstadoCarga.Cargando,
    val solicitud: SolicitudResumen? = null,
    val esAdmin: Boolean = false,
    /** Acción esperando confirmación en el diálogo (null = sin diálogo). */
    val confirmando: AccionSolicitud? = null,
    /** Texto del diálogo de rechazo (opcional). */
    val motivoRechazo: String = "",
    /** Acción enviada al backend: su botón muestra el indicador y los demás se deshabilitan. */
    val enCurso: AccionSolicitud? = null,
    val hoy: LocalDate = LocalDate.now()
) {
    private val estado get() = solicitud?.estado

    /** El admin solo puede responder las pendientes. */
    val puedeResponder: Boolean get() = esAdmin && estado == EstadoSolicitud.PENDIENTE

    /** El usuario puede retirar una pendiente o una aprobada (el backend revisa que no haya comenzado). */
    val puedeCancelar: Boolean
        get() = !esAdmin && (estado == EstadoSolicitud.PENDIENTE || estado == EstadoSolicitud.APROBADA)
}

sealed interface SolicitudEvento {
    /** La acción se hizo: avisar y volver al Inicio (allí la tarjeta ya no aparece). */
    data class Hecho(val mensaje: String) : SolicitudEvento
    data class Error(val mensaje: String) : SolicitudEvento
}

/**
 * Detalle de una solicitud. El usuario la puede cancelar; el admin, aceptar o rechazar.
 * Recibe el id por la ruta de navegación (SavedStateHandle).
 */
class SolicitudViewModel(
    savedStateHandle: SavedStateHandle,
    private val sesionRepository: SesionRepository,
    private val solicitudRepository: SolicitudRepository
) : ViewModel() {

    private val solicitudId: Long = checkNotNull(savedStateHandle[ARG_SOLICITUD_ID])

    private val _uiState = MutableStateFlow(SolicitudUiState())
    val uiState: StateFlow<SolicitudUiState> = _uiState.asStateFlow()

    private val _eventos = Channel<SolicitudEvento>(Channel.BUFFERED)
    val eventos: Flow<SolicitudEvento> = _eventos.receiveAsFlow()

    init {
        cargar()
    }

    fun reintentar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(carga = EstadoCarga.Cargando) }
            val esAdmin = sesionRepository.usuario.first()?.esAdmin == true
            intentar { solicitudRepository.detalle(solicitudId) }
                .onSuccess { s ->
                    _uiState.update {
                        it.copy(carga = EstadoCarga.Listo, solicitud = s, esAdmin = esAdmin, hoy = LocalDate.now())
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(carga = EstadoCarga.Error(e.aMensaje())) } }
        }
    }

    // Confirmación (diálogo)
    fun pedirConfirmacion(accion: AccionSolicitud) =
        _uiState.update { it.copy(confirmando = accion, motivoRechazo = "") }

    fun cancelarConfirmacion() = _uiState.update { it.copy(confirmando = null) }

    fun onMotivoRechazoChange(texto: String) =
        _uiState.update { it.copy(motivoRechazo = texto.take(MAX_MOTIVO_RECHAZO)) }

    /** El usuario confirmó en el diálogo: se envía al backend. */
    fun confirmar() {
        val s = _uiState.value
        val accion = s.confirmando ?: return
        if (s.enCurso != null) return
        _uiState.update { it.copy(confirmando = null, enCurso = accion) }

        viewModelScope.launch {
            intentar {
                when (accion) {
                    AccionSolicitud.CANCELAR -> solicitudRepository.cancelar(solicitudId)
                    AccionSolicitud.APROBAR -> solicitudRepository.aprobar(solicitudId)
                    AccionSolicitud.RECHAZAR -> solicitudRepository.rechazar(solicitudId, s.motivoRechazo)
                }
            }
                .onSuccess { _eventos.send(SolicitudEvento.Hecho(mensajeHecho(accion))) }
                .onFailure { e ->
                    // Ej.: otro admin ya la revisó, o la reserva ya comenzó: se muestra el estado real
                    _eventos.send(SolicitudEvento.Error(e.aMensaje()))
                    cargar()
                }
            _uiState.update { it.copy(enCurso = null) }
        }
    }

    private fun mensajeHecho(accion: AccionSolicitud) = when (accion) {
        AccionSolicitud.CANCELAR -> "Solicitud cancelada"
        AccionSolicitud.APROBAR -> "Solicitud aceptada. Se avisó a quien la pidió"
        AccionSolicitud.RECHAZAR -> "Solicitud rechazada. Se avisó a quien la pidió"
    }

    companion object {
        /** Argumento de la ruta de navegación. */
        const val ARG_SOLICITUD_ID = "solicitudId"

        /** Igual que el backend (RechazarRequest). */
        const val MAX_MOTIVO_RECHAZO = 300
    }
}
