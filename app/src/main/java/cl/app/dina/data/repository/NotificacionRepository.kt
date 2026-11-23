package cl.app.dina.data.repository

import cl.app.dina.data.model.Notificacion
import cl.app.dina.data.remote.DinaApi
import cl.app.dina.data.remote.dto.aModelo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Avisos del usuario. Además guarda cuántos hay sin leer ([noLeidas]): lo muestra la campana de la
 * barra de abajo y se mantiene al día cada vez que se listan o se borran (abrir un aviso lo borra).
 * Las funciones lanzan una excepción si falla la red.
 */
interface NotificacionRepository {
    val noLeidas: StateFlow<Int>

    suspend fun listar(): List<Notificacion>

    /** Solo el número de no leídas (más liviano que [listar]). */
    suspend fun actualizarConteo()

    suspend fun borrar(notificacion: Notificacion)

    suspend fun borrarTodas()
}

class RemoteNotificacionRepository(private val api: DinaApi) : NotificacionRepository {

    private val _noLeidas = MutableStateFlow(0)
    override val noLeidas: StateFlow<Int> = _noLeidas.asStateFlow()

    override suspend fun listar(): List<Notificacion> =
        api.notificaciones().map { it.aModelo() }.also { lista -> _noLeidas.value = lista.count { !it.leida } }

    override suspend fun actualizarConteo() {
        _noLeidas.value = api.notificacionesNoLeidas().noLeidas.toInt()
    }

    override suspend fun borrar(notificacion: Notificacion) {
        api.borrarNotificacion(notificacion.id)
        if (!notificacion.leida) _noLeidas.update { (it - 1).coerceAtLeast(0) }
    }

    override suspend fun borrarTodas() {
        api.borrarNotificaciones()
        _noLeidas.value = 0
    }
}
