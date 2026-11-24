package cl.app.dina.di

import android.content.Context
import cl.app.dina.data.local.CacheLocal
import cl.app.dina.data.local.preferenciasDataStore
import cl.app.dina.data.local.sesionDataStore
import cl.app.dina.data.remote.DinaApi
import cl.app.dina.data.remote.FeriadosApi
import cl.app.dina.data.remote.RetrofitClient
import cl.app.dina.data.repository.AuthRepository
import cl.app.dina.data.repository.ChatRepository
import cl.app.dina.data.repository.EspacioRepository
import cl.app.dina.data.repository.FeriadoRepository
import cl.app.dina.data.repository.NotificacionRepository
import cl.app.dina.data.repository.PerfilRepository
import cl.app.dina.data.repository.PreferenciasRepository
import cl.app.dina.data.repository.RemoteAuthRepository
import cl.app.dina.data.repository.RemoteChatRepository
import cl.app.dina.data.repository.RemoteEspacioRepository
import cl.app.dina.data.repository.RemoteFeriadoRepository
import cl.app.dina.data.repository.RemoteNotificacionRepository
import cl.app.dina.data.repository.RemotePerfilRepository
import cl.app.dina.data.repository.RemoteSolicitudRepository
import cl.app.dina.data.repository.SesionRepository
import cl.app.dina.data.repository.SolicitudRepository
import okhttp3.OkHttpClient

/** Inyección de dependencias manual: el único lugar que crea los repositorios. */
interface AppContainer {
    val authRepository: AuthRepository
    val sesionRepository: SesionRepository
    val espacioRepository: EspacioRepository
    val solicitudRepository: SolicitudRepository
    val perfilRepository: PerfilRepository
    val preferenciasRepository: PreferenciasRepository
    val chatRepository: ChatRepository
    val notificacionRepository: NotificacionRepository

    /** API externa (Nager.Date): feriados de Chile. */
    val feriadoRepository: FeriadoRepository

    /** Cliente HTTP con el token de sesión: lo usa también Coil para cargar las fotos de perfil. */
    val okHttpClient: OkHttpClient
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    override val sesionRepository: SesionRepository by lazy {
        SesionRepository(appContext.sesionDataStore)
    }

    override val preferenciasRepository: PreferenciasRepository by lazy {
        PreferenciasRepository(appContext.preferenciasDataStore)
    }

    override val okHttpClient: OkHttpClient by lazy { RetrofitClient.crearOkHttp(sesionRepository) }

    /** Cliente de dina_backend (URL en app/build.gradle.kts → API_BASE_URL). */
    private val api: DinaApi by lazy { RetrofitClient.crear(okHttpClient) }

    // Para probar sin backend: FakeAuthRepository()
    override val authRepository: AuthRepository by lazy { RemoteAuthRepository(api) }

    override val espacioRepository: EspacioRepository by lazy { RemoteEspacioRepository(api) }

    /** Copia local para ver solicitudes y chats sin conexión (en el archivo de la sesión: se borra con ella). */
    private val cache: CacheLocal by lazy { CacheLocal(appContext.sesionDataStore) }

    override val solicitudRepository: SolicitudRepository by lazy { RemoteSolicitudRepository(api, cache) }

    override val chatRepository: ChatRepository by lazy { RemoteChatRepository(api, cache) }

    override val notificacionRepository: NotificacionRepository by lazy { RemoteNotificacionRepository(api) }

    /** API externa de feriados: cliente propio, sin el token de dina_backend. */
    override val feriadoRepository: FeriadoRepository by lazy { RemoteFeriadoRepository(FeriadosApi.crear()) }

    override val perfilRepository: PerfilRepository by lazy {
        RemotePerfilRepository(api, sesionRepository, appContext.contentResolver)
    }
}
