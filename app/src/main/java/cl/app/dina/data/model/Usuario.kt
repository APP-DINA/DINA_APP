package cl.app.dina.data.model

/**
 * Usuario con sesión iniciada.
 * @param rol tipo de cuenta según el dominio del correo (Estudiante / Profesor).
 * @param esAdmin lo decide el backend: un administrador aprueba o rechaza solicitudes.
 */
data class Usuario(
    val correo: String,
    val nombre: String,
    val rol: Rol,
    val esAdmin: Boolean = false
)
