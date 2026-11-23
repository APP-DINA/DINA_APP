package cl.app.dina.data.remote

import cl.app.dina.data.remote.dto.FeriadoDto
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * API EXTERNA (de terceros): feriados de Chile desde Nager.Date (https://date.nager.at).
 * Es pública, gratuita y no pide clave. No tiene nada que ver con dina_backend (el microservicio
 * propio): usa su propia URL y su propio cliente HTTP, sin el token de sesión de DINA.
 */
interface FeriadosApi {

    /** GET https://date.nager.at/api/v3/PublicHolidays/{anio}/CL */
    @GET("api/v3/PublicHolidays/{anio}/CL")
    suspend fun feriados(@Path("anio") anio: Int): List<FeriadoDto>

    companion object {
        const val URL_BASE = "https://date.nager.at/"

        /**
         * Cliente aparte del de dina_backend: así el token de sesión nunca viaja a un servidor
         * de terceros, y una falla de esta API no afecta al resto de la app.
         */
        fun crear(): FeriadosApi =
            Retrofit.Builder()
                .baseUrl(URL_BASE)
                .client(
                    OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .build()
                )
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FeriadosApi::class.java)
    }
}
