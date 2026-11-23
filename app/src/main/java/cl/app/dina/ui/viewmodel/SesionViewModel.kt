package cl.app.dina.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.app.dina.data.model.Usuario
import cl.app.dina.data.repository.AuthRepository
import cl.app.dina.data.repository.SesionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de la sesión al abrir la app. */
sealed interface EstadoSesion {
    /** Todavía leyendo el teléfono: no mostrar ni login ni app (evita un parpadeo). */
    data object Cargando : EstadoSesion
    data object SinSesion : EstadoSesion
    data class ConSesion(val usuario: Usuario) : EstadoSesion
}

/** Decide qué ve el usuario: Login o la app con la TabBar. */
class SesionViewModel(
    private val sesionRepository: SesionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val estado: StateFlow<EstadoSesion> = sesionRepository.usuario
        .map { usuario -> if (usuario == null) EstadoSesion.SinSesion else EstadoSesion.ConSesion(usuario) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EstadoSesion.Cargando
        )

    init {
        // Al abrir la app con sesión guardada, se valida con el backend en segundo plano:
        // - si el token ya no sirve (401), AuthTokenInterceptor borra la sesión → vuelve al Login;
        // - si cambió el nombre o el permiso de administrador, se actualiza;
        // - sin conexión, se mantiene la sesión guardada.
        viewModelScope.launch {
            if (sesionRepository.usuario.first() == null) return@launch
            authRepository.usuarioActual()?.let { sesionRepository.actualizarUsuario(it) }
        }
    }
}
