package cl.app.dina.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.ui.theme.DINATheme
import cl.app.dina.ui.theme.Escuela

/** Escuela de la paleta Duoc a partir del código que guarda el espacio, o null si es de uso general. */
fun escuelaDe(codigo: String?): Escuela? = Escuela.entries.firstOrNull { it.name == codigo }

/** Nombre para mostrar: "Informática", o "Uso general" si el espacio no tiene escuela. */
fun nombreEscuela(codigo: String?): String = escuelaDe(codigo)?.nombre ?: codigo ?: "Uso general"

/** Color de la escuela (banda y puntos); gris para uso general. Lo usan la tarjeta y los filtros. */
@Composable
fun colorEscuela(codigo: String?): Color =
    escuelaDe(codigo)?.let { DINATheme.escuelas[it].banda } ?: MaterialTheme.colorScheme.outline

/** "S11" para las salas ("Sala S11"); el nombre completo para el resto ("Set de TV"). */
val Espacio.titulo: String get() = if (nombre.endsWith(codigo)) codigo else nombre

/**
 * Tarjeta de un espacio en el listado de ReservaEspacio.
 * La banda de la izquierda tiene el color de la escuela de uso principal (gris si es de uso general).
 */
@Composable
fun EspacioCard(espacio: Espacio, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val escuela = escuelaDe(espacio.escuela)
    val banda = colorEscuela(espacio.escuela)
    val descripcion = buildString {
        append(espacio.titulo).append(", ").append(espacio.tipo.etiqueta)
        append(", ").append(espacio.edificioNombre).append(", piso ").append(espacio.piso)
        append(", capacidad ").append(espacio.capacidad).append(" personas")
        escuela?.let { append(", escuela ").append(it.nombre) }
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = descripcion }
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Banda de la escuela
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(banda)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${espacio.edificioNombre} · Piso ${espacio.piso}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        espacio.titulo,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Dato(Icons.Outlined.Category, espacio.tipo.etiqueta)
                        Dato(Icons.Outlined.Groups, "${espacio.capacidad} personas")
                    }
                    if (escuela != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .background(banda, CircleShape)
                            )
                            Text(
                                escuela.nombre,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Dato(icono: ImageVector, texto: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EspacioCardPreview() {
    DINATheme {
        EspacioCard(
            espacio = Espacio(
                id = 1, codigo = "SET-TV", nombre = "Set de TV", edificio = "NORTE",
                edificioNombre = "Edificio Norte", piso = 2, tipo = TipoEspacio.ESTUDIO, capacidad = 20,
                escuela = "COMUNICACION", proyector = false, computadores = true, climatizacion = true
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
