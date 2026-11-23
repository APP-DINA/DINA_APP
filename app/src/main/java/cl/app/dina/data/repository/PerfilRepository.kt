package cl.app.dina.data.repository

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import cl.app.dina.data.model.Usuario
import cl.app.dina.data.remote.DinaApi
import cl.app.dina.data.remote.dto.aModelo
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/** Perfil del usuario con sesión: por ahora, la foto. */
interface PerfilRepository {
    /**
     * Recorta la imagen elegida en un cuadrado de [LADO_FOTO] px, la sube al backend y guarda
     * la nueva foto en la sesión (el encabezado se actualiza solo). Lanza una excepción si falla.
     */
    suspend fun cambiarFoto(imagen: Uri): Usuario
}

/** Lado de la foto que se sube: de sobra para un avatar y pesa unos 50–100 KB en JPEG. */
const val LADO_FOTO = 512

class RemotePerfilRepository(
    private val api: DinaApi,
    private val sesionRepository: SesionRepository,
    private val contentResolver: ContentResolver
) : PerfilRepository {

    override suspend fun cambiarFoto(imagen: Uri): Usuario {
        val jpeg = withContext(Dispatchers.Default) { comprimir(leerCuadrado(imagen)) }
        val parte = MultipartBody.Part.createFormData(
            "foto", "perfil.jpg", jpeg.toRequestBody("image/jpeg".toMediaType())
        )
        val usuario = api.subirFoto(parte).aModelo()
            ?: throw IllegalStateException("Tu cuenta no tiene un dominio institucional válido")
        sesionRepository.actualizarUsuario(usuario)
        return usuario
    }

    /** Lee la imagen ya reducida (no carga una foto de 12 MP completa en memoria) y la recorta al centro. */
    private fun leerCuadrado(uri: Uri): Bitmap {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder también endereza las fotos según su orientación (EXIF)
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, info, _ ->
                val escala = LADO_FOTO.toFloat() / min(info.size.width, info.size.height)
                if (escala < 1f) {
                    decoder.setTargetSize((info.size.width * escala).toInt(), (info.size.height * escala).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, limites) }
            var muestra = 1
            while (min(limites.outWidth, limites.outHeight) / (muestra * 2) >= LADO_FOTO) muestra *= 2
            val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
            contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, opciones) }
        } ?: throw IllegalArgumentException("No se pudo leer la imagen")

        val lado = min(bitmap.width, bitmap.height)
        val recorte = Bitmap.createBitmap(bitmap, (bitmap.width - lado) / 2, (bitmap.height - lado) / 2, lado, lado)
        return if (lado > LADO_FOTO) Bitmap.createScaledBitmap(recorte, LADO_FOTO, LADO_FOTO, true) else recorte
    }

    private fun comprimir(bitmap: Bitmap): ByteArray =
        ByteArrayOutputStream().use { salida ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, salida)
            salida.toByteArray()
        }
}
