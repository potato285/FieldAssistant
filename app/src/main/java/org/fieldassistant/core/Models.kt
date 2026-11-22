package org.fieldassistant.core

data class CropLabel(val id: String, val crop: String, val condition: String, val healthy: Boolean)
data class Candidate(val label: CropLabel, val score: Float)
data class VisionResult(
    val candidates: List<Candidate>,
    val modelId: String,
    val modelSha256: String,
    val inferenceMs: Double,
    val preprocessingMs: Double
)
enum class FindingStatus { POSSIBLE_MATCH, UNCERTAIN, CROP_CONFLICT, UNSUPPORTED, NEEDS_CROP }
data class Finding(val status: FindingStatus, val selectedCrop: String?, val vision: VisionResult?) {
    // No validated measurement exists. This must never be derived from a class score.
    val severity: String = "Not assessed"
    val candidate: Candidate? get() = vision?.candidates?.firstOrNull()
}

interface ResultAggregator {
    fun aggregate(result: VisionResult?, crop: String?): Finding
}

class ConservativeResultAggregator(private val supportedCrops: Set<String>) : ResultAggregator {
    override fun aggregate(result: VisionResult?, crop: String?): Finding {
        if (crop != null && crop !in supportedCrops) return Finding(FindingStatus.UNSUPPORTED, crop, result)
        val sorted = result?.candidates.orEmpty().sortedByDescending { it.score }
        if (sorted.isEmpty() || sorted.any { !it.score.isFinite() || it.score !in 0f..1f })
            return Finding(FindingStatus.UNCERTAIN, crop, result)
        val orderedResult = result?.copy(candidates = sorted)
        val top = sorted.first()
        // Never renormalize or filter scores to force a match to the user's crop.
        if (crop != null && crop != top.label.crop) return Finding(FindingStatus.CROP_CONFLICT, crop, orderedResult)
        val margin = top.score - (sorted.getOrNull(1)?.score ?: 0f)
        // Uncalibrated prototype heuristics, not a validated out-of-distribution detector.
        if (top.score < 0.65f || margin < 0.15f) return Finding(FindingStatus.UNCERTAIN, crop, orderedResult)
        if (crop == null) return Finding(FindingStatus.NEEDS_CROP, null, orderedResult)
        return Finding(FindingStatus.POSSIBLE_MATCH, crop, orderedResult)
    }
}
