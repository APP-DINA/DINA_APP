package cl.app.dina.ui.views

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.EstadoSolicitud
import cl.app.dina.data.model.Rol
import cl.app.dina.data.model.Solicitante
import cl.app.dina.data.model.SolicitudResumen
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.ui.components.BotonPrincipal
import cl.app.dina.ui.components.CargandoView
import cl.app.dina.ui.components.EncabezadoVolver
import cl.app.dina.ui.components.EspacioCard
import cl.app.dina.ui.components.FotoPerfil
import cl.app.dina.ui.components.MensajeView
import cl.app.dina.ui.components.fechaEvento
import cl.app.dina.ui.components.titulo
import cl.app.dina.ui.theme.DINATheme
import cl.app.dina.ui.theme.RojoAccion
import cl.app.dina.ui.theme.VerdeAccion
import cl.app.dina.ui.viewmodel.AccionSolicitud
import cl.app.dina.ui.viewmodel.AppViewModelProvider
import cl.app.dina.ui.viewmodel.EstadoCarga
import cl.app.dina.ui.viewmodel.SolicitudEvento
import cl.app.dina.ui.viewmodel.SolicitudUiState
import cl.app.dina.ui.viewmodel.SolicitudViewModel
import java.time.LocalDate

class SolicitudAcciones(
    val onVolver: () -> Unit = {},
    val onReintentar: () -> Unit = {},
    val onAccion: (AccionSolicitud) -> Unit = {},
    val onConfirmar: () -> Unit = {},
    val onCancelarConfirmacion: () -> Unit = {},
    val onMotivoRechazoChange: (String) -> Unit = {},
    val onAbrirChat: () -> Unit = {}
)

/**
 * Detalle de una solicitud.
 * Usuario: "Cancelar solicitud" y "Abrir chat". Admin: "Rechazar" (rojo) y "Aceptar" (verde), y "Abrir chat".
 * Cada decisión se confirma en un diálogo, como al cerrar sesión.
 */
@Composable
fun SolicitudScreen(
    onVolver: () -> Unit,
    onAbrirChat: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SolicitudViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                is SolicitudEvento.Hecho -> {
                    Toast.makeText(context, evento.mensaje, Toast.LENGTH_LONG).show()
                    onVolver() // el Inicio se actualiza al volver: la tarjeta ya no aparece
                }
                is SolicitudEvento.Error -> Toast.makeText(context, evento.mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }

    SolicitudContenido(
        state = state,
        acciones = SolicitudAcciones(
            onVolver = onVolver,
            onReintentar = viewModel::reintentar,
            onAccion = viewModel::pedirConfirmacion,
            onConfirmar = viewModel::confirmar,
            onCancelarConfirmacion = viewModel::cancelarConfirmacion,
            onMotivoRechazoChange = viewModel::onMotivoRechazoChange,
            onAbrirChat = onAbrirChat
        ),
        modifier = modifier
    )
}

/** Solo dibuja: recibe el estado y avisa lo que hace el usuario. */
@Composable
private fun SolicitudContenido(state: SolicitudUiState, acciones: SolicitudAcciones, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        EncabezadoVolver("Solicitud", acciones.onVolver, descripcionVolver = "Volver al Inicio")

        val solicitud = state.solicitud
        when (val carga = state.carga) {
            is EstadoCarga.Error -> MensajeView(
                titulo = "No se pudo abrir la solicitud",
                detalle = carga.mensaje,
                textoBoton = "Reintentar",
                onBoton = acciones.onReintentar,
                modifier = Modifier.height(400.dp)
            )
            EstadoCarga.Cargando -> CargandoView(Modifier.height(300.dp))
            EstadoCarga.Listo -> if (solicitud != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                ) {
                    EspacioCard(espacio = solicitud.espacio, onClick = null)
                    if (state.esAdmin) TarjetaSolicitante(solicitud.solicitante)
                    TarjetaDetalle(solicitud, state.hoy)
                    Botones(state, acciones)
                }
            }
        }
    }

    state.solicitud?.let { s -> state.confirmando?.let { DialogoConfirmacion(it, s, state, acciones) } }
}

// Quién la pide (solo admin)
@Composable
private fun TarjetaSolicitante(persona: Solicitante) {
    TarjetaSeccion("Solicitante") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FotoPerfil(persona.nombre, persona.fotoUrl, tamano = 48.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    persona.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOfNotNull(persona.rol?.etiqueta, persona.correo.ifBlank { null }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Información general
@Composable
private fun TarjetaDetalle(solicitud: SolicitudResumen, hoy: LocalDate) {
    TarjetaSeccion("Detalle", extra = { EtiquetaEstado(solicitud.estado) }) {
        FilaDetalle(Icons.Outlined.Event, "Día", fechaEvento(solicitud.fecha, hoy))
        FilaDetalle(
            Icons.Outlined.Schedule,
            "Horario",
            "${solicitud.inicio} a ${solicitud.fin} · ${solicitud.horas} h"
        )
        FilaDetalle(
            Icons.Outlined.Groups,
            "Asistentes",
            "${solicitud.asistentes} de ${solicitud.espacio.capacidad} personas"
        )
        FilaDetalle(Icons.AutoMirrored.Outlined.Notes, "Motivo de la reserva", solicitud.motivo)
        solicitud.comentarioAdmin?.let {
            FilaDetalle(Icons.Outlined.SupervisorAccount, "Comentario del administrador", it)
        }
    }
}

/** Tarjeta con título ("Detalle", "Solicitante") y, opcionalmente, algo a la derecha del título. */
@Composable
private fun TarjetaSeccion(
    titulo: String,
    extra: @Composable () -> Unit = {},
    contenido: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                extra()
            }
            contenido()
        }
    }
}

