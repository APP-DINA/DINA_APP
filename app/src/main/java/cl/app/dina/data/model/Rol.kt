package cl.app.dina.data.model

/**
 * Rol del usuario dentro de DINA, deducido del dominio de su correo institucional.
 *
 * Para agregar un dominio nuevo basta con sumarlo a la lista del rol que corresponda,
 * o crear un rol nuevo aquí. El resto de la app (validación, login, sesión) lo toma solo.
 *
 * Ojo: el dominio solo sirve para validar y orientar al usuario en el login.
 * Quien decide el rol real será el BACKEND cuando exista.
 */
enum class Rol(val etiqueta: String, val dominios: List<String>) {
    ESTUDIANTE("Estudiante", listOf("duocuc.cl")),
    PROFESOR("Profesor", listOf("profesor.duoc.cl"));

    companion object {
        /** Todos los dominios aceptados, para mostrarlos en mensajes de error. */
        val dominiosPermitidos: List<String> get() = entries.flatMap { it.dominios }

        /**
         * Rol según lo que va después de la @, o null si el dominio no es institucional.
         * Compara el dominio completo: "x@falso.duocuc.cl" NO es válido.
         */
        fun desdeCorreo(correo: String): Rol? {
            val dominio = correo.trim().substringAfterLast('@', missingDelimiterValue = "").lowercase()
            if (dominio.isEmpty()) return null
            return entries.firstOrNull { dominio in it.dominios }
        }
    }
}
