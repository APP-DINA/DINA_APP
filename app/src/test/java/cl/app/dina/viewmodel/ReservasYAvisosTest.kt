package cl.app.dina.viewmodel

import androidx.lifecycle.SavedStateHandle
import cl.app.dina.data.model.Edificio
import cl.app.dina.data.model.EstadoBloque
import cl.app.dina.data.model.Feriado
import cl.app.dina.data.model.SolicitudCreada
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.data.model.TipoNotificacion
import cl.app.dina.data.remote.ErrorDelBackend
import cl.app.dina.data.repository.EspacioRepository
import cl.app.dina.data.repository.FeriadoRepository
import cl.app.dina.data.repository.NotificacionRepository
import cl.app.dina.data.repository.SolicitudRepository
import cl.app.dina.soporte.ReglaCorrutinas
import cl.app.dina.soporte.bloque
import cl.app.dina.soporte.espacio
import cl.app.dina.soporte.notificacion
import cl.app.dina.ui.viewmodel.AgregarEspacioEvento
import cl.app.dina.ui.viewmodel.AgregarEspacioViewModel
import cl.app.dina.ui.viewmodel.ConteoAvisosViewModel
import cl.app.dina.ui.viewmodel.EstadoCarga
import cl.app.dina.ui.viewmodel.FiltroEscuela
import cl.app.dina.ui.viewmodel.NotificacionesEvento
import cl.app.dina.ui.viewmodel.NotificacionesViewModel
import cl.app.dina.ui.viewmodel.ReservaEspacio2Evento
import cl.app.dina.ui.viewmodel.ReservaEspacio2ViewModel
import cl.app.dina.ui.viewmodel.ReservaEspacioViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReservasYAvisosTest {

    @get:Rule
    val regla = ReglaCorrutinas()

    private val espacios = mockk<EspacioRepository>(relaxed = true)
    private val solicitudes = mockk<SolicitudRepository>(relaxed = true)
    private val feriados = mockk<FeriadoRepository>()

    // Catálogo (Reservar)
    private val catalogo = listOf(
        espacio(1, "S12", edificio = "SUR"),
        espacio(2, "LC10", tipo = TipoEspacio.LABORATORIO, escuela = "INFORMATICA", nombre = "Laboratorio de Diseño"),
        espacio(3, "LC2", tipo = TipoEspacio.LABORATORIO, escuela = "INFORMATICA"),
        espacio(4, "S11")
    )

    @Test
    fun elCatalogoSeOrdenaPorEdificioYSeFiltraEnElTelefono() = runTest(regla.dispatcher) {
        coEvery { espacios.obtenerEspacios() } returns catalogo
        val vm = ReservaEspacioViewModel(espacios)
        backgroundScope.launch { vm.uiState.collect {} }

        val estado = vm.uiState.value
        assertEquals(listOf("Edificio NORTE", "Edificio SUR"), estado.grupos.map { it.edificioNombre })
        assertEquals(listOf("LC2", "LC10", "S11"), estado.grupos.first().espacios.map { it.codigo }) // orden natural
        assertEquals(listOf(TipoEspacio.SALA, TipoEspacio.LABORATORIO), estado.tiposDisponibles)
        assertEquals(listOf(FiltroEscuela.De("INFORMATICA"), FiltroEscuela.UsoGeneral), estado.escuelasDisponibles)

        vm.onBusquedaChange("diseno") // sin tilde encuentra "Diseño"
        assertEquals(listOf("LC10"), vm.uiState.value.grupos.flatMap { it.espacios }.map { it.codigo })
        assertTrue(vm.uiState.value.filtros.activos)

        vm.limpiarFiltros()
        vm.seleccionarTipo(TipoEspacio.SALA)
        assertEquals(listOf("S11", "S12"), vm.uiState.value.grupos.flatMap { it.espacios }.map { it.codigo })

        vm.seleccionarTipo(null)
        vm.seleccionarEscuela(FiltroEscuela.UsoGeneral)
        assertEquals(listOf("S11", "S12"), vm.uiState.value.grupos.flatMap { it.espacios }.map { it.codigo })

        val manana = LocalDate.now().plusDays(1)
        vm.seleccionarFecha(manana)
        assertEquals(manana, vm.uiState.value.calendario.fecha)
    }

    @Test
    fun errorDeCargaYRefrescoQueConservaLaLista() = runTest(regla.dispatcher) {
        coEvery { espacios.obtenerEspacios() } throws IOException()
        val vm = ReservaEspacioViewModel(espacios)
        backgroundScope.launch { vm.uiState.collect {} }
        assertTrue(vm.uiState.value.carga is EstadoCarga.Error)

        coEvery { espacios.obtenerEspacios() } returns catalogo
        vm.reintentar()
        assertEquals(EstadoCarga.Listo, vm.uiState.value.carga)

        coEvery { espacios.obtenerEspacios() } throws IOException()
        vm.refrescar()
        assertEquals(EstadoCarga.Listo, vm.uiState.value.carga) // la lista sigue visible
        assertFalse(vm.uiState.value.refrescando)
    }

    // Solicitar espacio (ReservaEspacio2) + API externa de feriados
    private val hoy = LocalDate.now()
    private val navidad = Feriado(hoy.plusDays(3), "Navidad")

    private fun formulario(): ReservaEspacio2ViewModel {
        coEvery { espacios.obtenerEspacio(1) } returns espacio(1, capacidad = 30)
        coEvery { solicitudes.disponibilidad(1, any()) } returns (1..6).map { bloque(it) }
        coEvery { feriados.feriadoEn(any()) } returns null
        coEvery { feriados.feriadoEn(navidad.fecha) } returns navidad
        return ReservaEspacio2ViewModel(
            SavedStateHandle(mapOf(ReservaEspacio2ViewModel.ARG_ESPACIO_ID to 1L)), espacios, solicitudes, feriados
        )
    }

    @Test
    fun alElegirUnFeriadoSeMuestraElAvisoDeLaApiExterna() = runTest(regla.dispatcher) {
        val vm = formulario()
        assertNull(vm.uiState.value.feriado)

        vm.seleccionarFecha(navidad.fecha)
        assertEquals(navidad, vm.uiState.value.feriado)

        vm.seleccionarFecha(hoy.plusDays(4))
        assertNull(vm.uiState.value.feriado)
    }

    @Test
    fun siLaApiDeFeriadosFallaLaReservaSigueFuncionando() = runTest(regla.dispatcher) {
        val vm = formulario()
        coEvery { feriados.feriadoEn(any()) } throws IOException()
        vm.seleccionarFecha(navidad.fecha)
        assertNull(vm.uiState.value.feriado)
        assertEquals(EstadoCarga.Listo, vm.uiState.value.cargaBloques)
    }

    @Test
    fun validaAntesDeEnviar() = runTest(regla.dispatcher) {
        val vm = formulario()
        vm.solicitar()
        assertEquals("Elige al menos un bloque", vm.uiState.value.errorBloques)
        assertEquals("Indica entre 1 y 30 personas", vm.uiState.value.errorAsistentes)
        assertEquals("Escribe el motivo de la reserva", vm.uiState.value.errorMotivo)
        coVerify(exactly = 0) { solicitudes.solicitar(any()) }

        vm.onAsistentesChange("4a5") // solo dígitos
        assertEquals("45", vm.uiState.value.asistentes)
    }

    @Test
    fun noSePuedenJuntarMasDeCuatroBloques() = runTest(regla.dispatcher) {
        val vm = formulario()
        (1..4).forEach { vm.onBloqueClick(bloque(it)) }
        assertEquals("08:00 a 12:00 · 4 h", vm.uiState.value.resumenSeleccion)
        vm.onBloqueClick(bloque(5))
        assertTrue(vm.uiState.value.errorBloques!!.startsWith("Puedes juntar como máximo 4"))
    }

    @Test
    fun solicitudEnviadaEmiteElEvento() = runTest(regla.dispatcher) {
        val creada = SolicitudCreada(9, hoy, "08:00", "09:00", "PENDIENTE")
        coEvery { solicitudes.solicitar(any()) } returns creada
        val vm = formulario()
        vm.onBloqueClick(bloque(1))
        vm.onAsistentesChange("20")
        vm.onMotivoChange("  Clase de prueba  ")
        vm.solicitar()

        assertEquals(ReservaEspacio2Evento.SolicitudEnviada(creada), vm.eventos.first())
        coVerify { solicitudes.solicitar(match { it.motivo == "Clase de prueba" && it.bloqueIds == listOf(1L) && !it.confirmarCruce }) }
    }

    @Test
    fun cruceConOtraSolicitudPreguntaYSePuedeConfirmar() = runTest(regla.dispatcher) {
        coEvery { solicitudes.solicitar(match { !it.confirmarCruce }) } throws
            ErrorDelBackend("Ya tienes una solicitud en ese horario", ErrorDelBackend.CRUCE_PROPIO)
        coEvery { solicitudes.solicitar(match { it.confirmarCruce }) } returns SolicitudCreada(9, hoy, "08:00", "09:00", "PENDIENTE")
        val vm = formulario()
        vm.onBloqueClick(bloque(1)); vm.onAsistentesChange("5"); vm.onMotivoChange("Clase")

        vm.solicitar()
        assertEquals("Ya tienes una solicitud en ese horario", vm.uiState.value.avisoCruce)
        vm.cancelarCruce()
        assertNull(vm.uiState.value.avisoCruce)

        vm.solicitar()
        vm.solicitarDeTodasFormas()
        assertTrue(vm.eventos.first() is ReservaEspacio2Evento.SolicitudEnviada)
    }

    @Test
    fun siElBloqueYaSeOcupoSeMuestraElErrorYSeActualiza() = runTest(regla.dispatcher) {
        coEvery { solicitudes.solicitar(any()) } throws ErrorDelBackend("El espacio ya está reservado", null)
        val vm = formulario()
        vm.onBloqueClick(bloque(2)); vm.onAsistentesChange("5"); vm.onMotivoChange("Clase")
        coEvery { solicitudes.disponibilidad(1, any()) } returns listOf(bloque(1), bloque(2, EstadoBloque.OCUPADO))

        vm.solicitar()
        assertEquals("El espacio ya está reservado", vm.uiState.value.errorGeneral)
        assertTrue(vm.uiState.value.seleccion.isEmpty()) // el bloque elegido ya no está libre
    }

    @Test
    fun errorAlCargarElEspacioYLosBloques() = runTest(regla.dispatcher) {
        coEvery { espacios.obtenerEspacio(1) } throws NoSuchElementException("El espacio ya no está disponible")
        coEvery { solicitudes.disponibilidad(1, any()) } throws IOException()
        coEvery { feriados.feriadoEn(any()) } returns null
        val vm = ReservaEspacio2ViewModel(
            SavedStateHandle(mapOf(ReservaEspacio2ViewModel.ARG_ESPACIO_ID to 1L)), espacios, solicitudes, feriados
        )
        assertEquals("El espacio ya no está disponible", vm.uiState.value.errorEspacio)
        assertTrue(vm.uiState.value.cargaBloques is EstadoCarga.Error)
        coEvery { solicitudes.disponibilidad(1, any()) } returns listOf(bloque(1, EstadoBloque.NO_DISPONIBLE))
        vm.reintentarBloques()
        assertTrue(vm.uiState.value.diaSinBloques)
    }

    // Agregar espacio (admin)
    @Test
    fun agregarEspacioValidaYEnvia() = runTest(regla.dispatcher) {
        coEvery { espacios.crear(any()) } returns espacio(9, "S99")
        val vm = AgregarEspacioViewModel(espacios)
        vm.onPisoChange("")
        vm.agregar()
        assertEquals("Escribe el código del espacio (ej.: S41)", vm.uiState.value.errorCodigo)
        assertTrue(vm.uiState.value.errorPiso != null && vm.uiState.value.errorCapacidad != null)

        vm.onCodigoChange("s99")
        vm.onPisoChange("-1x")
        vm.onCapacidadChange("30 personas")
        vm.onEdificioChange(Edificio.SUR)
        vm.onTipoChange(TipoEspacio.LABORATORIO)
        vm.onEscuelaChange("INFORMATICA")
        vm.onProyectorChange(true); vm.onComputadoresChange(true); vm.onClimatizacionChange(true)
        vm.onNombreChange("")
        assertEquals("S99", vm.uiState.value.codigo)
        assertEquals("-1", vm.uiState.value.piso)
        assertEquals("30", vm.uiState.value.capacidad)
        assertEquals("Laboratorio S99", vm.uiState.value.vistaPrevia.nombre)

        vm.agregar()
        assertEquals(AgregarEspacioEvento.Agregado(espacio(9, "S99")), vm.eventos.first())
        coVerify { espacios.crear(match { it.edificio == Edificio.SUR && it.piso == -1 && it.computadores }) }
    }

    @Test
    fun codigoRepetidoMuestraElErrorDelBackend() = runTest(regla.dispatcher) {
        coEvery { espacios.crear(any()) } throws ErrorDelBackend("Ya existe un espacio con el código S11", null)
        val vm = AgregarEspacioViewModel(espacios).apply {
            onCodigoChange("S11"); onCapacidadChange("20"); agregar()
        }
        assertEquals("Ya existe un espacio con el código S11", vm.uiState.value.errorGeneral)
        assertFalse(vm.uiState.value.enviando)
    }

    // Avisos
    private val avisos = mockk<NotificacionRepository>(relaxed = true)

    @Test
    fun tocarUnAvisoLoBorraYAbreSuDestino() = runTest(regla.dispatcher) {
        coEvery { avisos.listar() } returns listOf(
            notificacion(1, TipoNotificacion.MENSAJE_NUEVO, solicitudId = 50),
            notificacion(2, TipoNotificacion.NUEVA_SOLICITUD, solicitudId = 60),
            notificacion(3, solicitudId = null)
        )
        val vm = NotificacionesViewModel(avisos)
        assertEquals(3, vm.uiState.value.notificaciones.size)

        vm.abrir(vm.uiState.value.notificaciones[0])
        assertEquals(NotificacionesEvento.AbrirChat(50), vm.eventos.first())
        vm.abrir(vm.uiState.value.notificaciones[0])
        assertEquals(NotificacionesEvento.AbrirSolicitud(60), vm.eventos.first())
        vm.abrir(vm.uiState.value.notificaciones[0]) // sin solicitud: solo se borra
        assertTrue(vm.uiState.value.notificaciones.isEmpty())
        coVerify(exactly = 3) { avisos.borrar(any()) }
    }

    @Test
    fun deslizarBorraConDeshacer() = runTest(regla.dispatcher) {
        val n1 = notificacion(1)
        val n2 = notificacion(2)
        coEvery { avisos.listar() } returns listOf(n2, n1)
        val vm = NotificacionesViewModel(avisos)

        vm.borrar(n1)
        vm.borrar(n1) // el gesto puede avisar dos veces
        assertEquals(NotificacionesEvento.Borrada(n1), vm.eventos.first())
        vm.deshacer(n1)
        assertEquals(listOf(n2, n1), vm.uiState.value.notificaciones)

        vm.borrar(n1)
        vm.confirmarBorrado(n1)
        coVerify(exactly = 1) { avisos.borrar(n1) }

        vm.borrar(n2) // sin tocar nada: se borra solo al pasar el tiempo máximo
        advanceTimeBy(NotificacionesViewModel.ESPERA_MAXIMA_MS + 1)
        coVerify { avisos.borrar(n2) }
    }

    @Test
    fun siElBorradoFallaElAvisoVuelve() = runTest(regla.dispatcher) {
        val n1 = notificacion(1)
        coEvery { avisos.listar() } returns listOf(n1)
        coEvery { avisos.borrar(n1) } throws IOException()
        val vm = NotificacionesViewModel(avisos)
        vm.borrar(n1)
        vm.eventos.first() // "Borrada"
        vm.confirmarBorrado(n1)
        assertEquals(listOf(n1), vm.uiState.value.notificaciones)
        assertTrue(vm.eventos.first() is NotificacionesEvento.Mensaje)
    }

    @Test
    fun borrarTodasPideConfirmacion() = runTest(regla.dispatcher) {
        coEvery { avisos.listar() } returns listOf(notificacion(1), notificacion(2))
        val vm = NotificacionesViewModel(avisos)
        vm.pedirBorrarTodas()
        assertTrue(vm.uiState.value.confirmandoBorrarTodas)
        vm.cancelarBorrarTodas()
        assertFalse(vm.uiState.value.confirmandoBorrarTodas)

        vm.borrar(notificacion(1))
        vm.eventos.first()
        vm.borrarTodas()
        assertTrue(vm.uiState.value.notificaciones.isEmpty())
        assertEquals(NotificacionesEvento.Mensaje("Se borraron todas las notificaciones"), vm.eventos.first())

        coEvery { avisos.borrarTodas() } throws IOException()
        vm.borrarTodas()
        assertTrue(vm.eventos.first() is NotificacionesEvento.Mensaje)
        assertFalse(vm.uiState.value.borrandoTodas)
    }

    @Test
    fun avisosConErrorYRefresco() = runTest(regla.dispatcher) {
        coEvery { avisos.listar() } throws IOException()
        val vm = NotificacionesViewModel(avisos)
        assertTrue(vm.uiState.value.carga is EstadoCarga.Error)
        coEvery { avisos.listar() } returns listOf(notificacion(1))
        vm.reintentar()
        assertEquals(EstadoCarga.Listo, vm.uiState.value.carga)
        coEvery { avisos.listar() } throws IOException()
        vm.refrescar()
        assertTrue(vm.eventos.first() is NotificacionesEvento.Mensaje)
        vm.actualizar()
        assertEquals(1, vm.uiState.value.notificaciones.size)
    }

    @Test
    fun laCampanaConsultaCadaTreintaSegundosMientrasEstaVisible() = runTest(regla.dispatcher) {
        every { avisos.noLeidas } returns MutableStateFlow(4)
        val vm = ConteoAvisosViewModel(avisos)
        assertEquals(4, vm.noLeidas.value)

        vm.reanudar()
        advanceTimeBy(ConteoAvisosViewModel.INTERVALO_MS + 1)
        vm.pausar()
        coVerify(exactly = 2) { avisos.actualizarConteo() } // al reanudar y a los 30 s

        vm.actualizar()
        coVerify(exactly = 3) { avisos.actualizarConteo() }
    }
}
