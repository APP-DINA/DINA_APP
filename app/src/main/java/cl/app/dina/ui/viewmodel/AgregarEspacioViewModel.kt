package cl.app.dina.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.app.dina.data.model.Edificio
import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.NuevoEspacio
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.data.remote.aMensaje
import cl.app.dina.data.repository.EspacioRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Formulario para agregar un espacio. Los números se guardan como texto (lo que escribe el usuario). */
data class AgregarEspacioUiState(
    val codigo: String = "",
    val nombre: String = "",
    val edificio: Edificio = Edificio.NORTE,
    val piso: String = "1",
    val tipo: TipoEspacio = TipoEspacio.SALA,
    val capacidad: String = "",
    /** Nombre del enum Escuela ("INFORMATICA"), o null = uso general. */
    val escuela: String? = null,
    val proyector: Boolean = false,
    val computadores: Boolean = false,
    val climatizacion: Boolean = false,
    val errorCodigo: String? = null,
    val errorNombre: String? = null,
    val errorPiso: String? = null,
    val errorCapacidad: String? = null,
    /** Error que viene del backend (p. ej. código repetido). */
    val errorGeneral: String? = null,
    val enviando: Boolean = false
) {
    /**
     * Cómo se verá en Reservar (tarjeta de vista previa). Si el nombre está vacío se usa
     * "Sala S99" / "Laboratorio LC9" según el tipo, igual que el que se enviará.
     */
    val vistaPrevia: Espacio
        get() = Espacio(
            id = 0,
            codigo = codigo.trim().uppercase().ifEmpty { "—" },
            nombre = nombreFinal.ifEmpty { "Nuevo espacio" },
            edificio = edificio.name,
            edificioNombre = edificio.nombre,
            piso = piso.toIntOrNull() ?: 0,
            tipo = tipo,
            capacidad = capacidad.toIntOrNull() ?: 0,
            escuela = escuela,
            proyector = proyector,
            computadores = computadores,
            climatizacion = climatizacion
        )

    /** Nombre escrito, o "Tipo CÓDIGO" si se dejó vacío ("Sala S99"). */
    val nombreFinal: String
        get() = nombre.trim().ifEmpty {
            codigo.trim().uppercase().takeIf { it.isNotEmpty() }?.let { "${tipo.etiqueta} $it" }.orEmpty()
        }
}

sealed interface AgregarEspacioEvento {
    data class Agregado(val espacio: Espacio) : AgregarEspacioEvento
}

/** Agregar espacio (solo ADMIN): valida igual que el backend y lo envía. */
class AgregarEspacioViewModel(private val espacioRepository: EspacioRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AgregarEspacioUiState())
    val uiState: StateFlow<AgregarEspacioUiState> = _uiState.asStateFlow()

    private val _eventos = Channel<AgregarEspacioEvento>(Channel.BUFFERED)
    val eventos: Flow<AgregarEspacioEvento> = _eventos.receiveAsFlow()

    // ── Campos ──────────────────────────────────────────────────────────

    fun onCodigoChange(texto: String) = _uiState.update {
        it.copy(codigo = texto.uppercase().take(MAX_CODIGO), errorCodigo = null, errorGeneral = null)
    }

    fun onNombreChange(texto: String) = _uiState.update {
        it.copy(nombre = texto.take(MAX_NOMBRE), errorNombre = null, errorGeneral = null)
    }

    fun onEdificioChange(edificio: Edificio) = _uiState.update { it.copy(edificio = edificio) }

    /** Acepta un "-" inicial (subterráneo) y hasta 2 dígitos. */
    fun onPisoChange(texto: String) = _uiState.update {
        val negativo = texto.startsWith("-")
        val digitos = texto.filter(Char::isDigit).take(2)
        it.copy(piso = (if (negativo) "-" else "") + digitos, errorPiso = null)
    }

    fun onTipoChange(tipo: TipoEspacio) = _uiState.update { it.copy(tipo = tipo) }

    fun onCapacidadChange(texto: String) = _uiState.update {
        it.copy(capacidad = texto.filter(Char::isDigit).take(3), errorCapacidad = null)
    }

    fun onEscuelaChange(escuela: String?) = _uiState.update { it.copy(escuela = escuela) }

    fun onProyectorChange(valor: Boolean) = _uiState.update { it.copy(proyector = valor) }
    fun onComputadoresChange(valor: Boolean) = _uiState.update { it.copy(computadores = valor) }
    fun onClimatizacionChange(valor: Boolean) = _uiState.update { it.copy(climatizacion = valor) }

    // ── Guardar ─────────────────────────────────────────────────────────

    fun agregar() {
        val s = _uiState.value
        if (s.enviando) return

        // 1. Validar en el teléfono (mismas reglas que el backend)
        val piso = s.piso.toIntOrNull()?.takeIf { it in PISOS }
        val capacidad = s.capacidad.toIntOrNull()?.takeIf { it in CAPACIDADES }
        val errorCodigo = "Escribe el código del espacio (ej.: S41)".takeIf { s.codigo.isBlank() }
        val errorNombre = "Escribe el nombre o completa el código".takeIf { s.nombreFinal.isEmpty() }
        val errorPiso = "Piso entre ${PISOS.first} y ${PISOS.last}".takeIf { piso == null }
        val errorCapacidad = "Entre ${CAPACIDADES.first} y ${CAPACIDADES.last} personas".takeIf { capacidad == null }
        if (piso == null || capacidad == null || errorCodigo != null || errorNombre != null) {
            _uiState.update {
                it.copy(
                    errorCodigo = errorCodigo,
                    errorNombre = errorNombre,
                    errorPiso = errorPiso,
                    errorCapacidad = errorCapacidad
                )
            }
            return
        }

        // 2. Enviar al backend
        _uiState.update { it.copy(enviando = true, errorGeneral = null) }
        val nuevo = NuevoEspacio(
            codigo = s.codigo.trim(),
            nombre = s.nombreFinal,
            edificio = s.edificio,
            piso = piso,
            tipo = s.tipo,
            capacidad = capacidad,
            escuela = s.escuela,
            proyector = s.proyector,
            computadores = s.computadores,
            climatizacion = s.climatizacion
        )
        viewModelScope.launch {
            intentar { espacioRepository.crear(nuevo) }
                .onSuccess { _eventos.send(AgregarEspacioEvento.Agregado(it)) }
                .onFailure { e -> _uiState.update { it.copy(errorGeneral = e.aMensaje()) } }
            _uiState.update { it.copy(enviando = false) }
        }
    }

    companion object {
        /** Igual que el backend (CrearEspacioRequest). */
        const val MAX_CODIGO = 30
        const val MAX_NOMBRE = 120
        val PISOS = -2..10
        val CAPACIDADES = 1..500
    }
}
