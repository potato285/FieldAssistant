package org.fieldassistant.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.core.content.ContextCompat
import java.io.File

/** Local TTS -> private temporary WAV -> MediaPlayer, for genuine audio-position pause/resume.
 * All state and player operations run on the main thread. No network voice is selected. */
class OfflineSpeech(context: Context, private val onChange: () -> Unit) : AutoCloseable {
    enum class State { IDLE, PREPARING, PLAYING, PAUSED, FINISHED, ERROR }
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val audio = this.context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes).setOnAudioFocusChangeListener({ change ->
            if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) pause()
        }, main).build()
    private var ready = false
    private var closed = false
    private var engine: TextToSpeech? = null
    private var player: MediaPlayer? = null
    private var playerReady = false
    private var session = 0L
    private var file: File? = null
    private var parts = emptyList<String>()
    private var original = ""
    private var language = "en"
    var state = State.IDLE; private set
    var chunkIndex = 0; private set
    val chunkCount get() = parts.size
    val positionMs get() = if (playerReady) runCatching { player?.currentPosition ?: 0 }.getOrDefault(0) else 0
    var selectedVoice: String? = null; private set
    var failure: String? = null; private set
    private val unplugged = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { pause() }
    }
    init {
        ContextCompat.registerReceiver(this.context, unplugged,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
        engine = TextToSpeech(this.context) { status -> main.post {
            if (!closed) { ready = status == TextToSpeech.SUCCESS; onChange() }
        } }
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) { main.post { if (id == requestId() && !closed) preparePlayer() } }
            @Deprecated("Android callback compatibility")
            override fun onError(id: String?) { onError(id, -1) }
            override fun onError(id: String?, code: Int) { main.post {
                if (id == requestId() && !closed) fail("Speech synthesis failed: $code")
            } }
        })
    }
    private fun requestId() = "field-$session-$chunkIndex"
    private fun changed(value: State) { state = value; if (!closed) onChange() }
    private fun voice(language: String): Voice? = if (!ready) null else engine?.voices.orEmpty()
        .filter { it.locale.language == language && !it.isNetworkConnectionRequired &&
            !it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
        .sortedWith(compareByDescending<Voice> { it.locale.country == "IN" }.thenByDescending { it.quality }.thenBy { it.latency }.thenBy { it.name })
        .firstOrNull()
    fun voiceSetupIntent() = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).setPackage(engine?.defaultEngine)
    fun available(tamil: Boolean) = available(if (tamil) "ta" else "en")
    fun available(language: String) = !closed && voice(language) != null
    fun speak(text: String, tamil: Boolean) = speak(text, if (tamil) "ta" else "en")
    fun speak(text: String, language: String): Boolean {
        stop()
        if (closed) return false
        val chosen = voice(language) ?: return false
        val tts = engine ?: return false
        if (tts.setVoice(chosen) != TextToSpeech.SUCCESS) return false
        tts.setSpeechRate(0.95f)
        original = text
        this.language = language
        selectedVoice = chosen.name
        failure = null
        parts = SpeechText.chunks(text, minOf(1200, TextToSpeech.getMaxSpeechInputLength() - 1))
        if (parts.isEmpty()) return false
        synthesizePart()
        return state != State.ERROR
    }
    private fun synthesizePart() {
        if (closed || chunkIndex !in parts.indices) return
        changed(State.PREPARING)
        try {
            file = File.createTempFile("field-speech-", ".wav", context.cacheDir)
            if (engine?.synthesizeToFile(parts[chunkIndex], Bundle(), file!!, requestId()) != TextToSpeech.SUCCESS)
                fail("Speech request rejected")
        } catch (error: Exception) { fail(error.message ?: "Cannot create speech audio") }
    }
    private fun preparePlayer() {
        val output = file ?: return
        if (output.length() <= 44) { fail("Speech audio is empty"); return }
        val currentSession = session
        val currentPart = chunkIndex
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(output.absolutePath)
                setOnPreparedListener {
                    if (session == currentSession && chunkIndex == currentPart && !closed) {
                        playerReady = true
                        if (state != State.PAUSED) startPlayer()
                    }
                }
                setOnCompletionListener {
                    if (session == currentSession && chunkIndex == currentPart && !closed) {
                        val paused = state == State.PAUSED
                        releasePart()
                        chunkIndex++
                        if (chunkIndex < parts.size) { synthesizePart(); if (paused) changed(State.PAUSED) }
                        else { audio.abandonAudioFocusRequest(focus); changed(State.FINISHED) }
                    }
                }
                setOnErrorListener { _, what, extra ->
                    if (session == currentSession && !closed) fail("Audio playback failed: $what/$extra")
                    true
                }
                prepareAsync()
            }
        } catch (error: Exception) { fail(error.message ?: "Cannot prepare audio") }
    }
    private fun startPlayer() {
        if (!playerReady) return
        if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            changed(State.PAUSED)
            return
        }
        try { player?.start(); changed(State.PLAYING) }
        catch (error: Exception) { fail(error.message ?: "Cannot play audio") }
    }
    fun pause() {
        if (state != State.PLAYING && state != State.PREPARING) return
        if (playerReady) runCatching { player?.pause() }.onFailure { fail(it.message ?: "Cannot pause"); return }
        audio.abandonAudioFocusRequest(focus)
        changed(State.PAUSED)
    }
    fun resume() {
        if (state != State.PAUSED) return
        if (playerReady) startPlayer() else changed(State.PREPARING)
    }
    fun restart(): Boolean = original.isNotBlank() && speak(original, language)
    private fun releasePart() {
        playerReady = false
        player?.release()
        player = null
        file?.delete()
        file = null
    }
    private fun fail(message: String) {
        session++
        engine?.stop()
        releasePart()
        audio.abandonAudioFocusRequest(focus)
        failure = message
        changed(State.ERROR)
    }
    fun stop() {
        session++ // Late synthesis callbacks cannot revive cancelled speech.
        engine?.stop()
        releasePart()
        parts = emptyList()
        chunkIndex = 0
        audio.abandonAudioFocusRequest(focus)
        changed(State.IDLE)
    }
    override fun close() {
        if (closed) return
        closed = true
        stop()
        engine?.shutdown()
        engine = null
        context.unregisterReceiver(unplugged)
    }
}
