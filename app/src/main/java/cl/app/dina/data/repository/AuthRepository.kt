package cl.app.dina.data.repository

import cl.app.dina.data.model.Rol
import cl.app.dina.data.model.Usuario
import kotlinx.coroutines.delay

/** Resultado de intentar iniciar sesión. */
sealed interface ResultadoLogin {
    /** [token] lo entregará el backend; se guarda para autenticar las próximas peticiones. */
    data class Exito(val usuario: Usuario, val token: String) : ResultadoLogin
    data class Error(val mensaje: String) : ResultadoLogin
}

/**
 * Autenticación contra el backend.
 * La app solo conoce esta interfaz; AppContainer decide si usa el backend real
 * (RemoteAuthRepository) o el simulado (FakeAuthRepository). ViewModels y pantallas no cambian.
 */
interface AuthRepository {
    suspend fun login(correo: String, password: String): ResultadoLogin

    /** Invalida el token en el servidor. Si no hay conexión, no lanza error. */
    suspend fun logout()

    /** Datos actualizados del usuario con sesión, o null si no se pudo consultar. */
    suspend fun usuarioActual(): Usuario?
}

/**
 * Backend SIMULADO, para probar la app sin levantar dina_backend.
 * Usuarios ficticios (Ley 19.628 / 21.719: no usar datos reales en pruebas).
 */
class FakeAuthRepository : AuthRepository {

    private data class CuentaDemo(val nombre: String, val password: String)

    private val cuentas = mapOf(
        "estudiante.prueba@duocuc.cl" to CuentaDemo("Estudiante de Prueba", "duoc1234"),
        "profesor.prueba@profesor.duoc.cl" to CuentaDemo("Profesor de Prueba", "duoc1234")
    )

    override suspend fun login(correo: String, password: String): ResultadoLogin {
        delay(800) // simula la demora de la red
        val email = correo.trim().lowercase()
        val rol = Rol.desdeCorreo(email)
            ?: return ResultadoLogin.Error("Dominio no autorizado")
        val cuenta = cuentas[email]
        if (cuenta == null || cuenta.password != password) {
            return ResultadoLogin.Error("Correo o contraseña incorrectos")
        }
        return ResultadoLogin.Exito(
            usuario = Usuario(correo = email, nombre = cuenta.nombre, rol = rol),
            token = "token-demo-$email"
        )
    }

    override suspend fun logout() = Unit

    override suspend fun usuarioActual(): Usuario? = null
}
