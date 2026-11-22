package org.fieldassistant

import android.os.Build
import android.os.Debug
import android.provider.Settings
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.fieldassistant.advisory.AssetGuidanceRepository
import org.fieldassistant.advisory.ExecuTorchAdvisoryEngine
import org.fieldassistant.core.ConservativeResultAggregator
import org.fieldassistant.vision.ImageInput
import org.fieldassistant.vision.LiteRtVisionEngine
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class LlamaSmokeTest {
    @Test fun realLlamaChoosesApprovedGuidanceFromRealVisionFinding() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = AssetGuidanceRepository(context)
        val bitmap = TestImages.tomato(context)
        val finding = LiteRtVisionEngine(context).use { vision ->
            ConservativeResultAggregator(vision.labels.map { it.crop }.toSet()).aggregate(vision.analyze(bitmap), "Tomato")
        }
        bitmap.recycle()
        val report = JSONObject().put("model", ExecuTorchAdvisoryEngine.MODEL)
            .put("model_sha256", ExecuTorchAdvisoryEngine.MODEL_HASH)
            .put("tokenizer_sha256", ExecuTorchAdvisoryEngine.TOKENIZER_HASH)
            .put("device", "${Build.MANUFACTURER} ${Build.MODEL}").put("abi", Build.SUPPORTED_ABIS.first())
            .put("android", Build.VERSION.RELEASE).put("airplane_mode", Settings.Global.getInt(context.contentResolver, "airplane_mode_on", 0))
            .put("vision_candidate", finding.candidate?.label?.id)
        ExecuTorchAdvisoryEngine(context, repository).use { engine ->
            try {
                assertTrue("Llama model and tokenizer must be provisioned", engine.filesPresent())
                val result = engine.advise(finding, "Tamil Nadu, India")
                assertFalse(result.fallback)
                assertTrue(result.actions.any { it.id == "expert" })
                val allowed = repository.allowedActions(finding, "Tamil Nadu, India").map { it.id }
                assertTrue(result.actions.all { it.id in allowed })
                val memory = Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }
                report.put("status", "SUCCESS").put("elapsed_including_hash_load_ms", result.elapsedMs)
                    .put("tokens_per_second", result.tokensPerSecond).put("actions", JSONArray(result.actions.map { it.id }))
                    .put("process_pss_kb_with_model_loaded", memory.totalPss)
            } catch (failure: Throwable) {
                report.put("status", "FAILED").put("error", failure.toString())
                throw failure
            } finally {
                report.put("raw_generated_output", engine.lastRawOutput)
                val directory = File(context.getExternalFilesDir(null), "evidence").apply { mkdirs() }
                File(directory, "llama-smoke.json").writeText(report.toString(2))
                Log.i("FieldAssistantEvidence", report.toString())
            }
        }
    }
}
