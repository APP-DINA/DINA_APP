package cl.app.dina.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.app.dina.data.model.Chat
import cl.app.dina.data.model.EstadoSolicitud
import cl.app.dina.data.remote.aMensaje
import cl.app.dina.data.repository.ChatRepository
import cl.app.dina.data.repository.SesionRepository
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Las pestañas de la pantalla Chat. */
enum class PestaniaChat(val etiqueta: String) {
    /** Conversaciones en curso: alguien ya escribió. */
    ABIERTOS("Abiertos"),
    /** Solicitudes aceptadas cuyo chat todavía nadie usa. */
    ACEPTADOS("Aceptados"),
    /** La reserva terminó o el admin cerró el chat. */
    CERRADOS("Cerrados")
}

data class ChatUiState(
    val carga: EstadoCarga = EstadoCarga.Cargando,
    val refrescando: Boolean = false,
    val pestania: PestaniaChat = PestaniaChat.ABIERTOS,
    val chats: List<Chat> = emptyList(),
    /** Para saber qué mensajes son "Tú:" en la vista previa. */
    val miId: Long = 0,
    val esAdmin: Boolean = false,
    val hoy: LocalDate = LocalDate.now()
) {
    /** Cada chat en una sola pestaña. */
    fun pestaniaDe(chat: Chat): PestaniaChat = when {
        !chat.abierto -> PestaniaChat.CERRADOS
        // Aceptada y sin mensajes de nadie. Una cancelada va a Abiertos aunque nadie haya escrito:
        // el chat sigue abierto justamente para aclarar la cancelación
        !chat.iniciado && chat.solicitud.estado == EstadoSolicitud.APROBADA -> PestaniaChat.ACEPTADOS
        else -> PestaniaChat.ABIERTOS
    }

    fun de(pestania: PestaniaChat): List<Chat> = chats.filter { pestaniaDe(it) == pestania }

    val visibles: List<Chat> get() = de(pestania)
}

/**
 * Pestaña Chat: un chat por cada solicitud aprobada, en "Abiertos" (alguien ya escribió),
 * "Aceptados" (aún nadie lo usa) o "Cerrados" (la reserva terminó o el admin lo cerró).
 * Las rechazadas no tienen chat.
 */
class ChatViewModel(
    private val sesionRepository: SesionRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _mensajes = Channel<String>(Channel.BUFFERED)
    /** Errores al actualizar con la lista ya visible (Toast). */
    val mensajes: Flow<String> = _mensajes.receiveAsFlow()

    private var trabajo: Job? = null

    init {
        cargar(refresco = false)
    }

    fun seleccionarPestania(pestania: PestaniaChat) = _uiState.update { it.copy(pestania = pestania) }

    fun reintentar() = cargar(refresco = false)

    fun refrescar() = cargar(refresco = true)

    /** Al volver a la pestaña: sin indicador, solo si ya hay lista y no hay otra carga en curso. */
    fun actualizar() {
        if (_uiState.value.carga == EstadoCarga.Listo && trabajo?.isActive != true) cargar(refresco = true, silencioso = true)
    }

    private fun cargar(refresco: Boolean, silencioso: Boolean = false) {
        trabajo?.cancel()
        trabajo = viewModelScope.launch {
            when {
                silencioso -> Unit
                refresco -> _uiState.update { it.copy(refrescando = true) }
                else -> _uiState.update { it.copy(carga = EstadoCarga.Cargando) }
            }
            val usuario = sesionRepository.usuario.first()
            intentar { chatRepository.chats() }
                .onSuccess { chats ->
                    _uiState.update {
                        it.copy(
                            carga = EstadoCarga.Listo,
                            chats = chats,
                            miId = usuario?.id ?: 0,
                            esAdmin = usuario?.esAdmin == true,
                            hoy = LocalDate.now()
                        )
                    }
                }
                .onFailure { e ->
                    // Con la lista visible se conserva: el gesto avisa del error, la actualización automática no
                    if (!refresco) _uiState.update { it.copy(carga = EstadoCarga.Error(e.aMensaje())) }
                    else if (!silencioso) _mensajes.send(e.aMensaje())
                }
            _uiState.update { it.copy(refrescando = false) }
        }
    }
}
