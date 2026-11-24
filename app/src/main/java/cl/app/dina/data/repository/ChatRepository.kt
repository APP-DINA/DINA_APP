package cl.app.dina.data.repository

import cl.app.dina.data.local.CacheLocal
import cl.app.dina.data.model.Chat
import cl.app.dina.data.model.Conversacion
import cl.app.dina.data.model.Mensaje
import cl.app.dina.data.remote.DinaApi
import cl.app.dina.data.remote.dto.ChatDetalleDto
import cl.app.dina.data.remote.dto.ChatDto
import cl.app.dina.data.remote.dto.EnviarMensajeDto
import cl.app.dina.data.remote.dto.MensajeDto
import cl.app.dina.data.remote.dto.aModelo

/** Chats de las solicitudes aprobadas. Todas las funciones lanzan una excepción si falla la red. */
interface ChatRepository {
    /** Abiertos y cerrados, del de actividad más reciente al más antiguo. */
    suspend fun chats(): List<Chat>

    /** El chat y sus mensajes; con [despuesDe] (id del último que ya se tiene) solo los nuevos. */
    suspend fun conversacion(solicitudId: Long, despuesDe: Long = 0): Conversacion

    suspend fun enviar(solicitudId: Long, texto: String): Mensaje

    /** Solo ADMIN: lo pasa a "Cerrados". */
    suspend fun cerrar(solicitudId: Long): Chat

    /** Sin conexión: la última lista de [chats] guardada en el teléfono, o null si no hay. */
    suspend fun chatsGuardados(): List<Chat>? = null

    /** Sin conexión: la conversación completa guardada en el teléfono, o null si nunca se abrió. */
    suspend fun conversacionGuardada(solicitudId: Long): Conversacion? = null
}

/** [cache]: copia local de chats y conversaciones para leerlos sin conexión (null = no se guarda nada). */
class RemoteChatRepository(
    private val api: DinaApi,
    private val cache: CacheLocal? = null
) : ChatRepository {

    override suspend fun chats(): List<Chat> =
        api.chats().also { cache?.guardar(CACHE_CHATS, it) }.map { it.aModelo() }

    override suspend fun conversacion(solicitudId: Long, despuesDe: Long): Conversacion {
        val r = api.chat(solicitudId, despuesDe)
        guardarConversacion(solicitudId, r.chat, r.mensajes, completa = despuesDe == 0L)
        return Conversacion(r.chat.aModelo(), r.mensajes.map { it.aModelo() })
    }

    override suspend fun enviar(solicitudId: Long, texto: String): Mensaje {
        val m = api.enviarMensaje(solicitudId, EnviarMensajeDto(texto.trim()))
        // La revisión siguiente pide solo los posteriores a este: se agrega aquí para que quede guardado
        cache?.leer<ChatDetalleDto>(claveConversacion(solicitudId))?.let { guardada ->
            guardarConversacion(solicitudId, guardada.chat, listOf(m), completa = false)
        }
        return m.aModelo()
    }

    override suspend fun cerrar(solicitudId: Long): Chat {
        val chat = api.cerrarChat(solicitudId)
        guardarConversacion(solicitudId, chat, emptyList(), completa = false)
        return chat.aModelo()
    }

    override suspend fun chatsGuardados(): List<Chat>? =
        cache?.leer<List<ChatDto>>(CACHE_CHATS)?.map { it.aModelo() }

    override suspend fun conversacionGuardada(solicitudId: Long): Conversacion? =
        cache?.leer<ChatDetalleDto>(claveConversacion(solicitudId))
            ?.let { Conversacion(it.chat.aModelo(), it.mensajes.map { m -> m.aModelo() }) }

    /**
     * [completa] = true: la conversación entera (reemplaza lo guardado). Si no, son los mensajes
     * nuevos y se suman a los guardados; sin una conversación completa guardada no se guarda un pedazo.
     */
    private suspend fun guardarConversacion(
        solicitudId: Long,
        chat: ChatDto,
        mensajes: List<MensajeDto>,
        completa: Boolean
    ) {
        val cache = cache ?: return
        val clave = claveConversacion(solicitudId)
        val anteriores = if (completa) {
            emptyList()
        } else {
            val guardada = cache.leer<ChatDetalleDto>(clave) ?: return
            // La revisión cada 5 s casi siempre no trae nada nuevo: no se reescribe el archivo
            if (mensajes.isEmpty() && guardada.chat == chat) return
            guardada.mensajes
        }
        val todos = (anteriores + mensajes).distinctBy { it.id }.sortedBy { it.id }
        cache.guardar(clave, ChatDetalleDto(chat, todos))
    }

    private fun claveConversacion(solicitudId: Long) = "chat_$solicitudId"

    private companion object {
        const val CACHE_CHATS = "chats"
    }
}
