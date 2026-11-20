package cl.app.dina.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * ruta - destino en el NavHost
 * etiqueta - texto bajo el ícono
 * icono - vector en res/drawable
 */
enum class Pestania(
    val ruta: String,
    val etiqueta: String,
    val iconoSeleccionado: ImageVector,
    val iconoNoSeleccionado: ImageVector
) {
    INICIO(
        ruta = "inicio",
        etiqueta = "Inicio",
        iconoSeleccionado = Icons.Filled.Home,
        iconoNoSeleccionado = Icons.Outlined.Home
    ),
    RESERVA_ESPACIO(
        ruta = "reserva_espacio",
        etiqueta = "Reservar",
        iconoSeleccionado = Icons.Filled.DateRange,
        iconoNoSeleccionado = Icons.Outlined.DateRange
    ),
    CHAT(
        ruta = "chat",
        etiqueta = "Chat",
        iconoSeleccionado = Icons.Filled.ChatBubble,
        iconoNoSeleccionado = Icons.Outlined.ChatBubbleOutline
    ),
    NOTIFICACIONES(
        ruta = "notificaciones",
        etiqueta = "Avisos",
        iconoSeleccionado = Icons.Filled.Notifications,
        iconoNoSeleccionado = Icons.Outlined.Notifications
    ),
    MENU(
        ruta = "menu",
        etiqueta = "Menú",
        iconoSeleccionado = Icons.Filled.Menu,
        iconoNoSeleccionado = Icons.Outlined.Menu
    )
}