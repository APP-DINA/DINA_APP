package cl.app.dina.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import cl.app.dina.data.local.CacheLocal
import cl.app.dina.data.remote.DinaApi
import cl.app.dina.data.remote.dto.ChatDetalleDto
import cl.app.dina.data.remote.dto.ChatDto
import cl.app.dina.data.remote.dto.MensajeDto
import cl.app.dina.data.repository.RemoteChatRepository
import cl.app.dina.data.repository.RemoteSolicitudRepository
import cl.app.dina.data.repository.SesionRepository
import cl.app.dina.soporte.ALUMNO
import cl.app.dina.soporte.DataStoreEnMemoria
import cl.app.dina.soporte.solicitudDto
import cl.app.dina.soporte.usuarioDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Copia local para ver datos sin conexión: un mismo DataStore para la sesión y la copia (como
 * en la app, que usa el archivo "sesion") y la API de dina_backend simulada con MockK.
 */
class CacheLocalTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var sesion: SesionRepository
    private lateinit var cache: CacheLocal
    private val api = mockk<DinaApi>(relaxed = true)

    @Before
    fun preparar() {
        dataStore = DataStoreEnMemoria()
        sesion = SesionRepository(dataStore)
        cache = CacheLocal(dataStore)
    }

    private fun mensajeDto(id: Long) = MensajeDto(id, "Hola $id", usuarioDto(), "2026-11-23T10:00:00")

    private fun chatDto(estado: String = "ABIERTO") =
        ChatDto(solicitudDto(id = 100, estado = "APROBADA"), estado, null, conversacionIniciada = true)

    @Test
    fun sinSesionNoSeGuardaNada() = runTest {
        cache.guardar("x", listOf(1, 2))
        assertNull(cache.leerJson("x"))
    }

    @Test
    fun seGuardaConSesionYSeBorraAlCerrarla() = runTest {
        sesion.iniciar(ALUMNO, "tok")
        cache.guardar("numeros", listOf(1, 2, 3))
        assertEquals(listOf(1, 2, 3), cache.leer<List<Int>>("numeros"))
        assertEquals(ALUMNO, sesionUsuario()) // compartir el archivo no altera la sesión

        sesion.cerrar()
        assertNull(cache.leer<List<Int>>("numeros"))
    }

    @Test
    fun unJsonQueNoCorrespondeSeIgnora() = runTest {
        sesion.iniciar(ALUMNO, "tok")
        cache.guardar("texto", "no es un chat")
        assertNull(cache.leer<ChatDetalleDto>("texto"))
    }

    @Test
    fun lasPendientesGuardadasSeRecuperanOrdenadas() = runTest {
        sesion.iniciar(ALUMNO, "tok")
        coEvery { api.misSolicitudes() } returns listOf(
            solicitudDto(id = 2, fecha = "2026-12-02"),
            solicitudDto(id = 1, fecha = "2026-12-01"),
            solicitudDto(id = 3, estado = "APROBADA") // no es pendiente: no se guarda
        )
        coEvery { api.solicitudesAdmin("PENDIENTE") } returns listOf(solicitudDto(id = 9))
        val repo = RemoteSolicitudRepository(api, cache)
        assertNull(repo.pendientesGuardadas(admin = false)) // todavía no se ha pedido nada

        repo.misPendientes()
        repo.porRevisar()
        assertEquals(listOf(1L, 2L), repo.pendientesGuardadas(admin = false)!!.map { it.id })
        assertEquals(listOf(9L), repo.pendientesGuardadas(admin = true)!!.map { it.id })

        assertNull(RemoteSolicitudRepository(api).pendientesGuardadas(admin = false)) // sin copia local
    }

    @Test
    fun losChatsYLaConversacionSeGuardanYSeCompletan() = runTest {
        sesion.iniciar(ALUMNO, "tok")
        val repo = RemoteChatRepository(api, cache)
        coEvery { api.chats() } returns listOf(chatDto())
        coEvery { api.chat(100, 0) } returns ChatDetalleDto(chatDto(), listOf(mensajeDto(1), mensajeDto(2)))
        coEvery { api.chat(100, 2) } returns ChatDetalleDto(chatDto(), listOf(mensajeDto(2), mensajeDto(3)))
        coEvery { api.enviarMensaje(100, any()) } returns mensajeDto(4)
        coEvery { api.cerrarChat(100) } returns chatDto(estado = "CERRADO")

        repo.chats()
        assertEquals(listOf(100L), repo.chatsGuardados()!!.map { it.solicitud.id })

        repo.conversacion(100)                 // completa: 1 y 2
        repo.conversacion(100, despuesDe = 2)  // revisión: llega otra vez el 2 y el nuevo 3
        repo.enviar(100, "Hola")               // el propio: 4
        assertEquals(listOf(1L, 2L, 3L, 4L), repo.conversacionGuardada(100)!!.mensajes.map { it.id })

        repo.cerrar(100)
        assertFalse(repo.conversacionGuardada(100)!!.chat.abierto)
        assertEquals(4, repo.conversacionGuardada(100)!!.mensajes.size)
    }

    @Test
    fun sinLaConversacionCompletaNoSeGuardaUnPedazo() = runTest {
        sesion.iniciar(ALUMNO, "tok")
        val repo = RemoteChatRepository(api, cache)
        coEvery { api.chat(100, 5) } returns ChatDetalleDto(chatDto(), listOf(mensajeDto(6)))
        coEvery { api.enviarMensaje(100, any()) } returns mensajeDto(7)

        repo.conversacion(100, despuesDe = 5)
        repo.enviar(100, "Hola")
        assertNull(repo.conversacionGuardada(100))
        assertNull(RemoteChatRepository(api).chatsGuardados()) // sin copia local
    }

    private suspend fun sesionUsuario() = sesion.usuario.first()
}
