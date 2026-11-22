package org.fieldassistant.advisory

import android.content.Context
import android.os.SystemClock
import org.fieldassistant.core.Finding
import org.pytorch.executorch.extension.llm.LlmCallback
import org.pytorch.executorch.extension.llm.LlmModule
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Native ExecuTorch selection plus a separately screened English explanation.
 * Treatment instructions remain sourced, saved text. */
class ExecuTorchAdvisoryEngine(context: Context, private val repository: GuidanceRepository) : AdvisoryEngine, AutoCloseable {
    companion object {
        const val MODEL = "Llama 3.2 1B Instruct SpinQuant INT4 EO8 / ExecuTorch 0.6.0"
        const val MODEL_HASH = "8715cdba9e91f6bede00cc5f2d6b12397b95225da5630c4972a8da03001cda3b"
        const val TOKENIZER_HASH = "82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55"

        fun validateSelection(raw: String, allowed: List<GuidanceAction>) = AdvisoryOutput.validate(raw, allowed)
    }

    private val directory = File(context.filesDir, "llama")
    private val model = File(directory, "llama.pte")
    private val tokenizer = File(directory, "tokenizer.model")
    @Volatile private var module: LlmModule? = null
    private var verified = false
    var lastRawOutput: String = ""
        private set
    var lastAttempts: List<String> = emptyList()
        private set
    var lastExplanationOutput: String = ""
        private set
    @Volatile private var stopped = false

    fun filesPresent(): Boolean = model.length() == 1135951488L && tokenizer.length() == 2183982L

    private fun verify(file: File, expected: String) {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        require(digest.digest().joinToString("") { "%02x".format(it) } == expected) { "Llama asset checksum mismatch" }
    }

