package com.example.kanjilearning.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

class KanjiDrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 14f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val dotPaint = Paint(strokePaint).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DADADA")
        strokeWidth = 1.5f * resources.displayMetrics.density
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B8B8B8")
        strokeWidth = 1.8f * resources.displayMetrics.density
    }

    private var renderedStrokes: MutableList<StrokePath> = mutableListOf()
    private var touchActive = false

    var onStrokeStart: ((Offset) -> Unit)? = null
    var onStrokeContinue: ((Offset) -> Unit)? = null
    var onDrawingStateChanged: ((Boolean) -> Unit)? = null

    fun syncFromState(strokes: List<StrokePath>) {
        if (touchActive) return

        if (!sameAsRendered(strokes)) {
            renderedStrokes = strokes.map { StrokePath(it.points.toMutableList()) }.toMutableList()
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)

        val thirdX = width / 3f
        val thirdY = height / 3f
        canvas.drawLine(thirdX, 0f, thirdX, height.toFloat(), gridPaint)
        canvas.drawLine(thirdX * 2f, 0f, thirdX * 2f, height.toFloat(), gridPaint)
        canvas.drawLine(0f, thirdY, width.toFloat(), thirdY, gridPaint)
        canvas.drawLine(0f, thirdY * 2f, width.toFloat(), thirdY * 2f, gridPaint)
        canvas.drawLine(width / 2f, 0f, width / 2f, height.toFloat(), centerPaint)
        canvas.drawLine(0f, height / 2f, width.toFloat(), height / 2f, centerPaint)

        renderedStrokes.forEach { stroke ->
            if (stroke.points.isEmpty()) return@forEach

            if (stroke.points.size == 1) {
                val point = stroke.points.first()
                canvas.drawCircle(point.x * width, point.y * height, strokePaint.strokeWidth / 2.3f, dotPaint)
                return@forEach
            }

            val path = Path().apply {
                moveTo(stroke.points.first().x * width, stroke.points.first().y * height)
                stroke.points.drop(1).forEach { point ->
                    lineTo(point.x * width, point.y * height)
                }
            }
            canvas.drawPath(path, strokePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                touchActive = true
                onDrawingStateChanged?.invoke(true)

                val point = normalize(event.x, event.y)
                renderedStrokes.add(StrokePath(mutableListOf(point)))
                onStrokeStart?.invoke(point)
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                appendPointsFromEvent(event)
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                appendPointsFromEvent(event)
                finishTouch()
                performClick()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                finishTouch()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun finishTouch() {
        touchActive = false
        onDrawingStateChanged?.invoke(false)
        parent?.requestDisallowInterceptTouchEvent(false)
        invalidate()
    }

    private fun appendPointsFromEvent(event: MotionEvent) {
        for (index in 0 until event.historySize) {
            appendPoint(normalize(event.getHistoricalX(index), event.getHistoricalY(index)))
        }
        appendPoint(normalize(event.x, event.y))
    }

    private fun appendPoint(point: Offset) {
        val activeStroke = renderedStrokes.lastOrNull() ?: return
        val lastPoint = activeStroke.points.lastOrNull()

        if (lastPoint != null && isNearlySamePoint(lastPoint, point)) {
            return
        }

        activeStroke.points.add(point)
        onStrokeContinue?.invoke(point)
    }

    private fun normalize(x: Float, y: Float): Offset {
        val normalizedX = (x / width.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        val normalizedY = (y / height.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        return Offset(normalizedX, normalizedY)
    }

    private fun sameAsRendered(strokes: List<StrokePath>): Boolean {
        if (renderedStrokes.size != strokes.size) return false

        return renderedStrokes.zip(strokes).all { (current, incoming) ->
            if (current.points.size != incoming.points.size) return@all false

            current.points.zip(incoming.points).all { (left, right) ->
                isNearlySamePoint(left, right)
            }
        }
    }

    private fun isNearlySamePoint(first: Offset, second: Offset): Boolean {
        return abs(first.x - second.x) < 0.001f && abs(first.y - second.y) < 0.001f
    }
}
