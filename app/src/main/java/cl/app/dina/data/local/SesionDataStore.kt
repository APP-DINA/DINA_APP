package cl.app.dina.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Archivo de preferencias "sesion" en el almacenamiento interno de la app.
 * Se declara una sola vez (a nivel de archivo) para que exista una única instancia.
 */
val Context.sesionDataStore: DataStore<Preferences> by preferencesDataStore(name = "sesion")