    @Synchronized override fun advise(finding: Finding, region: String): Advisory {
        stopped = false
        lastRawOutput = ""
        lastAttempts = emptyList()
        lastExplanationOutput = ""
        check(filesPresent()) { "Llama model/tokenizer not provisioned. Bundled guidance remains a fallback." }
        val start = SystemClock.elapsedRealtime()
        if (!verified) {
            verify(model, MODEL_HASH)
            verify(tokenizer, TOKENIZER_HASH)
            verified = true
        }
        val runner = module ?: LlmModule(model.absolutePath, tokenizer.absolutePath, 0.0f).also { module = it }
        val loadStatus = runner.load()
        check(loadStatus == 0) { "ExecuTorch model load failed: $loadStatus" }
        check(!stopped) { "Advice preparation stopped" }
        val eligible = repository.allowedActions(finding, region)
        val allowed = if (eligible.any { it.section != "reminder" })
            eligible.filter { it.section != "reminder" || it.id == "expert" } else eligible
        val maximum = minOf(32, allowed.size)
        val prefix = "{\"action_ids\":["
        fun prompt(retry: Boolean) = """<|begin_of_text|><|start_header_id|>system<|end_header_id|>

You select next steps for a farmer. The image finding is unconfirmed. Severity is not assessed. Choose 1 to $maximum UNIQUE action IDs from the allowed list. Never repeat an ID. Always include expert. When care or prevention actions are available, include at least one. Return one JSON object only, with the key action_ids and an array of strings. No explanation, markdown, other keys or treatment text. ${if (retry) "The previous response was invalid. Follow the format exactly and include a care or prevention action if available." else ""}
<|eot_id|><|start_header_id|>user<|end_header_id|>

Region: $region
Farmer's crop: ${finding.selectedCrop ?: "not confirmed"}
Finding status: ${finding.status}
Unconfirmed image candidate: ${finding.candidate?.label?.id ?: "none"}
Allowed actions:
${allowed.joinToString("\n") { "${it.id} (${it.section}): ${it.english}" }}
Available IDs (choose only these, each at most once): ${allowed.joinToString(",") { it.id }}
<|eot_id|><|start_header_id|>assistant<|end_header_id|>

$prefix"""
        var tps: Float? = null
        val timeout = Executors.newSingleThreadScheduledExecutor()
        val stopTask = timeout.schedule({ stopped = true; runner.stop() }, 90, TimeUnit.SECONDS)
        try {
            repeat(2) { attempt ->
                check(!stopped) { "Advice preparation stopped or timed out" }
                val output = StringBuilder()
                val status = runner.generate(prompt(attempt > 0), 1536, object : LlmCallback {
                    override fun onResult(result: String) { output.append(result) }
                    override fun onStats(value: Float) { tps = value }
                }, false)
                // The JSON prefix is supplied in the assistant prompt. All IDs must be
                // generated by Llama, then checked against the allowed actions.
                lastRawOutput = output.toString()
                lastAttempts = lastAttempts + lastRawOutput
                check(!stopped) { "Advice preparation stopped or timed out" }
                check(status == 0) { "ExecuTorch generation failed: $status" }
                val selected = runCatching { validateSelection(prefix + lastRawOutput, allowed) }
                    .recoverCatching { validateSelection(lastRawOutput, allowed) }
                    .mapCatching { actions ->
                        if (allowed.any { it.section == "care" || it.section == "prevention" })
                            require(actions.any { it.section == "care" || it.section == "prevention" }) { "No care or prevention action selected" }
                        actions
                    }
                if (selected.isSuccess) {
                    val facts = selected.getOrThrow().filter { it.section != "treatment" && it.section != "reminder" }.take(4)
                    // Do not invent a crop-specific explanation when vision is uncertain/unsupported.
                    var explanation: String? = null
                    var issue: String? = null
                    if (facts.isNotEmpty()) {
                        val explanationPrompt = """<|begin_of_text|><|start_header_id|>system<|end_header_id|>

Explain saved agricultural guidance in simple English. Write only one short paragraph of two or three sentences, under seventy words. Explain why the supplied care steps may help. The image finding is only possible. Use the word may or might. Use only facts supplied below. Do not add actions, chemicals, sprays, quantities, cure promises or certainty claims. No heading, list, JSON or markdown.
<|eot_id|><|start_header_id|>user<|end_header_id|>

Possible leaf problem: ${finding.candidate?.label?.condition} on ${finding.selectedCrop}.
Saved facts:
${facts.joinToString("\n") { it.english }}
Explain why these steps may help, without repeating treatment instructions.
<|eot_id|><|start_header_id|>assistant<|end_header_id|>

"""
                        val generated = StringBuilder()
                        val explanationStatus = runner.generate(explanationPrompt, 1024, object : LlmCallback {
                            override fun onResult(result: String) { generated.append(result) }
                            override fun onStats(value: Float) { tps = value }
                        }, false)
                        lastExplanationOutput = generated.toString()
                        check(!stopped) { "Advice preparation stopped or timed out" }
                        val checked = runCatching {
                            check(explanationStatus == 0) { "Explanation generation failed: $explanationStatus" }
                            ExplanationGuard.validate(lastExplanationOutput)
                        }
                        explanation = checked.getOrNull()
                        issue = checked.exceptionOrNull()?.message
                    }
                    return Advisory(selected.getOrThrow(), MODEL, false,
                        "Llama selected action IDs; ${attempt + 1} attempt(s). Saved English/Tamil/Telugu action wording. Separate Llama English explanation screened for format and restricted topics, not fact-verified.",
                        SystemClock.elapsedRealtime() - start, tps, explanation, issue)
                }
                if (attempt == 1) throw IllegalStateException("Llama returned invalid guidance after two attempts", selected.exceptionOrNull())
            }
            error("No advisory generated")
        } finally {
            stopTask.cancel(false)
            timeout.shutdown()
        }
    }

    fun stop() { stopped = true; module?.stop() }
    @Synchronized override fun close() {
        module?.resetNative()
        module = null
    }
}
