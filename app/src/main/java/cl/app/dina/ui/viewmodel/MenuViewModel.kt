package cl.app.dina.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.app.dina.data.model.ModoTema
import cl.app.dina.data.model.Usuario
import cl.app.dina.data.remote.aMensaje
import cl.app.dina.data.repository.AuthRepository
import cl.app.dina.data.repository.PerfilRepository
import cl.app.dina.data.repository.PreferenciasRepository
import cl.app.dina.data.repository.SesionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Menú: perfil (con foto), opciones de administración, tema y cerrar sesión. */
class MenuViewModel(
    private val sesionRepository: SesionRepository,
    private val authRepository: AuthRepository,
    private val perfilRepository: PerfilRepository,
    private val preferenciasRepository: PreferenciasRepository
) : ViewModel() {

    /** Usuario con sesión, para mostrar foto, nombre, correo y rol. */
    val usuario: StateFlow<Usuario?> = sesionRepository.usuario
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Tema elegido en "Modo oscuro" (se guarda en el teléfono y lo aplica MainActivity). */
    val modoTema: StateFlow<ModoTema> = preferenciasRepository.modoTema
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModoTema.SISTEMA)

    private val _confirmandoCierre = MutableStateFlow(false)
    /** true mientras se muestra el diálogo "¿Cerrar sesión?". */
    val confirmandoCierre: StateFlow<Boolean> = _confirmandoCierre.asStateFlow()

    private val _subiendoFoto = MutableStateFlow(false)
    /** true mientras se sube o se borra la foto (el círculo muestra un indicador). */
    val subiendoFoto: StateFlow<Boolean> = _subiendoFoto.asStateFlow()

    private val _mensajes = Channel<String>(Channel.BUFFERED)
    /** Mensajes de una sola vez (Toast). */
    val mensajes: Flow<String> = _mensajes.receiveAsFlow()

    // ── Tema ────────────────────────────────────────────────────────────

    fun cambiarModoTema(modo: ModoTema) {
        viewModelScope.launch { preferenciasRepository.guardarModoTema(modo) }
    }

    // ── Foto de perfil ──────────────────────────────────────────────────

    /** [imagen]: la elegida en la Galería o la que se acaba de tomar con la Cámara. */
    fun cambiarFoto(imagen: Uri) =
        accionFoto("Foto de perfil actualizada") { perfilRepository.cambiarFoto(imagen) }

    fun borrarFoto() = accionFoto("Foto de perfil borrada") { perfilRepository.borrarFoto() }

    /** Una acción con la foto a la vez; al terminar avisa con [exito] o con el error. */
    private fun accionFoto(exito: String, accion: suspend () -> Unit) {
        if (_subiendoFoto.value) return
        _subiendoFoto.value = true
        viewModelScope.launch {
            intentar { accion() }
                .onSuccess { _mensajes.send(exito) }
                .onFailure { e -> _mensajes.send(e.aMensaje()) }
            _subiendoFoto.value = false
        }
    }

    // ── Cerrar sesión ───────────────────────────────────────────────────

    fun pedirCerrarSesion() { _confirmandoCierre.value = true }

    fun cancelarCerrarSesion() { _confirmandoCierre.value = false }

    /**
     * Invalida el token en el backend y borra la sesión del teléfono (y con ella la copia local
     * de solicitudes y chats). SesionViewModel detecta el cambio y vuelve al Login. El tema se mantiene.
     *
     * Sin conexión el backend no responde y la petición esperaría hasta el tiempo límite de OkHttp
     * (10 s o más): por eso se espera como máximo [ESPERA_LOGOUT_MS] y la sesión se cierra igual.
     */
    fun cerrarSesion() {
        _confirmandoCierre.value = false
        viewModelScope.launch {
            withTimeoutOrNull(ESPERA_LOGOUT_MS) { authRepository.logout() }
            sesionRepository.cerrar()
        }
    }

    companion object {
        const val ESPERA_LOGOUT_MS = 2_000L
    }
}
