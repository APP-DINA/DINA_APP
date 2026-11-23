package cl.app.dina.ui.views

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
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
import cl.app.dina.ui.components.EncabezadoVolver
import cl.app.dina.ui.components.MensajeView
import cl.app.dina.ui.components.diaConversacion
import cl.app.dina.ui.components.fechaEvento
import cl.app.dina.ui.components.hhmm
import cl.app.dina.ui.components.titulo
import cl.app.dina.ui.theme.DINATheme
import cl.app.dina.ui.viewmodel.AccionChat
import cl.app.dina.ui.viewmodel.AppViewModelProvider
import cl.app.dina.ui.viewmodel.ConversacionUiState
import cl.app.dina.ui.viewmodel.ConversacionViewModel
import cl.app.dina.ui.viewmodel.EstadoCarga
import java.time.LocalDate
import java.time.LocalDateTime

class ConversacionAcciones(
    val onVolver: () -> Unit = {},
    val onVerSolicitud: () -> Unit = {},
    val onReintentar: () -> Unit = {},
    val onTextoChange: (String) -> Unit = {},
    val onEnviar: () -> Unit = {},
    val onAccion: (AccionChat) -> Unit = {},
    val onMotivoChange: (String) -> Unit = {},
    val onConfirmar: () -> Unit = {},
    val onCancelarConfirmacion: () -> Unit = {}
)

/**
 * Conversación de una solicitud aprobada entre quien la pidió y el administrador.
 * - Usuario: puede cancelar su reserva al instante (el chat sigue abierto para aclarar cosas).
 * - Admin: puede cancelar la reserva explicando el motivo, y pasar el chat a "Cerrados".
 * Mientras está visible, pregunta cada 5 s si hay mensajes nuevos.
 */
@Composable
fun ConversacionScreen(
    onVolver: () -> Unit,
    onVerSolicitud: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConversacionViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Revisa mensajes nuevos solo mientras la pantalla se ve (no en segundo plano)
    LifecycleResumeEffect(viewModel) {
        viewModel.reanudar()
        onPauseOrDispose { viewModel.pausar() }
    }

    LaunchedEffect(viewModel) {
        viewModel.avisos.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    ConversacionContenido(
        state = state,
        acciones = ConversacionAcciones(
            onVolver = onVolver,
            onVerSolicitud = { state.chat?.let { onVerSolicitud(it.solicitud.id) } },
            onReintentar = viewModel::reintentar,
            onTextoChange = viewModel::onTextoChange,
            onEnviar = viewModel::enviar,
            onAccion = viewModel::pedirConfirmacion,
            onMotivoChange = viewModel::onMotivoChange,
            onConfirmar = viewModel::confirmar,
            onCancelarConfirmacion = viewModel::cancelarConfirmacion
        ),
        modifier = modifier
    )
}

/** Solo dibuja: recibe el estado y avisa lo que hace el usuario. */
@Composable
private fun ConversacionContenido(
    state: ConversacionUiState,
    acciones: ConversacionAcciones,
    modifier: Modifier = Modifier
) {
    val chat = state.chat
    Column(
        modifier
            .fillMaxSize()
            .imePadding()
    ) {
        EncabezadoVolver(chat?.solicitud?.espacio?.titulo ?: "Chat", acciones.onVolver)

        when (val carga = state.carga) {
            EstadoCarga.Cargando -> CargandoView()
            is EstadoCarga.Error -> MensajeView(
                titulo = "No se pudo abrir el chat",
                detalle = carga.mensaje,
                textoBoton = "Reintentar",
                onBoton = acciones.onReintentar
            )
            EstadoCarga.Listo -> if (chat != null) {
                Resumen(chat, state, acciones.onVerSolicitud)
                AccionesChat(state, acciones.onAccion)
                Mensajes(state, Modifier.weight(1f))
                if (state.puedeEscribir) BarraEscribir(state, acciones)
                else AvisoCerrado()
            }
        }
    }

    state.chat?.let { c -> state.confirmando?.let { DialogoChat(it, c, state, acciones) } }
}

// Arriba: de qué reserva es el chat
@Composable
private fun Resumen(chat: Chat, state: ConversacionUiState, onClick: () -> Unit) {
    val s = chat.solicitud
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${fechaEvento(s.fecha, state.hoy)} · ${s.inicio} a ${s.fin}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    if (state.soyElSolicitante) "${s.espacio.edificioNombre} · Piso ${s.espacio.piso}"
                    else "${s.solicitante.nombre} · ${s.solicitante.rol?.etiqueta ?: "Usuario"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            EtiquetaChat(
                when {
                    s.estado == EstadoSolicitud.CANCELADA -> "Cancelada"
                    !chat.abierto -> "Cerrado"
                    else -> "Aprobada"
                },
                destacada = s.estado == EstadoSolicitud.CANCELADA
            )
        }
    }
}

