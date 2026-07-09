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

package com.shezik.drawanywhere.view.canvas

import android.util.Log
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import com.shezik.drawanywhere.DrawViewModel
import com.shezik.drawanywhere.model.StrokeModifier
import com.shezik.drawanywhere.model.StrokeSample
import kotlin.math.sqrt

private const val FINGER_HOVER_DELAY_MS = 300L
private const val STYLUS_HOVER_DELAY_MS = 0L
private const val HOVER_FADE_MS = 200L
private const val PRESSURE_ERASER_RELEASE_GAP = 0.08f

/**
 * @param point Current or last-known pointer position.
 * @param fadeStartTimeMs Timestamp when fade-out began; 0 = still tracking (full opacity).
 * @param delayMs Pause before fading begins (finger: [FINGER_HOVER_DELAY_MS]; stylus: [STYLUS_HOVER_DELAY_MS]).
 * @param fadeMs Fade-out duration; set to 0 for instant off (debug).
 */
internal data class HoverState(
    val point: Offset,
    val fadeStartTimeMs: Long = 0L,
    val delayMs: Long = STYLUS_HOVER_DELAY_MS,
    val fadeMs: Long = HOVER_FADE_MS,
) {
    val isFading: Boolean get() = fadeStartTimeMs > 0L
}

/**
 * Touch dispatch for the drawing canvas. Routes [MotionEvent] into:
 *
 * ① **Multi-touch** — pan/zoom (2-finger) and tap gestures (2+, see [TapGestureDetector]).
 *    Three+ fingers suppress pan/zoom; only tap detection runs.
 * ② **Multi-touch entry** — second finger PTR_DOWN.
 * ③ **Single-pointer** — finger (debounced) or stylus (immediate).
 * ④ **Hover** — stylus/mouse hover tracks pointer position for size preview.
 *
 * Drawing, viewport manipulation, and gesture actions are delegated to
 * [viewModel] and callbacks. Tap detection is delegated to
 * [TapGestureDetector]. This class owns the touch routing and
 * drawing state machine.
 *
 * @property fingerDrawingEnabled lambda checked on each DOWN — toggle off
 *   to ignore finger input (stylus-only mode).
 */
