package cl.app.dina.data.model

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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

/** Una solicitud: tarjetas del Inicio y detalle en SolicitudScreen. */
data class SolicitudResumen(
    val id: Long,
    val espacio: Espacio,
    val fecha: LocalDate,
    /** "10:00" */
    val inicio: String,
    val fin: String,
    val motivo: String,
    val asistentes: Int,
    val solicitante: Solicitante,
    val estado: EstadoSolicitud = EstadoSolicitud.PENDIENTE,
    /** Lo que escribió el administrador al aprobar o rechazar. */
    val comentarioAdmin: String? = null,
    /** Cuándo se pidió (ordena quién pidió primero en una doble reserva). */
    val creadaEn: LocalDateTime? = null
) {
    /** Duración en horas ("10:00" a "12:00" → 2): cada bloque dura 1 hora. */
    val horas: Long get() = Duration.between(LocalTime.parse(inicio), LocalTime.parse(fin)).toHours()
}

/** Quien pidió el espacio. [rol] = Estudiante o Docente según su correo (null si el dominio no se reconoce). */
data class Solicitante(
    val id: Long,
    val nombre: String,
    val rol: Rol?,
    val fotoUrl: String?,
    val correo: String = ""
)

enum class EstadoSolicitud(val etiqueta: String) {
    PENDIENTE("En revisión"),
    APROBADA("Aprobada"),
    RECHAZADA("Rechazada"),
    CANCELADA("Cancelada");

    companion object {
        fun desde(valor: String): EstadoSolicitud = entries.firstOrNull { it.name == valor } ?: PENDIENTE
    }
}
