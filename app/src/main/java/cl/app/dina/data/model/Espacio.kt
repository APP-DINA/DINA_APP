package cl.app.dina.data.model

/** Espacio que se puede reservar por bloques (sala, laboratorio, estudio...). */
data class Espacio(
    val id: Long,
    /** Código visible en la sede: "S11", "LC3", "SET-TV"... */
    val codigo: String,
    /** Nombre para mostrar: "Sala S11", "Set de TV"... */
    val nombre: String,
    /** Código del edificio (NORTE, CENTRAL, SUR, CTI): sirve para agrupar y ordenar. */
    val edificio: String,
    val edificioNombre: String,
    val piso: Int,
    val tipo: TipoEspacio,
    val capacidad: Int,
    /**
     * Escuela de uso principal (nombre del enum Escuela de ui/theme: "INFORMATICA"...),
     * o null si es de uso general. Se guarda como texto para que el modelo no dependa de la UI.
     */
    val escuela: String?,
    val proyector: Boolean,
    val computadores: Boolean,
    val climatizacion: Boolean
)

/** Edificios de la sede con espacios reservables (los mismos códigos que el backend). */
enum class Edificio(val nombre: String) {
    NORTE("Edificio Norte"),
    CENTRAL("Edificio Central"),
    SUR("Edificio Sur"),
    CTI("Edificio Tecnológico CTI")
}

/** Lo que el administrador envía para agregar un espacio. [escuela] null = uso general. */
data class NuevoEspacio(
    val codigo: String,
    val nombre: String,
    val edificio: Edificio,
    val piso: Int,
    val tipo: TipoEspacio,
    val capacidad: Int,
    val escuela: String?,
    val proyector: Boolean,
    val computadores: Boolean,
    val climatizacion: Boolean
)

enum class TipoEspacio(val etiqueta: String) {
    SALA("Sala"),
    LABORATORIO("Laboratorio"),
    ESTUDIO("Estudio"),
    AUDITORIO("Auditorio"),
    DEPORTIVO("Deportivo"),
    MULTIPROPOSITO("Multipropósito"),
    /** Un tipo nuevo del backend que la app todavía no conoce. */
    OTRO("Otro");

    companion object {
        fun desde(valor: String): TipoEspacio = entries.firstOrNull { it.name == valor } ?: OTRO
    }
}
