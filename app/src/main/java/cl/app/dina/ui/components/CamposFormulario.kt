package cl.app.dina.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/**
 * Campo de texto de un formulario (ReservaEspacio2, AgregarEspacio): muestra el [error] en rojo
 * debajo o, si no hay, la [ayuda].
 */
@Composable
fun CampoFormulario(
    valor: String,
    onChange: (String) -> Unit,
    etiqueta: String,
    error: String?,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
    habilitado: Boolean = true,
    ejemplo: String? = null,
    lineas: Int = 1,
    teclado: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(etiqueta) },
        placeholder = ejemplo?.let { { Text(it) } },
        singleLine = lineas == 1,
        minLines = lineas,
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        enabled = habilitado,
        modifier = modifier.fillMaxWidth()
    )
}

/** Texto gris pequeño de ayuda bajo una sección. */
@Composable
fun TextoAyuda(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** Error de una sección o del backend, en rojo. */
@Composable
fun TextoError(texto: String, modifier: Modifier = Modifier) {
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = modifier)
}
