package com.example.kanjilearning.data

object KanjiStrokeDiagram {
    private const val kanjiVgBaseUrl = "https://raw.githubusercontent.com/KanjiVG/kanjivg/master/kanji"

    fun svgUrlFor(kanji: String): String? {
        val codePoints = kanji.codePoints().toArray()
        if (codePoints.size != 1) return null

        val codePoint = codePoints.first()
        val hexCode = codePoint.toString(16).padStart(5, '0')
        return "$kanjiVgBaseUrl/$hexCode.svg"
    }
}
