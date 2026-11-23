package cl.app.dina.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.Rol
import cl.app.dina.data.model.Solicitante
import cl.app.dina.data.model.SolicitudResumen
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.ui.theme.DINATheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Solicitud pendiente, con el mismo formato que la tarjeta de espacio (banda con el color de la
 * escuela del espacio), el día, el horario y el comienzo del motivo. Arriba muestra:
 * - [paraAdmin] = true: quién la pide y su rol (Inicio del administrador);
 * - [paraAdmin] = false: el espacio pedido y "En revisión" (Inicio del usuario: son las suyas).
 */
@Composable
fun SolicitudCard(
    solicitud: SolicitudResumen,
    hoy: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    paraAdmin: Boolean = true,
    /** En una doble reserva: orden de llegada (1 = la primera en pedirse). Muestra además cuándo se pidió. */
    orden: Int? = null
) {
    val persona = solicitud.solicitante
    val rol = persona.rol?.etiqueta ?: "Usuario"
    val dia = fechaEvento(solicitud.fecha, hoy)
    val horario = "${solicitud.inicio} a ${solicitud.fin}"
    val lugar = "${solicitud.espacio.titulo} · ${solicitud.espacio.edificioNombre}"
    // La fecha ya pasó sin revisarse: se marca en rojo (solo se podrá rechazar)
    val colorDia = if (solicitud.fecha < hoy) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurfaceVariant

    val descripcion = if (paraAdmin) "Solicitud de ${persona.nombre}, $rol. $dia, de $horario, en $lugar."
    else "Tu solicitud de $lugar, en revisión. $dia, de $horario."

    TarjetaConBanda(
        banda = colorEscuela(solicitud.espacio.escuela),
        descripcion = "$descripcion Motivo: ${solicitud.motivo}",
        onClick = onClick,
        modifier = modifier
    ) {
        if (paraAdmin) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FotoPerfil(persona.nombre, persona.fotoUrl, tamano = 44.dp)
                Titulo(persona.nombre, rol, Modifier.weight(1f))
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Titulo(
                    solicitud.espacio.titulo,
                    "${solicitud.espacio.edificioNombre} · Piso ${solicitud.espacio.piso}",
                    Modifier.weight(1f)
                )
                EtiquetaEnRevision()
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(top = 6.dp)
        ) {
            Dato(Icons.Outlined.Event, dia, colorDia)
            Dato(Icons.Outlined.Schedule, "$horario · ${personas(solicitud.asistentes)}")
            if (paraAdmin) Dato(Icons.Outlined.Place, lugar)
            if (orden != null) {
                val cuando = solicitud.creadaEn?.let { " · ${it.toLocalDate().diaCorto}, ${it.toLocalTime().hhmm()}" }.orEmpty()
                Dato(Icons.Outlined.History, "$orden.ª en pedirla$cuando", MaterialTheme.colorScheme.onSurface)
            }
        }
        Text(
            "“${solicitud.motivo}”",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** Título y subtítulo de la tarjeta: "Victor Godoy / Estudiante" o "S11 / Edificio Norte · Piso 1". */
@Composable
private fun Titulo(titulo: String, subtitulo: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            titulo,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            subtitulo,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Píldora "En revisión": la solicitud espera la respuesta del administrador. */
@Composable
private fun EtiquetaEnRevision() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Outlined.HourglassTop, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(
                "En revisión",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/** 10:15:30 → "10:15" */
fun LocalTime.hhmm(): String = "%02d:%02d".format(hour, minute)

/** Hora de un mensaje en la lista de chats: "10:15" si es de hoy, "Ayer" o "Lun. 23". */
fun horaChat(momento: LocalDateTime, hoy: LocalDate): String = when (momento.toLocalDate()) {
    hoy -> momento.toLocalTime().hhmm()
    hoy.minusDays(1) -> "Ayer"
    else -> momento.toLocalDate().diaCorto
}

/** Separador de día dentro de una conversación: "Hoy", "Ayer" o "Lun. 23 de noviembre". */
fun diaConversacion(dia: LocalDate, hoy: LocalDate): String = when (dia) {
    hoy -> "Hoy"
    hoy.minusDays(1) -> "Ayer"
    else -> "${dia.diaCorto} de ${dia.nombreMes.lowercase()}"
}

/** "Hoy · Lun. 23 de noviembre", "Mañana · Mar. 24 de noviembre" o "Mié. 25 de noviembre". */
fun fechaEvento(fecha: LocalDate, hoy: LocalDate): String {
    val prefijo = when (fecha) {
        hoy -> "Hoy · "
        hoy.plusDays(1) -> "Mañana · "
        else -> ""
    }
    return "$prefijo${fecha.diaCorto} de ${fecha.nombreMes.lowercase()}"
}

@Preview(showBackground = true)
@Composable
private fun SolicitudCardPreview() {
    val hoy = LocalDate.now()
    DINATheme {
        SolicitudCard(
            solicitud = SolicitudResumen(
                id = 1,
                espacio = Espacio(
                    3, "LC1", "Laboratorio de Computación 1", "CTI", "Edificio Tecnológico CTI", 2,
                    TipoEspacio.LABORATORIO, 30, "INFORMATICA", true, true, true
                ),
                fecha = hoy.plusDays(1),
                inicio = "10:00",
                fin = "12:00",
                motivo = "Taller de programación para primer año, necesitamos los computadores y el proyector.",
                asistentes = 20,
                solicitante = Solicitante(2, "Estudiante de Prueba", Rol.ESTUDIANTE, null)
            ),
            hoy = hoy,
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
