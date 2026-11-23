package cl.app.dina

import cl.app.dina.data.model.BloqueDia
import cl.app.dina.data.model.EstadoBloque
import cl.app.dina.ui.viewmodel.SeleccionBloques
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeleccionBloquesTest {

    /** Bloques 1..14; el 6 está ocupado. */
    private val bloques = (1..14).map { n ->
        BloqueDia(n.toLong(), n, "", "", if (n == 6) EstadoBloque.OCUPADO else EstadoBloque.DISPONIBLE, 0, false)
    }

    private fun b(n: Int) = bloques[n - 1]
    private fun nums(sel: List<BloqueDia>?) = sel?.map { it.numero }

    @Test
    fun seAlargaHaciaAmbosLadosHastaCuatro() {
        var sel = SeleccionBloques.tocar(emptyList(), b(3))!!
        sel = SeleccionBloques.tocar(sel, b(4))!!
        sel = SeleccionBloques.tocar(sel, b(2))!!
        assertEquals(listOf(2, 3, 4), nums(sel))
        sel = SeleccionBloques.tocar(sel, b(5))!!
        assertEquals(listOf(2, 3, 4, 5), nums(sel))
        // El quinto supera el máximo
        assertNull(SeleccionBloques.tocar(sel, b(1)))
    }

    @Test
    fun tocarUnExtremoLoQuita() {
        val sel = listOf(b(3), b(4), b(5))
        assertEquals(listOf(4, 5), nums(SeleccionBloques.tocar(sel, b(3))))
        assertEquals(listOf(3, 4), nums(SeleccionBloques.tocar(sel, b(5))))
        assertEquals(emptyList<Int>(), nums(SeleccionBloques.tocar(listOf(b(3)), b(3))))
    }

    @Test
    fun bloqueSueltoOElDelMedioEmpiezanDeNuevo() {
        val sel = listOf(b(3), b(4), b(5))
        assertEquals(listOf(9), nums(SeleccionBloques.tocar(sel, b(9))))
        assertEquals(listOf(4), nums(SeleccionBloques.tocar(sel, b(4))))
    }

    @Test
    fun bloqueYaPedidoPorMiNoSePuedeElegir() {
        val mio = b(8).copy(mio = true)
        assertEquals(listOf(7), nums(SeleccionBloques.tocar(listOf(b(7)), mio)))
    }

    @Test
    fun bloqueOcupadoNoCambiaNada() {
        val sel = listOf(b(4), b(5))
        assertEquals(listOf(4, 5), nums(SeleccionBloques.tocar(sel, b(6))))
    }
}
