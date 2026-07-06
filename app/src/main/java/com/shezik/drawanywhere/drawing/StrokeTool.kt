package com.shezik.drawanywhere.drawing

import androidx.compose.ui.geometry.Offset
import com.shezik.drawanywhere.model.StrokeSample

/**
 * Lifecycle of a single stroke gesture: start → move (zero or more) → finish.
 *
 * Tools are instantiated per-gesture by [com.shezik.drawanywhere.model.PenType.createTool].
 * The same [ToolContext] is shared across all tools.
 */
interface StrokeTool {
    fun onStart(sample: StrokeSample)
    fun onMove(sample: StrokeSample)
    fun onFinish()

    fun onStart(point: Offset) = onStart(StrokeSample(point))
    fun onMove(point: Offset) = onMove(StrokeSample(point))
}
