package org.fieldassistant

import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.fieldassistant.vision.ImageInput
import java.io.File

/** Photos are test-APK fixtures only; the installed farmer app contains no example photo. */
object TestImages {
    fun tomatoUri(context: Context): android.net.Uri {
        val file = File(context.cacheDir, "captures/test-tomato.jpg").apply { parentFile!!.mkdirs() }
        InstrumentationRegistry.getInstrumentation().context.assets.open("samples/tomato_early_blight.jpg").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }
    fun tomato(context: Context) = ImageInput.fromUri(context, tomatoUri(context))
}
