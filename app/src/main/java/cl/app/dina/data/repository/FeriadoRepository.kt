package cl.app.dina.data.repository

import cl.app.dina.data.model.Feriado
import cl.app.dina.data.remote.FeriadosApi
import cl.app.dina.data.remote.dto.FeriadoDto
import java.time.LocalDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Feriados de Chile (API externa). Solo informa: no cambia las reglas de reserva de dina_backend. */
interface FeriadoRepository {
    /** Feriados del año, ordenados por fecha. */
    suspend fun feriadosDe(anio: Int): List<Feriado>

    /** El feriado de ese día, o null si es un día normal. */
    suspend fun feriadoEn(fecha: LocalDate): Feriado? = feriadosDe(fecha.year).firstOrNull { it.fecha == fecha }
}

/**
 * Pide los feriados a Nager.Date una vez por año y los guarda en memoria: cambiar de día en el
 * calendario no vuelve a llamar a la API.
 */
class RemoteFeriadoRepository(private val api: FeriadosApi) : FeriadoRepository {

    private val porAnio = mutableMapOf<Int, List<Feriado>>()
    private val candado = Mutex()

    override suspend fun feriadosDe(anio: Int): List<Feriado> = candado.withLock {
        porAnio[anio] ?: api.feriados(anio).mapNotNull { it.aModelo() }.sortedBy { it.fecha }
            .also { porAnio[anio] = it }
    }
}

/** null si la fecha no se puede leer (se ignora ese feriado en vez de fallar entero). */
fun FeriadoDto.aModelo(): Feriado? {
    val dia = runCatching { LocalDate.parse(fecha) }.getOrNull() ?: return null
    val texto = nombreLocal?.takeIf { it.isNotBlank() } ?: nombre?.takeIf { it.isNotBlank() } ?: "Feriado"
    return Feriado(dia, texto)
}
