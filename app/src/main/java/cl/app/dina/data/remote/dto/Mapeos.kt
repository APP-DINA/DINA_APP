package cl.app.dina.data.remote.dto

import cl.app.dina.BuildConfig
import cl.app.dina.data.model.Espacio
import cl.app.dina.data.model.EstadoSolicitud
import cl.app.dina.data.model.Rol
import cl.app.dina.data.model.Solicitante
import cl.app.dina.data.model.SolicitudResumen
import cl.app.dina.data.model.TipoEspacio
import cl.app.dina.data.model.Usuario
import java.time.LocalDate
import java.time.LocalDateTime

/*
 * DTO del backend → modelo de la app. Un solo lugar para cada conversión:
 * los repositorios las comparten (p. ej. el espacio viene en el catálogo y dentro de cada solicitud).
 */

/** Usuario con sesión, o null si el dominio del correo no es institucional. El permiso de admin lo da el backend. */
fun UsuarioDto.aModelo(): Usuario? {
    val tipoCuenta = Rol.desdeCorreo(correo) ?: return null
    return Usuario(
        correo = correo,
        nombre = nombre,
        rol = tipoCuenta,
        esAdmin = rol == "ADMIN",
        id = id,
        fotoUrl = fotoUrl?.urlDelBackend()
    )
}

fun EspacioDto.aModelo() = Espacio(
    id = id,
    codigo = codigo,
    nombre = nombre,
    edificio = edificio,
    edificioNombre = edificioNombre,
    piso = piso,
    tipo = TipoEspacio.desde(tipo),
    capacidad = capacidad,
    escuela = escuela,
    proyector = proyector,
    computadores = computadores,
    climatizacion = climatizacion
)

fun SolicitudDto.aResumen() = SolicitudResumen(
    id = id,
    espacio = espacio.aModelo(),
    fecha = LocalDate.parse(fecha),
    inicio = horaInicio.hhmm(),
    fin = horaFin.hhmm(),
    motivo = actividad,
    asistentes = asistentes,
    solicitante = Solicitante(
        id = solicitante.id,
        nombre = solicitante.nombre,
        rol = Rol.desdeCorreo(solicitante.correo),
        fotoUrl = solicitante.fotoUrl?.urlDelBackend(),
        correo = solicitante.correo
    ),
    estado = EstadoSolicitud.desde(estado),
    comentarioAdmin = comentarioAdmin,
    creadaEn = creadaEn?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
)

/** "10:00:00" → "10:00" */
fun String.hhmm(): String = take(5)

/** "api/usuarios/5/foto?v=…" → "http://127.0.0.1:8080/api/usuarios/5/foto?v=…" */
private fun String.urlDelBackend(): String = BuildConfig.API_BASE_URL + removePrefix("/")
