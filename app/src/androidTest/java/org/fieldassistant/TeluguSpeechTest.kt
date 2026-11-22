package org.fieldassistant

import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TeluguSpeechTest {
    @Test fun installedTeluguVoiceProducesAudioWithoutChangingConnectivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val initialized = CountDownLatch(1)
        var initStatus = -1
        var tts: TextToSpeech? = null
        instrumentation.runOnMainSync {
            tts = TextToSpeech(context) { status -> initStatus = status; initialized.countDown() }
        }
        assertTrue(initialized.await(10, TimeUnit.SECONDS))
        assertEquals(TextToSpeech.SUCCESS, initStatus)
        val engine = tts!!
        val directory = File(context.getExternalFilesDir(null), "evidence").apply { mkdirs() }
        val rows = JSONArray()
        try {
            for ((language, phrase) in listOf(
                "te" to "చికిత్సకు ముందు కారణాన్ని వ్యవసాయ అధికారితో నిర్ధారించుకోండి. మంచి విత్తన దుంపలు వాడండి. సోకిన మొక్కల అవశేషాలను సురక్షితంగా తొలగించండి."
            )) {
                val voice = engine.voices.firstOrNull {
                    it.locale.language == language && !it.isNetworkConnectionRequired &&
                        !it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
                }
                assertNotNull("No installed offline $language voice; install Telugu speech data before rerunning", voice)
                assertEquals(TextToSpeech.SUCCESS, engine.setVoice(voice))
                val finished = CountDownLatch(1)
                var synthesisError: Int? = null
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) = Unit
                    override fun onDone(id: String?) { finished.countDown() }
                    @Deprecated("Android callback compatibility")
                    override fun onError(id: String?) { synthesisError = -1; finished.countDown() }
                    override fun onError(id: String?, code: Int) { synthesisError = code; finished.countDown() }
                })
                val output = File(directory, "v3-speech-$language.wav")
                val start = android.os.SystemClock.elapsedRealtime()
                assertEquals(TextToSpeech.SUCCESS, engine.synthesizeToFile(phrase, Bundle(), output, "test-$language"))
                assertTrue("Synthesis timeout", finished.await(30, TimeUnit.SECONDS))
                assertNull("Synthesis failed", synthesisError)
                assertTrue("No audio samples written", output.length() > 44)
                rows.put(JSONObject().put("language", language).put("voice", voice!!.name)
                    .put("bytes", output.length()).put("elapsed_ms", android.os.SystemClock.elapsedRealtime() - start)
                    .put("text", phrase).put("file", output.name))
            }
            File(directory, "v3-telugu-speech.json").writeText(JSONObject().put("airplane_mode", Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0))
                .put("connectivity_changed_by_test", false)
                .put("synthesis_callback", "onDone for Telugu").put("speaker_playback_tested", false)
                .put("pronunciation_reviewed", false).put("results", rows).toString(2))
        } finally { instrumentation.runOnMainSync { engine.shutdown() } }
    }
}
