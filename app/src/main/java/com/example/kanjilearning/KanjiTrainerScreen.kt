package com.example.kanjilearning

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kanjilearning.data.KanjiDeck
import com.example.kanjilearning.logic.DrawingResult
import com.example.kanjilearning.ui.KanjiDrawingView
import com.example.kanjilearning.ui.KanjiViewModel
import com.example.kanjilearning.ui.StrokeOrderDiagram
import com.example.kanjilearning.ui.StrokePath

@Composable
fun KanjiTrainerScreen(vm: KanjiViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    var isDrawing by remember { mutableStateOf(false) }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = !isDrawing
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Kanji Trainer (Top 100)", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Kanji: ${state.current.kanji}", fontSize = 48.sp)
                        Text("Hiragana: ${state.current.hiragana}")
                        Text("English: ${state.current.english}")
                        Text("German: ${state.current.german}")
                        StrokeOrderDiagram(
                            kanji = state.current.kanji,
                            strokeOrderHint = state.current.strokeOrderHint,
                            strokeCount = state.current.strokeCount,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Write the kanji here", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        HandwritingPad(
                            strokes = state.strokes,
                            onStrokeStart = vm::startStroke,
                            onStrokeContinue = vm::continueStroke,
                            onDrawingStateChanged = { isDrawing = it }
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Scrolling is disabled while your finger is down so vertical strokes stay inside the writing area.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = vm::evaluate) { Text("Check") }
                            Button(onClick = vm::clearStrokes) { Text("Clear") }
                            Button(onClick = vm::loadNext) { Text("Next (Random)") }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Progress", fontWeight = FontWeight.Bold)
                        Text("Attempts: ${state.attemptCount}")
                        Text("Correct: ${state.correctCount}")
                        val rate = if (state.attemptCount == 0) 0 else (100 * state.correctCount / state.attemptCount)
                        Text("Success rate: $rate%")
                        state.evaluation?.let { evaluation ->
                            val label = when (evaluation.result) {
                                DrawingResult.CORRECT -> "Correct"
                                DrawingResult.PARTLY_FALSE -> "Partly false"
                                DrawingResult.FALSE -> "False"
                            }
                            Text(
                                "Result: $label (score ${"%.2f".format(evaluation.score)})",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(evaluation.details)
                        }
                    }
                }
            }
            item {
                Text("Kanji list", fontWeight = FontWeight.Bold)
            }
            items(KanjiDeck.top100) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.kanji, fontSize = 30.sp, modifier = Modifier.size(48.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.hiragana, fontWeight = FontWeight.SemiBold)
                            Text("${item.english} / ${item.german}")
                            Text("Strokes: ${item.strokeCount}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HandwritingPad(
    strokes: List<StrokePath>,
    onStrokeStart: (Offset) -> Unit,
    onStrokeContinue: (Offset) -> Unit,
    onDrawingStateChanged: (Boolean) -> Unit
) {
    AndroidView(
        factory = { context ->
            KanjiDrawingView(context).apply {
                this.onStrokeStart = onStrokeStart
                this.onStrokeContinue = onStrokeContinue
                this.onDrawingStateChanged = onDrawingStateChanged
            }
        },
        update = { view ->
            view.onStrokeStart = onStrokeStart
            view.onStrokeContinue = onStrokeContinue
            view.onDrawingStateChanged = onDrawingStateChanged
            view.syncFromState(strokes)
        },
        modifier = Modifier
            .size(300.dp)
            .border(2.dp, Color.Gray)
            .background(Color.White)
    )
}