class CanvasTouchHandler(
    private val viewModel: DrawViewModel,
    private val onInvalidate: () -> Unit,
    // Gesture callbacks — actions are decided by the caller
    onTwoFingerDoubleTap: () -> Unit = {},
    onTwoFingerTripleTap: () -> Unit = {},
    onThreeFingerDoubleTap: () -> Unit = {},
    onThreeFingerTripleTap: () -> Unit = {},
    // Double-tap deferral: 0 = immediate, TAP_INTERVAL_MS = wait for triple-tap
    twoFingerDoubleTapDelayMs: Long = 0L,
    threeFingerDoubleTapDelayMs: Long = TapGestureDetector.TAP_INTERVAL_MS,
    // Finger drawing toggle: checked on every DOWN to support runtime changes
    private val fingerDrawingEnabled: () -> Boolean = { true },
) {

    // ═══════════════════════════════════════════════════════════════
    //  Drawing state
    // ═══════════════════════════════════════════════════════════════

    private val TAG = "DrawTouch"

    private var activePointerId: Int = -1
    // strokePending XOR strokeInProgress: pending during finger debounce (50ms),
    // progress once committed. Both false when idle or in multi-touch.
    private var strokeInProgress: Boolean = false
    private var strokePending: Boolean = false
    private var pendingModifier: StrokeModifier = StrokeModifier.None
    private var pressureEraserActive: Boolean = false
    private val pendingMovePoints = mutableListOf<Offset>()
    private var downTimeMs: Long = 0L
    private val fingerDebounceMs: Long = 50L

    // ═══════════════════════════════════════════════════════════════
    //  Multi-touch state
    // ═══════════════════════════════════════════════════════════════

    private var isMultiTouch: Boolean = false
    private var multiTouchStartDist: Float = 0f
    private var multiTouchStartMid: Pair<Float, Float> = 0f to 0f
    private var multiTouchStartPanX: Float = 0f
    private var multiTouchStartPanY: Float = 0f
    private var multiTouchStartZoom: Float = 1f
    private var multiTouchStartLockMode: LockMode = LockMode.NONE

    // ═══════════════════════════════════════════════════════════════
    //  Tap detection (delegated)
    // ═══════════════════════════════════════════════════════════════

    private val tapDetector = TapGestureDetector(
        onTwoFingerDoubleTap = onTwoFingerDoubleTap,
        onTwoFingerTripleTap = onTwoFingerTripleTap,
        onThreeFingerDoubleTap = onThreeFingerDoubleTap,
        onThreeFingerTripleTap = onThreeFingerTripleTap,
        twoFingerDoubleTapDelayMs = twoFingerDoubleTapDelayMs,
        threeFingerDoubleTapDelayMs = threeFingerDoubleTapDelayMs,
    )

    // ═══════════════════════════════════════════════════════════════
    //  Logging helpers
    // ═══════════════════════════════════════════════════════════════

    private fun toolTypeName(toolType: Int): String = when (toolType) {
        MotionEvent.TOOL_TYPE_FINGER -> "FINGER"
        MotionEvent.TOOL_TYPE_STYLUS -> "STYLUS"
        MotionEvent.TOOL_TYPE_ERASER -> "ERASER"
        MotionEvent.TOOL_TYPE_MOUSE -> "MOUSE"
        else -> "UNKNOWN($toolType)"
    }

    private fun eventActionName(action: Int): String = when (action) {
        MotionEvent.ACTION_DOWN -> "DOWN"
        MotionEvent.ACTION_UP -> "UP"
        MotionEvent.ACTION_MOVE -> "MOVE"
        MotionEvent.ACTION_CANCEL -> "CANCEL"
        MotionEvent.ACTION_POINTER_DOWN -> "PTR_DOWN"
        MotionEvent.ACTION_POINTER_UP -> "PTR_UP"
        else -> "?$action"
    }

    private fun logPointers(event: MotionEvent, prefix: String = "") {
        val sb = StringBuilder("$prefix ptrs=${event.pointerCount} [")
        for (i in 0 until event.pointerCount) {
            val pid = event.getPointerId(i)
            val tt = toolTypeName(event.getToolType(i))
            sb.append("#$pid=$tt(${event.getX(i).toInt()},${event.getY(i).toInt()})")
            if (i < event.pointerCount - 1) sb.append(", ")
        }
        sb.append("]")
        Log.d(TAG, sb.toString())
    }

    // ═══════════════════════════════════════════════════════════════
    //  Public entry point
    // ═══════════════════════════════════════════════════════════════

    /** Current hover state for size preview; null when not showing. */
    internal var hoverState: HoverState? = null
        private set

    fun handleEvent(event: MotionEvent): Boolean {
        // Track pointer position for size preview
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1) {
                    hoverState = HoverState(Offset(event.x, event.y),
                        delayMs = if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER)
                            FINGER_HOVER_DELAY_MS else STYLUS_HOVER_DELAY_MS)
                }
            }
            MotionEvent.ACTION_HOVER_MOVE -> {
                hoverState = HoverState(Offset(event.x, event.y), delayMs = STYLUS_HOVER_DELAY_MS)
                onInvalidate()
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                hoverState?.let { s ->
                    hoverState = s.copy(fadeStartTimeMs = System.currentTimeMillis())
                }
                onInvalidate()
            }
            MotionEvent.ACTION_HOVER_EXIT -> {
                hoverState?.let { s ->
                    hoverState = s.copy(fadeStartTimeMs = System.currentTimeMillis(), delayMs = STYLUS_HOVER_DELAY_MS)
                }
                onInvalidate()
            }
        }

        if (event.actionMasked != MotionEvent.ACTION_MOVE || event.pointerCount >= 2) {
            logPointers(event, eventActionName(event.actionMasked))
        }

        // ① Multi-touch active → dispatch
        if (isMultiTouch && handleMultiTouchMode(event)) return true

        // ② Enter multi-touch on second finger
        if (shouldEnterMultiTouch(event)) {
            enterMultiTouch(event)
            return true
        }

        // ③ Single-pointer drawing
        handleSinglePointer(event)
        return true
    }

    // ═══════════════════════════════════════════════════════════════
    //  ① Multi-touch mode
    // ═══════════════════════════════════════════════════════════════

    private fun handleMultiTouchMode(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                Log.d(TAG, "  → multi-touch interrupted by DOWN: exiting")
                exitMultiTouch()
                return false
            }
            MotionEvent.ACTION_CANCEL -> {
                Log.d(TAG, "  → multi-touch cancelled: exiting, viewport restored")
                exitMultiTouch()
                cancelAnyStroke()
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2 && tapDetector.maxPointerCount < 3) handleMultiTouchMove(event)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val idToPos = mutableMapOf<Int, Pair<Float, Float>>()
                for (i in 0 until event.pointerCount) {
                    idToPos[event.getPointerId(i)] = event.getX(i) to event.getY(i)
                }
                tapDetector.onPointerDown(event.pointerCount, event.eventTime, idToPos)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 2) tapDetector.onMultiTouchEnd(event).also { isMultiTouch = false }
            }
            MotionEvent.ACTION_UP -> {
                tapDetector.onMultiTouchEnd(event).also { isMultiTouch = false }
            }
        }
        if (isMultiTouch) {
            onInvalidate()
            return true
        }
        return false
    }

    private fun exitMultiTouch() {
        isMultiTouch = false
        viewModel.setViewport(CanvasViewport(
            zoom = multiTouchStartZoom,
            panX = multiTouchStartPanX,
            panY = multiTouchStartPanY,
        ))
    }

    private fun cancelAnyStroke() {
        if (strokeInProgress) {
            strokeInProgress = false
            activePointerId = -1
            viewModel.finishStroke()
        }
        pressureEraserActive = false
        strokePending = false
    }

    // ═══════════════════════════════════════════════════════════════
    //  ② Enter multi-touch
    // ═══════════════════════════════════════════════════════════════

    private fun shouldEnterMultiTouch(event: MotionEvent): Boolean =
        event.actionMasked == MotionEvent.ACTION_POINTER_DOWN && event.pointerCount >= 2

    private fun enterMultiTouch(event: MotionEvent) {
        Log.d(TAG, "  → multi-touch entered (${event.pointerCount} pointers)")
        val idToPos = mutableMapOf<Int, Pair<Float, Float>>()
        for (i in 0 until event.pointerCount) {
            idToPos[event.getPointerId(i)] = event.getX(i) to event.getY(i)
        }
        tapDetector.onMultiTouchEnter(event.pointerCount, event.eventTime, idToPos)
        discardPendingStroke()
        cancelAnyStroke()
        isMultiTouch = true

        // Fade out the hover circle immediately — it shouldn't stick during multi-touch
        hoverState = hoverState?.copy(fadeStartTimeMs = System.currentTimeMillis())

        startMultiTouch(event)
        onInvalidate()
    }

    private fun discardPendingStroke() {
        strokePending = false
        pendingMovePoints.clear()
    }

    // ═══════════════════════════════════════════════════════════════
    //  ③ Single-pointer drawing
    // ═══════════════════════════════════════════════════════════════

    private fun handleSinglePointer(event: MotionEvent) {
        val vp = viewModel.viewport.value
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN              -> onPointerDown(event, vp)
            MotionEvent.ACTION_MOVE              -> onPointerMove(event, vp)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL            -> onPointerUp(event)
        }
    }

    private fun onPointerDown(event: MotionEvent, vp: CanvasViewport) {
        tapDetector.cancelPending()

        activePointerId = event.getPointerId(0)
        val isStylus = event.getToolType(0).let {
            it == MotionEvent.TOOL_TYPE_STYLUS || it == MotionEvent.TOOL_TYPE_ERASER
        }
        downTimeMs = event.eventTime
        pendingModifier = detectStrokeModifier(event)
        val worldPt = vp.screenToWorld(Offset(event.x, event.y))

        if (isStylus) {
            val sample = strokeSample(event, 0, null, vp)
            pressureEraserActive = shouldPressureErase(sample, startingStroke = true)
            Log.d(TAG, "  → stylus DOWN: stroke started (ptr=#$activePointerId, mod=$pendingModifier)")
            viewModel.startStroke(sample, effectiveModifier())
            strokeInProgress = true
            strokePending = false
        } else {
            if (!fingerDrawingEnabled()) {
                Log.d(TAG, "  → finger DOWN: ignored (finger drawing disabled)")
                return
            }
            Log.d(TAG, "  → finger DOWN: pending (debounce=${fingerDebounceMs}ms)")
            pendingMovePoints.clear()
            pendingMovePoints.add(worldPt)
            strokePending = true
        }
    }

    private fun onPointerMove(event: MotionEvent, vp: CanvasViewport) {
        if (strokePending) {
            accumulatePendingPoints(event, vp)
            if (event.eventTime - downTimeMs < fingerDebounceMs) return
            flushPendingStroke(event.eventTime)
            onInvalidate()
            return
        }
        if (!strokeInProgress) return
        appendPointsToStroke(event, vp)
        onInvalidate()
    }

    private fun accumulatePendingPoints(event: MotionEvent, vp: CanvasViewport) {
        val pi = event.findPointerIndex(activePointerId)
        if (pi < 0) return
        for (i in 0 until event.historySize) {
            pendingMovePoints.add(vp.screenToWorld(
                Offset(event.getHistoricalX(pi, i), event.getHistoricalY(pi, i))))
        }
        pendingMovePoints.add(vp.screenToWorld(
            Offset(event.getX(pi), event.getY(pi))))
    }

    private fun flushPendingStroke(eventTime: Long) {
        Log.d(TAG, "  → debounce ended (${eventTime - downTimeMs}ms): flushing ${pendingMovePoints.size} pts")
        viewModel.startStroke(pendingMovePoints.removeAt(0), pendingModifier)
        for (pt in pendingMovePoints) viewModel.updateStroke(pt)
        pendingMovePoints.clear()
        strokeInProgress = true
        strokePending = false
    }

    private fun appendPointsToStroke(event: MotionEvent, vp: CanvasViewport) {
        val pi = event.findPointerIndex(activePointerId)
        if (pi < 0) return
        val isStylus = event.getToolType(pi).let {
            it == MotionEvent.TOOL_TYPE_STYLUS || it == MotionEvent.TOOL_TYPE_ERASER
        }
        for (i in 0 until event.historySize) {
            appendStrokeSample(strokeSample(event, pi, i, vp), isStylus)
        }
        appendStrokeSample(strokeSample(event, pi, null, vp), isStylus)
    }

    private fun appendStrokeSample(sample: StrokeSample, isStylus: Boolean) {
        if (isStylus && viewModel.uiState.value.pressureEraserEnabled) {
            val shouldErase = shouldPressureErase(sample, startingStroke = false)
            if (shouldErase != pressureEraserActive) {
                viewModel.finishStroke()
                pressureEraserActive = shouldErase
                viewModel.startStroke(sample, effectiveModifier())
                return
            }
        }
        viewModel.updateStroke(sample)
    }

    private fun effectiveModifier(): StrokeModifier =
        if (pressureEraserActive) StrokeModifier.PrimaryButton else pendingModifier

    private fun shouldPressureErase(sample: StrokeSample, startingStroke: Boolean): Boolean {
        val state = viewModel.uiState.value
        if (!state.pressureEraserEnabled) return false
        if (startingStroke && state.currentPenType.isEraser) return false
        if (!startingStroke && !pressureEraserActive && state.currentPenType.isEraser) return false

        val enterThreshold = state.pressureEraserThreshold
        val releaseThreshold = (enterThreshold - PRESSURE_ERASER_RELEASE_GAP).coerceAtLeast(0f)
        return if (pressureEraserActive) {
            sample.pressure > releaseThreshold
        } else {
            sample.pressure >= enterThreshold
        }
    }

    private fun strokeSample(
        event: MotionEvent,
        pointerIndex: Int,
        historyIndex: Int?,
        vp: CanvasViewport,
    ): StrokeSample {
        val screenPoint = if (historyIndex == null) {
            Offset(event.getX(pointerIndex), event.getY(pointerIndex))
        } else {
            Offset(event.getHistoricalX(pointerIndex, historyIndex), event.getHistoricalY(pointerIndex, historyIndex))
        }
        val pressure = if (historyIndex == null) {
            event.getPressure(pointerIndex)
        } else {
            event.getHistoricalPressure(pointerIndex, historyIndex)
        }
        val tilt = if (historyIndex == null) {
            event.getAxisValue(MotionEvent.AXIS_TILT, pointerIndex)
        } else {
            event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, pointerIndex, historyIndex)
        }
        val orientation = if (historyIndex == null) {
            event.getOrientation(pointerIndex)
        } else {
            event.getHistoricalOrientation(pointerIndex, historyIndex)
        }
        val timeMs = if (historyIndex == null) event.eventTime else event.getHistoricalEventTime(historyIndex)
        return StrokeSample(
            position = vp.screenToWorld(screenPoint),
            pressure = pressure.coerceIn(0f, 1f),
            tilt = tilt,
            orientation = orientation,
            timeMs = timeMs,
        )
    }

    private fun onPointerUp(event: MotionEvent) {
        if (strokePending) {
            if (pendingMovePoints.size > 1) {
                Log.d(TAG, "  → finger lifted before debounce: flushing ${pendingMovePoints.size} pts")
                flushPendingStroke(event.eventTime)
                viewModel.finishStroke()
                strokeInProgress = false
            } else {
                Log.d(TAG, "  → tap: single-point dot")
                viewModel.startStroke(pendingMovePoints.first(), pendingModifier)
                viewModel.finishStroke()
            }
            discardPendingStroke()
        }
        if (strokeInProgress) {
            Log.d(TAG, "  → ${eventActionName(event.actionMasked)}: stroke finished (ptr=#$activePointerId)")
            strokeInProgress = false
            activePointerId = -1
            viewModel.finishStroke()
        }
        pressureEraserActive = false
        onInvalidate()
    }

    // ═══════════════════════════════════════════════════════════════
    //  Multi-touch gesture math
    // ═══════════════════════════════════════════════════════════════

    private fun startMultiTouch(event: MotionEvent) {
        val x0 = event.getX(0); val y0 = event.getY(0)
        val x1 = event.getX(1); val y1 = event.getY(1)
        multiTouchStartDist = sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0))
        multiTouchStartMid = ((x0 + x1) / 2f) to ((y0 + y1) / 2f)

        val vp = viewModel.viewport.value
        multiTouchStartPanX = vp.panX
        multiTouchStartPanY = vp.panY
        multiTouchStartZoom = vp.zoom
        multiTouchStartLockMode = viewModel.lockMode.value
    }

    private fun handleMultiTouchMove(event: MotionEvent) {
        val x0 = event.getX(0); val y0 = event.getY(0)
        val x1 = event.getX(1); val y1 = event.getY(1)

        val curMidX = (x0 + x1) / 2f
        val curMidY = (y0 + y1) / 2f

        val panDx = curMidX - multiTouchStartMid.first
        val panDy = curMidY - multiTouchStartMid.second

        val startVp = CanvasViewport(
            zoom = multiTouchStartZoom,
            panX = multiTouchStartPanX,
            panY = multiTouchStartPanY,
        )
        var vp = if (multiTouchStartLockMode != LockMode.ALL) startVp.pan(panDx, panDy) else startVp

        val curDist = sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0))
        if (multiTouchStartDist > 0f && multiTouchStartLockMode == LockMode.NONE) {
            val zoomFactor = curDist / multiTouchStartDist
            vp = vp.zoomAt(zoomFactor, Offset(curMidX, curMidY))
        }

        viewModel.setViewport(vp)
    }

    // ═══════════════════════════════════════════════════════════════
    //  Stylus button detection
    // ═══════════════════════════════════════════════════════════════

    private fun detectStrokeModifier(event: MotionEvent, pointerIndex: Int = 0): StrokeModifier {
        if (event.getToolType(pointerIndex) != MotionEvent.TOOL_TYPE_STYLUS) return StrokeModifier.None
        val b = event.buttonState
        val primary = (b and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0
        val secondary = (b and MotionEvent.BUTTON_STYLUS_SECONDARY) != 0
        return when {
            primary && secondary -> StrokeModifier.Both
            primary -> StrokeModifier.PrimaryButton
            secondary -> StrokeModifier.SecondaryButton
            else -> StrokeModifier.None
        }
    }
}
