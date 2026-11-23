package cl.app.dina.ui.viewmodel

import java.time.LocalDate

/**
 * Día elegido para la reserva y sus reglas: desde [hoy] hasta [fechaMaxima].
 * Todas las reglas de fechas de la pantalla viven aquí, en un solo lugar.
 */
data class Calendario(val hoy: LocalDate, val fecha: LocalDate = hoy) {
    val fechaMaxima: LocalDate get() = hoy.plusDays(DIAS_ANTICIPACION)

    /** El carrusel parte hoy; si se eligió un día fuera de esta semana, parte en ese día. */
    private val inicioCarrusel: LocalDate
        get() = if (fecha.isAfter(hoy.plusDays(DIAS_CARRUSEL - 1))) fecha else hoy

    val diasCarrusel: List<LocalDate> get() = (0 until DIAS_CARRUSEL).map { inicioCarrusel.plusDays(it) }

    /** true cuando el carrusel no muestra hoy: la pantalla ofrece el atajo "Hoy". */
    val lejosDeHoy: Boolean get() = inicioCarrusel != hoy

    /** ¿Se puede reservar ese día? (de hoy a la fecha máxima) */
    fun permite(dia: LocalDate): Boolean = dia in hoy..fechaMaxima

    /** Elige un día, siempre dentro del rango permitido. */
    fun elegir(dia: LocalDate): Calendario = copy(fecha = dia.coerceIn(hoy, fechaMaxima))

    /** Si la app quedó abierta de un día para otro, mueve "hoy" y no deja la fecha en el pasado. */
    fun alDia(ahora: LocalDate = LocalDate.now()): Calendario =
        if (ahora == hoy) this else Calendario(ahora).elegir(fecha)

    companion object {
        /** Días del carrusel (una semana). */
        const val DIAS_CARRUSEL = 7L

        /** Igual que la regla de dina_backend (dina.solicitudes.max-dias-anticipacion). */
        const val DIAS_ANTICIPACION = 30L
    }
}
