package cl.app.dina.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.app.dina.data.model.Notificacion
import cl.app.dina.data.model.TipoNotificacion
import cl.app.dina.ui.components.CargandoView
import cl.app.dina.ui.components.MensajeView
import cl.app.dina.ui.components.diaConversacion
import cl.app.dina.ui.components.horaChat
import cl.app.dina.ui.theme.DINATheme
import cl.app.dina.ui.theme.DuocAmarillo
import cl.app.dina.ui.theme.RojoAccion
import cl.app.dina.ui.theme.VerdeAccion
import cl.app.dina.ui.viewmodel.AppViewModelProvider
import cl.app.dina.ui.viewmodel.EstadoCarga
import cl.app.dina.ui.viewmodel.NotificacionesEvento
import cl.app.dina.ui.viewmodel.NotificacionesUiState
import cl.app.dina.ui.viewmodel.NotificacionesViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.launch

class NotificacionesAcciones(
    val onAbrir: (Notificacion) -> Unit = {},
    val onBorrar: (Notificacion) -> Unit = {},
    val onBorrarTodas: () -> Unit = {},
    val onConfirmarBorrarTodas: () -> Unit = {},
    val onCancelarBorrarTodas: () -> Unit = {},
    val onReintentar: () -> Unit = {},
    val onRefrescar: () -> Unit = {}
)

/**
 * Pestaña Avisos.
 * - Tocar una notificación la marca como leída y abre lo que avisa (el chat o el detalle de la solicitud).
 * - Deslizarla hacia la derecha la borra; durante unos segundos se puede "Deshacer".
 * - "Borrar todas" (arriba a la derecha) pide confirmación.
 */
@Composable
fun NotificacionesScreen(
    onAbrirChat: (Long) -> Unit,
    onAbrirSolicitud: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificacionesViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Al volver a la pestaña se actualiza (pueden haber llegado avisos nuevos)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.actualizar() }

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                is NotificacionesEvento.AbrirChat -> onAbrirChat(evento.solicitudId)
                is NotificacionesEvento.AbrirSolicitud -> onAbrirSolicitud(evento.solicitudId)
                is NotificacionesEvento.Borrada -> scope.launch {
                    avisos.currentSnackbarData?.dismiss()
                    val r = avisos.showSnackbar("Notificación borrada", "Deshacer", duration = SnackbarDuration.Short)
                    if (r == SnackbarResult.ActionPerformed) viewModel.deshacer(evento.notificacion)
                    else viewModel.confirmarBorrado(evento.notificacion)
                }
                is NotificacionesEvento.Mensaje -> scope.launch {
                    avisos.currentSnackbarData?.dismiss()
                    avisos.showSnackbar(evento.texto)
                }
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        NotificacionesContenido(
            state = state,
            acciones = NotificacionesAcciones(
                onAbrir = viewModel::abrir,
                onBorrar = viewModel::borrar,
                onBorrarTodas = viewModel::pedirBorrarTodas,
                onConfirmarBorrarTodas = viewModel::borrarTodas,
                onCancelarBorrarTodas = viewModel::cancelarBorrarTodas,
                onReintentar = viewModel::reintentar,
                onRefrescar = viewModel::refrescar
            )
        )
        SnackbarHost(avisos, Modifier.align(Alignment.BottomCenter))
    }
}

