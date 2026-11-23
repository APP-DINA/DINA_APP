package cl.app.dina.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.app.dina.data.model.Notificacion
import cl.app.dina.data.remote.aMensaje
import cl.app.dina.data.repository.NotificacionRepository
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificacionesUiState(
    val carga: EstadoCarga = EstadoCarga.Cargando,
    val refrescando: Boolean = false,
    /** De la más reciente a la más antigua (sin las que se están borrando). */
    val notificaciones: List<Notificacion> = emptyList(),
    val confirmandoBorrarTodas: Boolean = false,
    val borrandoTodas: Boolean = false,
    val hoy: LocalDate = LocalDate.now()
)

sealed interface NotificacionesEvento {
    /** Abrir lo que avisa la notificación. */
    data class AbrirChat(val solicitudId: Long) : NotificacionesEvento
    data class AbrirSolicitud(val solicitudId: Long) : NotificacionesEvento
    /** Se borró una: mostrar "Deshacer" unos segundos. */
    data class Borrada(val notificacion: Notificacion) : NotificacionesEvento
    data class Mensaje(val texto: String) : NotificacionesEvento
}

/**
 * Pestaña Avisos: tocar abre lo que avisa (chat o detalle de la solicitud) y la marca como leída;
 * deslizar a la derecha la borra (con "Deshacer"); "Borrar todas" pide confirmación.
 */
class NotificacionesViewModel(private val repository: NotificacionRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificacionesUiState())
    val uiState: StateFlow<NotificacionesUiState> = _uiState.asStateFlow()

    private val _eventos = Channel<NotificacionesEvento>(Channel.BUFFERED)
    val eventos: Flow<NotificacionesEvento> = _eventos.receiveAsFlow()

    private var trabajo: Job? = null

    /** Borrados esperando: se envían al backend si no se toca "Deshacer" a tiempo. */
    private val borrados = mutableMapOf<Long, Pair<Notificacion, Job>>()

    init {
        cargar(refresco = false)
    }

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
            intentar { repository.listar() }
                .onSuccess { lista ->
                    // Las que se están borrando no deben reaparecer
                    val visibles = lista.filter { it.id !in borrados }
                    _uiState.update {
                        it.copy(carga = EstadoCarga.Listo, notificaciones = visibles, hoy = LocalDate.now())
                    }
                }
                .onFailure { e ->
                    if (!refresco) _uiState.update { it.copy(carga = EstadoCarga.Error(e.aMensaje())) }
                    else if (!silencioso) _eventos.send(NotificacionesEvento.Mensaje(e.aMensaje()))
                }
            _uiState.update { it.copy(refrescando = false) }
        }
    }

    // Tocar: abrir lo que avisa
    /**
     * Abre lo que avisa y la borra (ya cumplió su función), sin "Deshacer".
     * Si el backend no la puede borrar, vuelve a la lista para que no se pierda.
     */
    fun abrir(n: Notificacion) {
        _uiState.update { s -> s.copy(notificaciones = s.notificaciones.filterNot { it.id == n.id }) }
        viewModelScope.launch {
            intentar { repository.borrar(n) }.onFailure { devolver(n) }
        }
        val id = n.solicitudId ?: return
        viewModelScope.launch {
            _eventos.send(
                if (n.tipo.abreChat) NotificacionesEvento.AbrirChat(id) else NotificacionesEvento.AbrirSolicitud(id)
            )
        }
    }

    // Deslizar: borrar (con "Deshacer")
    /**
     * Se quita de la lista al tiro y se muestra "Deshacer". En el backend se borra cuando el aviso se
     * cierra sin tocar "Deshacer" ([confirmarBorrado]) o, si se sale de la pantalla antes, a los
     * [ESPERA_MAXIMA_MS] de todos modos.
     */
    fun borrar(n: Notificacion) {
        // El gesto de deslizar puede avisar dos veces: la segunda no debe cerrar el "Deshacer" de la primera
        if (n.id in borrados) return
        _uiState.update { s -> s.copy(notificaciones = s.notificaciones.filterNot { it.id == n.id }) }
        val envio = viewModelScope.launch {
            delay(ESPERA_MAXIMA_MS)
            enviarBorrado(n)
        }
        borrados[n.id] = n to envio
        viewModelScope.launch { _eventos.send(NotificacionesEvento.Borrada(n)) }
    }

    /** El aviso "Deshacer" se cerró sin tocarlo: se borra ahora. */
    fun confirmarBorrado(n: Notificacion) {
        val (_, espera) = borrados[n.id] ?: return
        espera.cancel()
        viewModelScope.launch { enviarBorrado(n) }
    }

    private suspend fun enviarBorrado(n: Notificacion) {
        if (borrados.remove(n.id) == null) return // ya se envió o se deshizo
        intentar { repository.borrar(n) }
            .onFailure { e ->
                devolver(n)
                _eventos.send(NotificacionesEvento.Mensaje(e.aMensaje()))
            }
    }

    fun deshacer(n: Notificacion) {
        val (notificacion, envio) = borrados.remove(n.id) ?: return
        envio.cancel()
        devolver(notificacion)
    }

    private fun devolver(n: Notificacion) = _uiState.update { s ->
        s.copy(notificaciones = (s.notificaciones + n).sortedByDescending { it.creadaEn })
    }

    // Borrar todas (con confirmación)
    fun pedirBorrarTodas() = _uiState.update { it.copy(confirmandoBorrarTodas = true) }

    fun cancelarBorrarTodas() = _uiState.update { it.copy(confirmandoBorrarTodas = false) }

    fun borrarTodas() {
        _uiState.update { it.copy(confirmandoBorrarTodas = false, borrandoTodas = true) }
        viewModelScope.launch {
            intentar { repository.borrarTodas() }
                .onSuccess {
                    // Las que esperaban "Deshacer" también quedaron borradas
                    borrados.values.forEach { (_, envio) -> envio.cancel() }
                    borrados.clear()
                    _uiState.update { it.copy(notificaciones = emptyList()) }
                    _eventos.send(NotificacionesEvento.Mensaje("Se borraron todas las notificaciones"))
                }
                .onFailure { e -> _eventos.send(NotificacionesEvento.Mensaje(e.aMensaje())) }
            _uiState.update { it.copy(borrandoTodas = false) }
        }
    }

    companion object {
        /** Si se sale de la pantalla con un "Deshacer" pendiente, se borra igual después de esto. */
        const val ESPERA_MAXIMA_MS = 10_000L
    }
}

/** Número de avisos sin leer, para la campana de la barra de abajo. */
class ConteoAvisosViewModel(private val repository: NotificacionRepository) : ViewModel() {

    val noLeidas: StateFlow<Int> = repository.noLeidas

    private var revision: Job? = null

    /** Consulta ahora y luego cada [INTERVALO_MS] mientras la app está visible. */
    fun reanudar() {
        revision?.cancel()
        revision = viewModelScope.launch {
            while (true) {
                intentar { repository.actualizarConteo() }
                delay(INTERVALO_MS)
            }
        }
    }

    fun pausar() {
        revision?.cancel()
    }

    /** Al cambiar de pantalla (p. ej. después de leer un chat). */
    fun actualizar() {
        viewModelScope.launch { intentar { repository.actualizarConteo() } }
    }

    companion object {
        const val INTERVALO_MS = 30_000L
    }
}
