package cl.app.dina.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

// cada espacio tiene un uso principal.
enum class Escuela(val nombre: String, val principal: Color, val secundario: Color) {
    ADMINISTRACION_Y_FINANZAS("Administracion y Finanzas", AdministracionPrincipal, AdministracionSecundario),
    COMUNICACION("Comunicaciones", ComunicacionPrincipal, ComunicacionSecundario),
    CONSTRUCCION("Construccion", ConstruccionPrincipal, ConstruccionSecundario),
    DISENO("Diseño", DisenioPrincipal, DisenioSecundario),
    GASTRONOMIA("Gastronomia", GastronomiaPrincipal, GastronomiaSecundario),
    INFORMATICA("Informática", InformaticaPrincipal, InformaticaSecundario),
    INGENIERIA_RN_MEDIO_AMBIENTE("Ingeniería RN Medio Ambiente", MedioAmbientePrincipal, MedioAmbienteSecundario),
    SALUD("Salud", SaludPrincipal, SaludSecundario),
    TURISMO("Turismo", TurismoPrincipal, TurismoSecundario)
}

/**
 * Colores listos para dibujar la banda de una escuela.
 * banda - color sólido de la banda.
 * onBanda - texto/ícono legible sobre la banda (negro o blanco según el contraste).
 * contenedor - fondo suave para la tarjeta.
 */
@Immutable
data class BandaEscuela(
    val banda: Color,
    val onBanda: Color,
    val contenedor: Color
)

// Conjunto de bandas para un tema (claro u oscuro).
@Immutable
class EscuelaColors internal constructor(private val bandas: Map<Escuela, BandaEscuela>) {
    operator fun get(escuela: Escuela): BandaEscuela = bandas.getValue(escuela)
}

// Colores claros (amarillo, lima, celeste) llevan texto negro; los demás, blanco.
private fun textoSobre(fondo: Color): Color =
    if (fondo.luminance() > 0.4f) DuocNegro else DuocBlanco

// Tema claro: contenedor = color secundario muy suave sobre blanco.
internal val LightEscuelaColors = EscuelaColors(
    Escuela.entries.associateWith { e ->
        BandaEscuela(
            banda = e.principal,
            onBanda = textoSobre(e.principal),
            contenedor = e.secundario.copy(alpha = 0.20f).compositeOver(DuocBlanco)
        )
    }
)

// Tema oscuro: la banda se mantiene; el contenedor es el principal apagado sobre negro.
internal val DarkEscuelaColors = EscuelaColors(
    Escuela.entries.associateWith { e ->
        BandaEscuela(
            banda = e.principal,
            onBanda = textoSobre(e.principal),
            contenedor = e.principal.copy(alpha = 0.22f).compositeOver(DuocNegro)
        )
    }
)

// Se entrega desde DINATheme; las pantallas lo leen con DINATheme.escuelas.
internal val LocalEscuelaColors = staticCompositionLocalOf { LightEscuelaColors }
