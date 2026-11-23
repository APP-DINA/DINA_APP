package cl.app.dina.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.app.dina.data.model.Usuario
import cl.app.dina.data.repository.AuthRepository
import cl.app.dina.data.repository.SesionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MenuViewModel(
    private val sesionRepository: SesionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    /** Usuario con sesión, para mostrar nombre, correo y rol. */
    val usuario: StateFlow<Usuario?> = sesionRepository.usuario
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _confirmandoCierre = MutableStateFlow(false)
    /** true mientras se muestra el diálogo "¿Cerrar sesión?". */
    val confirmandoCierre: StateFlow<Boolean> = _confirmandoCierre.asStateFlow()

    fun pedirCerrarSesion() { _confirmandoCierre.value = true }

    fun cancelarCerrarSesion() { _confirmandoCierre.value = false }

    /**
     * Invalida el token en el backend y borra la sesión del teléfono.
     * SesionViewModel detecta el cambio y vuelve al Login.
     */
    fun cerrarSesion() {
        _confirmandoCierre.value = false
        viewModelScope.launch {
            authRepository.logout()     // si no hay conexión, igual se cierra en el teléfono
            sesionRepository.cerrar()
        }
    }
}
