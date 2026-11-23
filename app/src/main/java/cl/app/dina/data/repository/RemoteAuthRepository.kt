package cl.app.dina.data.repository

import cl.app.dina.data.model.Rol
import cl.app.dina.data.model.Usuario
import cl.app.dina.data.remote.DinaApi
import cl.app.dina.data.remote.aMensaje
import cl.app.dina.data.remote.dto.LoginRequestDto
import cl.app.dina.data.remote.dto.UsuarioDto
import kotlinx.coroutines.CancellationException

/** Autenticación real contra dina_backend (POST /api/auth/login, /me, /logout). */
class RemoteAuthRepository(private val api: DinaApi) : AuthRepository {

    override suspend fun login(correo: String, password: String): ResultadoLogin = try {
        val respuesta = api.login(LoginRequestDto(correo.trim().lowercase(), password))
        val usuario = respuesta.usuario.aModelo()
        if (usuario == null) {
            ResultadoLogin.Error("Tu cuenta no tiene un dominio institucional válido")
        } else {
            ResultadoLogin.Exito(usuario = usuario, token = respuesta.token)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // 401 "Correo o contraseña incorrectos", 400 dominio, sin conexión, etc.
        ResultadoLogin.Error(e.aMensaje())
    }

    override suspend fun logout() {
        try {
            api.logout()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Sin conexión o token ya inválido: igual se cierra la sesión en el teléfono
        }
    }

    override suspend fun usuarioActual(): Usuario? = try {
        api.me().aModelo()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null // un 401 ya lo maneja AuthTokenInterceptor cerrando la sesión
    }
}

/** DTO del backend → modelo de la app. El tipo de cuenta sale del dominio; el permiso, del backend. */
private fun UsuarioDto.aModelo(): Usuario? {
    val tipoCuenta = Rol.desdeCorreo(correo) ?: return null
    return Usuario(correo = correo, nombre = nombre, rol = tipoCuenta, esAdmin = rol == "ADMIN")
}
