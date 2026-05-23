package com.example.paceup.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.io.ByteArrayOutputStream

private const val MAX_DIMENSION = 512
private const val JPEG_QUALITY = 80

/**
 * Transparent Activity that opens the system image gallery, resizes the selected
 * image to [MAX_DIMENSION]×[MAX_DIMENSION] max, and submits the JPEG bytes to [ImagePickerResult].
 */
class PhotoPickerActivity : ComponentActivity() {

    private val pickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val inputStream = contentResolver.openInputStream(uri)
                val original = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                val scaled = scaleBitmap(original)
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                ImagePickerResult.submit(out.toByteArray())
            }
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickerLauncher.launch("image/*")
    }

    private fun scaleBitmap(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= MAX_DIMENSION && h <= MAX_DIMENSION) return src
        val scale = MAX_DIMENSION.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(src, (w * scale).toInt(), (h * scale).toInt(), true)
    }
}
