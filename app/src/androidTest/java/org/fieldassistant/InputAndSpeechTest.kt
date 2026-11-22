package org.fieldassistant

import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.fieldassistant.vision.ImageInput
import org.fieldassistant.vision.LiteRtVisionEngine
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class InputAndSpeechTest {
    @Test fun realPhotoContentUriAndOfflineVoiceInventory() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val directory = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(directory, "instrumentation-leaf.jpg")
        InstrumentationRegistry.getInstrumentation().context.assets.open("samples/tomato_early_blight.jpg").use { source -> file.outputStream().use { source.copyTo(it) } }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val bitmap = ImageInput.fromUri(context, uri)
        val result = LiteRtVisionEngine(context).use { it.analyze(bitmap) }
        assertEquals("tomato_early_blight", result.candidates.first().label.id)
        bitmap.recycle()
        file.delete()
        @Suppress("DEPRECATION")
        val permissions = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse(permissions.contains("android.permission.INTERNET"))

        val latch = CountDownLatch(1)
        var status = -999
        var tts: TextToSpeech? = null
        instrumentation.runOnMainSync {
            tts = TextToSpeech(context) { value -> status = value; latch.countDown() }
        }
        val initialized = latch.await(10, TimeUnit.SECONDS)
        val voices = if (initialized && status == TextToSpeech.SUCCESS) tts?.voices.orEmpty() else emptySet()
        val report = JSONObject().put("content_uri_photo", "bundled real photograph through FileProvider and ImageDecoder")
            .put("top_label", result.candidates.first().label.id).put("score", result.candidates.first().score)
            .put("internet_permission", false).put("tts_initialized", initialized && status == TextToSpeech.SUCCESS)
            .put("audio_playback_tested", false)
            .put("voices", JSONArray(voices.filter { it.locale.language in setOf("en", "ta") }.map {
                JSONObject().put("name", it.name).put("language", it.locale.toLanguageTag())
                    .put("requires_network", it.isNetworkConnectionRequired)
                    .put("not_installed", it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))
            }))
        instrumentation.runOnMainSync { tts?.shutdown() }
        File(context.getExternalFilesDir(null), "evidence").apply { mkdirs() }.let {
            File(it, "input-and-speech.json").writeText(report.toString(2))
        }
    }
}
