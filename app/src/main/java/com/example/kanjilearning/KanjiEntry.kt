package com.example.kanjilearning.data

data class KanjiEntry(
    val kanji: String,
    val hiragana: String,
    val english: String,
    val german: String,
    val strokeCount: Int,
    val strokeOrderHint: String
)
