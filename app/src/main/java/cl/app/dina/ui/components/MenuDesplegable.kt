package cl.app.dina.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Menú desplegable de Material 3 (ExposedDropdownMenuBox). Lo usan los filtros de ReservaEspacio
 * y el formulario de AgregarEspacio.
 * @param textoNinguno texto de la opción "sin elegir" (null en [seleccion]): "Todos", "Uso general"…
 *   Si es null, esa opción no existe y siempre hay que elegir una de [opciones].
 * @param color punto de color delante de cada opción (p. ej. el color de la escuela).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> MenuDesplegable(
    etiqueta: String,
    seleccion: T?,
    opciones: List<T>,
    texto: (T) -> String,
    onSeleccion: (T?) -> Unit,
    modifier: Modifier = Modifier,
    textoNinguno: String? = null,
    habilitado: Boolean = true,
    color: (@Composable (T) -> Color)? = null
) {
    var abierto by remember { mutableStateOf(false) }

    // Punto de color de la opción; espacio vacío para "ninguno"; nada si el menú no usa colores.
    val punto: (T?) -> (@Composable () -> Unit)? = { opcion ->
        color?.let { colorDe -> { PuntoColor(opcion?.let { colorDe(it) }) } }
    }
    val todas: List<T?> = if (textoNinguno != null) listOf(null) + opciones else opciones

    ExposedDropdownMenuBox(
        expanded = abierto,
        onExpandedChange = { if (habilitado) abierto = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = seleccion?.let(texto) ?: textoNinguno.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = habilitado,
            singleLine = true,
            label = { Text(etiqueta) },
            leadingIcon = seleccion?.let { punto(it) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, habilitado)
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            todas.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion?.let(texto) ?: textoNinguno.orEmpty()) },
                    leadingIcon = punto(opcion),
                    onClick = {
                        onSeleccion(opcion)
                        abierto = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
private fun PuntoColor(color: Color?) {
    Box(
        Modifier
            .size(10.dp)
            .background(color ?: Color.Transparent, CircleShape)
    )
}
