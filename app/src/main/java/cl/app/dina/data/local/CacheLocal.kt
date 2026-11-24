package cl.app.dina.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import cl.app.dina.data.repository.SesionRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

/**
 * Copia en el teléfono de la última respuesta del backend (solicitudes pendientes, chats y
 * conversaciones), para seguir mostrándolas cuando no hay conexión.
 *
 * - Guarda los DTO tal como llegan, en JSON (Gson, el mismo que usa Retrofit).
 * - Vive en el mismo archivo que la sesión ("sesion"): al cerrar sesión se borra junto con ella,
 *   así quien entre después en el mismo teléfono nunca ve datos de otra persona.
 */
class CacheLocal(private val dataStore: DataStore<Preferences>) {

    val gson = Gson()

    /** Guarda [valor] como JSON. Si ya no hay sesión (se cerró mientras llegaba la respuesta), no guarda. */
    suspend fun guardar(clave: String, valor: Any) {
        dataStore.edit { prefs ->
            if (prefs[SesionRepository.CLAVE_TOKEN].isNullOrBlank()) return@edit
            prefs[stringPreferencesKey(PREFIJO + clave)] = gson.toJson(valor)
        }
    }

    /** El JSON guardado en [clave], o null si no hay o no se pudo leer el archivo. */
    suspend fun leerJson(clave: String): String? =
        runCatching { dataStore.data.first()[stringPreferencesKey(PREFIJO + clave)] }.getOrNull()

    /** Lo guardado en [clave] convertido de vuelta a [T], o null si no hay. */
    suspend inline fun <reified T> leer(clave: String): T? {
        val json = leerJson(clave) ?: return null
        return runCatching { gson.fromJson<T>(json, object : TypeToken<T>() {}.type) }.getOrNull()
    }

    private companion object {
        /** Distingue estas claves de las de la sesión dentro del mismo archivo. */
        const val PREFIJO = "cache_"
    }
}
