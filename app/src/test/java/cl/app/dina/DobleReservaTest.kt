package cl.app.dina

import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.Solicitante
import cl.app.dina.data.model.SolicitudResumen
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.ui.viewmodel.DobleReserva
import cl.app.dina.ui.viewmodel.EntradaRevision
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DobleReservaTest {

    private val s11 = espacio(1, "S11")
    private val s21 = espacio(2, "S21")
    private val martes = LocalDate.of(2026, 11, 24)
    private val pidio = LocalDateTime.of(2026, 11, 23, 9, 0)

    private fun espacio(id: Long, codigo: String) =
        Espacio(id, codigo, "Sala $codigo", "NORTE", "Edificio Norte", 1, TipoEspacio.SALA, 35, null, true, false, false)

    /** [minutos]: cuántos minutos después de [pidio] se hizo la solicitud. */
    private fun sol(id: Long, espacio: Espacio, inicio: String, fin: String, minutos: Long, fecha: LocalDate = martes) =
        SolicitudResumen(
            id, espacio, fecha, inicio, fin, "Motivo $id", 10,
            Solicitante(id, "Persona $id", null, null), creadaEn = pidio.plusMinutes(minutos)
        )

    /** "R1" = Reserva con la solicitud 1; "D[3, 2]" = Doble reserva con 3 y 2, en ese orden. */
    private fun resumen(entradas: List<EntradaRevision>) = entradas.map {
        when (it) {
            is EntradaRevision.Reserva -> "R${it.solicitud.id}"
            is EntradaRevision.Doble -> "D${it.masRecientePrimero.map { s -> s.id }}"
        }
    }

    @Test
    fun dobleReservaVaDeLaMasRecienteALaMasAntigua() {
        val a = sol(1, s11, "10:00", "12:00", minutos = 5)
        val b = sol(2, s11, "11:00", "12:00", minutos = 30)
        val r = DobleReserva.agrupar(listOf(a, b))
        assertEquals(listOf("D[2, 1]"), resumen(r))
        // El número de llegada no depende del orden en que se muestran
        val doble = r.single() as EntradaRevision.Doble
        assertEquals(1, doble.ordenDeLlegada(a))
        assertEquals(2, doble.ordenDeLlegada(b))
    }

    @Test
    fun mismoDiaYHoraEnDistintoEspacioSeOrdenaPorCodigo() {
        // Llegan con S21 primero, pero se muestran por código: S11 y luego S21
        val r = DobleReserva.agrupar(listOf(sol(1, s21, "10:00", "11:00", 0), sol(2, s11, "10:00", "11:00", 1)))
        assertEquals(listOf("R2", "R1"), resumen(r))
    }

    @Test
    fun todoSeOrdenaPorElDiaYHoraMasCercanos() {
        val manana = martes.plusDays(1)
        val r = DobleReserva.agrupar(
            listOf(
                sol(1, s21, "09:00", "10:00", 0, fecha = manana),
                sol(2, s11, "10:00", "12:00", 1),
                sol(3, s11, "11:00", "12:00", 2),
                sol(4, s21, "08:00", "09:00", 3)
            )
        )
        assertEquals(listOf("R4", "D[3, 2]", "R1"), resumen(r))
    }

    @Test
    fun horariosSeguidosOtroEspacioUOtroDiaNoChocan() {
        val r = DobleReserva.agrupar(
            listOf(
                sol(1, s11, "10:00", "12:00", 0),
                sol(2, s11, "12:00", "13:00", 1),
                sol(3, s21, "10:00", "12:00", 2),
                sol(4, s11, "10:00", "12:00", 3, fecha = martes.plusDays(1))
            )
        )
        assertEquals(listOf("R1", "R3", "R2", "R4"), resumen(r))
    }

    @Test
    fun choqueEnCadenaQuedaEnUnaSolaDobleReserva() {
        // 10–12 choca con 11–13, y 11–13 con 12–14 (aunque 10–12 y 12–14 no se cruzan)
        val r = DobleReserva.agrupar(
            listOf(sol(1, s11, "10:00", "12:00", 2), sol(2, s11, "11:00", "13:00", 1), sol(3, s11, "12:00", "14:00", 0))
        )
        assertEquals(listOf("D[1, 2, 3]"), resumen(r))
    }
}
