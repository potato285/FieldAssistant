package org.fieldassistant

import android.os.SystemClock
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.fieldassistant.ui.OfflineSpeech
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class SpeechPlaybackTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun <T> onMain(block: () -> T): T {
        var value: T? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { value = block() }
        @Suppress("UNCHECKED_CAST") return value as T
    }
    private fun waitFor(message: String, predicate: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 20_000
        while (!onMain(predicate) && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(50)
        assertTrue(message, onMain(predicate))
    }
    @Test fun threeLocalVoicesPauseResumeRestartAndCancel() {
        val speech = onMain { OfflineSpeech(compose.activity) {} }
        val records = JSONArray()
        val texts = mapOf(
            "en" to "Ask an agricultural officer to confirm the cause before treatment. Some crop problems look alike. Check the leaves regularly and keep a record of changes.",
            "ta" to "சிகிச்சைக்கு முன் காரணத்தை உறுதி செய்ய வேளாண் அலுவலரை அணுகுங்கள். சில பயிர் பிரச்சினைகள் ஒரே மாதிரியாகத் தோன்றலாம். இலைகளை அடிக்கடி கவனித்து மாற்றங்களைப் பதிவு செய்யுங்கள்.",
            "te" to "చికిత్సకు ముందు వ్యవసాయ అధికారితో కారణాన్ని నిర్ధారించుకోండి. కొన్ని పంట సమస్యలు ఒకేలా కనిపిస్తాయి. ఆకులను తరచుగా పరిశీలించి మార్పులను నమోదు చేయండి."
        )
        try {
            texts.forEach { (language, text) ->
                waitFor("Offline voice unavailable: $language") { speech.available(language) }
                onMain { assertTrue(speech.speak(text, language)); speech.pause() }
                SystemClock.sleep(700)
                onMain { assertEquals(OfflineSpeech.State.PAUSED, speech.state); assertEquals(0, speech.positionMs); speech.resume() }
                waitFor("Playback did not start: $language") { speech.state == OfflineSpeech.State.PLAYING && speech.positionMs >= 800 }
                val paused = onMain { speech.pause(); speech.positionMs }
                SystemClock.sleep(600)
                val held = onMain { speech.positionMs }
                assertTrue("Position advanced while paused", kotlin.math.abs(held - paused) <= 60)
                onMain { assertEquals(OfflineSpeech.State.PAUSED, speech.state); speech.resume() }
                waitFor("Resume did not advance position") { speech.positionMs > paused + 350 }
                onMain { assertTrue(speech.restart()); assertEquals(0, speech.chunkIndex); assertEquals(0, speech.positionMs) }
                waitFor("Restart failed") { speech.state == OfflineSpeech.State.PLAYING }
                records.put(onMain { JSONObject().put("language", language).put("voice", speech.selectedVoice)
                    .put("paused_position_ms", paused).put("held_position_ms", held)
                    .put("prepare_pause_passed", true).put("resume_passed", true).put("restart_passed", true) })
                onMain { speech.stop(); assertTrue(speech.speak(text, language)); speech.stop() }
                SystemClock.sleep(600)
                onMain { assertEquals(OfflineSpeech.State.IDLE, speech.state) }
            }
            onMain { assertTrue(speech.speak("Check the leaves.\n\nAsk your agricultural officer.", "en")) }
            waitFor("Sentence sequence did not finish") { speech.state == OfflineSpeech.State.FINISHED }
            onMain { assertEquals(2, speech.chunkCount); assertEquals(2, speech.chunkIndex) }
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            File(context.getExternalFilesDir(null), "evidence").apply { mkdirs() }.let {
                File(it, "speech-playback-v4.json").writeText(JSONObject().put("voices", records)
                    .put("sentence_sequence_completed", true).put("human_pronunciation_review", false)
                    .put("network_settings_changed", false).toString(2))
            }
        } finally { onMain { speech.close() } }
    }
}
