package cl.app.dina.ui.views

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.app.dina.data.model.BloqueDia
import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.EstadoBloque
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.ui.components.BotonPrincipal
import cl.app.dina.ui.components.CargandoView
import cl.app.dina.ui.components.EncabezadoVolver
import cl.app.dina.ui.components.EspacioCard
import cl.app.dina.ui.components.MensajeView
import cl.app.dina.ui.components.SelectorDia
import cl.app.dina.ui.components.diaCorto
import cl.app.dina.ui.theme.DINATheme
import cl.app.dina.ui.viewmodel.AppViewModelProvider
import cl.app.dina.ui.viewmodel.Calendario
import cl.app.dina.ui.viewmodel.EstadoCarga
import cl.app.dina.ui.viewmodel.ReservaEspacio2Evento
import cl.app.dina.ui.viewmodel.ReservaEspacio2UiState
import cl.app.dina.ui.viewmodel.ReservaEspacio2ViewModel
import java.time.LocalDate

/** Lo que el usuario puede hacer en el formulario (mismo patrón que ReservaEspacioAcciones). */
class ReservaEspacio2Acciones(
    val onVolver: () -> Unit = {},
    val onFechaChange: (LocalDate) -> Unit = {},
    val onBloqueClick: (BloqueDia) -> Unit = {},
    val onReintentarBloques: () -> Unit = {},
    val onAsistentesChange: (String) -> Unit = {},
    val onMotivoChange: (String) -> Unit = {},
    val onSolicitar: () -> Unit = {}
)

/**
 * Formulario para solicitar el espacio elegido en ReservaEspacio.
 * El espacio y el día llegan por la navegación; aquí solo se puede cambiar el día,
 * elegir los bloques, indicar asistentes y el motivo. [onVolver] regresa a ReservaEspacio.
 */
@Composable
fun ReservaEspacio2Screen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReservaEspacio2ViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Solicitud enviada: aviso y vuelta al listado (una sola vez, aunque se gire el teléfono)
    LaunchedEffect(Unit) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                is ReservaEspacio2Evento.SolicitudEnviada -> {
                    val s = evento.solicitud
                    val aviso = "Solicitud enviada para el ${s.fecha.diaCorto}, de ${s.inicio} a ${s.fin}. " +
                        "Te avisaremos cuando el administrador la revise."
                    Toast.makeText(context, aviso, Toast.LENGTH_LONG).show()
                    onVolver()
                }
            }
        }
    }

    ReservaEspacio2Contenido(
        state = state,
        acciones = ReservaEspacio2Acciones(
            onVolver = onVolver,
            onFechaChange = viewModel::seleccionarFecha,
            onBloqueClick = viewModel::onBloqueClick,
            onReintentarBloques = viewModel::reintentarBloques,
            onAsistentesChange = viewModel::onAsistentesChange,
            onMotivoChange = viewModel::onMotivoChange,
            onSolicitar = viewModel::solicitar
        ),
        modifier = modifier
    )
}

/** Solo dibuja: recibe el estado y avisa lo que hace el usuario. */
@Composable
private fun ReservaEspacio2Contenido(
    state: ReservaEspacio2UiState,
    acciones: ReservaEspacio2Acciones,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        EncabezadoVolver("Solicitar espacio", acciones.onVolver, descripcionVolver = "Volver a Reserva de Espacio")

        val espacio = state.espacio
        when {
            state.errorEspacio != null -> MensajeView(
                titulo = "No se pudo abrir el espacio",
                detalle = state.errorEspacio,
                textoBoton = "Volver",
                onBoton = acciones.onVolver
            )
            espacio == null -> CargandoView(Modifier.height(200.dp))
            else -> {
                EspacioCard(espacio = espacio, onClick = null, modifier = Modifier.padding(horizontal = 16.dp))
                SelectorDia(state.calendario, acciones.onFechaChange, modifier = Modifier.padding(top = 8.dp))
                SeccionBloques(state, acciones)
                Formulario(state, espacio, acciones)
            }
        }
    }
}

// ── Bloques del día ─────────────────────────────────────────────────────────

