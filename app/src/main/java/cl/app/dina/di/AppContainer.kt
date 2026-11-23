package cl.app.dina.di

import android.content.Context
import cl.app.dina.data.local.sesionDataStore
import cl.app.dina.data.remote.DinaApi
import cl.app.dina.data.remote.RetrofitClient
import cl.app.dina.data.repository.AuthRepository
import cl.app.dina.data.repository.EspacioRepository
import cl.app.dina.data.repository.RemoteAuthRepository
import cl.app.dina.data.repository.RemoteEspacioRepository
import cl.app.dina.data.repository.RemoteSolicitudRepository
import cl.app.dina.data.repository.SesionRepository
import cl.app.dina.data.repository.SolicitudRepository

/** Inyección de dependencias manual: el único lugar que crea los repositorios. */
interface AppContainer {
    val authRepository: AuthRepository
    val sesionRepository: SesionRepository
    val espacioRepository: EspacioRepository
    val solicitudRepository: SolicitudRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    override val sesionRepository: SesionRepository by lazy {
        SesionRepository(appContext.sesionDataStore)
    }

    /** Cliente de dina_backend (URL en app/build.gradle.kts → API_BASE_URL). */
    private val api: DinaApi by lazy { RetrofitClient.crear(sesionRepository) }

    // Para probar sin backend: FakeAuthRepository()
    override val authRepository: AuthRepository by lazy { RemoteAuthRepository(api) }

    override val espacioRepository: EspacioRepository by lazy { RemoteEspacioRepository(api) }

    override val solicitudRepository: SolicitudRepository by lazy { RemoteSolicitudRepository(api) }
}
