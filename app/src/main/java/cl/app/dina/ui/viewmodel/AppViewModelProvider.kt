package cl.app.dina.ui.viewmodel

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.app.dina.DinaApplication

/**
 * Fábrica única de ViewModels: a cada uno le entrega sus repositorios desde el AppContainer.
 * Uso en una pantalla: `viewModel: LoginViewModel = viewModel(factory = AppViewModelProvider.Factory)`
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            SesionViewModel(
                sesionRepository = dinaApplication().container.sesionRepository,
                authRepository = dinaApplication().container.authRepository
            )
        }
        initializer {
            LoginViewModel(
                authRepository = dinaApplication().container.authRepository,
                sesionRepository = dinaApplication().container.sesionRepository
            )
        }
        initializer {
            InicioViewModel(
                sesionRepository = dinaApplication().container.sesionRepository,
                solicitudRepository = dinaApplication().container.solicitudRepository
            )
        }
        initializer {
            ReservaEspacioViewModel(dinaApplication().container.espacioRepository)
        }
        initializer {
            ReservaEspacio2ViewModel(
                savedStateHandle = createSavedStateHandle(),
                espacioRepository = dinaApplication().container.espacioRepository,
                solicitudRepository = dinaApplication().container.solicitudRepository
            )
        }
        initializer {
            SolicitudViewModel(
                savedStateHandle = createSavedStateHandle(),
                sesionRepository = dinaApplication().container.sesionRepository,
                solicitudRepository = dinaApplication().container.solicitudRepository
            )
        }
        initializer {
            MenuViewModel(
                sesionRepository = dinaApplication().container.sesionRepository,
                authRepository = dinaApplication().container.authRepository,
                perfilRepository = dinaApplication().container.perfilRepository,
                preferenciasRepository = dinaApplication().container.preferenciasRepository
            )
        }
        initializer {
            ChatViewModel(
                sesionRepository = dinaApplication().container.sesionRepository,
                chatRepository = dinaApplication().container.chatRepository
            )
        }
        initializer {
            ConversacionViewModel(
                savedStateHandle = createSavedStateHandle(),
                sesionRepository = dinaApplication().container.sesionRepository,
                chatRepository = dinaApplication().container.chatRepository,
                solicitudRepository = dinaApplication().container.solicitudRepository
            )
        }
        initializer {
            NotificacionesViewModel(dinaApplication().container.notificacionRepository)
        }
        initializer {
            ConteoAvisosViewModel(dinaApplication().container.notificacionRepository)
        }
        initializer {
            AgregarEspacioViewModel(dinaApplication().container.espacioRepository)
        }
    }
}

private fun CreationExtras.dinaApplication(): DinaApplication =
    this[APPLICATION_KEY] as DinaApplication
