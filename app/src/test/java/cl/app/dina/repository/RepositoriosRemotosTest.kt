package cl.app.dina.repository

import cl.app.dina.data.model.Edificio
import cl.app.dina.data.model.EstadoBloque
import cl.app.dina.data.model.EstadoSolicitud
import cl.app.dina.data.model.NuevaSolicitud
import cl.app.dina.data.model.NuevoEspacio
import cl.app.dina.data.model.Rol
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.data.remote.DinaApi
import cl.app.dina.data.remote.ErrorDelBackend
import cl.app.dina.data.remote.dto.BloqueDisponibilidadDto
import cl.app.dina.data.remote.dto.CancelarDto
import cl.app.dina.data.remote.dto.ChatDetalleDto
import cl.app.dina.data.remote.dto.ChatDto
import cl.app.dina.data.remote.dto.ConteoDto
import cl.app.dina.data.remote.dto.DisponibilidadDto
import cl.app.dina.data.remote.dto.EnviarMensajeDto
import cl.app.dina.data.remote.dto.LoginRequestDto
import cl.app.dina.data.remote.dto.LoginResponseDto
import cl.app.dina.data.remote.dto.MensajeDto
import cl.app.dina.data.remote.dto.NotificacionDto
import cl.app.dina.data.remote.dto.RechazarDto
import cl.app.dina.data.repository.FakeAuthRepository
import cl.app.dina.data.repository.RemoteAuthRepository
import cl.app.dina.data.repository.RemoteChatRepository
import cl.app.dina.data.repository.RemoteEspacioRepository
import cl.app.dina.data.repository.RemoteNotificacionRepository
import cl.app.dina.data.repository.RemoteSolicitudRepository
import cl.app.dina.data.repository.ResultadoLogin
import cl.app.dina.soporte.espacioDto
import cl.app.dina.soporte.errorHttp
import cl.app.dina.soporte.notificacion
import cl.app.dina.soporte.solicitudDto
import cl.app.dina.soporte.usuarioDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Repositorios que hablan con dina_backend. Se reemplaza DinaApi (Retrofit) por un mock de MockK:
 * se prueba la traducción DTO → modelo y las reglas propias del repositorio, sin red.
 */
class RepositoriosRemotosTest {

    private val api = mockk<DinaApi>(relaxed = true)

    // Auth 
    @Test
    fun loginExitosoNormalizaElCorreoYDevuelveUsuarioYToken() = runTest {
        coEvery { api.login(any()) } returns LoginResponseDto("tok-123", null, usuarioDto(rol = "ADMIN"))

        val r = RemoteAuthRepository(api).login("  Alumno.Prueba@DuocUC.cl ", "clave")

        coVerify { api.login(LoginRequestDto("alumno.prueba@duocuc.cl", "clave")) }
        r as ResultadoLogin.Exito
        assertEquals("tok-123", r.token)
        assertTrue(r.usuario.esAdmin)
        assertEquals(Rol.ESTUDIANTE, r.usuario.rol)
    }

    @Test
    fun loginConDominioNoInstitucionalEsError() = runTest {
        coEvery { api.login(any()) } returns LoginResponseDto("t", null, usuarioDto(correo = "x@gmail.com"))
        val r = RemoteAuthRepository(api).login("x@gmail.com", "clave")
        assertTrue(r is ResultadoLogin.Error)
    }

    @Test
    fun loginSinConexionDevuelveMensajeDeRed() = runTest {
        coEvery { api.login(any()) } throws IOException("sin red")
        val r = RemoteAuthRepository(api).login("a@duocuc.cl", "clave") as ResultadoLogin.Error
        assertEquals("No se pudo conectar con el servidor. Revisa tu conexión", r.mensaje)
    }

    @Test
    fun loginConCredencialesMalasMuestraElMensajeDelBackend() = runTest {
        coEvery { api.login(any()) } throws errorHttp(401, """{"estado":401,"mensaje":"Correo o contraseña incorrectos"}""")
        val r = RemoteAuthRepository(api).login("a@duocuc.cl", "mala") as ResultadoLogin.Error
        assertEquals("Correo o contraseña incorrectos", r.mensaje)
    }

    @Test
    fun logoutSinConexionNoLanzaError() = runTest {
        coEvery { api.logout() } throws IOException()
        RemoteAuthRepository(api).logout() // no debe lanzar
        coVerify { api.logout() }
    }

