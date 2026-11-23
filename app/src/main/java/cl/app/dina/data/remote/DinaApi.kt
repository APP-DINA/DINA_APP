package cl.app.dina.data.remote

import cl.app.dina.data.remote.dto.CrearEspacioDto
import cl.app.dina.data.remote.dto.CrearSolicitudDto
import cl.app.dina.data.remote.dto.DisponibilidadDto
import cl.app.dina.data.remote.dto.EspacioDto
import cl.app.dina.data.remote.dto.LoginRequestDto
import cl.app.dina.data.remote.dto.LoginResponseDto
import cl.app.dina.data.remote.dto.RechazarDto
import cl.app.dina.data.remote.dto.SolicitudDto
import cl.app.dina.data.remote.dto.UsuarioDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
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

    /** Solo ADMIN. Agrega un espacio reservable (409 si el código ya existe). */
    @POST("api/admin/espacios")
    suspend fun crearEspacio(@Body body: CrearEspacioDto): EspacioDto

    /** Estado de cada bloque del día. [fecha] = "AAAA-MM-DD". */
    @GET("api/espacios/{id}/disponibilidad")
    suspend fun disponibilidad(@Path("id") espacioId: Long, @Query("fecha") fecha: String): DisponibilidadDto

    /** Crea una solicitud de 1 a 4 bloques seguidos (queda PENDIENTE). */
    @POST("api/solicitudes")
    suspend fun crearSolicitud(@Body body: CrearSolicitudDto): SolicitudDto

    /** Todas las solicitudes del usuario con sesión (cualquier estado). */
    @GET("api/solicitudes/mias")
    suspend fun misSolicitudes(): List<SolicitudDto>

    /** Detalle de una solicitud: la propia, o cualquiera si es ADMIN. */
    @GET("api/solicitudes/{id}")
    suspend fun solicitud(@Path("id") id: Long): SolicitudDto

    /** El usuario cancela su solicitud (pendiente, o aprobada que aún no comienza). */
    @PATCH("api/solicitudes/{id}/cancelar")
    suspend fun cancelarSolicitud(@Path("id") id: Long): SolicitudDto

    /** Solo ADMIN. Aprueba y rechaza automáticamente las pendientes que chocan con ella. */
    @PATCH("api/admin/solicitudes/{id}/aprobar")
    suspend fun aprobarSolicitud(@Path("id") id: Long): SolicitudDto

    /** Solo ADMIN. */
    @PATCH("api/admin/solicitudes/{id}/rechazar")
    suspend fun rechazarSolicitud(@Path("id") id: Long, @Body body: RechazarDto): SolicitudDto

    /** Sube o reemplaza la foto de perfil propia (campo "foto"). Devuelve el usuario con su nueva fotoUrl. */
    @Multipart
    @PUT("api/usuarios/me/foto")
    suspend fun subirFoto(@Part foto: MultipartBody.Part): UsuarioDto

    /** Quita la foto de perfil propia (vuelven a verse las iniciales). */
    @DELETE("api/usuarios/me/foto")
    suspend fun borrarFoto(): UsuarioDto

    /** Solo ADMIN. [estado] = PENDIENTE, APROBADA, RECHAZADA o CANCELADA (null = todas). */
    @GET("api/admin/solicitudes")
    suspend fun solicitudesAdmin(@Query("estado") estado: String?): List<SolicitudDto>
}
