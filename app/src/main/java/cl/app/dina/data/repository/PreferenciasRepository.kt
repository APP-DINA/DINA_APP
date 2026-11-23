package cl.app.dina.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import cl.app.dina.data.model.ModoTema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Preferencias de la app en este teléfono (por ahora, el tema). Se mantienen al cerrar sesión. */
class PreferenciasRepository(private val dataStore: DataStore<Preferences>) {

    private val claveTema = stringPreferencesKey("modo_tema")

    /** Tema elegido en el Menú; SISTEMA si nunca se eligió. */
    val modoTema: Flow<ModoTema> = dataStore.data.map { prefs ->
        prefs[claveTema]?.let { guardado -> ModoTema.entries.firstOrNull { it.name == guardado } } ?: ModoTema.SISTEMA
    }

    suspend fun guardarModoTema(modo: ModoTema) {
        dataStore.edit { it[claveTema] = modo.name }
    }
}
