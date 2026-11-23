package cl.app.dina.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cl.app.dina.ui.components.TabBar
import cl.app.dina.ui.views.ChatScreen
import cl.app.dina.ui.views.ConversacionScreen
import cl.app.dina.ui.views.InicioScreen
import cl.app.dina.ui.views.AgregarEspacioScreen
import cl.app.dina.ui.views.MenuScreen
import cl.app.dina.ui.views.NotificacionesScreen
import cl.app.dina.ui.views.ReservaEspacio2Screen
import cl.app.dina.ui.views.ReservaEspacioScreen
import cl.app.dina.ui.views.SolicitudScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import cl.app.dina.ui.viewmodel.ReservaEspacio2ViewModel
import cl.app.dina.ui.viewmodel.ConteoAvisosViewModel
import cl.app.dina.ui.viewmodel.ConversacionViewModel
import cl.app.dina.ui.viewmodel.SolicitudViewModel
import java.time.LocalDate

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

    // La transición depende solo de QUÉ se muestra, no de los datos del usuario: si cambia su
    // foto o su nombre, la app sigue en la misma pantalla (no se crea una navegación nueva).
    val pantalla = when (estado) {
        EstadoSesion.Cargando -> PantallaRaiz.CARGANDO
        EstadoSesion.SinSesion -> PantallaRaiz.LOGIN
        is EstadoSesion.ConSesion -> PantallaRaiz.APP
    }

    Crossfade(targetState = pantalla, label = "sesion") { p ->
        when (p) {
            // Mientras se lee el teléfono se muestra solo el fondo (sin parpadeo de Login)
            PantallaRaiz.CARGANDO -> Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
            PantallaRaiz.LOGIN -> LoginScreen()
            PantallaRaiz.APP -> AppNavigation()
        }
    }
}

/** Qué muestra NavegacionRaiz. */
private enum class PantallaRaiz { CARGANDO, LOGIN, APP }

