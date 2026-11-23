package cl.app.dina.ui.viewmodel

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
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
            MenuViewModel(
                sesionRepository = dinaApplication().container.sesionRepository,
                authRepository = dinaApplication().container.authRepository
            )
        }
    }
}

private fun CreationExtras.dinaApplication(): DinaApplication =
    this[APPLICATION_KEY] as DinaApplication
