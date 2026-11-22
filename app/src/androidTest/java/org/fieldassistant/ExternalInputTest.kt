package org.fieldassistant

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class ExternalInputTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun systemCameraAndGalleryLaunchAndCancel() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val report = JSONObject().put("capture_or_gallery_selection_completed", false)
        for (tag in listOf("camera", "gallery")) {
            compose.onNodeWithTag(tag).performScrollTo().performClick()
            var destination = ""
            val deadline = SystemClock.elapsedRealtime() + 10_000
            while (SystemClock.elapsedRealtime() < deadline) {
                destination = automation.rootInActiveWindow?.packageName?.toString().orEmpty()
                if (destination.isNotBlank() && destination != "org.fieldassistant") break
                SystemClock.sleep(100)
            }
            assertTrue("$tag did not launch an external activity", destination.isNotBlank() && destination != "org.fieldassistant")
            report.put(tag + "_destination", destination)
            automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            SystemClock.sleep(500)
            // Some camera apps have an initial onboarding layer. Back again if needed.
            if (automation.rootInActiveWindow?.packageName?.toString() != "org.fieldassistant") {
                automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                SystemClock.sleep(500)
            }
            compose.onNodeWithTag(tag).assertExists()
        }
        File(instrumentation.targetContext.getExternalFilesDir(null), "evidence").apply { mkdirs() }
            .resolve("external-input.json").writeText(report.toString(2))
    }
}
