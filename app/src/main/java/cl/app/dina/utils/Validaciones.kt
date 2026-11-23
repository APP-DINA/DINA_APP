package cl.app.dina.utils

import android.util.Patterns
import cl.app.dina.data.model.Rol

/** Formato de correo válido (algo@algo.algo). */
fun esCorreoValido(correo: String): Boolean =
    Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()

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
