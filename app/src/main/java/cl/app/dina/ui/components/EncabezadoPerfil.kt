package cl.app.dina.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.app.dina.data.model.Rol
import cl.app.dina.data.model.Usuario
import cl.app.dina.ui.theme.DINATheme

/**
 * Encabezado del Inicio (igual para todos los roles): foto de perfil a la izquierda,
 * saludo, nombre y rol, sobre un fondo amarillo Duoc con círculos decorativos.
 * Solo muestra la foto: se cambia desde el Menú.
 */
@Composable
fun EncabezadoPerfil(usuario: Usuario, modifier: Modifier = Modifier) {
    val tinta = MaterialTheme.colorScheme.onPrimaryContainer

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .drawBehind { circulosDecorativos(tinta.copy(alpha = 0.07f)) }
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        FotoPerfil(
            nombre = usuario.nombre,
            fotoUrl = usuario.fotoUrl,
            tamano = 76.dp,
            modifier = Modifier.border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
        )

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Hola,", style = MaterialTheme.typography.titleSmall, color = tinta.copy(alpha = 0.75f))
            Text(
                usuario.nombre,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = tinta,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                shape = CircleShape,
                color = tinta.copy(alpha = 0.1f),
                contentColor = tinta,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    usuario.descripcionRol,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/** Dos círculos grandes y suaves que asoman por la derecha: el "fondo" del encabezado. */
private fun DrawScope.circulosDecorativos(color: Color) {
    drawCircle(color, radius = size.height * 0.9f, center = Offset(size.width * 0.95f, size.height * 0.05f))
    drawCircle(color, radius = size.height * 0.55f, center = Offset(size.width * 0.72f, size.height * 1.05f))
}

@Preview(showBackground = true)
@Composable
private fun EncabezadoPerfilPreview() {
    DINATheme {
        EncabezadoPerfil(
            usuario = Usuario("estudiante.prueba@duocuc.cl", "Estudiante de Prueba", Rol.ESTUDIANTE, esAdmin = true)
        )
    }
}
