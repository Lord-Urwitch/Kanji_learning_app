package com.example.kanjilearning.logic

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import com.example.kanjilearning.ui.StrokePath
import kotlin.math.abs
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
    private const val PADDING = 28f

    fun evaluate(inputStrokes: List<StrokePath>, kanji: String, expectedStrokeCount: Int): Evaluation {
        if (inputStrokes.isEmpty()) {
            return Evaluation(DrawingResult.FALSE, 0f, "No strokes detected.")
        }

        val normalizedStrokes = normalizeStrokes(inputStrokes)
        if (normalizedStrokes.isEmpty()) {
            return Evaluation(DrawingResult.FALSE, 0f, "The drawing is too small to evaluate.")
        }

        val inputBitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val targetBitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        drawUserStrokes(inputBitmap, normalizedStrokes)
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
        val strokeRatio = 1f - ((inputStrokes.size - expectedStrokeCount).toFloat() / max(expectedStrokeCount, 1))
            .let { abs(it).coerceAtMost(1f) }
        val targetAspect = targetAspectRatio(kanji)
        val inputAspect = aspectRatio(normalizedStrokes)
        val aspectScore = 1f - abs(inputAspect - targetAspect).coerceAtMost(1f)

        val score = 0.35f * iou + 0.25f * coverage + 0.15f * precision + 0.15f * strokeRatio + 0.10f * aspectScore
        val result = when {
            score >= 0.70f && coverage >= 0.58f && strokeRatio >= 0.6f -> DrawingResult.CORRECT
            score >= 0.45f -> DrawingResult.PARTLY_FALSE
            else -> DrawingResult.FALSE
        }

        return Evaluation(
            result = result,
            score = score,
            details = "Shape=${"%.2f".format(iou)}, coverage=${"%.2f".format(coverage)}, precision=${"%.2f".format(precision)}, stroke match=${"%.2f".format(strokeRatio)}"
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
        val dotPaint = Paint(paint).apply {
            style = Paint.Style.FILL
        }

        strokes.forEach { stroke ->
            val path = Path()
            stroke.points.firstOrNull()?.let { first ->
                val startX = first.x * SIZE
                val startY = first.y * SIZE
                path.moveTo(startX, startY)
                if (stroke.points.size == 1) {
                    canvas.drawCircle(startX, startY, paint.strokeWidth / 2.4f, dotPaint)
                }
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
            textSize = 200f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val x = SIZE / 2f
        val y = SIZE / 2f - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(kanji, x, y, paint)
    }

    private fun normalizeStrokes(strokes: List<StrokePath>): List<StrokePath> {
        val allPoints = strokes.flatMap { it.points }
        if (allPoints.isEmpty()) return emptyList()

        val minX = allPoints.minOf { it.x }
        val maxX = allPoints.maxOf { it.x }
        val minY = allPoints.minOf { it.y }
        val maxY = allPoints.maxOf { it.y }

        val width = max(maxX - minX, 0.02f)
        val height = max(maxY - minY, 0.02f)
        if (width <= 0.02f && height <= 0.02f) {
            return emptyList()
        }

        val drawingSpace = SIZE - PADDING * 2
        val scale = drawingSpace / max(width, height)
        val scaledWidth = width * scale
        val scaledHeight = height * scale
        val offsetX = (SIZE - scaledWidth) / 2f
        val offsetY = (SIZE - scaledHeight) / 2f

        return strokes.map { stroke ->
            StrokePath(
                stroke.points.map { point ->
                    val normalizedX = ((point.x - minX) * scale + offsetX) / SIZE
                    val normalizedY = ((point.y - minY) * scale + offsetY) / SIZE
                    Offset(normalizedX, normalizedY)
                }.toMutableList()
            )
        }
    }

    private fun aspectRatio(strokes: List<StrokePath>): Float {
        val allPoints = strokes.flatMap { it.points }
        if (allPoints.isEmpty()) return 1f

        val width = max(allPoints.maxOf { it.x } - allPoints.minOf { it.x }, 0.01f)
        val height = max(allPoints.maxOf { it.y } - allPoints.minOf { it.y }, 0.01f)
        return (width / height).coerceIn(0f, 2f)
    }

    private fun targetAspectRatio(kanji: String): Float {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        drawKanjiTarget(bitmap, kanji)

        var minX = SIZE
        var maxX = 0
        var minY = SIZE
        var maxY = 0

        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                if (bitmap.getPixel(x, y) != Color.WHITE) {
                    minX = minOf(minX, x)
                    maxX = maxOf(maxX, x)
                    minY = minOf(minY, y)
                    maxY = maxOf(maxY, y)
                }
            }
        }

        val width = max((maxX - minX).toFloat(), 1f)
        val height = max((maxY - minY).toFloat(), 1f)
        return (width / height).coerceIn(0f, 2f)
    }
}


