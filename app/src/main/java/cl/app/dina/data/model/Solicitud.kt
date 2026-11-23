package cl.app.dina.data.model

import java.time.LocalDate

/** Un bloque de 1 hora de un espacio en un día concreto. */
data class BloqueDia(
    val id: Long,
    /** Orden en el día: dos bloques son seguidos si sus números son consecutivos. */
    val numero: Int,
    /** "10:00" */
    val inicio: String,
    val fin: String,
    val estado: EstadoBloque,
    /** Cuántas solicitudes pendientes compiten por este bloque. */
    val pendientes: Int,
    /** El usuario ya pidió este bloque (pendiente o aprobado). */
    val mio: Boolean
) {
    /** Se puede elegir: está disponible y el usuario no lo pidió ya (el backend lo rechazaría). */
    val seleccionable: Boolean get() = estado == EstadoBloque.DISPONIBLE && !mio
}

enum class EstadoBloque {
    /** Se puede solicitar. */
    DISPONIBLE,
    /** Ya tiene una solicitud aprobada. */
    OCUPADO,
    /** Ya pasó, o la sede no atiende (domingo, sábado en la tarde). */
    NO_DISPONIBLE;

    companion object {
        fun desde(valor: String): EstadoBloque = entries.firstOrNull { it.name == valor } ?: NO_DISPONIBLE
    }
}

/** Lo que el formulario envía al backend. */
data class NuevaSolicitud(
    val espacioId: Long,
    val fecha: LocalDate,
    val bloqueIds: List<Long>,
    val motivo: String,
    val asistentes: Int
)

/** Solicitud ya creada (queda PENDIENTE hasta que el administrador la revise). */
data class SolicitudCreada(
    val id: Long,
    val fecha: LocalDate,
    val inicio: String,
    val fin: String,
    val estado: String
)
