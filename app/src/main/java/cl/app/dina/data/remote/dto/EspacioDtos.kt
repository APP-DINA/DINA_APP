package cl.app.dina.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Un espacio tal como lo entrega GET /api/espacios de dina_backend. */
data class EspacioDto(
    @SerializedName("id") val id: Long,
    @SerializedName("codigo") val codigo: String,
    @SerializedName("nombre") val nombre: String,
    /** NORTE, CENTRAL, SUR o CTI */
    @SerializedName("edificio") val edificio: String,
    /** "Edificio Norte", "Edificio Tecnológico CTI"... */
    @SerializedName("edificioNombre") val edificioNombre: String,
    @SerializedName("piso") val piso: Int,
    /** SALA, LABORATORIO, ESTUDIO, AUDITORIO, DEPORTIVO o MULTIPROPOSITO */
    @SerializedName("tipo") val tipo: String,
    @SerializedName("capacidad") val capacidad: Int,
    /** Nombre del enum Escuela (INFORMATICA, DISENO...) o null si es de uso general. */
    @SerializedName("escuela") val escuela: String?,
    @SerializedName("proyector") val proyector: Boolean,
    @SerializedName("computadores") val computadores: Boolean,
    @SerializedName("climatizacion") val climatizacion: Boolean
)
