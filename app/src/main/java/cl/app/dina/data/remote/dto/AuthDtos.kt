package cl.app.dina.data.remote.dto

import com.google.gson.annotations.SerializedName

/*
 * DTOs: clases que calzan exactamente con el JSON de dina_backend.
 * Solo se usan en data/remote; el resto de la app trabaja con data/model.
 */

data class LoginRequestDto(
    @SerializedName("correo") val correo: String,
    @SerializedName("password") val password: String
)

data class LoginResponseDto(
    @SerializedName("token") val token: String,
    @SerializedName("expiraEn") val expiraEn: String?,
    @SerializedName("usuario") val usuario: UsuarioDto
)

data class UsuarioDto(
    @SerializedName("id") val id: Long,
    @SerializedName("correo") val correo: String,
    @SerializedName("nombre") val nombre: String,
    /** "ADMIN" o "USUARIO" */
    @SerializedName("rol") val rol: String,
    /** Ruta relativa ("api/usuarios/5/foto?v=…") o null si no tiene foto. */
    @SerializedName("fotoUrl") val fotoUrl: String?
)

/** Forma de todos los errores del backend: { estado, error, mensaje, fecha } */
data class ErrorDto(
    @SerializedName("estado") val estado: Int?,
    @SerializedName("mensaje") val mensaje: String?
)