@Composable
private fun EtiquetaChat(texto: String, destacada: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (destacada) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (destacada) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(texto, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

/** "Cancelar reserva" (usuario o admin) y "Cerrar chat" (admin), solo si se pueden usar. */
@Composable
private fun AccionesChat(state: ConversacionUiState, onAccion: (AccionChat) -> Unit) {
    if (!state.puedeCancelarReserva && !state.puedeCerrarChat) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        if (state.puedeCancelarReserva) {
            BotonAccion(
                texto = if (state.cancelaComoAdmin) "Cancelar reserva" else "Solicitar cancelación",
                icono = Icons.Outlined.EventBusy,
                habilitado = !state.procesando,
                peligro = true
            ) { onAccion(AccionChat.CANCELAR_RESERVA) }
        }
        if (state.puedeCerrarChat) {
            BotonAccion("Cerrar chat", Icons.Outlined.Lock, habilitado = !state.procesando) { onAccion(AccionChat.CERRAR_CHAT) }
        }
    }
}

@Composable
private fun BotonAccion(
    texto: String,
    icono: ImageVector,
    habilitado: Boolean,
    peligro: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (peligro) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    AssistChip(
        onClick = onClick,
        enabled = habilitado,
        label = { Text(texto) },
        leadingIcon = { Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp)) },
        colors = AssistChipDefaults.assistChipColors(labelColor = color, leadingIconContentColor = color)
    )
}

// Mensajes
@Composable
private fun Mensajes(state: ConversacionUiState, modifier: Modifier = Modifier) {
    val lista = rememberLazyListState()
    // Al llegar un mensaje nuevo, se baja hasta él
    LaunchedEffect(state.mensajes.size) {
        if (state.mensajes.isNotEmpty()) lista.animateScrollToItem(state.mensajes.lastIndex)
    }
    LazyColumn(
        state = lista,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        itemsIndexed(state.mensajes, key = { _, m -> m.id }) { i, m ->
            val dia = m.creadoEn.toLocalDate()
            if (i == 0 || state.mensajes[i - 1].creadoEn.toLocalDate() != dia) {
                Text(
                    diaConversacion(dia, state.hoy),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
            when {
                m.autor == null -> AvisoSistema(m)
                else -> Burbuja(m, mia = m.autor.id == state.miId)
            }
        }
    }
}

/** Mensaje de una persona: los propios a la derecha (amarillo), los demás a la izquierda con su nombre. */
@Composable
private fun Burbuja(m: Mensaje, mia: Boolean) {
    val forma = if (mia) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    Column(
        horizontalAlignment = if (mia) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!mia) {
            Text(
                m.autor?.nombre.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            )
        }
        Surface(
            shape = forma,
            color = if (mia) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (mia) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(m.texto, style = MaterialTheme.typography.bodyLarge)
                Text(
                    m.creadoEn.toLocalTime().hhmm(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp)
                )
            }
        }
    }
}

