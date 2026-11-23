package cl.app.dina.data.remote

import cl.app.dina.data.remote.dto.EspacioDto
import cl.app.dina.data.remote.dto.LoginRequestDto
import cl.app.dina.data.remote.dto.LoginResponseDto
import cl.app.dina.data.remote.dto.UsuarioDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Endpoints de dina_backend. Retrofit escribe el código HTTP a partir de esta interfaz.
 * El token se agrega solo en AuthTokenInterceptor; aquí no hace falta pasarlo.
 */
interface DinaApi {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequestDto): LoginResponseDto

    @GET("api/auth/me")
    suspend fun me(): UsuarioDto

    @POST("api/auth/logout")
    suspend fun logout()

    /** Catálogo completo; la búsqueda y los filtros se hacen en la app. */
    @GET("api/espacios")
    suspend fun espacios(): List<EspacioDto>
}
