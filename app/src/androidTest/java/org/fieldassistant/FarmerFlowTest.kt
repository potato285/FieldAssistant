package org.fieldassistant

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.io.File

class FarmerFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun screenshot(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(350) // Allow platform dialog/window transitions to finish.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "evidence").apply { mkdirs() }
        // Capture only this app's Compose surface, never notifications or other apps.
        compose.onRoot().captureToImage().asAndroidBitmap().let { image ->
            File(directory, name).outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
            image.recycle()
        }
    }

    @Test fun realSampleTamilAndUnsupportedCropRemainHonest() {
        // Start in English irrespective of a saved user language; restore English at end.
        if (compose.onAllNodesWithText("English").fetchSemanticsNodes().isNotEmpty())
            compose.onNodeWithTag("language-en").performClick()
        screenshot("home-en.png")
        compose.onNodeWithTag("sample").assertDoesNotExist()
        compose.runOnIdle {
            val vm = androidx.lifecycle.ViewModelProvider(compose.activity)[org.fieldassistant.ui.FieldViewModel::class.java]
            vm.selectCrop("Tomato")
            vm.loadPhoto(TestImages.tomatoUri(compose.activity))
        }
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Early Blight").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("result").performScrollTo()
        compose.onNodeWithText("Damage level: Not assessed").assertDoesNotExist()
        compose.onNodeWithText("Model score:", substring = true).assertDoesNotExist()
        screenshot("result-en.png")
        compose.waitUntil(90_000) {
            compose.onAllNodesWithText("Your next steps are ready").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Your next steps are ready").performScrollTo()
        screenshot("llama-guidance-en.png")
        compose.onNodeWithTag("language-ta").performScrollTo().performClick()
        compose.onNodeWithTag("result").performScrollTo()
        compose.onNodeWithText("பாதிப்பின் தீவிரம்: அளவிடப்படவில்லை").assertDoesNotExist()
        screenshot("result-ta.png")
        compose.onNodeWithText("அடுத்து செய்ய வேண்டியவை தயார்").performScrollTo()
        screenshot("llama-guidance-ta.png")
        compose.onNodeWithTag("language-en").performScrollTo().performClick()
        compose.onNodeWithTag("crop-selector").performScrollTo().performClick()
        compose.onNodeWithText("Sapodilla • unsupported").performScrollTo().performClick()
        compose.onNodeWithTag("result").performScrollTo()
        compose.onNodeWithText("This crop is not supported yet").assertExists()
        compose.onNodeWithText("Early Blight").assertDoesNotExist()
        screenshot("unsupported-sapodilla.png")
    }
}
