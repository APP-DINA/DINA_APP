package cl.app.dina.data.model

/**
 * Opción "Modo oscuro" del Menú.
 * SISTEMA sigue la configuración del teléfono; CLARO y OSCURO la ignoran.
 */
enum class ModoTema(val etiqueta: String) {
    SISTEMA("Sistema"),
    CLARO("Claro"),
    OSCURO("Oscuro")
}
