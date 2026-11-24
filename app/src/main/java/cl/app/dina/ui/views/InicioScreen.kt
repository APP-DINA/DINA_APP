package cl.app.dina.ui.views

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.Rol
import cl.app.dina.data.model.Solicitante
import cl.app.dina.data.model.SolicitudResumen
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.data.model.Usuario
import cl.app.dina.ui.components.BandaSinConexion
import cl.app.dina.ui.components.CargandoView
import cl.app.dina.ui.components.EncabezadoPerfil
import cl.app.dina.ui.components.MensajeView
import cl.app.dina.ui.components.SolicitudCard
import cl.app.dina.ui.theme.DINATheme
import cl.app.dina.ui.theme.RojoAccion
import cl.app.dina.ui.viewmodel.AppViewModelProvider
import cl.app.dina.ui.viewmodel.EntradaRevision
import cl.app.dina.ui.viewmodel.EstadoCarga
import cl.app.dina.ui.viewmodel.InicioEvento
import cl.app.dina.ui.viewmodel.InicioUiState
import cl.app.dina.ui.viewmodel.InicioViewModel
import java.time.LocalDate

class InicioAcciones(
    val onReintentar: () -> Unit = {},
    val onRefrescar: () -> Unit = {},
    val onSolicitudClick: (Long) -> Unit = {},
    val onReservar: () -> Unit = {}
)

/**
 * Primera pantalla al abrir la app: encabezado con la foto y las solicitudes pendientes
 * (ADMIN: las que debe revisar; usuario: las suyas que esperan respuesta).
 */
@Composable
fun InicioScreen(
    onSolicitudClick: (Long) -> Unit,
    onReservar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InicioViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Al volver al Inicio (otra pestaña, otra app) se consulta otra vez: así desaparecen
    // las solicitudes que el administrador ya aprobó o rechazó
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.actualizar() }

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                is InicioEvento.Mensaje -> Toast.makeText(context, evento.texto, Toast.LENGTH_SHORT).show()
            }
        }
    }

    InicioContenido(
        state = state,
        acciones = InicioAcciones(
            onReintentar = viewModel::reintentar,
            onRefrescar = viewModel::refrescar,
            onSolicitudClick = onSolicitudClick,
            onReservar = onReservar
        ),
        modifier = modifier
    )
}

/** Solo dibuja: recibe el estado y avisa lo que hace el usuario. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InicioContenido(state: InicioUiState, acciones: InicioAcciones, modifier: Modifier = Modifier) {
    val usuario = state.usuario ?: return CargandoView(modifier)

    PullToRefreshBox(
        isRefreshing = state.refrescando,
        onRefresh = acciones.onRefrescar,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "encabezado") {
                EncabezadoPerfil(usuario)
            }
            if (state.sinConexion) {
                item(key = "sinConexion") {
                    BandaSinConexion(
                        Modifier
                            .animateItem()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp)
                    )
                }
            }
            solicitudesPendientes(state, acciones, paraAdmin = usuario.esAdmin)
        }
    }
}

/**
 * Lista de solicitudes pendientes; al tocar una se abre su detalle (SolicitudScreen).
 * ADMIN: las de todos, para aceptarlas o rechazarlas. Usuario: las propias que esperan respuesta;
 * cuando el admin responde (o el usuario la cancela) dejan de estar pendientes y desaparecen de aquí.
 */
