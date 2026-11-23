package cl.app.dina.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.app.dina.data.model.Chat
import cl.app.dina.data.model.EstadoSolicitud
import cl.app.dina.data.model.Mensaje
import cl.app.dina.data.remote.aMensaje
import cl.app.dina.data.repository.ChatRepository
import cl.app.dina.data.repository.SesionRepository
import cl.app.dina.data.repository.SolicitudRepository
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Acciones de la conversación que se confirman en un diálogo. */
enum class AccionChat {
    /** Usuario: cancela su reserva al instante (motivo opcional). Admin: la cancela (motivo obligatorio). */
    CANCELAR_RESERVA,
    /** Admin: pasa el chat a "Cerrados". */
    CERRAR_CHAT
}

data class ConversacionUiState(
    val carga: EstadoCarga = EstadoCarga.Cargando,
    val chat: Chat? = null,
    val mensajes: List<Mensaje> = emptyList(),
    val miId: Long = 0,
    val esAdmin: Boolean = false,
    val texto: String = "",
    val enviando: Boolean = false,
    val confirmando: AccionChat? = null,
    val motivo: String = "",
    val errorMotivo: String? = null,
    /** Se está cancelando o cerrando. */
    val procesando: Boolean = false,
    val hoy: LocalDate = LocalDate.now()
) {
    private val solicitud get() = chat?.solicitud
    private val abierto get() = chat?.abierto == true
    private val aprobada get() = solicitud?.estado == EstadoSolicitud.APROBADA

    /** Quien pidió el espacio (también si es un admin con una reserva propia). */
    val soyElSolicitante: Boolean get() = solicitud?.solicitante?.id == miId

    val puedeEscribir: Boolean get() = abierto

    /** El usuario puede cancelar su reserva aprobada; un admin, la de otra persona (explicando el motivo). */
    val puedeCancelarReserva: Boolean get() = abierto && aprobada && (soyElSolicitante || esAdmin)

    /** Admin cancelando una reserva ajena: el motivo es obligatorio. */
    val cancelaComoAdmin: Boolean get() = esAdmin && !soyElSolicitante

    val puedeCerrarChat: Boolean get() = abierto && esAdmin

    val puedeEnviar: Boolean get() = puedeEscribir && texto.isNotBlank() && !enviando
}

/**
 * Conversación de una solicitud aprobada. Mientras la pantalla está visible pregunta cada
 * [INTERVALO_MS] si hay mensajes nuevos (solo los posteriores al último que ya tiene).
 */
