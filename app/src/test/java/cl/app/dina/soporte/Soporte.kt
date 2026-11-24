package cl.app.dina.soporte

import cl.app.dina.data.model.BloqueDia
import cl.app.dina.data.model.Chat
import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.EstadoBloque
import cl.app.dina.data.model.EstadoSolicitud
import cl.app.dina.data.model.Mensaje
import cl.app.dina.data.model.Notificacion
import cl.app.dina.data.model.Rol
import cl.app.dina.data.model.Solicitante
import cl.app.dina.data.model.SolicitudResumen
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.data.model.TipoNotificacion
import cl.app.dina.data.model.Usuario
import cl.app.dina.data.remote.dto.EspacioDto
import cl.app.dina.data.remote.dto.SolicitudDto
import cl.app.dina.data.remote.dto.UsuarioDto
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import retrofit2.HttpException
import retrofit2.Response
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Los ViewModels lanzan corrutinas en viewModelScope (Dispatchers.Main), que no existe en la JVM.
 * Esta regla lo reemplaza por un dispatcher de prueba: usar runTest(regla.dispatcher) para que
 * delay() avance con el reloj virtual (sin esperar de verdad).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReglaCorrutinas(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

// Datos ficticios (Ley 19.628 / 21.719: nada real)
val ALUMNO = Usuario(correo = "alumno.prueba@duocuc.cl", nombre = "Alumno Prueba", rol = Rol.ESTUDIANTE, id = 10)
val ADMIN = Usuario(correo = "admin.prueba@duocuc.cl", nombre = "Admin Prueba", rol = Rol.ESTUDIANTE, esAdmin = true, id = 1)

fun espacio(id: Long = 1, codigo: String = "S11", capacidad: Int = 35, edificio: String = "NORTE",
            tipo: TipoEspacio = TipoEspacio.SALA, escuela: String? = null, piso: Int = 1,
            nombre: String = "Sala $codigo") = Espacio(
    id = id, codigo = codigo, nombre = nombre, edificio = edificio, edificioNombre = "Edificio $edificio",
    piso = piso, tipo = tipo, capacidad = capacidad, escuela = escuela,
    proyector = true, computadores = false, climatizacion = false
)

fun solicitante(id: Long = ALUMNO.id) = Solicitante(id, "Alumno Prueba", Rol.ESTUDIANTE, null, "alumno.prueba@duocuc.cl")

fun solicitud(
    id: Long = 100,
    estado: EstadoSolicitud = EstadoSolicitud.PENDIENTE,
    fecha: LocalDate = LocalDate.now().plusDays(2),
    inicio: String = "10:00",
    fin: String = "11:00",
    solicitanteId: Long = ALUMNO.id
) = SolicitudResumen(
    id = id, espacio = espacio(), fecha = fecha, inicio = inicio, fin = fin, motivo = "Taller de prueba",
    asistentes = 10, solicitante = solicitante(solicitanteId), estado = estado,
    creadaEn = LocalDateTime.of(2026, 11, 1, 9, 0)
)

fun chat(estado: EstadoSolicitud = EstadoSolicitud.APROBADA, abierto: Boolean = true, iniciado: Boolean = true,
         solicitanteId: Long = ALUMNO.id, id: Long = 100) =
    Chat(solicitud(id = id, estado = estado, solicitanteId = solicitanteId), abierto, null, iniciado)

fun mensaje(id: Long, texto: String = "Hola $id", autorId: Long? = ALUMNO.id) =
    Mensaje(id, texto, autorId?.let { solicitante(it) }, LocalDateTime.of(2026, 11, 1, 9, 0).plusMinutes(id))

fun bloque(numero: Int, estado: EstadoBloque = EstadoBloque.DISPONIBLE, mio: Boolean = false) = BloqueDia(
    id = numero.toLong(), numero = numero,
    inicio = "%02d:00".format(7 + numero), fin = "%02d:00".format(8 + numero),
    estado = estado, pendientes = 0, mio = mio
)

fun notificacion(id: Long, tipo: TipoNotificacion = TipoNotificacion.SOLICITUD_APROBADA, leida: Boolean = false,
                 solicitudId: Long? = 100) = Notificacion(
    id = id, tipo = tipo, titulo = "Aviso $id", mensaje = "Mensaje $id", solicitudId = solicitudId,
    leida = leida, creadaEn = LocalDateTime.of(2026, 11, 1, 9, 0).plusMinutes(id)
)

// DTOs (lo que respondería dina_backend)
fun usuarioDto(id: Long = 10, correo: String = "alumno.prueba@duocuc.cl", rol: String = "USUARIO", foto: String? = null) =
    UsuarioDto(id = id, correo = correo, nombre = "Alumno Prueba", rol = rol, fotoUrl = foto)

fun espacioDto(id: Long = 1, codigo: String = "S11") = EspacioDto(
    id = id, codigo = codigo, nombre = "Sala $codigo", edificio = "NORTE", edificioNombre = "Edificio Norte",
    piso = 1, tipo = "SALA", capacidad = 35, escuela = null, proyector = true, computadores = false, climatizacion = false
)

fun solicitudDto(id: Long = 100, estado: String = "PENDIENTE", fecha: String = "2026-12-01", inicio: String = "10:00:00") =
    SolicitudDto(
        id = id, espacio = espacioDto(), fecha = fecha, horaInicio = inicio, horaFin = "11:00:00",
        actividad = "Taller", asistentes = 10, estado = estado, comentarioAdmin = null,
        solicitante = usuarioDto(), tieneChat = false, creadaEn = "2026-11-01T09:00:00"
    )

/** Error HTTP como lo lanzaría Retrofit, con el cuerpo JSON de dina_backend. */
fun errorHttp(codigo: Int, json: String): HttpException =
    HttpException(Response.error<Any>(codigo, json.toResponseBody("application/json".toMediaType())))

/**
 * DataStore en memoria para las pruebas: se comporta igual que el real (lecturas como Flow,
 * escrituras de a una), pero sin archivo. El de archivo falla en Windows al reemplazar el
 * archivo en la segunda escritura ("Unable to rename …"), algo que en Android no pasa.
 */
class DataStoreEnMemoria : DataStore<Preferences> {
    private val estado = MutableStateFlow(emptyPreferences())
    private val escritura = Mutex()

    override val data: Flow<Preferences> = estado

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        escritura.withLock { transform(estado.value).also { estado.value = it } }
}
