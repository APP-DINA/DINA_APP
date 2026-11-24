package cl.app.dina.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import cl.app.dina.data.model.Rol
import cl.app.dina.data.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Guarda en el teléfono quién inició sesión, para no pedir login cada vez que se abre la app.
 * No guarda la contraseña: solo los datos del usuario y el token que entrega el backend.
 */
class SesionRepository(private val dataStore: DataStore<Preferences>) {

    private object Claves {
        val ID = longPreferencesKey("id")
        val CORREO = stringPreferencesKey("correo")
        val NOMBRE = stringPreferencesKey("nombre")
        val ROL = stringPreferencesKey("rol")
        val TOKEN = CLAVE_TOKEN
        val ADMIN = booleanPreferencesKey("admin")
        val FOTO = stringPreferencesKey("foto")
    }

    /** Usuario con sesión, o null si no hay. Se re-emite al iniciar o cerrar sesión, o al cambiar la foto. */
    val usuario: Flow<Usuario?> = dataStore.data.map { prefs ->
        val correo = prefs[Claves.CORREO] ?: return@map null
        val nombre = prefs[Claves.NOMBRE] ?: return@map null
        // Sin token no hay sesión válida con el backend
        if (prefs[Claves.TOKEN].isNullOrBlank()) return@map null
        // Si el rol guardado ya no existe (p. ej. se renombró), se trata como sin sesión
        val rol = prefs[Claves.ROL]?.let { r -> Rol.entries.firstOrNull { it.name == r } }
            ?: return@map null
        Usuario(
            correo = correo,
            nombre = nombre,
            rol = rol,
            esAdmin = prefs[Claves.ADMIN] ?: false,
            id = prefs[Claves.ID] ?: 0,
            fotoUrl = prefs[Claves.FOTO]
        )
    }.distinctUntilChanged() // la copia local (CacheLocal) comparte el archivo: solo avisa si cambió el usuario

    /** Token para las peticiones al backend (lo agrega AuthTokenInterceptor). */
    val token: Flow<String?> = dataStore.data.map { it[Claves.TOKEN] }

    suspend fun iniciar(usuario: Usuario, token: String) {
        dataStore.edit { prefs ->
            prefs[Claves.CORREO] = usuario.correo
            prefs[Claves.TOKEN] = token
            prefs.guardar(usuario)
        }
    }

    /** Actualiza nombre, permisos y foto (p. ej. si el backend cambió el rol), manteniendo el token. */
    suspend fun actualizarUsuario(usuario: Usuario) {
        dataStore.edit { prefs ->
            if (prefs[Claves.TOKEN].isNullOrBlank()) return@edit // se cerró la sesión mientras tanto
            prefs.guardar(usuario)
        }
    }

    suspend fun cerrar() {
        dataStore.edit { it.clear() }
    }

    /** Datos que pueden cambiar durante la sesión (el correo y el token no). */
    private fun MutablePreferences.guardar(usuario: Usuario) {
        this[Claves.ID] = usuario.id
        this[Claves.NOMBRE] = usuario.nombre
        this[Claves.ROL] = usuario.rol.name
        this[Claves.ADMIN] = usuario.esAdmin
        usuario.fotoUrl?.let { this[Claves.FOTO] = it } ?: remove(Claves.FOTO)
    }

    companion object {
        /** También la usa CacheLocal: sin token no guarda nada. */
        val CLAVE_TOKEN = stringPreferencesKey("token")
    }
}