class ConversacionViewModel(
    savedStateHandle: SavedStateHandle,
    private val sesionRepository: SesionRepository,
    private val chatRepository: ChatRepository,
    private val solicitudRepository: SolicitudRepository
) : ViewModel() {

    private val solicitudId: Long = checkNotNull(savedStateHandle[ARG_SOLICITUD_ID])

    private val _uiState = MutableStateFlow(ConversacionUiState())
    val uiState: StateFlow<ConversacionUiState> = _uiState.asStateFlow()

    private val _avisos = Channel<String>(Channel.BUFFERED)
    /** Mensajes de una sola vez (Toast). */
    val avisos: Flow<String> = _avisos.receiveAsFlow()

    private var revision: Job? = null

    init {
        cargar()
    }

    fun reintentar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(carga = EstadoCarga.Cargando) }
            val usuario = sesionRepository.usuario.first()
            intentar { chatRepository.conversacion(solicitudId) }
                .onSuccess { c ->
                    _uiState.update {
                        it.copy(
                            carga = EstadoCarga.Listo,
                            chat = c.chat,
                            mensajes = c.mensajes,
                            miId = usuario?.id ?: 0,
                            esAdmin = usuario?.esAdmin == true,
                            hoy = LocalDate.now()
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(carga = EstadoCarga.Error(e.aMensaje())) } }
        }
    }

    // Mensajes nuevos cada 5 s (solo con la pantalla visible)
    fun reanudar() {
        if (revision?.isActive == true) return
        revision = viewModelScope.launch {
            while (true) {
                delay(INTERVALO_MS)
                traerNuevos()
            }
        }
    }

    fun pausar() {
        revision?.cancel()
    }

    /** Pide los mensajes posteriores al último que tiene y el estado actual del chat (sin avisar errores). */
    private suspend fun traerNuevos() {
        if (_uiState.value.carga != EstadoCarga.Listo) return
        val ultimo = _uiState.value.mensajes.lastOrNull()?.id ?: 0
        intentar { chatRepository.conversacion(solicitudId, despuesDe = ultimo) }
            .onSuccess { c -> _uiState.update { it.copy(chat = c.chat, mensajes = it.mensajes.agregar(c.mensajes)) } }
    }

    // Escribir
    fun onTextoChange(texto: String) = _uiState.update { it.copy(texto = texto.take(MAX_TEXTO)) }

    fun enviar() {
        val s = _uiState.value
        if (!s.puedeEnviar) return
        _uiState.update { it.copy(enviando = true) }
        viewModelScope.launch {
            intentar { chatRepository.enviar(solicitudId, s.texto) }
                .onSuccess { m -> _uiState.update { it.copy(texto = "", mensajes = it.mensajes.agregar(listOf(m))) } }
                .onFailure { e ->
                    _avisos.send(e.aMensaje())
                    traerNuevos() // p. ej. "El chat está cerrado": se ve el estado real
                }
            _uiState.update { it.copy(enviando = false) }
        }
    }

    // Cancelar reserva / cerrar chat
    fun pedirConfirmacion(accion: AccionChat) =
        _uiState.update { it.copy(confirmando = accion, motivo = "", errorMotivo = null) }

    fun cancelarConfirmacion() = _uiState.update { it.copy(confirmando = null) }

    fun onMotivoChange(texto: String) =
        _uiState.update { it.copy(motivo = texto.take(MAX_MOTIVO), errorMotivo = null) }

    fun confirmar() {
        val s = _uiState.value
        val accion = s.confirmando ?: return
        if (s.procesando) return
        if (accion == AccionChat.CANCELAR_RESERVA && s.cancelaComoAdmin && s.motivo.isBlank()) {
            _uiState.update { it.copy(errorMotivo = "Explica al usuario por qué se cancela") }
            return
        }
        _uiState.update { it.copy(confirmando = null, procesando = true) }
        viewModelScope.launch {
            intentar {
                when (accion) {
                    AccionChat.CANCELAR_RESERVA ->
                        if (s.cancelaComoAdmin) solicitudRepository.cancelarComoAdmin(solicitudId, s.motivo)
                        else solicitudRepository.cancelar(solicitudId, s.motivo)
                    AccionChat.CERRAR_CHAT -> chatRepository.cerrar(solicitudId)
                }
            }
                .onSuccess {
                    _avisos.send(
                        when (accion) {
                            AccionChat.CANCELAR_RESERVA -> "Reserva cancelada"
                            AccionChat.CERRAR_CHAT -> "Chat cerrado. Lo encontrarás en Cerrados"
                        }
                    )
                }
                .onFailure { e -> _avisos.send(e.aMensaje()) }
            traerNuevos() // aviso del sistema en el chat y estado actualizado
            _uiState.update { it.copy(procesando = false) }
        }
    }

    companion object {
        const val ARG_SOLICITUD_ID = "solicitudId"
        const val INTERVALO_MS = 5_000L

        /** Igual que el backend. */
        const val MAX_TEXTO = 1000
        const val MAX_MOTIVO = 300
    }
}

/** Agrega mensajes sin repetir (el que se acaba de enviar puede volver a llegar en la revisión). */
private fun List<Mensaje>.agregar(nuevos: List<Mensaje>): List<Mensaje> {
    if (nuevos.isEmpty()) return this
    val ids = mapTo(HashSet()) { it.id }
    return (this + nuevos.filter { it.id !in ids }).sortedBy { it.id }
}
