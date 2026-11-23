package cl.app.dina.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.app.dina.ui.viewmodel.Calendario
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val MESES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)
private val DIAS_SEMANA = listOf("Lun.", "Mar.", "Mié.", "Jue.", "Vie.", "Sáb.", "Dom.")

/** "Noviembre" */
val LocalDate.nombreMes: String get() = MESES[monthValue - 1]

/** "Mar. 24" */
val LocalDate.diaCorto: String get() = "${DIAS_SEMANA[dayOfWeek.value - 1]} $dayOfMonth"

/**
 * Selector del día de la reserva (lo usan ReservaEspacio y ReservaEspacio2):
 * El calendario (DatePickerDialog de Material 3)
 */
@Composable
fun SelectorDia(
    calendario: Calendario,
    onFechaChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    extras: @Composable RowScope.() -> Unit = {}
) {
    var mostrarCalendario by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${calendario.fecha.nombreMes} ${calendario.fecha.year}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        extras()
        if (calendario.lejosDeHoy) {
            TextButton(onClick = { onFechaChange(calendario.hoy) }) { Text("Hoy") }
        }
        IconButton(onClick = { mostrarCalendario = true }) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Elegir fecha en el calendario")
        }
    }

    // ( Hoy )  ( Mar. 24 )  ( Mié. 25 ) ... una semana
    val dias = calendario.diasCarrusel
    val listaDias = rememberLazyListState()
    LaunchedEffect(dias.first()) { listaDias.scrollToItem(0) }
    LazyRow(
        state = listaDias,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        items(dias, key = { it.toEpochDay() }) { dia ->
            ChipDia(
                dia = dia,
                esHoy = dia == calendario.hoy,
                seleccionado = dia == calendario.fecha,
                onClick = { onFechaChange(dia) }
            )
        }
    }

    if (mostrarCalendario) {
        CalendarioDialog(
            calendario = calendario,
            onAceptar = {
                onFechaChange(it)
                mostrarCalendario = false
            },
            onCancelar = { mostrarCalendario = false }
        )
    }
}

@Composable
private fun ChipDia(dia: LocalDate, esHoy: Boolean, seleccionado: Boolean, onClick: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    val descripcion = (if (esHoy) "Hoy, " else "") + "${dia.diaCorto} de ${dia.nombreMes}"

    Surface(
        shape = CircleShape,
        color = if (seleccionado) colores.primaryContainer else colores.surfaceContainerHigh,
        contentColor = if (seleccionado) colores.onPrimaryContainer else colores.onSurface,
        modifier = Modifier
            .height(40.dp)
            .selectable(selected = seleccionado, onClick = onClick, role = Role.Tab)
            .semantics { contentDescription = descripcion }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 18.dp)) {
            Text(
                if (esHoy) "Hoy" else dia.diaCorto,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

/**
 * Selector de fechas de Material 3 (DatePickerDialog).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarioDialog(
    calendario: Calendario,
    onAceptar: (LocalDate) -> Unit,
    onCancelar: () -> Unit
) {
    val anios = calendario.hoy.year..calendario.fechaMaxima.year
    val estadoCalendario = rememberDatePickerState(
        initialSelectedDateMillis = calendario.fecha.aMillisUtc(),
        yearRange = anios,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = calendario.permite(utcTimeMillis.aFechaUtc())

            override fun isSelectableYear(year: Int) = year in anios
        }
    )
    val elegida = estadoCalendario.selectedDateMillis
    DatePickerDialog(
        onDismissRequest = onCancelar,
        confirmButton = {
            TextButton(onClick = { elegida?.let { onAceptar(it.aFechaUtc()) } }, enabled = elegida != null) {
                Text("Aceptar")
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    ) {
        DatePicker(state = estadoCalendario)
    }
}

private fun LocalDate.aMillisUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.aFechaUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
