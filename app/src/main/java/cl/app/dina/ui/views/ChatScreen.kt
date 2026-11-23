package cl.app.dina.ui.views

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.app.dina.data.model.Chat
import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.EstadoSolicitud
import cl.app.dina.data.model.Mensaje
import cl.app.dina.data.model.Rol
import cl.app.dina.data.model.Solicitante
import cl.app.dina.data.model.SolicitudResumen
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.ui.components.CargandoView
import cl.app.dina.ui.components.FotoPerfil
import cl.app.dina.ui.components.MensajeView
import cl.app.dina.ui.components.colorEscuela
import cl.app.dina.ui.components.diaCorto
import cl.app.dina.ui.components.horaChat
import cl.app.dina.ui.components.titulo
import cl.app.dina.ui.theme.DINATheme
import cl.app.dina.ui.viewmodel.AppViewModelProvider
import cl.app.dina.ui.viewmodel.ChatUiState
import cl.app.dina.ui.viewmodel.ChatViewModel
import cl.app.dina.ui.viewmodel.EstadoCarga
import cl.app.dina.ui.viewmodel.PestaniaChat
import java.time.LocalDate
import java.time.LocalDateTime

class ChatAcciones(
    val onPestaniaChange: (PestaniaChat) -> Unit = {},
    val onReintentar: () -> Unit = {},
    val onRefrescar: () -> Unit = {},
    val onChatClick: (Long) -> Unit = {}
)

/**
 * Pestaña Chat: un chat por cada solicitud aprobada, en "Abiertos" o "Cerrados".
 * Al tocar uno se abre la conversación ([onChatClick] recibe el id de la solicitud).
 */
@Composable
fun ChatScreen(
    onChatClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Al volver a la pestaña (o desde una conversación) se actualiza: último mensaje y estado
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.actualizar() }

    LaunchedEffect(viewModel) {
        viewModel.mensajes.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    ChatContenido(
        state = state,
        acciones = ChatAcciones(
            onPestaniaChange = viewModel::seleccionarPestania,
            onReintentar = viewModel::reintentar,
            onRefrescar = viewModel::refrescar,
            onChatClick = onChatClick
        ),
        modifier = modifier
    )
}

/** Solo dibuja: recibe el estado y avisa lo que hace el usuario. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatContenido(state: ChatUiState, acciones: ChatAcciones, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        Text(
            "Chat",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        PrimaryTabRow(selectedTabIndex = state.pestania.ordinal) {
            PestaniaChat.entries.forEach { pestania ->
                // Solo "Abiertos" muestra cuántos hay (los cerrados solo se acumulan)
                val conCantidad = pestania == PestaniaChat.ABIERTOS && state.carga == EstadoCarga.Listo
                Tab(
                    selected = pestania == state.pestania,
                    onClick = { acciones.onPestaniaChange(pestania) },
                    text = { Text(if (conCantidad) "${pestania.etiqueta} · ${state.abiertos.size}" else pestania.etiqueta) }
                )
            }
        }

        when (val carga = state.carga) {
            EstadoCarga.Cargando -> CargandoView()
            is EstadoCarga.Error -> MensajeView(
                titulo = "No se pudieron cargar los chats",
                detalle = carga.mensaje,
                textoBoton = "Reintentar",
                onBoton = acciones.onReintentar
            )
            EstadoCarga.Listo -> PullToRefreshBox(
                isRefreshing = state.refrescando,
                onRefresh = acciones.onRefrescar,
                modifier = Modifier.weight(1f)
            ) {
                if (state.visibles.isEmpty()) {
                    // Dentro de una lista para que se pueda deslizar y actualizar igual
                    LazyColumn(Modifier.fillMaxSize()) {
                        item { SinChats(state.pestania, state.esAdmin) }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.visibles, key = { it.solicitud.id }) { chat ->
                            FilaChat(chat, state, onClick = { acciones.onChatClick(chat.solicitud.id) })
                            HorizontalDivider(Modifier.padding(start = 76.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SinChats(pestania: PestaniaChat, esAdmin: Boolean) {
    MensajeView(
        titulo = if (pestania == PestaniaChat.ABIERTOS) "No hay chats abiertos" else "No hay chats cerrados",
        detalle = when {
            pestania == PestaniaChat.CERRADOS ->
                "Aquí quedan los chats de reservas que ya terminaron o que el administrador cerró."
            esAdmin -> "Cada solicitud que aceptes tendrá su chat con quien la pidió."
            else -> "Cuando el administrador acepte una de tus solicitudes, podrán conversar aquí."
        },
        modifier = Modifier.padding(top = 48.dp)
    )
}

/**
 * Un chat de la lista. Admin: foto y nombre de quien pidió el espacio. Usuario: el espacio.
 * Debajo, el día y horario, y el último mensaje ("Tú: …").
 */
@Composable
private fun FilaChat(chat: Chat, state: ChatUiState, onClick: () -> Unit) {
    val s = chat.solicitud
    val titulo = if (state.esAdmin) s.solicitante.nombre else s.espacio.titulo
    val detalle = buildString {
        if (state.esAdmin) append(s.espacio.titulo).append(" · ")
        append(s.fecha.diaCorto).append(" · ").append(s.inicio).append(" a ").append(s.fin)
    }
    val ultimo = chat.ultimoMensaje

    ListItem(
        leadingContent = {
            if (state.esAdmin) FotoPerfil(s.solicitante.nombre, s.solicitante.fotoUrl, tamano = 44.dp)
            else AvatarEspacio(s.espacio)
        },
        headlineContent = {
            Text(titulo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Column {
                Text(
                    detalle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (ultimo != null) {
                    Text(
                        vistaPrevia(ultimo, state.miId),
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = if (ultimo.esDelSistema) FontStyle.Italic else FontStyle.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ultimo?.let {
                    Text(
                        horaChat(it.creadoEn, state.hoy),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (s.estado == EstadoSolicitud.CANCELADA) {
                    Text(
                        "Cancelada",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/** Círculo con el color de la escuela del espacio y un ícono de sala. */
@Composable
private fun AvatarEspacio(espacio: Espacio) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .background(colorEscuela(espacio.escuela), CircleShape)
    ) {
        Icon(Icons.Outlined.MeetingRoom, contentDescription = null, tint = MaterialTheme.colorScheme.surface)
    }
}

/** "Tú: Gracias", "Victor: ¿Puedo llegar antes?" o el aviso del sistema tal cual. */
private fun vistaPrevia(m: Mensaje, miId: Long): String = when {
    m.autor == null -> m.texto
    m.autor.id == miId -> "Tú: ${m.texto}"
    else -> "${m.autor.nombre.substringBefore(' ')}: ${m.texto}"
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    val hoy = LocalDate.now()
    val espacio = Espacio(1, "S11", "Sala S11", "NORTE", "Edificio Norte", 1, TipoEspacio.SALA, 35, "INFORMATICA", true, false, false)
    val victor = Solicitante(2, "Victor Godoy", Rol.ESTUDIANTE, null)
    val solicitud = SolicitudResumen(1, espacio, hoy.plusDays(1), "10:00", "12:00", "Taller", 20, victor, EstadoSolicitud.APROBADA)
    DINATheme {
        ChatContenido(
            state = ChatUiState(
                carga = EstadoCarga.Listo,
                esAdmin = true,
                chats = listOf(Chat(solicitud, true, Mensaje(5, "¿Puedo llegar antes?", victor, LocalDateTime.now())))
            ),
            acciones = ChatAcciones()
        )
    }
}
