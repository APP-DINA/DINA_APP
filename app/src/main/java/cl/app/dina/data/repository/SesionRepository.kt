package cl.app.dina.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import cl.app.dina.data.model.Rol
import cl.app.dina.data.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Guarda en el teléfono quién inició sesión, para no pedir login cada vez que se abre la app.
 * No guarda la contraseña: solo los datos del usuario y el token que entrega el backend.
 */
class SesionRepository(private val dataStore: DataStore<Preferences>) {

    private object Claves {
        val CORREO = stringPreferencesKey("correo")
        val NOMBRE = stringPreferencesKey("nombre")
        val ROL = stringPreferencesKey("rol")
        val TOKEN = stringPreferencesKey("token")
        val ADMIN = booleanPreferencesKey("admin")
    }

    /** Usuario con sesión, o null si no hay. Se re-emite al iniciar o cerrar sesión. */
    val usuario: Flow<Usuario?> = dataStore.data.map { prefs ->
        val correo = prefs[Claves.CORREO] ?: return@map null
        val nombre = prefs[Claves.NOMBRE] ?: return@map null
        // Sin token no hay sesión válida con el backend
        if (prefs[Claves.TOKEN].isNullOrBlank()) return@map null
        // Si el rol guardado ya no existe (p. ej. se renombró), se trata como sin sesión
        val rol = prefs[Claves.ROL]?.let { r -> Rol.entries.firstOrNull { it.name == r } }
            ?: return@map null
        Usuario(correo = correo, nombre = nombre, rol = rol, esAdmin = prefs[Claves.ADMIN] ?: false)
    }

    /** Token para las peticiones al backend (lo agrega AuthTokenInterceptor). */
    val token: Flow<String?> = dataStore.data.map { it[Claves.TOKEN] }

    suspend fun iniciar(usuario: Usuario, token: String) {
        dataStore.edit { prefs ->
            prefs[Claves.CORREO] = usuario.correo
            prefs[Claves.NOMBRE] = usuario.nombre
            prefs[Claves.ROL] = usuario.rol.name
            prefs[Claves.TOKEN] = token
            prefs[Claves.ADMIN] = usuario.esAdmin
        }
    }

    /** Actualiza nombre y permisos (p. ej. si el backend cambió el rol), manteniendo el token. */
    suspend fun actualizarUsuario(usuario: Usuario) {
        dataStore.edit { prefs ->
            if (prefs[Claves.TOKEN].isNullOrBlank()) return@edit // se cerró la sesión mientras tanto
            prefs[Claves.NOMBRE] = usuario.nombre
            prefs[Claves.ROL] = usuario.rol.name
            prefs[Claves.ADMIN] = usuario.esAdmin
        }
    }

    suspend fun cerrar() {
        dataStore.edit { it.clear() }
    }
}
