package com.shezik.drawanywhere.model

import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class StrokeModifier {
    None, PrimaryButton, SecondaryButton, Both
}

data class StrokeSample(
    val position: Offset,
    val pressure: Float = 1f,
    val tilt: Float? = null,
    val orientation: Float? = null,
    val timeMs: Long = System.currentTimeMillis(),
)

data class Stroke(
    internal val _points: MutableList<Offset> = mutableListOf(),
    internal val _samples: MutableList<StrokeSample> = mutableListOf(),
    val color: Color,
    val width: Float,
    val alpha: Float,
    val penType: PenType = PenType.Pen,
    val createdAt: Long = System.currentTimeMillis(),
    var modifiedAt: Long = createdAt,
) {
    val points: List<Offset> get() = _points
    val samples: List<StrokeSample> get() = _samples
    val hasPressureSamples: Boolean
        get() = _samples.size == _points.size && _samples.any { it.pressure != 1f }

    fun addSample(sample: StrokeSample) {
        _points.add(sample.position)
        _samples.add(sample)
    }

    fun replaceSample(index: Int, sample: StrokeSample) {
        _points[index] = sample.position
        if (_samples.size == _points.size) {
            _samples[index] = sample
        }
    }

    fun render(canvas: Canvas, paint: Paint) {
        if (_points.isEmpty()) return
        paint.strokeWidth = width
        val argb = color.toArgb()
        val combinedAlpha = (color.alpha * alpha * 255).toInt().coerceIn(0, 255)
        paint.color = (argb and 0x00FFFFFF) or (combinedAlpha shl 24)
        penType.renderer.render(this, canvas, paint, System.currentTimeMillis())
    }
}

sealed class DrawAction {
    data class AddStroke(val stroke: Stroke) : DrawAction()
    data class EraseStroke(val stroke: Stroke) : DrawAction()
    data class ClearStrokes(val strokes: List<Stroke>) : DrawAction()
    data class CanvasSnapshot(val before: List<Stroke>, val after: List<Stroke>) : DrawAction()

    /** Returns this action with ephemeral strokes removed, or null if nothing remains. */
    fun withoutEphemeral(): DrawAction? = when (this) {
        is AddStroke -> if (stroke.penType.isEphemeral) null else this
        is EraseStroke -> if (stroke.penType.isEphemeral) null else this
        is ClearStrokes -> {
            val f = strokes.filter { !it.penType.isEphemeral }
            if (f.isEmpty()) null else copy(strokes = f)
        }
        is CanvasSnapshot -> {
            val b = before.filter { !it.penType.isEphemeral }
            val a = after.filter { !it.penType.isEphemeral }
            if (b.isEmpty() && a.isEmpty()) null
            else if (b == before && a == after) this
            else copy(before = b, after = a)
        }
    }
}
