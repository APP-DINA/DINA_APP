package cl.app.dina.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Encabezado de una pantalla secundaria, como en Ajustes de Android: "<  Solicitar espacio".
 * [descripcionVolver] es lo que lee TalkBack en la flecha.
 */
@Composable
fun EncabezadoVolver(
    titulo: String,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    descripcionVolver: String = "Volver"
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        IconButton(onClick = onVolver) {
            Icon(
                Icons.Filled.ArrowBackIosNew,
                contentDescription = descripcionVolver,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            titulo,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
