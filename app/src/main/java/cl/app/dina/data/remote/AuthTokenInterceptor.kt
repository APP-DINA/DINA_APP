package cl.app.dina.data.remote

import cl.app.dina.data.repository.SesionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Agrega "Authorization: Bearer <token>" a cada petición si hay sesión guardada.
 * Si el backend responde 401 a una petición que llevaba token (sesión expirada o cerrada
 * en el servidor), borra la sesión del teléfono: la app vuelve sola al Login.
 *
 * Corre en los hilos de OkHttp (nunca en el hilo principal), por eso runBlocking es seguro aquí.
 */
class AuthTokenInterceptor(private val sesionRepository: SesionRepository) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { sesionRepository.token.first() }
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }

        val response = chain.proceed(request)
        if (response.code == 401 && !token.isNullOrBlank()) {
            runBlocking { sesionRepository.cerrar() }
        }
        return response
    }
}
