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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import com.shezik.drawanywhere.DrawController
import com.shezik.drawanywhere.DrawViewModel
import com.shezik.drawanywhere.model.StylusButtonScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NativeDrawCanvasView(
    context: Context,
    private val drawController: DrawController,
    private val viewModel: DrawViewModel,
) : View(context) {

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    companion object {
        private const val FRAME_INTERVAL_MS = 16L          // ~60fps
        private const val HUD_MARGIN_DP = 24f
        private const val HUD_BOTTOM_OFFSET_PX = 48f
        private const val HUD_PADDING_DP = 8f
        private const val HUD_TEXT_SIZE_SP = 18f
        private const val HUD_TEXT_COLOR = 0xCC_FFFFFF.toInt()
        private const val HUD_BG_COLOR = 0x80_000000.toInt()
    }

    // ── Touch input (delegated) ───────────────────────────────────

    private val touchHandler = CanvasTouchHandler(
        viewModel = viewModel,
        onInvalidate = { invalidate() },
        onTwoFingerDoubleTap = {
            if (viewModel.lockMode.value == LockMode.NONE) {
                viewModel.resetViewport(Offset(width / 2f, height / 2f))
            }
        },
        onTwoFingerTripleTap = {
            val lm = viewModel.lockMode.value
            when (lm) {
                LockMode.NONE -> viewModel.setViewport(CanvasViewport())
                LockMode.ZOOM -> viewModel.setViewport(CanvasViewport(zoom = viewModel.viewport.value.zoom))
                LockMode.ALL -> {}  // ignored
            }
        },
        onThreeFingerDoubleTap = { viewModel.toggleCanvasPassthrough() },
        onThreeFingerTripleTap = { viewModel.setCanvasVisibility(false) },
        fingerDrawingEnabled = { viewModel.uiState.value.fingerDrawingEnabled },
    )

    override fun onTouchEvent(event: MotionEvent): Boolean =
        touchHandler.handleEvent(event)

    // ── Hover (stylus/mouse size preview) ──────────────────────

    override fun onHoverEvent(event: MotionEvent): Boolean =
        touchHandler.handleEvent(event)

    override fun onGenericMotionEvent(event: MotionEvent): Boolean =
        touchHandler.handleEvent(event)

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return handleXiaomiStylusKey(event) || super.dispatchKeyEvent(event)
    }

    fun requestStylusKeyFocus() {
        requestFocus()
    }

    private fun handleXiaomiStylusKey(event: KeyEvent): Boolean {
        if (viewModel.uiState.value.stylusButtonScheme != StylusButtonScheme.XiaomiSmartPen) {
            return false
        }
        if (!isXiaomiStylusButtonEvent(event)) return false
        if (event.action == KeyEvent.ACTION_UP) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_PAGE_UP ->
                    viewModel.performStylusButtonAction(viewModel.uiState.value.stylusPrimaryButtonAction)
                KeyEvent.KEYCODE_PAGE_DOWN ->
                    viewModel.performStylusButtonAction(viewModel.uiState.value.stylusSecondaryButtonAction)
            }
            invalidate()
        }
        return true
    }

    private fun isXiaomiStylusButtonEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_PAGE_UP &&
            event.keyCode != KeyEvent.KEYCODE_PAGE_DOWN
        ) return false
        val deviceName = event.device?.name ?: return false
        return deviceName.contains("Xiaomi Smart Pen", ignoreCase = true)
    }

    // ── Rendering ─────────────────────────────────────────────────

    private val pathPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val hudPaint = Paint().apply {
        color = HUD_TEXT_COLOR
        textSize = HUD_TEXT_SIZE_SP * context.resources.displayMetrics.density
        isAntiAlias = true
    }

    private val hudBgPaint = Paint().apply {
        color = HUD_BG_COLOR
        isAntiAlias = true
    }

    private val hoverPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val hoverFillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val vp = viewModel.viewport.value

        drawController.removeExpiredStrokes(System.currentTimeMillis())

        canvas.save()
        canvas.translate(-vp.panX * vp.zoom, -vp.panY * vp.zoom)
        canvas.scale(vp.zoom, vp.zoom)

        for (stroke in drawController.strokeList) {
            stroke.render(canvas, pathPaint)
        }
        canvas.restore()

        // ── Hover size preview (screen space, constant border) ────
        touchHandler.hoverState?.let { state ->
            val config = viewModel.uiState.value.currentPenConfig

            val alpha = if (state.isFading) {
                val elapsed = System.currentTimeMillis() - state.fadeStartTimeMs - state.delayMs
                (1f - elapsed / state.fadeMs.toFloat()).coerceIn(0f, 1f)
            } else 1f

            if (alpha > 0f) {
                val penColor = config.color
                val penAlpha = config.alpha
                val rgb = penColor.toArgb() and 0x00FFFFFF
                val colorAlpha = (penColor.toArgb() ushr 24) and 0xFF

                val outlineAlpha = (alpha * (colorAlpha / 255f) * penAlpha * 255).toInt().coerceIn(0, 255)
                val fillAlpha = (outlineAlpha / 2).coerceIn(0, 255)

                hoverFillPaint.alpha = fillAlpha
                hoverFillPaint.color = rgb or (fillAlpha shl 24)
                hoverPaint.alpha = outlineAlpha
                hoverPaint.color = rgb or (outlineAlpha shl 24)

                val screenR = (config.width * vp.zoom - hoverPaint.strokeWidth) / 2f
                if (screenR > 0f) {
                    canvas.drawCircle(state.point.x, state.point.y, screenR, hoverFillPaint)
                    canvas.drawCircle(state.point.x, state.point.y, screenR, hoverPaint)
                }
            }

            if (state.isFading && alpha > 0f) {
                postInvalidateDelayed(FRAME_INTERVAL_MS)
            }
        }

        // ── Zoom HUD ─────────────────────────────────────────────
        val zoomPct = (vp.zoom * 100).toInt()
        val label = if (zoomPct == 100) "" else "${zoomPct}%"
        val lockIcon = when (viewModel.lockMode.value) {
            LockMode.NONE -> ""
            LockMode.ZOOM -> " 🔒🔍"
            LockMode.ALL -> " 🔒"
        }
        if (label.isNotEmpty() || viewModel.lockMode.value != LockMode.NONE) {
            val text = "$label$lockIcon"
            val density = context.resources.displayMetrics.density
            val pad = HUD_PADDING_DP * density
            val x = HUD_MARGIN_DP * density
            val y = height - HUD_BOTTOM_OFFSET_PX
            val fm = hudPaint.fontMetrics
            val rect = android.graphics.RectF(
                x - pad,
                y + fm.ascent - pad,
                x + hudPaint.measureText(text) + pad,
                y + fm.descent + pad
            )
            canvas.drawRoundRect(rect, pad, pad, hudBgPaint)
            canvas.drawText(text, x, y, hudPaint)
        }

        // Keep refreshing while ephemeral strokes are fading
        if (drawController.strokeList.any { it.penType.isEphemeral }) {
            postInvalidateDelayed(FRAME_INTERVAL_MS)
        }
    }

    // ── Viewport observation (for HUD updates) ────────────────────

    private var viewportScope: CoroutineScope? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewportScope = CoroutineScope(Dispatchers.Main).apply {
            launch { viewModel.viewport.collect { invalidate() } }
            launch { viewModel.lockMode.collect { invalidate() } }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewportScope?.cancel()
        removeCallbacks(null)
    }

}
