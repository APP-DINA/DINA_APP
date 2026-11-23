package cl.app.dina.viewmodel

import android.net.Uri
import cl.app.dina.data.model.ModoTema
import cl.app.dina.data.model.Rol
import cl.app.dina.data.repository.AuthRepository
import cl.app.dina.data.repository.PerfilRepository
import cl.app.dina.data.repository.PreferenciasRepository
import cl.app.dina.data.repository.ResultadoLogin
import cl.app.dina.data.repository.SesionRepository
import cl.app.dina.soporte.ADMIN
import cl.app.dina.soporte.ALUMNO
import cl.app.dina.soporte.ReglaCorrutinas
import cl.app.dina.ui.viewmodel.EstadoSesion
import cl.app.dina.ui.viewmodel.LoginViewModel
import cl.app.dina.ui.viewmodel.MenuViewModel
import cl.app.dina.ui.viewmodel.SesionViewModel
import cl.app.dina.utils.errorCorreo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SesionLoginMenuTest {

    @get:Rule
    val regla = ReglaCorrutinas()

    private val auth = mockk<AuthRepository>(relaxed = true)
    private val sesion = mockk<SesionRepository>(relaxed = true)

    // Login
    @Test
    fun alEscribirElCorreoSeDetectaElRol() {
        val vm = LoginViewModel(auth, sesion)
        vm.onCorreoChange("  profe@profesor.duoc.cl ")
        assertEquals("profe@profesor.duoc.cl", vm.uiState.value.correo)
        assertEquals(Rol.PROFESOR, vm.uiState.value.rolDetectado)
        vm.onCorreoChange("x@gmail.com")
        assertNull(vm.uiState.value.rolDetectado)
    }

    @Test
    fun sinDatosNoLlamaAlBackendYMuestraErrores() {
        val vm = LoginViewModel(auth, sesion)
        vm.ingresar()
        assertEquals("Ingresa tu correo institucional", vm.uiState.value.errorCorreo)
        assertEquals("Ingresa tu contraseña", vm.uiState.value.errorPassword)
        coVerify(exactly = 0) { auth.login(any(), any()) }
    }

    @Test
    fun validacionDelCorreo() {
        assertEquals("El correo no tiene un formato válido", errorCorreo("sin-arroba"))
        assertEquals("Usa tu cuenta @duocuc.cl o @profesor.duoc.cl", errorCorreo("a@gmail.com"))
        assertNull(errorCorreo("a.b@duocuc.cl"))
    }

    @Test
    fun loginExitosoGuardaLaSesionYLimpiaLaContrasena() = runTest(regla.dispatcher) {
        coEvery { auth.login("alumno.prueba@duocuc.cl", "clave") } returns ResultadoLogin.Exito(ALUMNO, "tok")
        val vm = LoginViewModel(auth, sesion)
        vm.onCorreoChange("alumno.prueba@duocuc.cl")
        vm.onPasswordChange("clave")
        vm.onPasswordVisibleToggle()
        assertTrue(vm.uiState.value.passwordVisible)

        vm.ingresar()

        coVerify { sesion.iniciar(ALUMNO, "tok") }
        assertEquals("", vm.uiState.value.password)
        assertFalse(vm.uiState.value.cargando)
        assertEquals("alumno.prueba@duocuc.cl", vm.uiState.value.correo)
    }

    @Test
    fun loginRechazadoMuestraElMensaje() = runTest(regla.dispatcher) {
        coEvery { auth.login(any(), any()) } returns ResultadoLogin.Error("Correo o contraseña incorrectos")
        val vm = LoginViewModel(auth, sesion).apply {
            onCorreoChange("alumno.prueba@duocuc.cl"); onPasswordChange("mala"); ingresar()
        }
        assertEquals("Correo o contraseña incorrectos", vm.uiState.value.errorGeneral)
        assertFalse(vm.uiState.value.cargando)
        vm.onPasswordChange("otra") // escribir de nuevo borra el error
        assertNull(vm.uiState.value.errorGeneral)
    }

    @Test
    fun errorInesperadoAlIniciarSesion() = runTest(regla.dispatcher) {
        coEvery { auth.login(any(), any()) } throws IllegalStateException("boom")
        val vm = LoginViewModel(auth, sesion).apply {
            onCorreoChange("alumno.prueba@duocuc.cl"); onPasswordChange("x"); ingresar()
        }
        assertEquals("No se pudo conectar. Intenta nuevamente", vm.uiState.value.errorGeneral)
    }

    @Test
    fun noSeEnviaDosVecesMientrasCarga() = runTest(regla.dispatcher) {
        val respuesta = CompletableDeferred<ResultadoLogin>()
        coEvery { auth.login(any(), any()) } coAnswers { respuesta.await() }
        val vm = LoginViewModel(auth, sesion).apply {
            onCorreoChange("alumno.prueba@duocuc.cl"); onPasswordChange("x")
        }
        vm.ingresar()
        assertTrue(vm.uiState.value.cargando)
        vm.ingresar() // ignorado
        respuesta.complete(ResultadoLogin.Error("x"))
        coVerify(exactly = 1) { auth.login(any(), any()) }
    }

    // Sesión
    @Test
    fun conSesionGuardadaSeValidaConElBackend() = runTest(regla.dispatcher) {
        val guardado = MutableStateFlow<cl.app.dina.data.model.Usuario?>(ALUMNO)
        every { sesion.usuario } returns guardado
        coEvery { auth.usuarioActual() } returns ALUMNO.copy(esAdmin = true)
        val vm = SesionViewModel(sesion, auth)

        backgroundScope.launch { vm.estado.collect {} }
        assertEquals(EstadoSesion.ConSesion(ALUMNO), vm.estado.value)
        coVerify { sesion.actualizarUsuario(ALUMNO.copy(esAdmin = true)) }

        guardado.value = null // cerró sesión
        assertEquals(EstadoSesion.SinSesion, vm.estado.value)
    }

    @Test
    fun sinSesionNoPreguntaAlBackend() = runTest(regla.dispatcher) {
        every { sesion.usuario } returns flowOf(null)
        SesionViewModel(sesion, auth)
        coVerify(exactly = 0) { auth.usuarioActual() }
    }

    // Menú
    private val perfil = mockk<PerfilRepository>()
    private val preferencias = mockk<PreferenciasRepository>(relaxed = true)

    private fun menu(): MenuViewModel {
        every { sesion.usuario } returns flowOf(ADMIN)
        every { preferencias.modoTema } returns flowOf(ModoTema.OSCURO)
        return MenuViewModel(sesion, auth, perfil, preferencias)
    }

    @Test
    fun menuMuestraUsuarioYTemaYGuardaElTemaElegido() = runTest(regla.dispatcher) {
        val vm = menu()
        backgroundScope.launch { vm.usuario.collect {} }
        backgroundScope.launch { vm.modoTema.collect {} }
        assertEquals(ADMIN, vm.usuario.value)
        assertEquals(ModoTema.OSCURO, vm.modoTema.value)

        vm.cambiarModoTema(ModoTema.CLARO)
        coVerify { preferencias.guardarModoTema(ModoTema.CLARO) }
    }

    @Test
    fun cambiarYBorrarFotoAvisanElResultado() = runTest(regla.dispatcher) {
        val imagen = mockk<Uri>()
        coEvery { perfil.cambiarFoto(imagen) } returns ADMIN
        coEvery { perfil.borrarFoto() } throws IOException()
        val vm = menu()

        vm.cambiarFoto(imagen)
        assertEquals("Foto de perfil actualizada", vm.mensajes.first())
        assertFalse(vm.subiendoFoto.value)

        vm.borrarFoto()
        assertEquals("No se pudo conectar con el servidor. Revisa tu conexión", vm.mensajes.first())
    }

    @Test
    fun soloUnaAccionDeFotoALaVez() = runTest(regla.dispatcher) {
        val subida = CompletableDeferred<cl.app.dina.data.model.Usuario>()
        coEvery { perfil.borrarFoto() } coAnswers { subida.await() }
        val vm = menu()
        vm.borrarFoto()
        assertTrue(vm.subiendoFoto.value)
        vm.borrarFoto() // ignorado
        subida.complete(ADMIN)
        coVerify(exactly = 1) { perfil.borrarFoto() }
    }

    @Test
    fun cerrarSesionPideConfirmacionYLuegoCierraEnBackendYTelefono() = runTest(regla.dispatcher) {
        val vm = menu()
        vm.pedirCerrarSesion()
        assertTrue(vm.confirmandoCierre.value)
        vm.cancelarCerrarSesion()
        assertFalse(vm.confirmandoCierre.value)

        vm.pedirCerrarSesion()
        vm.cerrarSesion()
        assertFalse(vm.confirmandoCierre.value)
        coVerifyOrder {
            auth.logout()
            sesion.cerrar()
        }
        assertNotNull(vm)
    }
}
