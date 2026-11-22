package org.fieldassistant.ui

/** Preserve sentence/paragraph boundaries and never exceed a synthesis request limit. */
object SpeechText {
    fun chunks(text: String, limit: Int = 1200): List<String> {
        require(limit >= 32)
        return text.split(Regex("\\n+|(?<=[.!?।])\\s+"))
            .flatMap { paragraph ->
                val result = mutableListOf<String>()
                var remaining = paragraph.trim()
                while (remaining.length > limit) {
                    val split = remaining.lastIndexOf(' ', limit).takeIf { it > 0 } ?: limit
                    result.add(remaining.substring(0, split).trim())
                    remaining = remaining.substring(split).trim()
                }
                if (remaining.isNotEmpty()) result.add(remaining)
                result
            }
    }
}
