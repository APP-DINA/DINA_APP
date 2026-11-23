package cl.app.dina.data.remote

import cl.app.dina.data.remote.dto.CrearSolicitudDto
import cl.app.dina.data.remote.dto.DisponibilidadDto
import cl.app.dina.data.remote.dto.EspacioDto
import cl.app.dina.data.remote.dto.LoginRequestDto
import cl.app.dina.data.remote.dto.LoginResponseDto
import cl.app.dina.data.remote.dto.SolicitudDto
import cl.app.dina.data.remote.dto.UsuarioDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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

    /** Estado de cada bloque del día. [fecha] = "AAAA-MM-DD". */
    @GET("api/espacios/{id}/disponibilidad")
    suspend fun disponibilidad(@Path("id") espacioId: Long, @Query("fecha") fecha: String): DisponibilidadDto

    /** Crea una solicitud de 1 a 4 bloques seguidos (queda PENDIENTE). */
    @POST("api/solicitudes")
    suspend fun crearSolicitud(@Body body: CrearSolicitudDto): SolicitudDto
}
