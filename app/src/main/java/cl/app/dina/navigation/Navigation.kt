package cl.app.dina.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.app.dina.ui.viewmodel.AppViewModelProvider
import cl.app.dina.ui.viewmodel.EstadoSesion
import cl.app.dina.ui.viewmodel.SesionViewModel
import cl.app.dina.ui.views.LoginScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cl.app.dina.ui.components.TabBar
import cl.app.dina.ui.views.ChatScreen
import cl.app.dina.ui.views.InicioScreen
import cl.app.dina.ui.views.MenuScreen
import cl.app.dina.ui.views.NotificacionesScreen
import cl.app.dina.ui.views.ReservaEspacioScreen

/**
 * Punto de entrada de la UI: decide entre Login y la app según haya sesión guardada.
 * No usa NavController: al iniciar o cerrar sesión cambia el estado y se redibuja solo.
 * Así "atrás" desde la app nunca vuelve al Login, y al cerrar sesión no queda historial.
 */
@Composable
fun NavegacionRaiz(
    sesionViewModel: SesionViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val estado by sesionViewModel.estado.collectAsStateWithLifecycle()

    Crossfade(targetState = estado, label = "sesion") { e ->
        when (e) {
            // Mientras se lee el teléfono se muestra solo el fondo (sin parpadeo de Login)
            EstadoSesion.Cargando -> Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
            EstadoSesion.SinSesion -> LoginScreen()
            is EstadoSesion.ConSesion -> AppNavigation()
        }
    }
}

/** App con sesión iniciada: TabBar abajo y las 5 pantallas. */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val entradaActual by navController.currentBackStackEntryAsState()
    val pestaniaActual = Pestania.entries.firstOrNull { it.ruta == entradaActual?.destination?.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            TabBar(
                pestaniaActual = pestaniaActual,
                onPestaniaClick = { pestania ->
                    navController.navigate(pestania.ruta) {
                        // Volver a Inicio sin apilar pestañas al nivel de TabBar - "atrás" te lleva a InicioScreen
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true   // tocar la pestaña actual no la duplica
                        restoreState = true      // cada pestaña recuerda su estado
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Pestania.INICIO.ruta,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Pestania.INICIO.ruta) { InicioScreen() }
            composable(Pestania.RESERVA_ESPACIO.ruta) {
                ReservaEspacioScreen(
                    // TODO: navegar a EspacioScreen (formulario de reserva) con espacio.id y fecha
                    onEspacioClick = { _, _ -> }
                )
            }
            composable(Pestania.CHAT.ruta) { ChatScreen() }
            composable(Pestania.NOTIFICACIONES.ruta) { NotificacionesScreen() }
            composable(Pestania.MENU.ruta) { MenuScreen() }
        }
    }
}
