package org.fieldassistant.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.fieldassistant.BuildConfig
import org.fieldassistant.advisory.*
import org.fieldassistant.core.*
import org.fieldassistant.vision.*
import java.util.concurrent.Executors

class FieldViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Application = application
    private val worker = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    val labels = LiteRtVisionEngine.readLabels(context)
    val crops = labels.map { it.crop }.distinct().sorted()
    private val aggregator = ConservativeResultAggregator(crops.toSet())
    val repository: GuidanceRepository = AssetGuidanceRepository(context)
    private val fallback = LocalGuidanceFallback(repository)
    private val llama = ExecuTorchAdvisoryEngine(context, repository)
    private var vision: VisionEngine? = null
    private val preferences = context.getSharedPreferences("preferences", 0)

    var language by mutableStateOf(preferences.getString("language", null)
        ?: if (preferences.getBoolean("tamil", false)) "ta" else "en"); private set
    val tamil get() = language == "ta"
    var crop by mutableStateOf<String?>(null); private set
    var bitmap by mutableStateOf<Bitmap?>(null); private set
    var busy by mutableStateOf(false); private set
    var askingLlama by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var advisoryFailed by mutableStateOf(false); private set
    var advisoryDebug by mutableStateOf(""); private set
    var result by mutableStateOf<VisionResult?>(null); private set
    var finding by mutableStateOf<Finding?>(null); private set
    var advisory by mutableStateOf<Advisory?>(null); private set
    var llamaReady by mutableStateOf(llama.filesPresent()); private set

    fun toggleLanguage() {
        selectLanguage(if (tamil) "en" else "ta")
    }
    fun selectLanguage(value: String) {
        require(value in listOf("en", "ta", "te"))
        language = value
        preferences.edit().putString("language", value).apply()
    }
    fun refreshModelFiles() { llamaReady = llama.filesPresent() }
    fun selectCrop(value: String?) {
        if (busy) return
        crop = value
        if (bitmap != null) {
            aggregate()
            askLlama()
        }
    }
    private fun aggregate() {
        finding = aggregator.aggregate(result, crop)
        advisory = null
        advisoryDebug = ""
    }
    fun showError(message: String) { advisoryFailed = false; error = message }
    fun loadPhoto(uri: Uri) = load { ImageInput.fromUri(context, uri) }
    private fun load(decode: () -> Bitmap) {
        if (busy) return
        busy = true
        error = null
        advisoryFailed = false
        advisoryDebug = ""
        result = null
        finding = null
        advisory = null
        bitmap = null
        viewModelScope.launch {
            try {
                val input = withContext(worker) { decode() }
                bitmap = input
                if (crop != null && crop !in crops) {
                    aggregate()
                } else {
                    result = withContext(worker) {
                        val engine = vision ?: LiteRtVisionEngine(context).also { vision = it }
                        engine.analyze(input)
                    }
                    aggregate()
                }
                prepareAdvice(finding!!)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "Image analysis failed"
            } finally { busy = false }
        }
    }
    fun reanalyze() {
        val current = bitmap ?: return
        load { current }
    }
    fun askLlama() {
        val current = finding ?: return
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try { prepareAdvice(current) } finally { busy = false }
        }
    }
    private suspend fun prepareAdvice(current: Finding) {
        refreshModelFiles()
        askingLlama = true
        advisoryFailed = false
        advisory = null
        try {
            advisory = withContext(worker) { llama.advise(current, "Tamil Nadu, India") }
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (failure: Throwable) {
            if (failure is VirtualMachineError || failure is ThreadDeath) throw failure
            // Keep the real vision finding. A failed advisory never changes the diagnosis.
            advisory = fallback.advise(current, "Tamil Nadu, India")
            advisoryFailed = true
            error = "Llama: ${failure.message ?: failure.javaClass.simpleName}"
        } finally {
            if (BuildConfig.DEBUG) advisoryDebug = buildString {
                appendLine("Engine: ${advisory?.engine ?: "No completed advisory"}")
                appendLine("Fallback: ${advisory?.fallback}")
                appendLine("${advisory?.elapsedMs ?: 0} ms; attempts: ${llama.lastAttempts.size}")
                appendLine("Actions: ${advisory?.actions?.joinToString { it.id }}")
                appendLine("Generated explanation: ${advisory?.explanation}")
                appendLine("Explanation issue: ${advisory?.explanationIssue}")
                appendLine("Raw Llama continuation (JSON prefix supplied in prompt):")
                append(llama.lastRawOutput.take(2048))
            }
            askingLlama = false
        }
    }
    fun stopLlama() { llama.stop() }
    override fun onCleared() {
        llama.stop()
        // Close on the same serial worker after any native call has returned.
        worker.executor.execute { vision?.close(); llama.close() }
        worker.close()
    }
}