private fun LazyListScope.solicitudesPendientes(state: InicioUiState, acciones: InicioAcciones, paraAdmin: Boolean) {
    item(key = "titulo") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
        ) {
            Text(
                if (paraAdmin) "Solicitudes por revisar" else "Mis solicitudes pendientes",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (state.cargaSolicitudes == EstadoCarga.Listo && state.solicitudes.isNotEmpty()) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        "${state.solicitudes.size}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }

    val alturaMensaje = Modifier.height(260.dp)
    when (val carga = state.cargaSolicitudes) {
        EstadoCarga.Cargando -> item(key = "cargando") { CargandoView(alturaMensaje) }
        is EstadoCarga.Error -> item(key = "error") {
            MensajeView(
                titulo = "No se pudieron cargar las solicitudes",
                detalle = carga.mensaje,
                textoBoton = "Reintentar",
                onBoton = acciones.onReintentar,
                modifier = alturaMensaje
            )
        }
        EstadoCarga.Listo ->
            if (state.solicitudes.isEmpty()) {
                item(key = "vacio") {
                    if (paraAdmin) {
                        MensajeView(
                            titulo = "Todo al día",
                            detalle = "No hay solicitudes pendientes de revisar. Desliza hacia abajo para actualizar.",
                            modifier = alturaMensaje
                        )
                    } else {
                        MensajeView(
                            titulo = "No tienes solicitudes pendientes",
                            detalle = "Cuando pidas un espacio, aparecerá aquí hasta que el administrador responda.",
                            textoBoton = "Reservar un espacio",
                            onBoton = acciones.onReservar,
                            modifier = alturaMensaje
                        )
                    }
                }
            } else {
                if (paraAdmin) listaAdmin(state.entradas, state.hoy, acciones)
                else tarjetas(state.solicitudes, state.hoy, acciones, paraAdmin = false)
            }
    }
}

/**
 * ADMIN: una sola lista, del día y hora más cercanos a hoy al más lejano. Cada solicitud sola va
 * bajo "Reserva"; las que piden el mismo espacio y horario van juntas bajo "Doble reserva · N",
 * de la pedida más recientemente a la más antigua.
 */
private fun LazyListScope.listaAdmin(entradas: List<EntradaRevision>, hoy: LocalDate, acciones: InicioAcciones) {
    entradas.forEach { entrada ->
        when (entrada) {
            is EntradaRevision.Reserva -> {
                item(key = "titulo-${entrada.solicitud.id}") { TituloEntrada("Reserva") }
                tarjetas(listOf(entrada.solicitud), hoy, acciones, paraAdmin = true)
            }
            is EntradaRevision.Doble -> {
                // La clave usa la primera en pedirse: no cambia cuando llega una solicitud nueva al grupo
                item(key = "titulo-doble-${entrada.porLlegada.first().id}") {
                    TituloEntrada(
                        titulo = "Doble reserva · ${entrada.porLlegada.size}",
                        icono = Icons.Filled.Error,
                        colorIcono = RojoAccion
                    )
                }
                tarjetas(entrada.masRecientePrimero, hoy, acciones, paraAdmin = true, orden = entrada::ordenDeLlegada)
            }
        }
    }
}

/** Tarjetas de solicitud. Con orden (doble reserva), cada una muestra su orden de llegada */
private fun LazyListScope.tarjetas(
    solicitudes: List<SolicitudResumen>,
    hoy: LocalDate,
    acciones: InicioAcciones,
    paraAdmin: Boolean,
    orden: ((SolicitudResumen) -> Int)? = null
) {
    items(solicitudes, key = { it.id }) { solicitud ->
        SolicitudCard(
            solicitud = solicitud,
            hoy = hoy,
            onClick = { acciones.onSolicitudClick(solicitud.id) },
            paraAdmin = paraAdmin,
            orden = orden?.invoke(solicitud),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/** Título sobre cada elemento de la lista del admin: "Reserva" o "Doble reserva". */
@Composable
private fun TituloEntrada(
    titulo: String,
    icono: ImageVector? = null,
    colorIcono: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
    ) {
        if (icono != null) Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(20.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Preview(showBackground = true)
@Composable
private fun InicioAdminPreview() {
    val hoy = LocalDate.now()
    val espacio = Espacio(1, "S11", "Sala S11", "NORTE", "Edificio Norte", 1, TipoEspacio.SALA, 35, null, true, false, false)
    DINATheme {
        InicioContenido(
            state = InicioUiState(
                usuario = Usuario("admin.prueba@duocuc.cl", "Admin de Prueba", Rol.ESTUDIANTE, esAdmin = true),
                cargaSolicitudes = EstadoCarga.Listo,
                hoy = hoy,
                solicitudes = listOf(
                    SolicitudResumen(
                        1, espacio, hoy, "10:00", "12:00", "Reunión del centro de alumnos", 12,
                        Solicitante(2, "Estudiante de Prueba", Rol.ESTUDIANTE, null)
                    ),
                    SolicitudResumen(
                        2, espacio, hoy, "11:00", "12:00", "Clase de reforzamiento de cálculo", 30,
                        Solicitante(3, "Docente de Prueba", Rol.PROFESOR, null)
                    ),
                    SolicitudResumen(
                        3, espacio, hoy.plusDays(2), "14:00", "15:00", "Ensayo de presentación", 8,
                        Solicitante(2, "Estudiante de Prueba", Rol.ESTUDIANTE, null)
                    )
                )
            ),
            acciones = InicioAcciones()
        )
    }
}
