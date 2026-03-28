package com.example.kanjilearning

import com.example.kanjilearning.data.KanjiDeck
import com.example.kanjilearning.data.KanjiStrokeDiagram
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class KanjiDataTest {
    @Test
    fun topDeckContainsOneHundredEntries() {
        assertEquals(100, KanjiDeck.top100.size)
    }

    @Test
    fun strokeDiagramUrlMatchesKanjiVgConvention() {
        val url = KanjiStrokeDiagram.svgUrlFor("日")

        assertNotNull(url)
        assertEquals(
            "https://raw.githubusercontent.com/KanjiVG/kanjivg/master/kanji/065e5.svg",
            url
        )
    }
}
