package org.fieldassistant

import org.fieldassistant.ui.SpeechText
import org.junit.Assert.*
import org.junit.Test

class SpeechTextTest {
    @Test fun splitsParagraphsAndSentencesWithoutDroppingWords() {
        val text = "First step. Second step!\n\nதமிழ் வழிகாட்டுதல். తెలుగు సూచనలు."
        assertEquals(listOf("First step.", "Second step!", "தமிழ் வழிகாட்டுதல்.", "తెలుగు సూచనలు."), SpeechText.chunks(text))
    }
    @Test fun longTextFitsEngineLimitAndKeepsAllWords() {
        val text = (1..500).joinToString(" ") { "leaf$it" }
        val chunks = SpeechText.chunks(text, 64)
        assertTrue(chunks.all { it.length <= 64 })
        assertEquals(text, chunks.joinToString(" "))
        assertTrue(SpeechText.chunks(" \n ").isEmpty())
    }
}
