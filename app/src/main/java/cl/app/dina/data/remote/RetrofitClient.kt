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

    fun crear(sesionRepository: SesionRepository): DinaApi {
        val okHttp = OkHttpClient.Builder()
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

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DinaApi::class.java)
    }
}

/** Convierte un error de red o del backend en un mensaje para mostrar al usuario. */
fun Throwable.aMensaje(): String = when (this) {
    is HttpException -> {
        val cuerpo = response()?.errorBody()?.string()
        val mensaje = runCatching { Gson().fromJson(cuerpo, ErrorDto::class.java)?.mensaje }.getOrNull()
        mensaje ?: "El servidor respondió con un error (${code()})"
    }
    is IOException -> "No se pudo conectar con el servidor. Revisa tu conexión"
    is NoSuchElementException -> message ?: "No se encontró lo que buscabas"
    else -> "Ocurrió un error inesperado"
}