/** Aviso del sistema, centrado: "Solicitud aprobada…", "Victor Godoy canceló la reserva…". */
@Composable
private fun AvisoSistema(m: Mensaje) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
    ) {
        Text(
            "${m.texto} · ${m.creadoEn.toLocalTime().hhmm()}",
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

// Abajo: escribir o aviso de cerrado
@Composable
private fun BarraEscribir(state: ConversacionUiState, acciones: ConversacionAcciones) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = state.texto,
            onValueChange = acciones.onTextoChange,
            placeholder = { Text("Escribe un mensaje") },
            maxLines = 4,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.weight(1f)
        )
        FilledIconButton(onClick = acciones.onEnviar, enabled = state.puedeEnviar) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
        }
    }
}

@Composable
private fun AvisoCerrado() {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Outlined.Block, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Este chat está cerrado. Ya no se pueden enviar mensajes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Confirmaciones
@Composable
private fun DialogoChat(accion: AccionChat, chat: Chat, state: ConversacionUiState, acciones: ConversacionAcciones) {
    val s = chat.solicitud
    val reserva = "${s.espacio.titulo} el ${fechaEvento(s.fecha, state.hoy).lowercase()}, de ${s.inicio} a ${s.fin}"
    val (titulo, texto, confirmar) = when {
        accion == AccionChat.CERRAR_CHAT -> Triple(
            "¿Cerrar el chat?",
            "Pasará a Cerrados y nadie podrá seguir escribiendo.",
            "Cerrar chat"
        )
        state.cancelaComoAdmin -> Triple(
            "¿Cancelar esta reserva?",
            "Ya estaba aprobada: explica a ${s.solicitante.nombre} por qué se cancela $reserva. " +
                "El motivo le llegará en este chat y en sus avisos.",
            "Cancelar reserva"
        )
        else -> Triple(
            "¿Cancelar tu reserva?",
            "Se cancelará al instante $reserva y el horario quedará libre. " +
                "El chat seguirá abierto por si necesitas aclarar algo.",
            "Cancelar reserva"
        )
    }

    AlertDialog(
        onDismissRequest = acciones.onCancelarConfirmacion,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(texto)
                if (accion == AccionChat.CANCELAR_RESERVA) {
                    OutlinedTextField(
                        value = state.motivo,
                        onValueChange = acciones.onMotivoChange,
                        label = { Text(if (state.cancelaComoAdmin) "Motivo" else "Motivo (opcional)") },
                        isError = state.errorMotivo != null,
                        supportingText = state.errorMotivo?.let { { Text(it) } },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        // Acciones destructivas en rojo, como "Cerrar sesión"
        confirmButton = {
            TextButton(
                onClick = acciones.onConfirmar,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text(confirmar) }
        },
        dismissButton = {
            TextButton(onClick = acciones.onCancelarConfirmacion) { Text("Volver") }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ConversacionPreview() {
    val hoy = LocalDate.now()
    val ahora = LocalDateTime.now()
    val espacio = Espacio(1, "S11", "Sala S11", "NORTE", "Edificio Norte", 1, TipoEspacio.SALA, 35, null, true, false, false)
    val victor = Solicitante(2, "Victor Godoy", Rol.ESTUDIANTE, null)
    val admin = Solicitante(1, "Exequiel Guzman", Rol.ESTUDIANTE, null)
    val solicitud = SolicitudResumen(1, espacio, hoy.plusDays(1), "10:00", "12:00", "Taller", 20, victor, EstadoSolicitud.APROBADA)
    DINATheme {
        ConversacionContenido(
            state = ConversacionUiState(
                carga = EstadoCarga.Listo,
                chat = Chat(solicitud, true, null),
                miId = 2,
                mensajes = listOf(
                    Mensaje(1, "Solicitud aprobada. Este chat queda abierto hasta que termine la reserva.", null, ahora),
                    Mensaje(2, "¿Puedo llegar 10 minutos antes?", victor, ahora),
                    Mensaje(3, "Sí, sin problema", admin, ahora)
                )
            ),
            acciones = ConversacionAcciones()
        )
    }
}