    @Test
    fun usuarioActualDevuelveNullSiFalla() = runTest {
        coEvery { api.me() } returns usuarioDto(foto = "api/usuarios/10/foto?v=1")
        val usuario = RemoteAuthRepository(api).usuarioActual()!!
        assertTrue(usuario.fotoUrl!!.endsWith("api/usuarios/10/foto?v=1"))

        coEvery { api.me() } throws IOException()
        assertNull(RemoteAuthRepository(api).usuarioActual())
    }

    @Test
    fun fakeAuthAceptaSoloLasCuentasDeDemo() = runTest {
        val fake = FakeAuthRepository()
        assertTrue(fake.login("estudiante.prueba@duocuc.cl", "duoc1234") is ResultadoLogin.Exito)
        assertEquals(
            "Correo o contraseña incorrectos",
            (fake.login("estudiante.prueba@duocuc.cl", "otra") as ResultadoLogin.Error).mensaje
        )
        assertEquals("Dominio no autorizado", (fake.login("x@gmail.com", "duoc1234") as ResultadoLogin.Error).mensaje)
        fake.logout()
        assertNull(fake.usuarioActual())
    }

    // Espacios
    @Test
    fun obtenerEspacioUsaElCatalogoYaCargado() = runTest {
        coEvery { api.espacios() } returns listOf(espacioDto(1, "S11"), espacioDto(2, "S12"))
        val repo = RemoteEspacioRepository(api)

        assertEquals(2, repo.obtenerEspacios().size)
        assertEquals("S12", repo.obtenerEspacio(2).codigo)
        coVerify(exactly = 1) { api.espacios() } // el segundo no volvió a pedir el catálogo
    }

    @Test
    fun obtenerEspacioInexistenteLanzaNoSuchElement() = runTest {
        coEvery { api.espacios() } returns listOf(espacioDto(1))
        try {
            RemoteEspacioRepository(api).obtenerEspacio(99)
            fail("Debió lanzar")
        } catch (e: NoSuchElementException) {
            assertEquals("El espacio ya no está disponible", e.message)
        }
    }

    @Test
    fun crearEspacioLoSumaAlCatalogo() = runTest {
        coEvery { api.espacios() } returns listOf(espacioDto(1))
        coEvery { api.crearEspacio(any()) } returns espacioDto(5, "S99")
        val repo = RemoteEspacioRepository(api)
        repo.obtenerEspacios()

        val creado = repo.crear(
            NuevoEspacio("S99", "Sala S99", Edificio.NORTE, 2, TipoEspacio.SALA, 30, null, true, false, false)
        )

        assertEquals("S99", creado.codigo)
        assertEquals("S99", repo.obtenerEspacio(5).codigo) // ya está en el catálogo guardado
        coVerify { api.crearEspacio(match { it.edificio == "NORTE" && it.tipo == "SALA" && it.piso == 2 }) }
    }

    // Solicitudes
    @Test
    fun disponibilidadTraduceLosBloques() = runTest {
        coEvery { api.disponibilidad(1, "2026-12-01") } returns DisponibilidadDto(
            "2026-12-01",
            listOf(BloqueDisponibilidadDto(3, 3, "10:00:00", "11:00:00", "OCUPADO", 0, false))
        )
        val bloque = RemoteSolicitudRepository(api).disponibilidad(1, LocalDate.of(2026, 12, 1)).single()
        assertEquals("10:00", bloque.inicio)
        assertEquals("11:00", bloque.fin)
        assertEquals(EstadoBloque.OCUPADO, bloque.estado)
    }

    @Test
    fun solicitarEnviaElCuerpoEsperado() = runTest {
        coEvery { api.crearSolicitud(any()) } returns solicitudDto(id = 7)
        val creada = RemoteSolicitudRepository(api).solicitar(
            NuevaSolicitud(1, LocalDate.of(2026, 12, 1), listOf(3, 4), "Clase", 20, confirmarCruce = true)
        )
        assertEquals(7, creada.id)
        assertEquals("10:00", creada.inicio)
        coVerify {
            api.crearSolicitud(match { it.fecha == "2026-12-01" && it.actividad == "Clase" && it.confirmarCruce })
        }
    }

    @Test
    fun solicitarConCruceDevuelveErrorConCodigo() = runTest {
        coEvery { api.crearSolicitud(any()) } throws errorHttp(
            409, """{"estado":409,"mensaje":"Ya tienes una solicitud","codigo":"CRUCE_PROPIO"}"""
        )
        try {
            RemoteSolicitudRepository(api).solicitar(NuevaSolicitud(1, LocalDate.now(), listOf(1), "x", 1))
            fail("Debió lanzar")
        } catch (e: ErrorDelBackend) {
            assertEquals(ErrorDelBackend.CRUCE_PROPIO, e.codigo)
            assertEquals("Ya tienes una solicitud", e.message)
        }
    }

