package org.fieldassistant

import org.fieldassistant.advisory.ExecuTorchAdvisoryEngine
import org.fieldassistant.advisory.GuidanceAction
import org.junit.Assert.*
import org.junit.Test

class AdvisoryGuardTest {
    private val allowed = listOf("retake", "observe", "expert").map {
        GuidanceAction(it, it, it, "test", emptySet())
    }
    @Test fun mandatoryExpertStepSurvivesModelOmission() {
        val actions = ExecuTorchAdvisoryEngine.validateSelection("""{"action_ids":["observe"]}""", allowed)
        assertEquals(listOf("observe", "expert"), actions.map { it.id })
    }
    @Test fun inventedTreatmentAndForeignKeysAreRejected() {
        for (raw in listOf(
            """{"action_ids":["spray_pesticide"]}""",
            """{"action_ids":["expert"],"treatment":"invented dose"}""",
            """{"action_ids":[]}""",
            """{"action_ids":["expert","expert"]}""",
            "Do not consult an expert."
        )) {
            assertTrue("Unsafe output must be rejected: $raw", runCatching {
                ExecuTorchAdvisoryEngine.validateSelection(raw, allowed)
            }.isFailure)
        }
    }
    @Test fun introductoryTextAndCodeFencesDoNotBecomeFarmerAdvice() {
        val actions = ExecuTorchAdvisoryEngine.validateSelection(
            "Here are the steps:\n```json\n{\"action_ids\":[\"observe\",\"expert\"]}\n```\nExtra explanation.<|eot_id|>", allowed)
        assertEquals(listOf("observe", "expert"), actions.map { it.id })
    }
    @Test fun actualPotatoFailureStillRejectsRepeatedIdsAfterRemovingProse() {
        val raw = "Here are the selected action IDs in JSON format:\n```\n{\"action_ids\":[\"observe\",\"expert\",\"retake\",\"retake\",\"retake\"]}\n```"
        assertTrue(runCatching { ExecuTorchAdvisoryEngine.validateSelection(raw, allowed) }.isFailure)
    }
    @Test fun expandedPotatoOutputMayIncludeEveryApprovedAction() {
        // The v0.3 phone returned all ten eligible IDs, not the old five-item subset.
        val ids = listOf("expert", "potato_debris", "potato_seed", "potato_early_signs", "potato_water_feed",
            "potato_early_control", "potato_fungicide", "potato_rotation", "potato_early_dry", "potato_early_harvest")
        val actions = ids.map { GuidanceAction(it, it, it, "fixture", emptySet()) }
        val raw = org.json.JSONObject().put("action_ids", org.json.JSONArray(ids)).toString()
        assertEquals(ids, ExecuTorchAdvisoryEngine.validateSelection(raw, actions).map { it.id })
        assertTrue(runCatching { ExecuTorchAdvisoryEngine.validateSelection(raw, allowed) }.isFailure)
    }
    @Test fun ambiguousTruncatedAndWrongTypeResponsesAreRejected() {
        for (raw in listOf(
            "{\"action_ids\":[\"expert\"]} {\"action_ids\":[\"observe\"]}",
            "{\"action_ids\":[\"expert\"]", "{\"action_ids\":[23]}",
            "{\"action_ids\":\"expert\"}", "{\"action_ids\":[{\"id\":\"expert\"}]}",
            "No JSON here", "x".repeat(16_385)
        )) assertTrue(runCatching { ExecuTorchAdvisoryEngine.validateSelection(raw, allowed) }.isFailure)
    }
}
