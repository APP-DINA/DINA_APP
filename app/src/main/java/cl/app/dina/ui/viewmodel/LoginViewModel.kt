package cl.app.dina.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.app.dina.data.model.Rol
import cl.app.dina.data.repository.AuthRepository
import cl.app.dina.data.repository.ResultadoLogin
import cl.app.dina.data.repository.SesionRepository
import cl.app.dina.utils.errorCorreo
import cl.app.dina.utils.errorPassword
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val correo: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    /** Rol que se deduce mientras se escribe el correo (null si el dominio no es institucional). */
    val rolDetectado: Rol? = null,
    val errorCorreo: String? = null,
    val errorPassword: String? = null,
    /** Error que viene del backend (credenciales, red...). */
    val errorGeneral: String? = null,
    val cargando: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val sesionRepository: SesionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onCorreoChange(valor: String) {
        val correo = valor.trim()
        _uiState.update {
            it.copy(
                correo = correo,
                rolDetectado = Rol.desdeCorreo(correo),
                errorCorreo = null,
                errorGeneral = null
            )
        }
    }

    fun onPasswordChange(valor: String) {
        _uiState.update { it.copy(password = valor, errorPassword = null, errorGeneral = null) }
    }

    fun onPasswordVisibleToggle() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun ingresar() {
        val estado = _uiState.value
        if (estado.cargando) return

        // 1. Validar en el teléfono antes de llamar al backend
        val errCorreo = errorCorreo(estado.correo)
        val errPassword = errorPassword(estado.password)
        if (errCorreo != null || errPassword != null) {
            _uiState.update { it.copy(errorCorreo = errCorreo, errorPassword = errPassword) }
            return
        }

        // 2. Preguntar al backend
        _uiState.update { it.copy(cargando = true, errorGeneral = null) }
        viewModelScope.launch {
            try {
                when (val r = authRepository.login(estado.correo, estado.password)) {
                    is ResultadoLogin.Exito -> {
                        // 3. Guardar la sesión en el teléfono.
                        //    No hace falta navegar: SesionViewModel ve el cambio y muestra la app.
                        sesionRepository.iniciar(r.usuario, r.token)
                        // 4. Dejar el formulario listo para la próxima vez. Este ViewModel vive
                        //    mientras la Activity esté abierta: al cerrar sesión se vuelve a mostrar
                        //    el mismo, así que no puede quedar "cargando" ni con la contraseña escrita.
                        _uiState.value = LoginUiState(correo = estado.correo, rolDetectado = estado.rolDetectado)
                    }
                    is ResultadoLogin.Error ->
                        _uiState.update { it.copy(cargando = false, errorGeneral = r.mensaje) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(cargando = false, errorGeneral = "No se pudo conectar. Intenta nuevamente")
                }
            }
        }
    }
}
