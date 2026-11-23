package cl.app.dina

import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.ui.viewmodel.AgregarEspacioUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class AgregarEspacioTest {

    @Test
    fun sinNombreSeUsaTipoYCodigo() {
        assertEquals("Sala S99", AgregarEspacioUiState(codigo = " s99 ").nombreFinal)
        assertEquals("Laboratorio LC9", AgregarEspacioUiState(codigo = "LC9", tipo = TipoEspacio.LABORATORIO).nombreFinal)
        assertEquals("", AgregarEspacioUiState().nombreFinal)
    }

    @Test
    fun nombreEscritoTienePrioridad() {
        assertEquals("Set de TV", AgregarEspacioUiState(codigo = "SET-TV", nombre = "  Set de TV ").nombreFinal)
    }
}
