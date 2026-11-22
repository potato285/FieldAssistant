package org.fieldassistant.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

object ImageInput {
    // ImageDecoder applies EXIF orientation. Software allocation allows RGB pixel reads.
    private fun decode(source: ImageDecoder.Source): Bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val longest = max(info.size.width, info.size.height)
        if (longest > 2048) decoder.setTargetSize(
            (info.size.width * 2048L / longest).toInt().coerceAtLeast(1),
            (info.size.height * 2048L / longest).toInt().coerceAtLeast(1)
        )
        decoder.setTargetColorSpace(android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB))
    }

    fun fromUri(context: Context, uri: Uri): Bitmap = decode(ImageDecoder.createSource(context.contentResolver, uri))

    fun tensor(bitmap: Bitmap): ByteBuffer {
        require(bitmap.width > 0 && bitmap.height > 0) { "Empty image" }
        val scale = 256.0 / minOf(bitmap.width, bitmap.height)
        val resized = Bitmap.createScaledBitmap(bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(256),
            (bitmap.height * scale).toInt().coerceAtLeast(256), true)
        // Two documented centre crops: 256 square, then 224 square (16-pixel inset).
        val squareX = (resized.width - 256) / 2
        val squareY = (resized.height - 256) / 2
        val pixels = IntArray(224 * 224)
        resized.getPixels(pixels, 0, 224, squareX + 16, squareY + 16, 224, 224)
        val input = ByteBuffer.allocateDirect(224 * 224 * 3 * 4).order(ByteOrder.nativeOrder())
        for (pixel in pixels) {
            input.putFloat(((pixel shr 16) and 255).toFloat())
            input.putFloat(((pixel shr 8) and 255).toFloat())
            input.putFloat((pixel and 255).toFloat())
        }
        if (resized !== bitmap) resized.recycle()
        input.rewind()
        return input
    }
}
