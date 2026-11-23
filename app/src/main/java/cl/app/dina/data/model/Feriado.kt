package cl.app.dina.data.model

import java.time.LocalDate

/** Feriado nacional de Chile (viene de la API externa Nager.Date). */
data class Feriado(val fecha: LocalDate, val nombre: String)
