package org.fieldassistant

import android.os.Build
import android.os.Debug
import android.provider.Settings
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.fieldassistant.vision.ImageInput
import org.fieldassistant.vision.LiteRtVisionEngine
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class VisionSmokeTest {
    @Test fun actualPhotographRunsThroughBundledModel() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = TestImages.tomato(context)
        val runs = JSONArray()
        LiteRtVisionEngine(context).use { engine ->
            repeat(4) { index ->
                val result = engine.analyze(bitmap)
                assertEquals(54, result.candidates.size)
                assertEquals(LiteRtVisionEngine.SHA256, result.modelSha256)
                assertTrue(result.inferenceMs > 0)
                assertTrue(result.candidates.all { it.score.isFinite() })
                runs.put(JSONObject().put("run", index).put("inference_ms", result.inferenceMs)
                    .put("preprocess_ms", result.preprocessingMs).put("top3", JSONArray(result.candidates.take(3).map {
                        JSONObject().put("label", it.label.id).put("crop", it.label.crop).put("score", it.score)
                    })))
            }
        }
        val memory = Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }
        val report = JSONObject().put("model", LiteRtVisionEngine.MODEL_ID).put("sha256", LiteRtVisionEngine.SHA256)
            .put("input", "PlantVillage tomato early blight photograph; may overlap training data")
            .put("input_shape", "[1,224,224,3] FLOAT32 RGB 0..255").put("output_shape", "[1,54] FLOAT32 softmax")
            .put("device", "${Build.MANUFACTURER} ${Build.MODEL}").put("abi", Build.SUPPORTED_ABIS.first())
            .put("android", Build.VERSION.RELEASE).put("airplane_mode", Settings.Global.getInt(context.contentResolver, "airplane_mode_on", 0))
            .put("process_pss_kb_after_engine_close", memory.totalPss).put("runs", runs)
        val directory = File(context.getExternalFilesDir(null), "evidence").apply { mkdirs() }
        File(directory, "vision-smoke.json").writeText(report.toString(2))
        Log.i("FieldAssistantEvidence", report.toString())
        bitmap.recycle()
    }
}
