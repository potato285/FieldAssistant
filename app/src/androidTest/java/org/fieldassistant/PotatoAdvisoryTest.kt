package org.fieldassistant

import androidx.core.content.FileProvider
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

class PotatoAdvisoryTest {
    @Test fun potatoContentUriAndRepeatedCrossCropLlamaRequests() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val photo = File(context.cacheDir, "captures/potato-regression.jpg")
        photo.parentFile!!.mkdirs()
        instrumentation.context.assets.open("potato_early_blight.jpg").use { input ->
            photo.outputStream().use { input.copyTo(it) }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", photo)
        val repository = AssetGuidanceRepository(context)
        val rows = JSONArray()
        val failures = mutableListOf<String>()
        LiteRtVisionEngine(context).use { vision ->
            val aggregator = ConservativeResultAggregator(vision.labels.map { it.crop }.toSet())
            ExecuTorchAdvisoryEngine(context, repository).use { llama ->
                // Real photographs and genuine vision on every run. No synthetic model scores.
                for (crop in listOf("Potato", "Tomato", "Potato", null, "Other")) {
                    val bitmap = if (crop == "Potato") ImageInput.fromUri(context, uri) else TestImages.tomato(context)
                    val result = vision.analyze(bitmap)
                    bitmap.recycle()
                    val finding = aggregator.aggregate(result, crop)
                    val row = JSONObject().put("crop", crop).put("label", result.candidates.first().label.id)
                        .put("score", result.candidates.first().score).put("finding", finding.status)
                        .put("allowed_ids", JSONArray(repository.allowedActions(finding, "Tamil Nadu, India").map { it.id }))
                    try {
                        val advice = llama.advise(finding, "Tamil Nadu, India")
                        assertFalse(advice.fallback)
                        assertTrue(advice.actions.any { it.id == "expert" })
                        assertTrue(advice.actions.all { action -> repository.allowedActions(finding, "Tamil Nadu, India").any { it.id == action.id } })
                        row.put("status", "SUCCESS").put("actions", JSONArray(advice.actions.map { it.id }))
                            .put("elapsed_ms", advice.elapsedMs).put("generated_explanation", advice.explanation)
                            .put("explanation_issue", advice.explanationIssue)
                        if (crop == "Potato") assertNotNull("Potato explanation must execute", advice.explanation)
                    } catch (failure: Throwable) {
                        row.put("status", "FAILED").put("error", failure.toString())
                        failures.add("$crop: $failure")
                    }
                    row.put("raw_explanation", llama.lastExplanationOutput).put("raw_output", llama.lastRawOutput).put("attempts", JSONArray(llama.lastAttempts))
                    rows.put(row)
                }
            }
        }
        photo.delete()
        File(context.getExternalFilesDir(null), "evidence").apply { mkdirs() }
            .resolve("potato-advisory-regression.json").writeText(rows.toString(2))
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
