package cl.app.dina.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/**
 * Foto de perfil redonda. Mientras carga, si falla o si el usuario no tiene foto,
 * se ven sus iniciales ("Victor Godoy" → "VG").
 */
@Composable
fun FotoPerfil(
    nombre: String,
    fotoUrl: String?,
    modifier: Modifier = Modifier,
    tamano: Dp = 40.dp
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Text(
            iniciales(nombre),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontSize = (tamano.value * 0.38f).sp,
            fontWeight = FontWeight.Bold
        )
        if (fotoUrl != null) {
            AsyncImage(
                model = fotoUrl,
                contentDescription = null, // la descripción la da quien la usa (nombre al lado)
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/** Primera letra del primer y del último nombre, en mayúscula. */
fun iniciales(nombre: String): String {
    val partes = nombre.split(' ').filter { it.isNotBlank() }
    return listOfNotNull(partes.firstOrNull(), partes.drop(1).lastOrNull())
        .joinToString("") { it.first().uppercase() }
}