/** Solo dibuja: recibe el estado y avisa lo que hace el usuario. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificacionesContenido(
    state: NotificacionesUiState,
    acciones: NotificacionesAcciones,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp)
        ) {
            Text(
                "Notificaciones",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = acciones.onBorrarTodas,
                enabled = state.notificaciones.isNotEmpty() && !state.borrandoTodas
            ) {
                Icon(Icons.Outlined.DeleteSweep, contentDescription = "Borrar todas")
            }
        }

        when (val carga = state.carga) {
            EstadoCarga.Cargando -> CargandoView()
            is EstadoCarga.Error -> MensajeView(
                titulo = "No se pudieron cargar las notificaciones",
                detalle = carga.mensaje,
                textoBoton = "Reintentar",
                onBoton = acciones.onReintentar
            )
            EstadoCarga.Listo -> PullToRefreshBox(
                isRefreshing = state.refrescando,
                onRefresh = acciones.onRefrescar,
                modifier = Modifier.weight(1f)
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (state.notificaciones.isEmpty()) {
                        item(key = "vacio") {
                            MensajeView(
                                titulo = "No tienes notificaciones",
                                detalle = "Aquí te avisaremos cuando respondan tus solicitudes, " +
                                    "te escriban en un chat o tu reserva esté por comenzar.",
                                modifier = Modifier.padding(top = 48.dp)
                            )
                        }
                    }
                    // Agrupadas por día: "Hoy", "Ayer", "Lun. 23 de noviembre"
                    state.notificaciones.groupBy { it.creadaEn.toLocalDate() }.forEach { (dia, delDia) ->
                        item(key = "dia-$dia") {
                            Text(
                                diaConversacion(dia, state.hoy),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                            )
                        }
                        items(delDia, key = { it.id }) { n ->
                            FilaDeslizable(n, state.hoy, acciones)
                        }
                    }
                }
            }
        }
    }

    if (state.confirmandoBorrarTodas) {
        AlertDialog(
            onDismissRequest = acciones.onCancelarBorrarTodas,
            title = { Text("¿Borrar todas las notificaciones?") },
            text = { Text("Se borrarán ${state.notificaciones.size} notificaciones. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = acciones.onConfirmarBorrarTodas,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Borrar todas") }
            },
            dismissButton = {
                TextButton(onClick = acciones.onCancelarBorrarTodas) { Text("Cancelar") }
            }
        )
    }
}

/** Deslizar hacia la derecha la borra: debajo aparece el fondo rojo con el basurero. */
@Composable
private fun FilaDeslizable(n: Notificacion, hoy: LocalDate, acciones: NotificacionesAcciones) {
    val estado = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = estado,
        enableDismissFromEndToStart = false,
        onDismiss = {
            acciones.onBorrar(n)
            // Vuelve a su lugar: si se toca "Deshacer", la fila reaparece sin estar "deslizada"
            // (si no, se volvería a borrar sola)
            scope.launch { estado.snapTo(SwipeToDismissBoxValue.Settled) }
        },
        backgroundContent = {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxSize()
                    .background(RojoAccion)
                    .padding(start = 24.dp)
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.White)
            }
        }
    ) {
        // Fondo opaco en toda la fila (también bajo el separador): el rojo solo se ve al deslizar
        Column(Modifier.background(MaterialTheme.colorScheme.background)) {
            FilaNotificacion(n, hoy, onClick = { acciones.onAbrir(n) })
            HorizontalDivider(Modifier.padding(start = 72.dp))
        }
    }
}

@Composable
private fun FilaNotificacion(n: Notificacion, hoy: LocalDate, onClick: () -> Unit) {
    val (icono, color) = estilo(n.tipo)
    ListItem(
        leadingContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.16f), CircleShape)
            ) {
                Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
        },
        headlineContent = {
            Text(
                n.titulo,
                fontWeight = if (n.leida) FontWeight.Normal else FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(n.mensaje, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    horaChat(n.creadaEn, hoy),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!n.leida) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(DuocAmarillo, CircleShape)
                    )
                }
            }
        },
        // Fondo opaco: tapa el rojo de "borrar" mientras no se desliza
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

/** Ícono y color de cada tipo de aviso. */
@Composable
private fun estilo(tipo: TipoNotificacion): Pair<ImageVector, Color> = when (tipo) {
    TipoNotificacion.NUEVA_SOLICITUD -> Icons.Outlined.Inbox to MaterialTheme.colorScheme.primary
    TipoNotificacion.SOLICITUD_APROBADA -> Icons.Outlined.CheckCircle to VerdeAccion
    TipoNotificacion.SOLICITUD_RECHAZADA -> Icons.Outlined.Cancel to RojoAccion
    TipoNotificacion.RESERVA_CANCELADA, TipoNotificacion.CANCELADA_POR_USUARIO -> Icons.Outlined.EventBusy to RojoAccion
    TipoNotificacion.MENSAJE_NUEVO -> Icons.AutoMirrored.Outlined.Chat to MaterialTheme.colorScheme.onSurface
    TipoNotificacion.RECORDATORIO_RESERVA -> Icons.Outlined.Alarm to DuocAmarillo
    TipoNotificacion.OTRO -> Icons.Outlined.Notifications to MaterialTheme.colorScheme.onSurface
}

@Preview(showBackground = true)
@Composable
private fun NotificacionesPreview() {
    val ahora = LocalDateTime.now()
    DINATheme {
        NotificacionesContenido(
            state = NotificacionesUiState(
                carga = EstadoCarga.Listo,
                notificaciones = listOf(
                    Notificacion(3, TipoNotificacion.MENSAJE_NUEVO, "Mensaje nuevo · S21", "Exequiel Guzman: Sí, tiene proyector", 1, false, ahora),
                    Notificacion(2, TipoNotificacion.SOLICITUD_APROBADA, "Solicitud aprobada", "Tu solicitud de S21 fue aprobada.", 1, true, ahora),
                    Notificacion(1, TipoNotificacion.SOLICITUD_RECHAZADA, "Solicitud rechazada", "Tu solicitud de LC1 fue rechazada.", 2, true, ahora.minusDays(1))
                )
            ),
            acciones = NotificacionesAcciones()
        )
    }
}
