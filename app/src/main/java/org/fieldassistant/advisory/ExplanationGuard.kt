package org.fieldassistant.advisory

/** Conservative format/topic screening, NOT a proof of factual accuracy.
 * Chemicals, rates and cure claims belong only to the cited, saved treatment cards. */
object ExplanationGuard {
    fun validate(raw: String): String {
        val text = raw.substringBefore("<|eot_id|>").substringBefore("<|end_of_text|>").trim()
        require(text.length in 60..650 && text.split(Regex("\\s+")).size in 12..95) { "Explanation length invalid" }
        require(text.all { it.code in 32..126 || it == '\n' }) { "English explanation required" }
        require(text.none { it.isDigit() } && !text.contains(Regex("[{}<>%]"))) { "Unexpected quantities or markup" }
        val prohibited = Regex("\\b(spray\\w*|fungicid\\w*|pesticid\\w*|insecticid\\w*|herbicid\\w*|chemical\\w*|mancozeb|chlorothalonil|captan|copper|neem|dose\\w*|dosage\\w*|mix\\w*|cure\\w*|guarantee\\w*|definitely|certainly|severe|severity|diagnosed|confirmed|kill\\w*|burn\\w*|bleach|soap|oil|baking|antibiotic\\w*)\\b", RegexOption.IGNORE_CASE)
        require(!prohibited.containsMatchIn(text)) { "Explanation includes restricted treatment or certainty language" }
        require(text.contains(Regex("\\b(may|might|possible|could|if)\\b", RegexOption.IGNORE_CASE))) { "Explanation must preserve uncertainty" }
        return text
    }
}
