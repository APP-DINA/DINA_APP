package cl.app.dina.data.repository

import cl.app.dina.data.model.Chat
import cl.app.dina.data.model.Conversacion
import cl.app.dina.data.model.Mensaje
import cl.app.dina.data.remote.DinaApi
import cl.app.dina.data.remote.dto.EnviarMensajeDto
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
}

class RemoteChatRepository(private val api: DinaApi) : ChatRepository {

    override suspend fun chats(): List<Chat> = api.chats().map { it.aModelo() }

    override suspend fun conversacion(solicitudId: Long, despuesDe: Long): Conversacion {
        val r = api.chat(solicitudId, despuesDe)
        return Conversacion(r.chat.aModelo(), r.mensajes.map { it.aModelo() })
    }

    override suspend fun enviar(solicitudId: Long, texto: String): Mensaje =
        api.enviarMensaje(solicitudId, EnviarMensajeDto(texto.trim())).aModelo()

    override suspend fun cerrar(solicitudId: Long): Chat = api.cerrarChat(solicitudId).aModelo()
}
