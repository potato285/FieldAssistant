package org.fieldassistant.advisory

import org.fieldassistant.core.Finding

data class Advisory(
    val actions: List<GuidanceAction>, val engine: String, val fallback: Boolean,
    val detail: String, val elapsedMs: Long = 0, val tokensPerSecond: Float? = null,
    val explanation: String? = null, val explanationIssue: String? = null
)
interface AdvisoryEngine {
    fun advise(finding: Finding, region: String): Advisory
}
class LocalGuidanceFallback(private val repository: GuidanceRepository) : AdvisoryEngine {
    override fun advise(finding: Finding, region: String) = Advisory(
        repository.allowedActions(finding, region), "Bundled guidance • fallback", true,
        "Static local guidance. Llama has not generated this advisory."
    )
}
