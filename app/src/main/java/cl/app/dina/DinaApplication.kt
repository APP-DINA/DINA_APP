package cl.app.dina

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import cl.app.dina.di.AppContainer
import cl.app.dina.di.DefaultAppContainer

/**
 * Se crea una sola vez por proceso (antes que cualquier pantalla).
 * Aquí vive el AppContainer para que los repositorios, sobre todo la sesión,
 * sean únicos en toda la app y no se pierdan al rotar el teléfono.
 *
 * También configura Coil (AsyncImage): las fotos de perfil se descargan con el mismo
 * cliente HTTP de la API, que agrega el token de sesión.
 */
class DinaApplication : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { container.okHttpClient })) }
            .crossfade(true)
            .build()
}