    @Test
    fun misPendientesFiltraYOrdenaPorFechaYHora() = runTest {
        coEvery { api.misSolicitudes() } returns listOf(
            solicitudDto(1, "PENDIENTE", "2026-12-03"),
            solicitudDto(2, "APROBADA", "2026-12-01"),
            solicitudDto(3, "PENDIENTE", "2026-12-01", inicio = "15:00:00"),
            solicitudDto(4, "PENDIENTE", "2026-12-01", inicio = "09:00:00")
        )
        assertEquals(listOf(4L, 3L, 1L), RemoteSolicitudRepository(api).misPendientes().map { it.id })
    }

    @Test
    fun accionesDelAdminYDelUsuarioLimpianElMotivo() = runTest {
        coEvery { api.solicitudesAdmin("PENDIENTE") } returns listOf(solicitudDto())
        coEvery { api.solicitud(100) } returns solicitudDto()
        coEvery { api.aprobarSolicitud(100) } returns solicitudDto(estado = "APROBADA")
        coEvery { api.rechazarSolicitud(100, any()) } returns solicitudDto(estado = "RECHAZADA")
        coEvery { api.cancelarSolicitud(100, any()) } returns solicitudDto(estado = "CANCELADA")
        coEvery { api.cancelarReservaComoAdmin(100, any()) } returns solicitudDto(estado = "CANCELADA")
        val repo = RemoteSolicitudRepository(api)

        assertEquals(1, repo.porRevisar().size)
        assertEquals(100, repo.detalle(100).id)
        assertEquals(EstadoSolicitud.APROBADA, repo.aprobar(100).estado)
        assertEquals(EstadoSolicitud.RECHAZADA, repo.rechazar(100, "   ").estado)
        assertEquals(EstadoSolicitud.CANCELADA, repo.cancelar(100, "  No podré ir  ").estado)
        repo.cancelarComoAdmin(100, "Mantención")

        coVerify { api.rechazarSolicitud(100, RechazarDto(null)) }          // vacío → null
        coVerify { api.cancelarSolicitud(100, CancelarDto("No podré ir")) } // sin espacios de sobra
        coVerify { api.cancelarReservaComoAdmin(100, CancelarDto("Mantención")) }
    }

    // Chat
    @Test
    fun chatTraduceEstadoYMensajes() = runTest {
        val chatDto = ChatDto(solicitudDto(estado = "APROBADA"), "ABIERTO", null, conversacionIniciada = false)
        val msj = MensajeDto(5, "Hola", usuarioDto(), "2026-11-01T10:00:00")
        coEvery { api.chats() } returns listOf(chatDto)
        coEvery { api.chat(100, 4) } returns ChatDetalleDto(chatDto, listOf(msj))
        coEvery { api.enviarMensaje(100, any()) } returns msj
        coEvery { api.cerrarChat(100) } returns chatDto.copy(estado = "CERRADO")
        val repo = RemoteChatRepository(api)

        val chat = repo.chats().single()
        assertTrue(chat.abierto)
        assertFalse(chat.iniciado)
        assertEquals("Hola", repo.conversacion(100, despuesDe = 4).mensajes.single().texto)
        assertEquals(5, repo.enviar(100, "  Hola  ").id)
        assertFalse(repo.cerrar(100).abierto)
        coVerify { api.enviarMensaje(100, EnviarMensajeDto("Hola")) }
    }

    // Notificaciones
    @Test
    fun elConteoDeNoLeidasSeMantieneAlDia() = runTest {
        coEvery { api.notificaciones() } returns listOf(
            NotificacionDto(1, "MENSAJE_NUEVO", "t", "m", 100, false, "2026-11-01T10:00:00"),
            NotificacionDto(2, "TIPO_QUE_NO_EXISTE", "t", "m", null, true, "fecha rara")
        )
        coEvery { api.notificacionesNoLeidas() } returns ConteoDto(3)
        val repo = RemoteNotificacionRepository(api)

        val lista = repo.listar()
        assertEquals(1, repo.noLeidas.value)
        assertEquals(cl.app.dina.data.model.TipoNotificacion.OTRO, lista[1].tipo) // tipo desconocido

        repo.actualizarConteo()
        assertEquals(3, repo.noLeidas.value)

        repo.borrar(notificacion(1, leida = false))
        assertEquals(2, repo.noLeidas.value)
        repo.borrar(notificacion(2, leida = true)) // una leída no cambia el conteo
        assertEquals(2, repo.noLeidas.value)

        repo.borrarTodas()
        assertEquals(0, repo.noLeidas.value)
        coVerify { api.borrarNotificaciones() }
    }
}
