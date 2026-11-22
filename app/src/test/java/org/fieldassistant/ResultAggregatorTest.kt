package org.fieldassistant

import org.fieldassistant.core.*
import org.junit.Assert.*
import org.junit.Test

class ResultAggregatorTest {
    private val aggregator = ConservativeResultAggregator(setOf("Tomato", "Potato"))
    // Synthetic score fixtures test routing only; never used as application predictions.
    private fun scores(first: Float = 0.91f, second: Float = 0.03f) = VisionResult(listOf(
        Candidate(CropLabel("tomato_early_blight", "Tomato", "Early Blight", false), first),
        Candidate(CropLabel("potato_late_blight", "Potato", "Late Blight", false), second)
    ), "TEST FIXTURE", "NOT A MODEL", 1.0, 1.0)

    @Test fun unsupportedSapodillaCannotBeOverriddenByHighTomatoScore() {
        val finding = aggregator.aggregate(scores(0.99f), "Sapodilla")
        assertEquals(FindingStatus.UNSUPPORTED, finding.status)
        assertEquals("Not assessed", finding.severity)
    }
    @Test fun manuallyCorrectedCropDoesNotRenormalizeUnrelatedScores() {
        val finding = aggregator.aggregate(scores(), "Potato")
        assertEquals(FindingStatus.CROP_CONFLICT, finding.status)
        assertEquals(0.91f, finding.candidate!!.score)
    }
    @Test fun noCropRequiresConfirmationEvenWithHighScore() {
        assertEquals(FindingStatus.NEEDS_CROP, aggregator.aggregate(scores(), null).status)
    }
    @Test fun ambiguousPredictionsStayUncertain() {
        assertEquals(FindingStatus.UNCERTAIN, aggregator.aggregate(scores(0.52f, 0.44f), "Tomato").status)
    }
    @Test fun matchingCropProducesOnlyPossibleMatchAndNoSeverity() {
        val finding = aggregator.aggregate(scores(), "Tomato")
        assertEquals(FindingStatus.POSSIBLE_MATCH, finding.status)
        assertEquals("Not assessed", finding.severity)
    }
    @Test fun corruptOrMissingScoresCannotProduceDiagnosis() {
        assertEquals(FindingStatus.UNCERTAIN, aggregator.aggregate(scores(Float.NaN), "Tomato").status)
        assertEquals(FindingStatus.UNCERTAIN, aggregator.aggregate(null, "Tomato").status)
    }
    @Test fun replaceableEngineCanReturnUnsortedCandidates() {
        val result = scores().let { it.copy(candidates = it.candidates.reversed()) }
        val finding = aggregator.aggregate(result, "Tomato")
        assertEquals(FindingStatus.POSSIBLE_MATCH, finding.status)
        assertEquals("tomato_early_blight", finding.candidate!!.label.id)
    }
}
