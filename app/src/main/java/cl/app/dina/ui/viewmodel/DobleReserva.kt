package cl.app.dina.ui.viewmodel

import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.SolicitudResumen
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Un elemento de la lista del admin: una solicitud sola ("Reserva") o varias que piden el mismo
 * espacio, el mismo día, en horarios que se cruzan ("Doble reserva": solo se puede aceptar una).
 */
sealed interface EntradaRevision {
    val espacio: Espacio
    val fecha: LocalDate
    val inicio: String

    data class Reserva(val solicitud: SolicitudResumen) : EntradaRevision {
        override val espacio get() = solicitud.espacio
        override val fecha get() = solicitud.fecha
        override val inicio get() = solicitud.inicio
    }

    /** [porLlegada]: de la primera a la última en pedirse. */
    data class Doble(val porLlegada: List<SolicitudResumen>) : EntradaRevision {
        override val espacio get() = porLlegada.first().espacio
        override val fecha get() = porLlegada.first().fecha
        override val inicio get() = porLlegada.minOf { it.inicio }

        /** Orden en que se muestran: de la pedida más recientemente a la más antigua. */
        val masRecientePrimero: List<SolicitudResumen> get() = porLlegada.asReversed()

        /** 1 = la primera en pedirse. */
        fun ordenDeLlegada(solicitud: SolicitudResumen): Int = porLlegada.indexOf(solicitud) + 1
    }
}

object DobleReserva {

    /**
     * Arma la lista del admin, del día y hora más cercanos a hoy al más lejano
     * (en empate, por código del espacio, para que el orden no dependa de cómo llegaron).
     * Dos solicitudes chocan si son del mismo espacio y día y sus horarios se cruzan
     * (10–12 y 11–13 sí; 10–12 y 12–13 no). El choque es en cadena: si A choca con B y B con C,
     * las tres quedan en la misma doble reserva.
     */
    fun agrupar(solicitudes: List<SolicitudResumen>): List<EntradaRevision> =
        solicitudes
            .groupBy { it.espacio.id to it.fecha }
            .values
            .flatMap(::gruposQueChocan)
            .map { grupo ->
                if (grupo.size == 1) EntradaRevision.Reserva(grupo.single())
                else EntradaRevision.Doble(grupo.sortedWith(POR_LLEGADA))
            }
            .sortedWith(compareBy({ it.fecha }, { it.inicio }, { it.espacio.codigo }))

    /** Solicitudes de un mismo espacio y día, separadas en grupos de horarios que se cruzan. */
    private fun gruposQueChocan(delMismoEspacioYDia: List<SolicitudResumen>): List<List<SolicitudResumen>> {
        val grupos = mutableListOf<MutableList<SolicitudResumen>>()
        var finDelGrupo = ""
        // "HH:mm" se ordena bien como texto
        for (s in delMismoEspacioYDia.sortedBy { it.inicio }) {
            if (grupos.isNotEmpty() && s.inicio < finDelGrupo) {
                grupos.last() += s
                finDelGrupo = maxOf(finDelGrupo, s.fin)
            } else {
                grupos += mutableListOf(s)
                finDelGrupo = s.fin
            }
        }
        return grupos
    }

    /** Sin fecha de creación se usa el id, que también crece con el orden de llegada. */
    private val POR_LLEGADA = compareBy<SolicitudResumen>({ it.creadaEn ?: LocalDateTime.MAX }, { it.id })
}
