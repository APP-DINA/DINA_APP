package cl.app.dina.ui.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.ui.components.CargandoView
import cl.app.dina.ui.components.EspacioCard
import cl.app.dina.ui.components.MensajeView
import cl.app.dina.ui.components.MenuDesplegable
import cl.app.dina.ui.components.SelectorDia
import cl.app.dina.ui.components.colorEscuela
import cl.app.dina.ui.components.nombreEscuela
import cl.app.dina.ui.theme.DINATheme
import cl.app.dina.ui.viewmodel.AppViewModelProvider
import cl.app.dina.ui.viewmodel.Calendario
import cl.app.dina.ui.viewmodel.EstadoCarga
import cl.app.dina.ui.viewmodel.FiltroEscuela
import cl.app.dina.ui.viewmodel.GrupoEdificio
import cl.app.dina.ui.viewmodel.ReservaEspacioUiState
import cl.app.dina.ui.viewmodel.ReservaEspacioViewModel
import java.time.LocalDate

class ReservaEspacioAcciones(
    val onBusquedaChange: (String) -> Unit = {},
    val onTipoChange: (TipoEspacio?) -> Unit = {},
    val onEscuelaChange: (FiltroEscuela?) -> Unit = {},
    val onFechaChange: (LocalDate) -> Unit = {},
    val onLimpiarFiltros: () -> Unit = {},
    val onReintentar: () -> Unit = {},
    val onRefrescar: () -> Unit = {},
    val onEspacioClick: (Espacio) -> Unit = {}
)

/** Catalogo de espacios: buscador, filtros (menus), día de la reserva y listado por edificio. */
@Composable
fun ReservaEspacioScreen(
    onEspacioClick: (Espacio, LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReservaEspacioViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val acciones = ReservaEspacioAcciones(
        onBusquedaChange = viewModel::onBusquedaChange,
        onTipoChange = viewModel::seleccionarTipo,
        onEscuelaChange = viewModel::seleccionarEscuela,
        onFechaChange = viewModel::seleccionarFecha,
        onLimpiarFiltros = viewModel::limpiarFiltros,
        onReintentar = viewModel::reintentar,
        onRefrescar = viewModel::refrescar,
        onEspacioClick = { onEspacioClick(it, state.calendario.fecha) }
    )
    ReservaEspacioContenido(state, acciones, modifier)
}

/** Solo dibuja: recibe el estado y avisa lo que hace el usuario. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReservaEspacioContenido(
    state: ReservaEspacioUiState,
    acciones: ReservaEspacioAcciones,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        Text(
            "Reserva de Espacio",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
        )

        when (val carga = state.carga) {
            EstadoCarga.Cargando -> CargandoView()
            is EstadoCarga.Error -> MensajeView(
                titulo = "No se pudieron cargar los espacios",
                detalle = carga.mensaje,
                textoBoton = "Reintentar",
                onBoton = acciones.onReintentar
            )
            EstadoCarga.Listo -> {
                Buscador(state.filtros.busqueda, acciones.onBusquedaChange)
                Filtros(state, acciones)
                SelectorDia(state.calendario, acciones.onFechaChange) {
                    // Solo con búsqueda o algún filtro activo (el resto del tiempo no ocupa espacio)
                    if (state.filtros.activos) {
                        TextButton(onClick = acciones.onLimpiarFiltros) { Text("Limpiar filtros") }
                    }
                }
                PullToRefreshBox(
                    isRefreshing = state.refrescando,
                    onRefresh = acciones.onRefrescar,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.grupos.isEmpty()) {
                        MensajeView(
                            titulo = "No hay espacios con esa búsqueda",
                            detalle = "Prueba con otro nombre o quita algún filtro.",
                            textoBoton = if (state.filtros.activos) "Limpiar filtros" else null,
                            onBoton = acciones.onLimpiarFiltros
                        )
                    } else {
                        ListaEspacios(state.grupos, acciones.onEspacioClick)
                    }
                }
            }
        }
    }
}

/** Buscador */
@Composable
private fun Buscador(texto: String, onChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = texto,
        onValueChange = onChange,
        placeholder = { Text("Buscar por nombre: S11, Set de TV…") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (texto.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
    )
}

/** Filtros en menus desplegables */
@Composable
private fun Filtros(state: ReservaEspacioUiState, acciones: ReservaEspacioAcciones) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MenuDesplegable(
            etiqueta = "Tipo",
            textoNinguno = "Todos",
            seleccion = state.filtros.tipo,
            opciones = state.tiposDisponibles,
            texto = { it.etiqueta },
            onSeleccion = acciones.onTipoChange,
            modifier = Modifier.weight(1f)
        )
        MenuDesplegable(
            etiqueta = "Escuela",
            textoNinguno = "Todas",
            seleccion = state.filtros.escuela,
            opciones = state.escuelasDisponibles,
            texto = { nombreEscuela(it.codigo) },
            color = { colorEscuela(it.codigo) },
            onSeleccion = acciones.onEscuelaChange,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Listado */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListaEspacios(grupos: List<GrupoEdificio>, onEspacioClick: (Espacio) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        grupos.forEach { grupo ->
            stickyHeader(key = "edificio-${grupo.edificioNombre}") {
                Text(
                    "${grupo.edificioNombre} · ${grupo.espacios.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(top = 12.dp, bottom = 4.dp)
                )
            }
            items(grupo.espacios, key = { it.id }) { espacio ->
                EspacioCard(espacio = espacio, onClick = { onEspacioClick(espacio) })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReservaEspacioScreenPreview() {
    val hoy = LocalDate.now()
    val espacios = listOf(
        Espacio(1, "S11", "Sala S11", "NORTE", "Edificio Norte", 1, TipoEspacio.SALA, 35, null, true, false, false),
        Espacio(2, "SET-TV", "Set de TV", "NORTE", "Edificio Norte", 2, TipoEspacio.ESTUDIO, 20, "COMUNICACION", false, true, true),
        Espacio(3, "LC1", "Laboratorio de Computación 1", "CTI", "Edificio Tecnológico CTI", 2, TipoEspacio.LABORATORIO, 30, "INFORMATICA", true, true, true)
    )
    DINATheme {
        ReservaEspacioContenido(
            state = ReservaEspacioUiState(
                carga = EstadoCarga.Listo,
                calendario = Calendario(hoy).elegir(hoy.plusDays(1)),
                tiposDisponibles = espacios.map { it.tipo }.distinct(),
                escuelasDisponibles = listOf(FiltroEscuela.De("COMUNICACION"), FiltroEscuela.De("INFORMATICA"), FiltroEscuela.UsoGeneral),
                grupos = espacios.groupBy { it.edificioNombre }.map { GrupoEdificio(it.key, it.value) }
            ),
            acciones = ReservaEspacioAcciones()
        )
    }
}
