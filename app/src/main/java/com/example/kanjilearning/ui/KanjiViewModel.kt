package com.example.kanjilearning.ui

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.kanjilearning.data.KanjiDeck
import com.example.kanjilearning.data.KanjiEntry
import com.example.kanjilearning.logic.DrawingResult
import com.example.kanjilearning.logic.Evaluation
import com.example.kanjilearning.logic.StrokeEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

private val Application.dataStore by preferencesDataStore(name = "kanji_progress")

private val correctKey = intPreferencesKey("correct_count")
private val attemptsKey = intPreferencesKey("attempt_count")

data class StrokePath(val points: MutableList<Offset> = mutableListOf())

data class UiState(
    val current: KanjiEntry,
    val queue: List<KanjiEntry>,
    val strokes: List<StrokePath> = emptyList(),
    val evaluation: Evaluation? = null,
    val correctCount: Int = 0,
    val attemptCount: Int = 0
)

class KanjiViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(
        UiState(
            current = KanjiDeck.top100.first(),
            queue = KanjiDeck.top100.shuffled()
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val mutableStrokes = mutableListOf<StrokePath>()

    init {
        viewModelScope.launch {
            val prefs = getApplication<Application>().dataStore.data.first()
            _uiState.value = _uiState.value.copy(
                correctCount = prefs[correctKey] ?: 0,
                attemptCount = prefs[attemptsKey] ?: 0
            )
            loadNext()
        }
    }

    fun startStroke(point: Offset) {
        val path = StrokePath(mutableListOf(point))
        mutableStrokes.add(path)
        _uiState.value = _uiState.value.copy(strokes = mutableStrokes.toList(), evaluation = null)
    }

    fun continueStroke(point: Offset) {
        mutableStrokes.lastOrNull()?.points?.add(point)
        _uiState.value = _uiState.value.copy(strokes = mutableStrokes.toList())
    }

    fun clearStrokes() {
        mutableStrokes.clear()
        _uiState.value = _uiState.value.copy(strokes = emptyList(), evaluation = null)
    }

    fun evaluate() {
        val current = _uiState.value.current
        val evaluation = StrokeEvaluator.evaluate(mutableStrokes, current.kanji, current.strokeCount)
        val newAttempts = _uiState.value.attemptCount + 1
        val newCorrect = _uiState.value.correctCount + if (evaluation.result == DrawingResult.CORRECT) 1 else 0
        _uiState.value = _uiState.value.copy(
            evaluation = evaluation,
            attemptCount = newAttempts,
            correctCount = newCorrect
        )

        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[attemptsKey] = newAttempts
                prefs[correctKey] = newCorrect
            }
        }
    }

    fun loadNext() {
        val currentQueue = _uiState.value.queue.toMutableList()
        if (currentQueue.isEmpty()) {
            currentQueue.addAll(KanjiDeck.top100.shuffled(Random(System.currentTimeMillis())))
        }
        val next = currentQueue.removeAt(0)
        mutableStrokes.clear()
        _uiState.value = _uiState.value.copy(current = next, queue = currentQueue, strokes = emptyList(), evaluation = null)
    }
}

