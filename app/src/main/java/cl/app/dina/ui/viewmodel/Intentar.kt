package cl.app.dina.ui.viewmodel

import kotlinx.coroutines.CancellationException

/**
 * Como runCatching, pero deja pasar la cancelación de la corrutina (cambiar de pantalla o de día
 * cancela la petición anterior: eso no es un error que haya que mostrar).
 *
 * intentar { repo.algo() }
 *     .onSuccess { … }
 *     .onFailure { e -> mostrar(e.aMensaje()) }
 */
suspend inline fun <T> intentar(accion: suspend () -> T): Result<T> =
    try {
        Result.success(accion())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
