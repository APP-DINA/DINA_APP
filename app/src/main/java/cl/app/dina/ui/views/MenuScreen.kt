package cl.app.dina.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.app.dina.data.model.Rol
import cl.app.dina.data.model.Usuario
import cl.app.dina.ui.theme.DINATheme
import cl.app.dina.ui.viewmodel.AppViewModelProvider
import cl.app.dina.ui.viewmodel.MenuViewModel

@Composable
fun MenuScreen(
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val usuario by viewModel.usuario.collectAsStateWithLifecycle()
    val confirmandoCierre by viewModel.confirmandoCierre.collectAsStateWithLifecycle()

    MenuContenido(
        usuario = usuario,
        onCerrarSesionClick = viewModel::pedirCerrarSesion,
        modifier = modifier
    )

    if (confirmandoCierre) {
        AlertDialog(
            onDismissRequest = viewModel::cancelarCerrarSesion,
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tendrás que ingresar de nuevo con tu cuenta institucional.") },
            // Acción destructiva en rojo; "Cancelar" usa el color del tema
            confirmButton = {
                TextButton(
                    onClick = viewModel::cerrarSesion,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarCerrarSesion) { Text("Cancelar") }
            }
        )
    }
}

/** Solo dibuja: recibe el usuario y avisa cuando se toca "Cerrar sesión". */
@Composable
private fun MenuContenido(
    usuario: Usuario?,
    onCerrarSesionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Menú",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        // ── Perfil ─────────────────────────────────────────────────
        if (usuario != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(usuario.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(
                        usuario.correo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        usuario.descripcionRol,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // ── Cerrar sesión ──────────────────────────────────────────
        OutlinedButton(
            onClick = onCerrarSesionClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text("Cerrar sesión")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MenuScreenPreview() {
    DINATheme {
        MenuContenido(
            usuario = Usuario("profesor.prueba@profesor.duoc.cl", "Profesor de Prueba", Rol.PROFESOR),
            onCerrarSesionClick = {}
        )
    }
}
