package org.fieldassistant.advisory

import android.content.Context
import org.fieldassistant.core.Finding
import org.fieldassistant.core.FindingStatus
import org.json.JSONObject

data class GuidanceAction(val id: String, val english: String, val tamil: String, val sourceId: String, val labels: Set<String>,
    val telugu: String = english, val section: String = "care") {
    fun text(tamilLanguage: Boolean) = if (tamilLanguage) tamil else english
    fun text(language: String) = when (language) { "ta" -> tamil; "te" -> telugu; else -> english }
}
data class GuidanceSource(val id: String, val title: String, val url: String)
interface GuidanceRepository {
    val sources: List<GuidanceSource>
    fun allowedActions(finding: Finding, region: String): List<GuidanceAction>
    fun reminders(): List<GuidanceAction> = emptyList()
}
class AssetGuidanceRepository internal constructor(private val json: JSONObject) : GuidanceRepository {
    constructor(context: Context) : this(JSONObject(context.assets.open("guidance.json").bufferedReader().use { it.readText() }))
    override val sources = json.getJSONArray("sources").let { rows -> (0 until rows.length()).map { i ->
        rows.getJSONObject(i).let { GuidanceSource(it.getString("id"), it.getString("title"), it.getString("url")) }
    } }
    private val actions = json.getJSONArray("actions").let { rows -> (0 until rows.length()).map { i ->
        val row = rows.getJSONObject(i)
        val labels = row.getJSONArray("labels")
        GuidanceAction(row.getString("id"), row.getString("en"), row.getString("ta"), row.getString("source"),
            (0 until labels.length()).map { labels.getString(it) }.toSet(), row.getString("te"), row.getString("section"))
    } }
    override fun allowedActions(finding: Finding, region: String): List<GuidanceAction> {
        val label = finding.candidate?.label?.id.takeIf { finding.status == FindingStatus.POSSIBLE_MATCH && region == "Tamil Nadu, India" }
        return actions.filter { it.labels.isEmpty() || label in it.labels }
    }
    override fun reminders() = actions.filter { it.section == "reminder" }
}
