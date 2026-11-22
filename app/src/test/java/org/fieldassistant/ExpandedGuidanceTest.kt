package org.fieldassistant

import org.fieldassistant.advisory.ExplanationGuard
import org.fieldassistant.advisory.AssetGuidanceRepository
import org.fieldassistant.core.*
import org.fieldassistant.ui.Telugu
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ExpandedGuidanceTest {
    @Test fun narrativeRejectsRatesChemicalsAndCureClaims() {
        for (raw in listOf(
            "The possible problem may improve if you spray mancozeb regularly on all leaves of the crop.",
            "This possible disease may be cured with two doses of chemical treatment every week.",
            "Your plants may need 5 litres of water and 3 grams of product every day.",
            "The leaf is definitely infected and the plant will die unless all nearby plants are burned."
        )) assertTrue(runCatching { ExplanationGuard.validate(raw) }.isFailure)
        assertEquals("Healthy seed and removing infected debris may reduce sources of infection. Suitable watering can help plants grow steadily.",
            ExplanationGuard.validate("Healthy seed and removing infected debris may reduce sources of infection. Suitable watering can help plants grow steadily."))
    }
    @Test fun everyGuidanceEntryHasThreeLanguagesAndASource() {
        val json = JSONObject(File("src/main/assets/guidance.json").readText())
        val sources = json.getJSONArray("sources")
        val sourceIds = (0 until sources.length()).map { sources.getJSONObject(it).getString("id") }.toSet()
        val actions = json.getJSONArray("actions")
        val ids = mutableSetOf<String>()
        for (i in 0 until actions.length()) {
            val row = actions.getJSONObject(i)
            assertTrue(ids.add(row.getString("id")))
            assertTrue(sourceIds.contains(row.getString("source")))
            for (lang in listOf("en", "ta", "te")) assertTrue(row.getString(lang).length > 20)
            assertTrue(row.getString("te").any { it.code in 0x0C00..0x0C7F })
            assertTrue(row.getString("ta").any { it.code in 0x0B80..0x0BFF })
            if (row.getString("section") == "treatment") assertTrue(row.getJSONArray("labels").length() > 0)
        }
    }
    @Test fun teluguCoversFarmerUiAndAllModelLabels() {
        val source = File("src/main/java/org/fieldassistant/MainActivity.kt").readText()
        val literal = Regex("""\bw\(\s*"((?:\\.|[^"\\])*)"""")
        val missing = literal.findAll(source).map { JSONObject("{\"text\":\"${it.groupValues[1]}\"}").getString("text") }
            .filter { it !in Telugu.text }.toList()
        assertTrue("Missing Telugu UI translations: $missing", missing.isEmpty())
        val info = org.json.JSONArray(File("src/main/assets/vision/class_info.json").readText())
        for (i in 0 until info.length()) {
            val row = info.getJSONObject(i)
            assertTrue(row.getString("plant") in Telugu.crops)
            assertTrue(row.getString("disease") in Telugu.conditions)
        }
        assertEquals(21, Telugu.crops.size)
        assertEquals(31, Telugu.conditions.size)
    }
    @Test fun uncertainWrongRegionAndHealthyNeverReceiveChemicalAdvice() {
        val repository = AssetGuidanceRepository(JSONObject(File("src/main/assets/guidance.json").readText()))
        // Synthetic routing fixtures ONLY; not model inference evidence.
        fun finding(status: FindingStatus, label: String) = Finding(status, "Potato", VisionResult(
            listOf(Candidate(CropLabel(label, "Potato", "fixture", label.endsWith("healthy")), 0.9f)),
            "TEST FIXTURE", "TEST", 0.0, 0.0))
        for (status in FindingStatus.entries.filter { it != FindingStatus.POSSIBLE_MATCH })
            assertTrue(repository.allowedActions(finding(status, "potato_early_blight"), "Tamil Nadu, India").all { it.section == "reminder" })
        for (label in listOf("potato_early_blight", "potato_late_blight")) {
            val f = finding(FindingStatus.POSSIBLE_MATCH, label)
            val actions = repository.allowedActions(f, "Tamil Nadu, India")
            assertTrue(actions.any { it.id == "potato_fungicide" })
            assertTrue(actions.count { it.section == "prevention" } >= 3)
            assertTrue(repository.allowedActions(f, "Andhra Pradesh, India").all { it.section == "reminder" })
        }
        assertTrue(repository.allowedActions(finding(FindingStatus.POSSIBLE_MATCH, "potato_healthy"), "Tamil Nadu, India").all { it.section == "reminder" })
    }
}
