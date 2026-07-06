/*
DrawAnywhere: An Android application that lets you draw on top of other apps.
Copyright (C) 2025-2026 shezik

This program is free software: you can redistribute it and/or modify it under the
terms of the GNU Affero General Public License as published by the Free Software
Foundation, either version 3 of the License, or any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along
with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.shezik.drawanywhere

import androidx.compose.ui.geometry.Offset
import com.shezik.drawanywhere.drawing.StrokeTool
import com.shezik.drawanywhere.drawing.ToolContext
import com.shezik.drawanywhere.model.DrawAction
import com.shezik.drawanywhere.model.Stroke
import com.shezik.drawanywhere.model.PenConfig
import com.shezik.drawanywhere.model.StrokeSample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the stroke list and undo/redo stack. Drawing logic is delegated to
 * [StrokeTool] implementations via [PenType.createTool].
 */
class DrawController(initialConfig: PenConfig) {
    var penConfig: PenConfig = initialConfig
        private set

    fun setPenConfig(config: PenConfig) {
        penConfig = config
    }

    var onStrokesChanged: (() -> Unit)? = null

    private val _strokeList = mutableListOf<Stroke>()
    val strokeList: List<Stroke>
        get() = _strokeList

    private val undoRedo = UndoRedoManager()
    val canUndo: StateFlow<Boolean> = undoRedo.canUndo
    val canRedo: StateFlow<Boolean> = undoRedo.canRedo

    private val _canClear = MutableStateFlow(false)
    val canClearStrokes: StateFlow<Boolean> = _canClear.asStateFlow()

    private var activeTool: StrokeTool? = null

    private val toolContext get() = ToolContext(
        strokes = _strokeList,
        penConfig = penConfig,
        onUndoPush = undoRedo::push,
        onChanged = ::notifyChanged,
    )

    private fun notifyChanged() {
        _canClear.value = _strokeList.isNotEmpty()
        onStrokesChanged?.invoke()
    }

    fun createStroke(newPoint: Offset) {
        createStroke(StrokeSample(newPoint))
    }

    fun createStroke(sample: StrokeSample) {
        val tool = penConfig.penType.createTool(toolContext)
        activeTool = tool
        tool.onStart(sample)
    }

    fun updateLatestStroke(newPoint: Offset) {
        updateLatestStroke(StrokeSample(newPoint))
    }

    fun updateLatestStroke(sample: StrokeSample) {
        activeTool?.onMove(sample)
    }

    fun finishStroke() {
        activeTool?.onFinish()
        activeTool = null
    }

    fun clearStrokes() {
        if (_strokeList.isEmpty()) return
        undoRedo.push(DrawAction.ClearStrokes(_strokeList.toList()))
        _strokeList.clear()
        notifyChanged()
    }

    fun removeExpiredStrokes(now: Long) {
        if (
            _strokeList.removeAll { stroke ->
                val ttl = stroke.penType.ttlMs ?: return@removeAll false
                now - stroke.modifiedAt > ttl
            }
        ) notifyChanged()
    }

    fun undo() {
        val action = undoRedo.popUndo() ?: return
        when (action) {
            is DrawAction.AddStroke -> {
                if (_strokeList.remove(action.stroke)) undoRedo.pushRedo(action)
            }
            is DrawAction.EraseStroke -> {
                _strokeList.add(action.stroke)
                undoRedo.pushRedo(action)
            }
            is DrawAction.ClearStrokes -> {
                _strokeList.addAll(action.strokes)
                undoRedo.pushRedo(action)
            }
            is DrawAction.CanvasSnapshot -> {
                _strokeList.clear()
                _strokeList.addAll(action.before)
                undoRedo.pushRedo(action)
            }
        }
        notifyChanged()
    }

    fun redo() {
        val action = undoRedo.popRedo() ?: return
        when (action) {
            is DrawAction.AddStroke -> {
                _strokeList.add(action.stroke)
                undoRedo.push(action, clearRedo = false)
            }
            is DrawAction.EraseStroke -> {
                if (_strokeList.remove(action.stroke)) undoRedo.push(action, clearRedo = false)
            }
            is DrawAction.ClearStrokes -> {
                _strokeList.removeAll(action.strokes)
                undoRedo.push(action, clearRedo = false)
            }
            is DrawAction.CanvasSnapshot -> {
                _strokeList.clear()
                _strokeList.addAll(action.after)
                undoRedo.push(action, clearRedo = false)
            }
        }
        notifyChanged()
    }
}
