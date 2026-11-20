package cl.app.dina.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
            composable(Pestania.RESERVA_ESPACIO.ruta) { ReservaEspacioScreen() }
            composable(Pestania.CHAT.ruta) { ChatScreen() }
            composable(Pestania.NOTIFICACIONES.ruta) { NotificacionesScreen() }
            composable(Pestania.MENU.ruta) { MenuScreen() }
        }
    }
}
