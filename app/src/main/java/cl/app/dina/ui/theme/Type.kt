package cl.app.dina.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cl.app.dina.R

// Fuente Lato Duoc UC
val Lato = FontFamily(
    Font(R.font.lato_light, FontWeight.Light),
    Font(R.font.lato_light_italic, FontWeight.Light, FontStyle.Italic),
    Font(R.font.lato_regular, FontWeight.Normal),
    Font(R.font.lato_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.lato_bold, FontWeight.Bold),
    Font(R.font.lato_bold_italic, FontWeight.Bold, FontStyle.Italic)
)

val Typography = Typography(
    // h1 - Título principal
    displaySmall = TextStyle(fontFamily = Lato, fontWeight = FontWeight.Bold, fontSize = 43.sp, lineHeight = 52.sp),
    // h2 - Subtítulos y textos grandes
    headlineLarge = TextStyle(fontFamily = Lato, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 42.sp),
    // h3 - Títulos párrafos interiores
    headlineMedium = TextStyle(fontFamily = Lato, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    // h4 - Texto bajada
    titleLarge = TextStyle(fontFamily = Lato, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 28.sp),
    // h5 - Título 5to nivel
    titleMedium = TextStyle(fontFamily = Lato, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp),
    // p - Body
    bodyLarge = TextStyle(fontFamily = Lato, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Lato, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    // small - Texto pequeño de apoyo
    bodySmall = TextStyle(fontFamily = Lato, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    // Botones y etiquetas
    labelLarge = TextStyle(fontFamily = Lato, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Lato, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp)
)
