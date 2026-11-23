package cl.app.dina.utils

import cl.app.dina.data.model.Rol

/**
 * Mismo patrón que android.util.Patterns.EMAIL_ADDRESS, escrito en Kotlin para que la validación
 * también funcione en las pruebas unitarias (la JVM no tiene las clases de Android).
 */
private val FORMATO_CORREO = Regex(
    "[a-zA-Z0-9+._%\\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+"
)

/** Formato de correo válido (algo@algo.algo). */
fun esCorreoValido(correo: String): Boolean = FORMATO_CORREO.matches(correo.trim())

/** Error a mostrar bajo el campo correo, o null si está bien. */
fun errorCorreo(correo: String): String? = when {
    correo.isBlank() -> "Ingresa tu correo institucional"
    !esCorreoValido(correo) -> "El correo no tiene un formato válido"
    Rol.desdeCorreo(correo) == null ->
        "Usa tu cuenta " + Rol.dominiosPermitidos.joinToString(" o ") { "@$it" }
    else -> null
}

/** Error a mostrar bajo el campo contraseña, o null si está bien. */
fun errorPassword(password: String): String? = when {
    password.isBlank() -> "Ingresa tu contraseña"
    else -> null
}
