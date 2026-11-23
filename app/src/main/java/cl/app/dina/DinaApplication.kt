package cl.app.dina

import android.app.Application
import cl.app.dina.di.AppContainer
import cl.app.dina.di.DefaultAppContainer

/**
 * Se crea una sola vez por proceso (antes que cualquier pantalla).
 * Aquí vive el AppContainer para que los repositorios, sobre todo la sesión,
 * sean únicos en toda la app y no se pierdan al rotar el teléfono.
 */
class DinaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
