package org.fieldassistant

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.fieldassistant.ui.FieldViewModel
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class GalleryAdvisoryFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun actualPotatoUriAutomaticallyGetsLlamaAdviceAndKeepsDebugSeparate() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val photo = File(context.cacheDir, "captures/potato-ui.jpg").apply { parentFile!!.mkdirs() }
        instrumentation.context.assets.open("potato_early_blight.jpg").use { input -> photo.outputStream().use { input.copyTo(it) } }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", photo)
        lateinit var vm: FieldViewModel
        compose.runOnIdle {
            vm = ViewModelProvider(compose.activity)[FieldViewModel::class.java]
            vm.selectLanguage("en")
        }
        compose.onNodeWithTag("sample").assertDoesNotExist()
        assertFalse(context.assets.list("").orEmpty().contains("samples"))
        compose.onNodeWithTag("crop-selector").performClick()
        compose.onNodeWithText("Potato", useUnmergedTree = true).performScrollTo().performClick()
        // This is the exact real content-URI callback path used by gallery selection.
        // The system picker interaction itself is not automated by this test.
        compose.runOnIdle { vm.loadPhoto(uri) }
        compose.waitUntil(90_000) { !vm.busy && vm.advisory != null }
        compose.runOnIdle {
            assertNull(vm.error)
            assertNotNull(vm.bitmap)
            assertEquals("potato_early_blight", vm.result!!.candidates.first().label.id)
            assertFalse("Automatic advice must execute Llama", vm.advisory!!.fallback)
            assertNotNull("Real English explanation must be generated", vm.advisory!!.explanation)
            assertTrue(vm.advisory!!.actions.any { it.id == "expert" })
            assertFalse(vm.advisory!!.actions.any { it.id == "debris" || it.id == "rotation" })
        }
        compose.onNodeWithText("Early Blight").assertExists()
        compose.onNodeWithText("Damage level: Not assessed").assertDoesNotExist()
        compose.onNodeWithText("This photo cannot tell us how much of the crop is affected.").assertDoesNotExist()
        compose.onNodeWithTag("llama-explanation").assertExists()
        compose.onNodeWithText("Your next steps are ready").assertExists()
        compose.onNodeWithText("Model score:", substring = true).assertDoesNotExist()
        compose.onNodeWithText("No substitute prediction was created.").assertDoesNotExist()
        compose.onNodeWithTag("debug-panel").assertDoesNotExist()
        val directory = File(context.getExternalFilesDir(null), "evidence").apply { mkdirs() }
        fun screenshot(name: String) {
            compose.waitForIdle()
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        compose.onNodeWithTag("result").performScrollTo()
        screenshot("potato-result-v4.png")
        compose.onNodeWithText("Listen to these steps").performScrollTo()
        screenshot("potato-advice-v4.png")
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("speech-main").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("speech-main").assertIsEnabled().performClick()
        compose.onNodeWithText("Pause reading").assertExists()
        compose.onNodeWithTag("speech-main").performClick()
        compose.onNodeWithText("Resume reading").assertExists()
        compose.onNodeWithTag("speech-restart").performScrollTo().performClick()
        compose.onNodeWithText("Pause reading").assertExists()
        compose.onNodeWithTag("speech-main").performScrollTo().performClick()
        screenshot("speech-paused-v4.png")
        compose.onNodeWithTag("speech-stop").performScrollTo().performClick()
        compose.onNodeWithText("Listen to these steps").assertExists()

        compose.onNodeWithTag("debug-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("debug-panel").assertExists()
        compose.onNodeWithText("Fallback: false", substring = true).assertExists()
        compose.onNodeWithTag("debug-panel").performScrollTo()
        screenshot("potato-debug-v4.png")
        compose.onNodeWithTag("debug-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("language-ta").performScrollTo().performClick()
        compose.onNodeWithText("அடுத்து செய்ய வேண்டியவை தயார்").assertExists()
        compose.onNodeWithText("இந்தப் படிகளைக் கேளுங்கள்").performScrollTo()
        screenshot("potato-advice-ta-v4.png")
        compose.onNodeWithTag("language-te").performScrollTo().performClick()
        compose.onNodeWithText("తదుపరి చర్యలు సిద్ధంగా ఉన్నాయి").assertExists()
        compose.onNodeWithText("చికిత్స అవకాశాలు").performScrollTo()
        screenshot("potato-advice-te-v4.png")
        compose.runOnIdle {
            File(directory, "potato-ui-v4.json").writeText(JSONObject()
                .put("input", "PlantVillage potato photo through real gallery callback/content URI")
                .put("actual_system_picker_selection_automated", false)
                .put("automatic_llama", !vm.advisory!!.fallback)
                .put("label", vm.result!!.candidates.first().label.id)
                .put("score", vm.result!!.candidates.first().score)
                .put("actions", JSONArray(vm.advisory!!.actions.map { it.id }))
                .put("generated_english_explanation", vm.advisory!!.explanation)
                .put("telugu_ui_checked", true)
                .put("elapsed_ms", vm.advisory!!.elapsedMs)
                .put("debug_hidden_by_default", true).toString(2))
            vm.selectLanguage("en")
        }
        photo.delete()
    }
}
