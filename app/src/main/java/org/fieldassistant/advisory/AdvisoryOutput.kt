package org.fieldassistant.advisory

import org.json.JSONObject

/** Accept a single generated JSON object, optionally surrounded by prose/code fences.
 * Surrounding prose is discarded, never displayed as agricultural advice. */
object AdvisoryOutput {
    fun validate(raw: String, allowed: List<GuidanceAction>): List<GuidanceAction> {
        require(raw.length <= 16_384) { "Advisory response too long" }
        var depth = 0
        var quoted = false
        var escaped = false
        var start = -1
        var end = -1
        for ((i, char) in raw.withIndex()) {
            if (depth == 0) {
                if (char == '{') {
                    require(start == -1) { "Multiple advisory objects" }
                    start = i
                    depth = 1
                }
            } else if (quoted) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == '"') quoted = false
            } else when (char) {
                '"' -> quoted = true
                '{' -> depth++
                '}' -> { depth--; if (depth == 0) end = i + 1 }
            }
        }
        require(start >= 0 && end > start && depth == 0) { "No complete advisory object" }
        val json = JSONObject(raw.substring(start, end))
        require(json.keys().asSequence().toSet() == setOf("action_ids")) { "Unexpected advisory fields" }
        val list = json.getJSONArray("action_ids")
        require(list.length() in 1..minOf(32, allowed.size)) { "Invalid action count" }
        val ids = (0 until list.length()).map {
            val id = list.get(it)
            require(id is String) { "Action ID must be text" }
            id
        }
        require(ids.distinct().size == ids.size && ids.all { id -> allowed.any { it.id == id } }) { "Unapproved or repeated advisory action" }
        // Expert confirmation is a product requirement, independent of model selection.
        return (ids + "expert").distinct().map { id -> allowed.first { it.id == id } }
    }
}
