package com.example.kanjilearning.logic

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.example.kanjilearning.ui.StrokePath
import kotlin.math.max

enum class DrawingResult {
    CORRECT,
    PARTLY_FALSE,
    FALSE
}

data class Evaluation(
    val result: DrawingResult,
    val score: Float,
    val details: String
)

object StrokeEvaluator {
    private const val SIZE = 256

    fun evaluate(inputStrokes: List<StrokePath>, kanji: String, expectedStrokeCount: Int): Evaluation {
        if (inputStrokes.isEmpty()) {
            return Evaluation(DrawingResult.FALSE, 0f, "No strokes detected.")
        }

        val inputBitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val targetBitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        drawUserStrokes(inputBitmap, inputStrokes)
        drawKanjiTarget(targetBitmap, kanji)

        var overlap = 0
        var union = 0
        var inputPixels = 0
        var targetPixels = 0

        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                val i = inputBitmap.getPixel(x, y) != Color.WHITE
                val t = targetBitmap.getPixel(x, y) != Color.WHITE
                if (i) inputPixels++
                if (t) targetPixels++
                if (i && t) overlap++
                if (i || t) union++
            }
        }

        val iou = if (union == 0) 0f else overlap.toFloat() / union
        val coverage = if (targetPixels == 0) 0f else overlap.toFloat() / targetPixels
        val precision = if (inputPixels == 0) 0f else overlap.toFloat() / inputPixels
        val strokeRatio = 1f - ((inputStrokes.size - expectedStrokeCount).toFloat() / max(expectedStrokeCount, 1)).let { kotlin.math.abs(it).coerceAtMost(1f) }

        val score = 0.45f * iou + 0.25f * coverage + 0.2f * precision + 0.1f * strokeRatio
        val result = when {
            score >= 0.68f && coverage >= 0.55f -> DrawingResult.CORRECT
            score >= 0.45f -> DrawingResult.PARTLY_FALSE
            else -> DrawingResult.FALSE
        }

        return Evaluation(
            result = result,
            score = score,
            details = "IoU=${"%.2f".format(iou)}, coverage=${"%.2f".format(coverage)}, precision=${"%.2f".format(precision)}"
        )
    }

    private fun drawUserStrokes(bitmap: Bitmap, strokes: List<StrokePath>) {
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 22f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        strokes.forEach { stroke ->
            val path = Path()
            stroke.points.firstOrNull()?.let { first ->
                path.moveTo(first.x * SIZE, first.y * SIZE)
                stroke.points.drop(1).forEach { point ->
                    path.lineTo(point.x * SIZE, point.y * SIZE)
                }
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun drawKanjiTarget(bitmap: Bitmap, kanji: String) {
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 190f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val x = SIZE / 2f
        val y = SIZE / 2f - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(kanji, x, y, paint)
    }
}


