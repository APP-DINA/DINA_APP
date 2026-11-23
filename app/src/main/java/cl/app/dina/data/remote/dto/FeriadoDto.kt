package cl.app.dina.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Un feriado tal como lo entrega Nager.Date (API externa). Solo se leen los campos que usa la app. */
data class FeriadoDto(
    /** "2026-12-25" */
    @SerializedName("date") val fecha: String,
    /** Nombre en español: "Navidad" */
    @SerializedName("localName") val nombreLocal: String?,
    /** Nombre en inglés, por si falta el local: "Christmas Day" */
    @SerializedName("name") val nombre: String?
)
