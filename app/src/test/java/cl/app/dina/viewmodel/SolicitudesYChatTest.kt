package cl.app.dina.viewmodel

import androidx.lifecycle.SavedStateHandle
import cl.app.dina.data.model.Conversacion
import cl.app.dina.data.model.EstadoSolicitud
import cl.app.dina.data.model.Usuario
import cl.app.dina.data.repository.ChatRepository
import cl.app.dina.data.repository.SesionRepository
import cl.app.dina.data.repository.SolicitudRepository
import cl.app.dina.soporte.ADMIN
import cl.app.dina.soporte.ALUMNO
import cl.app.dina.soporte.ReglaCorrutinas
import cl.app.dina.soporte.chat
import cl.app.dina.soporte.mensaje
import cl.app.dina.soporte.solicitud
import cl.app.dina.ui.viewmodel.AccionChat
import cl.app.dina.ui.viewmodel.AccionSolicitud
import cl.app.dina.ui.viewmodel.ChatViewModel
import cl.app.dina.ui.viewmodel.ConversacionViewModel
import cl.app.dina.ui.viewmodel.EstadoCarga
import cl.app.dina.ui.viewmodel.InicioEvento
import cl.app.dina.ui.viewmodel.InicioViewModel
import cl.app.dina.ui.viewmodel.PestaniaChat
import cl.app.dina.ui.viewmodel.SolicitudEvento
import cl.app.dina.ui.viewmodel.SolicitudViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SolicitudesYChatTest {

    @get:Rule
    val regla = ReglaCorrutinas()

    private val usuario = MutableStateFlow<Usuario?>(ALUMNO)
    private val sesion = mockk<SesionRepository> { every { usuario } returns this@SolicitudesYChatTest.usuario }
    private val solicitudes = mockk<SolicitudRepository>(relaxed = true)
    private val chats = mockk<ChatRepository>(relaxed = true)

    // Inicio
    @Test
    fun inicioDelUsuarioMuestraSusPendientes() = runTest(regla.dispatcher) {
        coEvery { solicitudes.misPendientes() } returns listOf(solicitud(1), solicitud(2))
        val vm = InicioViewModel(sesion, solicitudes)

        assertEquals(EstadoCarga.Listo, vm.uiState.value.cargaSolicitudes)
        assertEquals(2, vm.uiState.value.solicitudes.size)
        assertTrue(vm.uiState.value.entradas.isEmpty()) // la agrupación es solo para el admin
        coVerify(exactly = 0) { solicitudes.porRevisar() }
    }

    @Test
    fun alPasarAAdminSeCambiaALasPorRevisar() = runTest(regla.dispatcher) {
        coEvery { solicitudes.porRevisar() } returns listOf(solicitud(1), solicitud(2, inicio = "10:00", fin = "11:00"))
        val vm = InicioViewModel(sesion, solicitudes)
        usuario.value = ADMIN

        coVerify { solicitudes.porRevisar() }
        assertEquals(1, vm.uiState.value.entradas.size) // las dos piden lo mismo: una "Doble reserva"
    }

    @Test
    fun errorAlCargarYReintentar() = runTest(regla.dispatcher) {
        coEvery { solicitudes.misPendientes() } throws IOException()
        val vm = InicioViewModel(sesion, solicitudes)
        assertTrue(vm.uiState.value.cargaSolicitudes is EstadoCarga.Error)

        coEvery { solicitudes.misPendientes() } returns emptyList()
        vm.reintentar()
        assertEquals(EstadoCarga.Listo, vm.uiState.value.cargaSolicitudes)
    }

    @Test
    fun refrescarConErrorConservaLaListaYAvisa() = runTest(regla.dispatcher) {
        coEvery { solicitudes.misPendientes() } returns listOf(solicitud(1))
        val vm = InicioViewModel(sesion, solicitudes)
        coEvery { solicitudes.misPendientes() } throws IOException()

        vm.refrescar()
        assertEquals(1, vm.uiState.value.solicitudes.size)
        assertFalse(vm.uiState.value.refrescando)
        assertTrue(vm.eventos.first() is InicioEvento.Mensaje)

        vm.actualizar() // silencioso: ni indicador ni aviso
        assertEquals(EstadoCarga.Listo, vm.uiState.value.cargaSolicitudes)
    }

    // Detalle de solicitud
    private fun detalle(s: cl.app.dina.data.model.SolicitudResumen = solicitud()): SolicitudViewModel {
        coEvery { solicitudes.detalle(100) } returns s
        return SolicitudViewModel(SavedStateHandle(mapOf(SolicitudViewModel.ARG_SOLICITUD_ID to 100L)), sesion, solicitudes)
    }

    @Test
    fun elUsuarioPuedeCancelarSuSolicitudPendiente() = runTest(regla.dispatcher) {
        val vm = detalle()
        assertTrue(vm.uiState.value.puedeCancelar)
        assertFalse(vm.uiState.value.puedeResponder)

        vm.pedirConfirmacion(AccionSolicitud.CANCELAR)
        assertEquals(AccionSolicitud.CANCELAR, vm.uiState.value.confirmando)
        vm.confirmar()

        coVerify { solicitudes.cancelar(100) }
        assertEquals(SolicitudEvento.Hecho("Solicitud cancelada"), vm.eventos.first())
        assertNull(vm.uiState.value.enCurso)
    }

    @Test
    fun elAdminApruebaORechazaConMotivo() = runTest(regla.dispatcher) {
        usuario.value = ADMIN
        val vm = detalle()
        assertTrue(vm.uiState.value.puedeResponder)
        assertFalse(vm.uiState.value.puedeCancelar)

        vm.pedirConfirmacion(AccionSolicitud.APROBAR)
        vm.confirmar()
        assertEquals(SolicitudEvento.Hecho("Solicitud aceptada. Se avisó a quien la pidió"), vm.eventos.first())

        vm.pedirConfirmacion(AccionSolicitud.RECHAZAR)
        vm.onMotivoRechazoChange("x".repeat(400))
        assertEquals(SolicitudViewModel.MAX_MOTIVO_RECHAZO, vm.uiState.value.motivoRechazo.length)
        vm.confirmar()
        coVerify { solicitudes.rechazar(100, "x".repeat(300)) }
        assertEquals(SolicitudEvento.Hecho("Solicitud rechazada. Se avisó a quien la pidió"), vm.eventos.first())
    }

    @Test
    fun siOtroAdminYaLaRevisoSeAvisaYSeRecarga() = runTest(regla.dispatcher) {
        usuario.value = ADMIN
        val vm = detalle()
        coEvery { solicitudes.aprobar(100) } throws IOException()
        coEvery { solicitudes.detalle(100) } returns solicitud(estado = EstadoSolicitud.APROBADA)

        vm.pedirConfirmacion(AccionSolicitud.APROBAR)
        vm.confirmar()

        assertTrue(vm.eventos.first() is SolicitudEvento.Error)
        assertEquals(EstadoSolicitud.APROBADA, vm.uiState.value.solicitud?.estado)
        assertFalse(vm.uiState.value.puedeResponder)
    }

    @Test
    fun cancelarElDialogoYErrorAlCargar() = runTest(regla.dispatcher) {
        val vm = detalle()
        vm.pedirConfirmacion(AccionSolicitud.CANCELAR)
        vm.cancelarConfirmacion()
        assertNull(vm.uiState.value.confirmando)
        vm.confirmar() // sin acción pendiente: no hace nada
        coVerify(exactly = 0) { solicitudes.cancelar(any(), any()) }

        coEvery { solicitudes.detalle(100) } throws IOException()
        vm.reintentar()
        assertTrue(vm.uiState.value.carga is EstadoCarga.Error)
    }

    // Lista de chats
    @Test
    fun losChatsSeRepartenEnPestanias() = runTest(regla.dispatcher) {
        coEvery { chats.chats() } returns listOf(
            chat(id = 1), chat(id = 2, iniciado = false), chat(id = 3, abierto = false)
        )
        val vm = ChatViewModel(sesion, chats)

        assertEquals(listOf(1L), vm.uiState.value.visibles.map { it.solicitud.id })
        vm.seleccionarPestania(PestaniaChat.ACEPTADOS)
        assertEquals(listOf(2L), vm.uiState.value.visibles.map { it.solicitud.id })
        vm.seleccionarPestania(PestaniaChat.CERRADOS)
        assertEquals(listOf(3L), vm.uiState.value.visibles.map { it.solicitud.id })
        assertEquals(ALUMNO.id, vm.uiState.value.miId)
    }

    @Test
    fun chatsConErrorYRefresco() = runTest(regla.dispatcher) {
        coEvery { chats.chats() } throws IOException()
        val vm = ChatViewModel(sesion, chats)
        assertTrue(vm.uiState.value.carga is EstadoCarga.Error)

        coEvery { chats.chats() } returns listOf(chat())
        vm.reintentar()
        assertEquals(EstadoCarga.Listo, vm.uiState.value.carga)

        coEvery { chats.chats() } throws IOException()
        vm.refrescar()
        assertEquals("No se pudo conectar con el servidor. Revisa tu conexión", vm.mensajes.first())
        assertEquals(1, vm.uiState.value.chats.size) // la lista se conserva
        vm.actualizar()
        assertFalse(vm.uiState.value.refrescando)
    }

    // Conversación
    private fun conversacion(c: cl.app.dina.data.model.Chat = chat(), mensajes: List<cl.app.dina.data.model.Mensaje> = listOf(mensaje(1))): ConversacionViewModel {
        coEvery { chats.conversacion(100, 0) } returns Conversacion(c, mensajes)
        return ConversacionViewModel(
            SavedStateHandle(mapOf(ConversacionViewModel.ARG_SOLICITUD_ID to 100L)), sesion, chats, solicitudes
        )
    }

    @Test
    fun cargaYEnviaSinRepetirMensajes() = runTest(regla.dispatcher) {
        val vm = conversacion()
        assertTrue(vm.uiState.value.soyElSolicitante)
        assertTrue(vm.uiState.value.puedeCancelarReserva)
        assertFalse(vm.uiState.value.puedeEnviar) // sin texto

        coEvery { chats.enviar(100, "Hola") } returns mensaje(2, "Hola")
        vm.onTextoChange("Hola")
        vm.enviar()
        assertEquals(listOf(1L, 2L), vm.uiState.value.mensajes.map { it.id })
        assertEquals("", vm.uiState.value.texto)

        // La revisión periódica trae otra vez el 2 y uno nuevo, el 3: no se repite el 2
        coEvery { chats.conversacion(100, 2) } returns Conversacion(chat(), listOf(mensaje(2), mensaje(3)))
        vm.reanudar()
        advanceTimeBy(ConversacionViewModel.INTERVALO_MS + 1)
        vm.pausar()
        assertEquals(listOf(1L, 2L, 3L), vm.uiState.value.mensajes.map { it.id })
    }

    @Test
    fun siElChatSeCerroAlEnviarSeAvisa() = runTest(regla.dispatcher) {
        val vm = conversacion()
        coEvery { chats.enviar(any(), any()) } throws IOException()
        coEvery { chats.conversacion(100, 1) } returns Conversacion(chat(abierto = false), emptyList())
        vm.onTextoChange("Hola")
        vm.enviar()
        assertEquals("No se pudo conectar con el servidor. Revisa tu conexión", vm.avisos.first())
        assertFalse(vm.uiState.value.puedeEscribir)
    }

    @Test
    fun elAdminDebeExplicarPorQueCancelaUnaReservaAjena() = runTest(regla.dispatcher) {
        usuario.value = ADMIN
        val vm = conversacion()
        assertTrue(vm.uiState.value.cancelaComoAdmin)

        vm.pedirConfirmacion(AccionChat.CANCELAR_RESERVA)
        vm.confirmar()
        assertEquals("Explica al usuario por qué se cancela", vm.uiState.value.errorMotivo)
        coVerify(exactly = 0) { solicitudes.cancelarComoAdmin(any(), any()) }

        vm.onMotivoChange("Mantención de la sala")
        vm.confirmar()
        coVerify { solicitudes.cancelarComoAdmin(100, "Mantención de la sala") }
        assertEquals("Reserva cancelada", vm.avisos.first())
        assertFalse(vm.uiState.value.procesando)
    }

    @Test
    fun elUsuarioCancelaSinMotivoYElAdminCierraElChat() = runTest(regla.dispatcher) {
        val vm = conversacion()
        vm.pedirConfirmacion(AccionChat.CANCELAR_RESERVA)
        vm.confirmar()
        coVerify { solicitudes.cancelar(100, "") }

        usuario.value = ADMIN
        val admin = conversacion()
        assertTrue(admin.uiState.value.puedeCerrarChat)
        admin.pedirConfirmacion(AccionChat.CERRAR_CHAT)
        admin.cancelarConfirmacion()
        assertNull(admin.uiState.value.confirmando)
        admin.pedirConfirmacion(AccionChat.CERRAR_CHAT)
        admin.confirmar()
        coVerify { chats.cerrar(100) }
        assertEquals("Chat cerrado. Lo encontrarás en Cerrados", admin.avisos.first())
    }

    @Test
    fun errorAlAbrirLaConversacion() = runTest(regla.dispatcher) {
        coEvery { chats.conversacion(100, 0) } throws IOException()
        val vm = ConversacionViewModel(
            SavedStateHandle(mapOf(ConversacionViewModel.ARG_SOLICITUD_ID to 100L)), sesion, chats, solicitudes
        )
        assertTrue(vm.uiState.value.carga is EstadoCarga.Error)
        coEvery { chats.conversacion(100, 0) } returns Conversacion(chat(), emptyList())
        vm.reintentar()
        assertEquals(EstadoCarga.Listo, vm.uiState.value.carga)
    }
}
