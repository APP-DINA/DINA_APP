package cl.app.dina.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import cl.app.dina.data.model.Feriado
import cl.app.dina.data.model.ModoTema
import cl.app.dina.data.remote.FeriadosApi
import cl.app.dina.data.remote.dto.FeriadoDto
import cl.app.dina.data.repository.PreferenciasRepository
import cl.app.dina.data.repository.RemoteFeriadoRepository
import cl.app.dina.data.repository.SesionRepository
import cl.app.dina.data.repository.aModelo
import cl.app.dina.soporte.ADMIN
import cl.app.dina.soporte.ALUMNO
import cl.app.dina.soporte.DataStoreEnMemoria
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Sesión y preferencias (DataStore en memoria) y la API externa de feriados. */
class RepositoriosLocalesTest {

    /** Un DataStore nuevo por prueba ([nombre] solo describe para qué es). */
    @Suppress("UNUSED_PARAMETER")
    private fun dataStore(nombre: String): DataStore<Preferences> = DataStoreEnMemoria()

    // Sesión

    @Test
    fun sinSesionGuardadaElUsuarioEsNull() = runTest {
        val repo = SesionRepository(dataStore("vacia"))
        assertNull(repo.usuario.first())
        assertNull(repo.token.first())
    }

    @Test
    fun iniciarGuardaUsuarioYTokenYCerrarLosBorra() = runTest {
        val repo = SesionRepository(dataStore("sesion"))
        repo.iniciar(ALUMNO.copy(fotoUrl = "http://x/foto"), "tok")

        assertEquals(ALUMNO.copy(fotoUrl = "http://x/foto"), repo.usuario.first())
        assertEquals("tok", repo.token.first())

        repo.actualizarUsuario(ALUMNO.copy(nombre = "Nuevo Nombre", esAdmin = true)) // sin foto: se borra
        val actualizado = repo.usuario.first()!!
        assertEquals("Nuevo Nombre", actualizado.nombre)
        assertEquals(true, actualizado.esAdmin)
        assertNull(actualizado.fotoUrl)
        assertEquals("tok", repo.token.first()) // el token se mantiene

        repo.cerrar()
        assertNull(repo.usuario.first())
    }

    @Test
    fun actualizarSinSesionNoCreaUnaSesion() = runTest {
        val repo = SesionRepository(dataStore("sin-token"))
        repo.actualizarUsuario(ADMIN)
        assertNull(repo.usuario.first())
    }

    @Test
    fun unRolGuardadoQueYaNoExisteSeTrataComoSinSesion() = runTest {
        val ds = dataStore("rol-viejo")
        val repo = SesionRepository(ds)
        repo.iniciar(ALUMNO, "tok")
        ds.edit { it[stringPreferencesKey("rol")] = "ROL_ANTIGUO" }
        assertNull(repo.usuario.first())
    }

    // Preferencias
    @Test
    fun elTemaPorDefectoEsSistemaYSeGuardaElElegido() = runTest {
        val repo = PreferenciasRepository(dataStore("prefs"))
        assertEquals(ModoTema.SISTEMA, repo.modoTema.first())
        repo.guardarModoTema(ModoTema.OSCURO)
        assertEquals(ModoTema.OSCURO, repo.modoTema.first())
    }

    // API externa: feriados (Nager.Date)
    private val api = mockk<FeriadosApi>()

    @Test
    fun feriadosSePidenUnaVezPorAnio() = runTest {
        coEvery { api.feriados(2026) } returns listOf(
            FeriadoDto("2026-12-25", "Navidad", "Christmas Day"),
            FeriadoDto("2026-12-08", "Inmaculada Concepción", "Immaculate Conception")
        )
        val repo = RemoteFeriadoRepository(api)

        assertEquals(listOf("Inmaculada Concepción", "Navidad"), repo.feriadosDe(2026).map { it.nombre }) // ordenados
        assertEquals("Navidad", repo.feriadoEn(LocalDate.of(2026, 12, 25))?.nombre)
        assertNull(repo.feriadoEn(LocalDate.of(2026, 12, 24)))
        coVerify(exactly = 1) { api.feriados(2026) } // las otras consultas usan lo guardado
    }

    @Test
    fun unFeriadoMalFormadoSeIgnoraYFaltaNombreLocalUsaElIngles() {
        assertNull(FeriadoDto("no-es-fecha", "X", "X").aModelo())
        assertEquals(Feriado(LocalDate.of(2026, 1, 1), "New Year's Day"), FeriadoDto("2026-01-01", " ", "New Year's Day").aModelo())
        assertEquals("Feriado", FeriadoDto("2026-01-01", null, null).aModelo()?.nombre)
    }
}