@Composable
private fun SeccionBloques(state: ReservaEspacio2UiState, acciones: ReservaEspacio2Acciones) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Bloques",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            state.resumenSeleccion?.let {
                Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }

        when (val carga = state.cargaBloques) {
            EstadoCarga.Cargando -> CargandoView(Modifier.height(120.dp))
            is EstadoCarga.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                TextoError(carga.mensaje, Modifier.weight(1f))
                TextButton(onClick = acciones.onReintentarBloques) { Text("Reintentar") }
            }
            EstadoCarga.Listo ->
                if (state.diaSinBloques) {
                    TextoAyuda("No hay bloques para este día: la sede no atiende o ya pasaron. Elige otro día.")
                } else {
                    GrillaBloques(state.bloques, state.seleccion, acciones.onBloqueClick)
                    TextoAyuda("Toca de 1 a ${ReservaEspacio2ViewModel.MAX_BLOQUES} bloques seguidos.")
                }
        }

        state.errorBloques?.let { TextoError(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GrillaBloques(bloques: List<BloqueDia>, seleccion: List<BloqueDia>, onClick: (BloqueDia) -> Unit) {
    val elegidos = seleccion.map { it.id }.toSet()
    FlowRow(
        maxItemsInEachRow = COLUMNAS_BLOQUES,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        bloques.forEach { bloque ->
            ChipBloque(bloque, bloque.id in elegidos, { onClick(bloque) }, Modifier.weight(1f))
        }
        // Completa la última fila para que todos los bloques tengan el mismo ancho
        repeat((COLUMNAS_BLOQUES - bloques.size % COLUMNAS_BLOQUES) % COLUMNAS_BLOQUES) { Box(Modifier.weight(1f)) }
    }
}

private const val COLUMNAS_BLOQUES = 3

@Composable
private fun ChipBloque(bloque: BloqueDia, seleccionado: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colores = MaterialTheme.colorScheme
    val nota = when {
        bloque.mio -> "Tu solicitud"
        bloque.estado == EstadoBloque.OCUPADO -> "Ocupado"
        bloque.estado == EstadoBloque.NO_DISPONIBLE -> "No disponible"
        bloque.pendientes > 0 -> "${bloque.pendientes} en espera"
        else -> null
    }
    val descripcion = listOfNotNull("Bloque de ${bloque.inicio} a ${bloque.fin}", nota, "seleccionado".takeIf { seleccionado })
        .joinToString(", ")

    Surface(
        onClick = onClick,
        enabled = bloque.seleccionable,
        shape = RoundedCornerShape(12.dp),
        color = if (seleccionado) colores.primaryContainer else colores.surfaceContainerHigh,
        contentColor = if (seleccionado) colores.onPrimaryContainer else colores.onSurface,
        modifier = modifier
            .height(56.dp)
            .alpha(if (bloque.seleccionable) 1f else 0.45f)
            .semantics { contentDescription = descripcion }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                "${bloque.inicio} – ${bloque.fin}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
            nota?.let { Text(it, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
        }
    }
}

// ── Asistentes, motivo y botón Solicitar ────────────────────────────────────

@Composable
private fun Formulario(state: ReservaEspacio2UiState, espacio: Espacio, acciones: ReservaEspacio2Acciones) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
    ) {
        CampoFormulario(
            valor = state.asistentes,
            onChange = acciones.onAsistentesChange,
            etiqueta = "Asistentes",
            error = state.errorAsistentes,
            ayuda = "Capacidad máxima: ${espacio.capacidad} personas",
            habilitado = !state.enviando,
            teclado = KeyboardType.Number
        )
        CampoFormulario(
            valor = state.motivo,
            onChange = acciones.onMotivoChange,
            etiqueta = "Motivo de la reserva",
            error = state.errorMotivo,
            ayuda = "${state.motivo.length}/${ReservaEspacio2ViewModel.MAX_MOTIVO}",
            habilitado = !state.enviando,
            ejemplo = "Ej.: Clase de Programación de Aplicaciones Móviles",
            lineas = 3
        )

        state.errorGeneral?.let { TextoError(it) }
        BotonPrincipal(texto = "Solicitar", onClick = acciones.onSolicitar, cargando = state.enviando)
        TextoAyuda("La solicitud queda pendiente hasta que un administrador la apruebe.")
    }
}

/**
 * Campo de texto del formulario: debajo muestra el [error] si lo hay, o la [ayuda].
 * [lineas] = 1 es un campo de una línea; más es un área de texto.
 */
@Composable
private fun CampoFormulario(
    valor: String,
    onChange: (String) -> Unit,
    etiqueta: String,
    error: String?,
    ayuda: String,
    habilitado: Boolean,
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
        supportingText = { Text(error ?: ayuda) },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        enabled = habilitado,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TextoAyuda(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TextoError(texto: String, modifier: Modifier = Modifier) {
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = modifier)
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun ReservaEspacio2ScreenPreview() {
    val hoy = LocalDate.now()
    val bloques = (1..14).map { n ->
        BloqueDia(
            id = n.toLong(), numero = n,
            inicio = "%02d:00".format(7 + n), fin = "%02d:00".format(8 + n),
            estado = if (n == 6) EstadoBloque.OCUPADO else EstadoBloque.DISPONIBLE,
            pendientes = if (n == 5) 2 else 0, mio = false
        )
    }
    DINATheme {
        ReservaEspacio2Contenido(
            state = ReservaEspacio2UiState(
                espacio = Espacio(1, "S11", "Sala S11", "NORTE", "Edificio Norte", 1, TipoEspacio.SALA, 35, null, true, false, false),
                calendario = Calendario(hoy).elegir(hoy.plusDays(1)),
                cargaBloques = EstadoCarga.Listo,
                bloques = bloques,
                seleccion = bloques.subList(2, 4),
                asistentes = "20"
            ),
            acciones = ReservaEspacio2Acciones()
        )
    }
}
