package com.shezik.drawanywhere.drawing

import androidx.compose.ui.geometry.Offset
import com.shezik.drawanywhere.model.DrawAction
import com.shezik.drawanywhere.model.Stroke
import com.shezik.drawanywhere.model.StrokeSample

/**
 * Rectangle / Ellipse — creates a 2-point stroke, replaces second point on move,
 * normalizes on finish.
 */
class ShapeTool(private val ctx: ToolContext) : StrokeTool {

    override fun onStart(sample: StrokeSample) {
        val point = sample.position
        ctx.strokes.add(Stroke(
            _points = mutableListOf(point, point),
            color = ctx.penConfig.color,
            width = ctx.penConfig.width,
            alpha = ctx.penConfig.alpha,
            penType = ctx.penConfig.penType,
        ))
    }

    override fun onMove(sample: StrokeSample) {
        val point = sample.position
        ctx.strokes.lastOrNull()?.let { it._points[1] = point }
    }

    override fun onFinish() {
        val stroke = ctx.strokes.lastOrNull() ?: return
        val p0 = stroke._points[0]; val p1 = stroke._points[1]
        val left = minOf(p0.x, p1.x); val top = minOf(p0.y, p1.y)
        val right = maxOf(p0.x, p1.x); val bottom = maxOf(p0.y, p1.y)
        if (right - left < 4f && bottom - top < 4f) {
            ctx.strokes.removeAt(ctx.strokes.lastIndex)
            return
        }
        stroke._points[0] = Offset(left, top)
        stroke._points[1] = Offset(right, bottom)
        ctx.pushUndo(DrawAction.AddStroke(stroke))
        ctx.notifyChanged()
    }
}
