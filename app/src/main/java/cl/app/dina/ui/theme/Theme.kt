package cl.app.dina.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Las 3 opciones que tendrá la sección "Tema" de la app.
 * SISTEMA sigue la configuración del teléfono; CLARO y OSCURO la ignoran.
 */
enum class ModoTema { SISTEMA, CLARO, OSCURO }

private val DarkColorScheme = darkColorScheme(
    primary = DuocAmarillo,
    onPrimary = DuocNegro,
    primaryContainer = AmarilloOscuro,
    onPrimaryContainer = AmarilloClaro,
    inversePrimary = AmarilloOscuro,

    secondary = GrisClaro,
    onSecondary = DuocNegro,
    secondaryContainer = GrisOscuro,
    onSecondaryContainer = DuocTerciario,

    tertiary = DuocTerciario,
    onTertiary = DuocNegro,
    tertiaryContainer = NegroElevado3,
    onTertiaryContainer = DuocTerciario,

    background = DuocNegro,
    onBackground = DuocTerciario,
    surface = DuocNegro,
    onSurface = DuocTerciario,
    surfaceVariant = NegroElevado3,
    onSurfaceVariant = GrisClaro,
    surfaceTint = DuocAmarillo,
    inverseSurface = DuocTerciario,
    inverseOnSurface = DuocNegro,

    surfaceBright = GrisOscuro,
    surfaceDim = DuocNegro,
    surfaceContainerLowest = DuocNegroPuro,
    surfaceContainerLow = NegroElevado1,
    surfaceContainer = NegroElevado2,
    surfaceContainerHigh = NegroElevado3,
    surfaceContainerHighest = GrisOscuro,

    outline = GrisMedio,
    outlineVariant = GrisOscuro,
    scrim = DuocNegroPuro
)

private val LightColorScheme = lightColorScheme(
    primary = DuocAmarillo,
    onPrimary = DuocNegro,
    primaryContainer = AmarilloClaro,
    onPrimaryContainer = DuocNegro,
    inversePrimary = DuocAmarillo,

    secondary = DuocGris,
    onSecondary = DuocBlanco,
    secondaryContainer = DuocTerciario,
    onSecondaryContainer = DuocNegro,

    tertiary = DuocNegro,
    onTertiary = DuocBlanco,
    tertiaryContainer = GrisSuave,
    onTertiaryContainer = DuocNegro,

    background = DuocBlanco,
    onBackground = DuocNegro,
    surface = DuocBlanco,
    onSurface = DuocNegro,
    surfaceVariant = DuocTerciario,
    onSurfaceVariant = DuocGris,
    surfaceTint = DuocAmarillo,
    inverseSurface = DuocNegro,
    inverseOnSurface = DuocBlanco,

    surfaceBright = DuocBlanco,
    surfaceDim = GrisSuave,
    surfaceContainerLowest = DuocBlanco,
    surfaceContainerLow = BlancoHumo,
    surfaceContainer = BlancoHumo2,
    surfaceContainerHigh = DuocTerciario,
    surfaceContainerHighest = GrisSuave,

    outline = GrisMedio,
    outlineVariant = GrisClaro,
    scrim = DuocNegroPuro
)

@Composable
fun DINATheme(
    modoTema: ModoTema = ModoTema.SISTEMA,
    content: @Composable () -> Unit
) {
    // 1. Opcion del usuario
    val oscuro = when (modoTema) {
        ModoTema.SISTEMA -> isSystemInDarkTheme()
        ModoTema.CLARO -> false
        ModoTema.OSCURO -> true
    }

    // 2. Elegir los colores de Material 3 y los de las escuelas para ese modo
    val colorScheme = if (oscuro) DarkColorScheme else LightColorScheme
    val escuelaColors = if (oscuro) DarkEscuelaColors else LightEscuelaColors

    // 3. Iconos de la barra de estado y de navegación: oscuros sobre fondo claro y al reves.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !oscuro
                isAppearanceLightNavigationBars = !oscuro
            }
        }
    }

    // 4. Entregar toda la pantalla
    CompositionLocalProvider(LocalEscuelaColors provides escuelaColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Uso forma de uso `DINATheme.escuelas[Escuela.INFORMATICA].banda`
object DINATheme {
    val escuelas: EscuelaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalEscuelaColors.current
}
