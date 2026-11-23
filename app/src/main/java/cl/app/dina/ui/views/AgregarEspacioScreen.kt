package cl.app.dina.ui.views

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.app.dina.data.model.Edificio
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.ui.components.BotonPrincipal
import cl.app.dina.ui.components.CampoFormulario
import cl.app.dina.ui.components.EncabezadoVolver
import cl.app.dina.ui.components.EspacioCard
import cl.app.dina.ui.components.MenuDesplegable
import cl.app.dina.ui.components.TextoAyuda
import cl.app.dina.ui.components.TextoError
import cl.app.dina.ui.components.colorEscuela
import cl.app.dina.ui.components.nombreEscuela
import cl.app.dina.ui.theme.DINATheme
import cl.app.dina.ui.theme.Escuela
import cl.app.dina.ui.viewmodel.AgregarEspacioEvento
import cl.app.dina.ui.viewmodel.AgregarEspacioUiState
import cl.app.dina.ui.viewmodel.AgregarEspacioViewModel
import cl.app.dina.ui.viewmodel.AppViewModelProvider

class AgregarEspacioAcciones(
    val onVolver: () -> Unit = {},
    val onCodigoChange: (String) -> Unit = {},
    val onNombreChange: (String) -> Unit = {},
    val onEdificioChange: (Edificio) -> Unit = {},
    val onPisoChange: (String) -> Unit = {},
    val onTipoChange: (TipoEspacio) -> Unit = {},
    val onCapacidadChange: (String) -> Unit = {},
    val onEscuelaChange: (String?) -> Unit = {},
    val onProyectorChange: (Boolean) -> Unit = {},
    val onComputadoresChange: (Boolean) -> Unit = {},
    val onClimatizacionChange: (Boolean) -> Unit = {},
    val onAgregar: () -> Unit = {}
)

/**
 * Agregar espacio (solo ADMIN), con el mismo formato que ReservaEspacio2: encabezado "<",
 * tarjeta del espacio (aquí, una vista previa que cambia mientras se escribe) y el formulario.
 */
@Composable
fun AgregarEspacioScreen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AgregarEspacioViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                is AgregarEspacioEvento.Agregado -> {
                    Toast.makeText(context, "${evento.espacio.codigo} agregado a los espacios", Toast.LENGTH_SHORT).show()
                    onVolver()
                }
            }
        }
    }

    AgregarEspacioContenido(
        state = state,
        acciones = AgregarEspacioAcciones(
            onVolver = onVolver,
            onCodigoChange = viewModel::onCodigoChange,
            onNombreChange = viewModel::onNombreChange,
            onEdificioChange = viewModel::onEdificioChange,
            onPisoChange = viewModel::onPisoChange,
            onTipoChange = viewModel::onTipoChange,
            onCapacidadChange = viewModel::onCapacidadChange,
            onEscuelaChange = viewModel::onEscuelaChange,
            onProyectorChange = viewModel::onProyectorChange,
            onComputadoresChange = viewModel::onComputadoresChange,
            onClimatizacionChange = viewModel::onClimatizacionChange,
            onAgregar = viewModel::agregar
        ),
        modifier = modifier
    )
}