@Composable
private fun FilaDetalle(icono: ImageVector, etiqueta: String, valor: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            icono,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Column {
            Text(
                etiqueta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(valor, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Píldora con el estado: amarilla en revisión, verde aprobada, roja rechazada o cancelada. */
@Composable
private fun EtiquetaEstado(estado: EstadoSolicitud) {
    val (fondo, texto) = when (estado) {
        EstadoSolicitud.PENDIENTE ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        EstadoSolicitud.APROBADA -> VerdeAccion to Color.White
        EstadoSolicitud.RECHAZADA, EstadoSolicitud.CANCELADA -> RojoAccion to Color.White
    }
    Surface(shape = CircleShape, color = fondo, contentColor = texto) {
        Text(
            estado.etiqueta,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// Botones
@Composable
private fun Botones(state: SolicitudUiState, acciones: SolicitudAcciones) {
    val ocupado = state.enCurso != null
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
        if (state.puedeResponder) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonPrincipal(
                    texto = "Rechazar",
                    onClick = { acciones.onAccion(AccionSolicitud.RECHAZAR) },
                    cargando = state.enCurso == AccionSolicitud.RECHAZAR,
                    habilitado = !ocupado,
                    colores = coloresFijos(RojoAccion),
                    modifier = Modifier.weight(1f)
                )
                BotonPrincipal(
                    texto = "Aceptar",
                    onClick = { acciones.onAccion(AccionSolicitud.APROBAR) },
                    cargando = state.enCurso == AccionSolicitud.APROBAR,
                    habilitado = !ocupado,
                    colores = coloresFijos(VerdeAccion),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (state.puedeCancelar) {
            BotonPrincipal(
                texto = "Cancelar solicitud",
                onClick = { acciones.onAccion(AccionSolicitud.CANCELAR) },
                cargando = state.enCurso == AccionSolicitud.CANCELAR,
                habilitado = !ocupado,
                colores = coloresFijos(RojoAccion)
            )
        }
        OutlinedButton(
            onClick = acciones.onAbrirChat,
            enabled = !ocupado,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text("Abrir chat")
        }
    }
}

/** Fondo de color fijo con texto blanco (igual en tema claro y oscuro). */
@Composable
private fun coloresFijos(fondo: Color) = ButtonDefaults.buttonColors(
    containerColor = fondo,
    contentColor = Color.White,
    disabledContainerColor = fondo.copy(alpha = 0.4f),
    disabledContentColor = Color.White.copy(alpha = 0.8f)
)

// Confirmación, como el diálogo de "Cerrar sesión"
@Composable
private fun DialogoConfirmacion(
    accion: AccionSolicitud,
    solicitud: SolicitudResumen,
    state: SolicitudUiState,
    acciones: SolicitudAcciones
) {
    val cuando = "${solicitud.espacio.titulo} el ${fechaEvento(solicitud.fecha, state.hoy).lowercase()}, " +
        "de ${solicitud.inicio} a ${solicitud.fin}"
    val (titulo, texto, confirmar) = when (accion) {
        AccionSolicitud.CANCELAR -> Triple(
            "¿Cancelar la solicitud?",
            "Se retirará tu solicitud de $cuando. Esta acción no se puede deshacer.",
            "Cancelar solicitud"
        )
        AccionSolicitud.APROBAR -> Triple(
            "¿Aceptar la solicitud?",
            "${solicitud.solicitante.nombre} podrá usar $cuando. Las otras solicitudes pendientes " +
                "en ese horario se rechazarán automáticamente.",
            "Aceptar"
        )
        AccionSolicitud.RECHAZAR -> Triple(
            "¿Rechazar la solicitud?",
            "Se avisará a ${solicitud.solicitante.nombre} que no podrá usar $cuando.",
            "Rechazar"
        )
    }
    // Las acciones que quitan el espacio van en rojo; "Aceptar" usa el color del tema
    val colorConfirmar = if (accion == AccionSolicitud.APROBAR) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.error

    AlertDialog(
        onDismissRequest = acciones.onCancelarConfirmacion,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(texto)
                if (accion == AccionSolicitud.RECHAZAR) {
                    OutlinedTextField(
                        value = state.motivoRechazo,
                        onValueChange = acciones.onMotivoRechazoChange,
                        label = { Text("Motivo (opcional)") },
                        supportingText = { Text("Se incluye en el aviso a quien la pidió") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = acciones.onConfirmar,
                colors = ButtonDefaults.textButtonColors(contentColor = colorConfirmar)
            ) { Text(confirmar) }
        },
        dismissButton = {
            TextButton(onClick = acciones.onCancelarConfirmacion) { Text("Volver") }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun SolicitudAdminPreview() {
    val hoy = LocalDate.now()
    DINATheme {
        SolicitudContenido(
            state = SolicitudUiState(
                carga = EstadoCarga.Listo,
                esAdmin = true,
                hoy = hoy,
                solicitud = SolicitudResumen(
                    id = 1,
                    espacio = Espacio(
                        1, "S11", "Sala S11", "NORTE", "Edificio Norte", 1, TipoEspacio.SALA, 35,
                        null, true, false, false
                    ),
                    fecha = hoy.plusDays(1),
                    inicio = "10:00",
                    fin = "12:00",
                    motivo = "Taller de Android para primer año",
                    asistentes = 20,
                    solicitante = Solicitante(2, "Estudiante de Prueba", Rol.ESTUDIANTE, null, "estudiante.prueba@duocuc.cl")
                )
            ),
            acciones = SolicitudAcciones()
        )
    }
}
