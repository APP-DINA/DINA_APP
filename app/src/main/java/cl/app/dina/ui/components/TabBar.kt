package cl.app.dina.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import cl.app.dina.navigation.Pestania
import cl.app.dina.ui.theme.DINATheme

@Composable
fun TabBar(
    pestaniaActual: Pestania?,
    onPestaniaClick: (Pestania) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        Pestania.entries.forEach { pestania ->
            val seleccionada = pestania == pestaniaActual
            NavigationBarItem(
                selected = seleccionada,
                onClick = { onPestaniaClick(pestania) },
                icon = {
                    Icon(
                        imageVector = if (seleccionada) {
                            pestania.iconoSeleccionado
                        } else {
                            pestania.iconoNoSeleccionado
                        },
                        contentDescription = pestania.etiqueta
                    )
                },
                label = {
                    Text(pestania.etiqueta, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                colors = NavigationBarItemDefaults.colors(
                    // Ícono negro sobre la "píldora" amarilla Duoc
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TabBarPreview() {
    DINATheme {
        TabBar(pestaniaActual = Pestania.INICIO, onPestaniaClick = {})
    }
}
