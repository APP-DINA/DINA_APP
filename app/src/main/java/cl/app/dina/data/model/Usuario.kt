package cl.app.dina.data.model

/**
 * Usuario con sesión iniciada.
 * @param rol tipo de cuenta según el dominio del correo (Estudiante / Docente).
 * @param esAdmin lo decide el backend: un administrador aprueba o rechaza solicitudes.
 * @param id id en el backend (0 en sesiones guardadas antes de que existiera; se completa con /me).
 * @param fotoUrl URL completa de la foto de perfil, o null si no tiene (se muestran las iniciales).
 */
data class Usuario(
    val correo: String,
    val nombre: String,
    val rol: Rol,
    val esAdmin: Boolean = false,
    val id: Long = 0,
    val fotoUrl: String? = null
) {
    /** Admin */
    val descripcionRol: String
        get() = if (esAdmin) "${rol.etiqueta} · Administrador" else rol.etiqueta
}
