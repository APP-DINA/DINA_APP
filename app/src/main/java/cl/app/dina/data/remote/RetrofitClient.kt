package cl.app.dina.data.remote

import cl.app.dina.BuildConfig
import cl.app.dina.data.remote.dto.ErrorDto
import cl.app.dina.data.repository.SesionRepository
import com.google.gson.Gson
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Crea el cliente HTTP una sola vez (lo llama AppContainer). */
object RetrofitClient {

    /**
     * Cliente con el token de sesión. Lo comparten Retrofit y Coil (fotos de perfil),
     * porque las fotos también exigen sesión.
     */
    fun crearOkHttp(sesionRepository: SesionRepository): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(AuthTokenInterceptor(sesionRepository))
            .addInterceptor(HttpLoggingInterceptor().apply {
                // BASIC: método, URL, código y tiempo. No registra cuerpos (contraseñas) ni headers (token).
                // En Logcat, filtrar por "okhttp".
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    fun crear(okHttp: OkHttpClient): DinaApi =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DinaApi::class.java)
}

/**
 * Error del backend ya leído (el cuerpo de un HttpException solo se puede leer una vez).
 * [codigo] permite reaccionar a casos concretos, p. ej. [CRUCE_PROPIO].
 */
class ErrorDelBackend(mensaje: String, val codigo: String?) : Exception(mensaje) {
    companion object {
        /** El usuario ya tiene otra solicitud a esa hora: se le pregunta si quiere pedir igual. */
        const val CRUCE_PROPIO = "CRUCE_PROPIO"
    }
}

/** Convierte un HttpException en [ErrorDelBackend] (con su mensaje y código); otros errores, tal cual. */
fun Throwable.leerErrorDelBackend(): Throwable {
    if (this !is HttpException) return this
    val cuerpo = response()?.errorBody()?.string()
    val error = runCatching { Gson().fromJson(cuerpo, ErrorDto::class.java) }.getOrNull()
    return ErrorDelBackend(error?.mensaje ?: "El servidor respondió con un error (${code()})", error?.codigo)
}

/**
 * El backend no respondió (sin internet, servidor apagado, tiempo agotado). No incluye las
 * respuestas con error del backend: esas sí llegaron. Con esto se decide mostrar lo guardado.
 */
fun Throwable.esSinConexion(): Boolean = this is IOException

/** Convierte un error de red o del backend en un mensaje para mostrar al usuario. */
fun Throwable.aMensaje(): String = when (this) {
    is ErrorDelBackend -> message.orEmpty()
    is HttpException -> {
        val cuerpo = response()?.errorBody()?.string()
        val mensaje = runCatching { Gson().fromJson(cuerpo, ErrorDto::class.java)?.mensaje }.getOrNull()
        mensaje ?: "El servidor respondió con un error (${code()})"
    }
    is IOException -> "No se pudo conectar con el servidor. Revisa tu conexión"
    is NoSuchElementException -> message ?: "No se encontró lo que buscabas"
    else -> "Ocurrió un error inesperado"
}
