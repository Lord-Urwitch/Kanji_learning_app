package com.example.kanjilearning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kanjilearning.logic.DrawingResult
import com.example.kanjilearning.ui.KanjiViewModel
import com.example.kanjilearning.ui.StrokePath

class MainActivity : ComponentActivity() {
    private val vm: KanjiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                KanjiApp(vm)
            }
        }
    }
}

@Composable
fun KanjiApp(vm: KanjiViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Kanji Trainer (Top 100)", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Kanji: ${state.current.kanji}", fontSize = 48.sp)
                        Text("Hiragana: ${state.current.hiragana}")
                        Text("English: ${state.current.english}")
                        Text("German: ${state.current.german}")
                        Text("Stroke order: ${state.current.strokeOrderHint}")
                        Text("Expected strokes: ${state.current.strokeCount}")
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Write the kanji here", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        DrawPad(
                            strokes = state.strokes,
                            onStrokeStart = vm::startStroke,
                            onStrokeContinue = vm::continueStroke
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
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Progress", fontWeight = FontWeight.Bold)
                        Text("Attempts: ${state.attemptCount}")
                        Text("Correct: ${state.correctCount}")
                        val rate = if (state.attemptCount == 0) 0 else (100 * state.correctCount / state.attemptCount)
                        Text("Success rate: $rate%")
                        state.evaluation?.let {
                            val label = when (it.result) {
                                DrawingResult.CORRECT -> "✅ Correct"
                                DrawingResult.PARTLY_FALSE -> "🟡 Partly false"
                                DrawingResult.FALSE -> "❌ False"
                            }
                            Text("Result: $label (score ${"%.2f".format(it.score)})", fontWeight = FontWeight.SemiBold)
                            Text(it.details)
                        }
                    }
                }
            }
            item {
                Text("Kanji list", fontWeight = FontWeight.Bold)
            }
            items(com.example.kanjilearning.data.KanjiDeck.top100) { item ->
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
private fun DrawPad(
    strokes: List<StrokePath>,
    onStrokeStart: (Offset) -> Unit,
    onStrokeContinue: (Offset) -> Unit
) {
    Canvas(
        modifier = Modifier
            .size(300.dp)
            .border(2.dp, Color.Gray)
            .background(Color.White)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onStrokeStart(offset.toNormalized())
                    },
                    onDrag = { change, _ ->
                        onStrokeContinue(change.position.toNormalized())
                    }
                )
            }
    ) {
        strokes.forEach { stroke ->
            if (stroke.points.isEmpty()) return@forEach
            val path = Path().apply {
                moveTo(stroke.points.first().x * size.width, stroke.points.first().y * size.height)
                stroke.points.drop(1).forEach { p ->
                    lineTo(p.x * size.width, p.y * size.height)
                }
            }
            drawPath(path = path, color = Color.Black, style = Stroke(width = 14f, cap = StrokeCap.Round))
        }
    }
}

private fun Offset.toNormalized(): Offset {
    val x = (x / 300f).coerceIn(0f, 1f)
    val y = (y / 300f).coerceIn(0f, 1f)
    return Offset(x, y)
}