/** Solo dibuja: recibe el estado y avisa lo que hace el usuario. */
@Composable
private fun AgregarEspacioContenido(
    state: AgregarEspacioUiState,
    acciones: AgregarEspacioAcciones,
    modifier: Modifier = Modifier
) {
    val habilitado = !state.enviando
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        EncabezadoVolver("Agregar espacio", acciones.onVolver, descripcionVolver = "Volver al Menú")

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
            TextoAyuda("Así se verá en Reservar:")
            EspacioCard(espacio = state.vistaPrevia, onClick = null)

            TituloFormulario("Identificación")
            CampoFormulario(
                valor = state.codigo,
                onChange = acciones.onCodigoChange,
                etiqueta = "Código",
                error = state.errorCodigo,
                ayuda = "El que se ve en la puerta. No se puede repetir",
                habilitado = habilitado,
                ejemplo = "Ej.: S41"
            )
            CampoFormulario(
                valor = state.nombre,
                onChange = acciones.onNombreChange,
                etiqueta = "Nombre (opcional)",
                error = state.errorNombre,
                ayuda = if (state.codigo.isBlank()) "Si lo dejas vacío se usa el tipo y el código (ej.: Sala S41)"
                else "Si lo dejas vacío: \"${state.nombreFinal}\"",
                habilitado = habilitado,
                ejemplo = "Ej.: Set de TV"
            )
            MenuDesplegable(
                etiqueta = "Tipo",
                seleccion = state.tipo,
                opciones = TipoEspacio.entries - TipoEspacio.OTRO,
                texto = { it.etiqueta },
                onSeleccion = { it?.let(acciones.onTipoChange) },
                habilitado = habilitado,
                modifier = Modifier.fillMaxWidth()
            )

            TituloFormulario("Ubicación")
            MenuDesplegable(
                etiqueta = "Edificio",
                seleccion = state.edificio,
                opciones = Edificio.entries,
                texto = { it.nombre },
                onSeleccion = { it?.let(acciones.onEdificioChange) },
                habilitado = habilitado,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoFormulario(
                    valor = state.piso,
                    onChange = acciones.onPisoChange,
                    etiqueta = "Piso",
                    error = state.errorPiso,
                    habilitado = habilitado,
                    teclado = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                CampoFormulario(
                    valor = state.capacidad,
                    onChange = acciones.onCapacidadChange,
                    etiqueta = "Capacidad",
                    error = state.errorCapacidad,
                    ayuda = "Personas",
                    habilitado = habilitado,
                    teclado = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }

            TituloFormulario("Uso principal")
            MenuDesplegable(
                etiqueta = "Escuela",
                seleccion = state.escuela,
                opciones = Escuela.entries.map { it.name },
                texto = ::nombreEscuela,
                onSeleccion = acciones.onEscuelaChange,
                textoNinguno = "Uso general",
                habilitado = habilitado,
                color = { colorEscuela(it) },
                modifier = Modifier.fillMaxWidth()
            )
            TextoAyuda("Define el color de la banda de la tarjeta.")

            TituloFormulario("Equipamiento")
            Interruptor("Proyector", state.proyector, acciones.onProyectorChange, habilitado)
            Interruptor("Computadores", state.computadores, acciones.onComputadoresChange, habilitado)
            Interruptor("Climatización", state.climatizacion, acciones.onClimatizacionChange, habilitado)

            state.errorGeneral?.let { TextoError(it, Modifier.padding(top = 8.dp)) }
            BotonPrincipal(
                texto = "Agregar espacio",
                onClick = acciones.onAgregar,
                cargando = state.enviando,
                modifier = Modifier.padding(top = 8.dp)
            )
            TextoAyuda("Quedará disponible para reservar por bloques de inmediato.")
        }
    }
}

@Composable
private fun TituloFormulario(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 12.dp)
    )
}

/** Fila "Proyector ........ (o)": toda la fila se puede tocar. */
@Composable
private fun Interruptor(texto: String, activo: Boolean, onCambio: (Boolean) -> Unit, habilitado: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = activo, enabled = habilitado, role = Role.Switch, onValueChange = onCambio)
            .padding(vertical = 4.dp)
    ) {
        Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = activo, onCheckedChange = null, enabled = habilitado)
    }
}

@Preview(showBackground = true, heightDp = 1300)
@Composable
private fun AgregarEspacioPreview() {
    DINATheme {
        AgregarEspacioContenido(
            state = AgregarEspacioUiState(codigo = "S99", capacidad = "40", escuela = "INFORMATICA", proyector = true),
            acciones = AgregarEspacioAcciones()
        )
    }
}
