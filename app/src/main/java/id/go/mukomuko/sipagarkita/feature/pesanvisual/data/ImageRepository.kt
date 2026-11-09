package id.go.mukomuko.sipagarkita.feature.pesanvisual.data

import android.graphics.Bitmap
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.ImagePart

class ImageRepository(private val model: GenerativeModel) {
    suspend fun generateImage(prompt: String): Result<Bitmap> = runCatching {
        val response = model.generateContent(prompt)
        val imagePart = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
        imagePart?.image ?: throw IllegalStateException("Model tidak mengembalikan gambar")
    }
}