/** App con sesión iniciada: TabBar abajo y las 5 pantallas. */
@Composable
fun AppNavigation(
    conteoAvisos: ConteoAvisosViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val navController = rememberNavController()

    // Avisos sin leer (campana): se consultan cada 30 s con la app visible y al cambiar de pantalla
    val noLeidas by conteoAvisos.noLeidas.collectAsStateWithLifecycle()
    LifecycleResumeEffect(conteoAvisos) {
        conteoAvisos.reanudar()
        onPauseOrDispose { conteoAvisos.pausar() }
    }

    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route
    LaunchedEffect(rutaActual) { conteoAvisos.actualizar() } // p. ej. al salir de un chat ya leído
    // Las pantallas secundarias mantienen marcada la pestaña de la que salen
    val pestaniaActual = when (rutaActual) {
        RUTA_RESERVA_ESPACIO_2 -> Pestania.RESERVA_ESPACIO
        RUTA_SOLICITUD -> Pestania.INICIO
        RUTA_AGREGAR_ESPACIO -> Pestania.MENU
        RUTA_CONVERSACION -> Pestania.CHAT
        else -> Pestania.entries.firstOrNull { it.ruta == rutaActual }
    }

    val irAPestania: (Pestania) -> Unit = { pestania ->
        navController.navigate(pestania.ruta) {
            // Volver a Inicio sin apilar pestañas al nivel de TabBar - "atrás" te lleva a InicioScreen
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true   // tocar la pestaña actual no la duplica
            restoreState = true      // cada pestaña recuerda su estado
        }
    }

    /**
     * Abre [ruta] dentro de la pestaña a la que pertenece (p. ej. desde un aviso: un chat en la
     * pestaña Chat, una solicitud en Inicio). Así queda marcada esa pestaña, "<" vuelve a su
     * pantalla principal y la pestaña de origen (Avisos) queda limpia para volver a tocarla.
     */
    /**
     * Va a la solicitud o a la conversación [solicitudId] (rutas [patron]/[ruta]). Si esa misma pantalla
     * es la anterior, simplemente vuelve a ella: así ir y venir entre el chat y la solicitud no apila
     * pantallas (si no, "<" recorría chat → solicitud → chat → solicitud…).
     */
    val irSinRepetir: (patron: String, argumento: String, solicitudId: Long, ruta: String) -> Unit =
        { patron, argumento, solicitudId, ruta ->
            val anterior = navController.previousBackStackEntry
            val esLaAnterior = anterior?.destination?.route == patron &&
                anterior.arguments?.getLong(argumento) == solicitudId
            if (esLaAnterior) navController.popBackStack() else navController.navigate(ruta)
        }

    val abrirEnPestania: (Pestania, String) -> Unit = { pestania, ruta ->
        navController.navigate(pestania.ruta) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            // Sin restaurar: si la pestaña tenía otra pantalla abierta, se parte desde su principal
        }
        navController.navigate(ruta)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            TabBar(
                pestaniaActual = pestaniaActual,
                onPestaniaClick = irAPestania,
                insignias = mapOf(Pestania.NOTIFICACIONES to noLeidas)
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Pestania.INICIO.ruta,
            // consumeWindowInsets: la barra de abajo ya está descontada, así el teclado (imePadding)
            // en la conversación no deja un hueco del alto de la TabBar
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(Pestania.INICIO.ruta) {
                InicioScreen(
                    onSolicitudClick = { id -> navController.navigate(rutaSolicitud(id)) },
                    onReservar = { irAPestania(Pestania.RESERVA_ESPACIO) }
                )
            }
            composable(
                route = RUTA_SOLICITUD,
                arguments = listOf(navArgument(SolicitudViewModel.ARG_SOLICITUD_ID) { type = NavType.LongType })
            ) { entrada ->
                val id = entrada.arguments?.getLong(SolicitudViewModel.ARG_SOLICITUD_ID) ?: 0
                SolicitudScreen(
                    onVolver = { navController.popBackStack() },
                    onAbrirChat = {
                        irSinRepetir(RUTA_CONVERSACION, ConversacionViewModel.ARG_SOLICITUD_ID, id, rutaConversacion(id))
                    }
                )
            }
            composable(Pestania.RESERVA_ESPACIO.ruta) {
                ReservaEspacioScreen(
                    onEspacioClick = { espacio, fecha ->
                        navController.navigate(rutaReservaEspacio2(espacio.id, fecha))
                    }
                )
            }
            composable(
                route = RUTA_RESERVA_ESPACIO_2,
                arguments = listOf(
                    navArgument(ReservaEspacio2ViewModel.ARG_ESPACIO_ID) { type = NavType.LongType },
                    navArgument(ReservaEspacio2ViewModel.ARG_FECHA) { type = NavType.StringType }
                )
            ) {
                ReservaEspacio2Screen(onVolver = { navController.popBackStack() })
            }
            composable(Pestania.CHAT.ruta) {
                ChatScreen(onChatClick = { id -> navController.navigate(rutaConversacion(id)) })
            }
            composable(
                route = RUTA_CONVERSACION,
                arguments = listOf(navArgument(ConversacionViewModel.ARG_SOLICITUD_ID) { type = NavType.LongType })
            ) {
                ConversacionScreen(
                    onVolver = { navController.popBackStack() },
                    onVerSolicitud = { id ->
                        irSinRepetir(RUTA_SOLICITUD, SolicitudViewModel.ARG_SOLICITUD_ID, id, rutaSolicitud(id))
                    }
                )
            }
            composable(Pestania.NOTIFICACIONES.ruta) {
                NotificacionesScreen(
                    onAbrirChat = { id -> abrirEnPestania(Pestania.CHAT, rutaConversacion(id)) },
                    onAbrirSolicitud = { id -> abrirEnPestania(Pestania.INICIO, rutaSolicitud(id)) }
                )
            }
            composable(Pestania.MENU.ruta) {
                MenuScreen(onAgregarEspacio = { navController.navigate(RUTA_AGREGAR_ESPACIO) })
            }
            composable(RUTA_AGREGAR_ESPACIO) {
                AgregarEspacioScreen(onVolver = { navController.popBackStack() })
            }
        }
    }
}

/** Formulario de reserva: el espacio elegido en ReservaEspacio y el día ("AAAA-MM-DD"). */
private const val RUTA_RESERVA_ESPACIO_2 =
    "reserva_espacio_2/{${ReservaEspacio2ViewModel.ARG_ESPACIO_ID}}/{${ReservaEspacio2ViewModel.ARG_FECHA}}"

private fun rutaReservaEspacio2(espacioId: Long, fecha: LocalDate) = "reserva_espacio_2/$espacioId/$fecha"

/** Detalle de una solicitud (se abre desde el Inicio del usuario y del administrador). */
/** Formulario para agregar un espacio (solo admin, desde el Menú). */
private const val RUTA_AGREGAR_ESPACIO = "agregar_espacio"

/** Conversación del chat de una solicitud aprobada (desde Chat o desde el detalle de la solicitud). */
private const val RUTA_CONVERSACION = "chat/{${ConversacionViewModel.ARG_SOLICITUD_ID}}"

private fun rutaConversacion(solicitudId: Long) = "chat/$solicitudId"

private const val RUTA_SOLICITUD = "solicitud/{${SolicitudViewModel.ARG_SOLICITUD_ID}}"

private fun rutaSolicitud(solicitudId: Long) = "solicitud/$solicitudId"
