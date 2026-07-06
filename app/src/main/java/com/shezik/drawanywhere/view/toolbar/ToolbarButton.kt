package com.shezik.drawanywhere.view.toolbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shezik.drawanywhere.R
import com.shezik.drawanywhere.UiState
import com.shezik.drawanywhere.model.PenType
import com.shezik.drawanywhere.model.StylusButtonAction
import com.shezik.drawanywhere.model.StylusButtonScheme
import com.shezik.drawanywhere.view.canvas.LockMode

@Composable
private fun LockZoomIcon() {
    val tint = MaterialTheme.colorScheme.onSurface
    Box(contentAlignment = Alignment.Center) {
        Icon(
            Icons.Default.ZoomOutMap,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.fillMaxSize()
        )
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
    }
}

internal data class ToolbarButton(
    val id: String,
    val icon: ImageVector,
    val color: Color? = null,
    val contentDescription: String,
    val isEnabled: Boolean = true,
    val onClick: (() -> Unit)? = null,
    val popupPages: List<@Composable () -> Unit> = emptyList(),
    val iconContent: (@Composable () -> Unit)? = null,
) {
    val hasPopup: Boolean
        get() = popupPages.isNotEmpty()
}

@Composable
internal fun createAllToolbarButtons(
    uiState: UiState,
    canUndo: Boolean,
    canRedo: Boolean,
    canClearCanvas: Boolean,
    onCanvasVisibilityToggle: () -> Unit,
    onCanvasPassthroughToggle: () -> Unit,
    onClearCanvas: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onPenTypeSwitch: (PenType) -> Unit,
    onColorChange: (Color) -> Unit,
    onPresetColorChange: (Color) -> Unit = onColorChange,
    onStrokeWidthChange: (Float) -> Unit,
    onAlphaChange: (Float) -> Unit,
    onChangeOrientation: (ToolbarOrientation) -> Unit,
    onChangeAutoClearCanvas: (Boolean) -> Unit,
    onChangeVisibleOnStart: (Boolean) -> Unit,
    onChangeToolbarMinimized: (Boolean) -> Unit,
    fingerDrawingEnabled: Boolean,
    onChangeFingerDrawingEnabled: (Boolean) -> Unit,
    stylusButtonScheme: StylusButtonScheme,
    onChangeStylusButtonScheme: (StylusButtonScheme) -> Unit,
    stylusPrimaryButtonAction: StylusButtonAction,
    onChangeStylusPrimaryButtonAction: (StylusButtonAction) -> Unit,
    stylusSecondaryButtonAction: StylusButtonAction,
    onChangeStylusSecondaryButtonAction: (StylusButtonAction) -> Unit,
    pressureEraserEnabled: Boolean,
    onChangePressureEraserEnabled: (Boolean) -> Unit,
    pressureEraserThreshold: Float,
    onChangePressureEraserThreshold: (Float) -> Unit,
    onCycleLockMode: () -> Unit,
    lockMode: LockMode,
    onQuitApplication: () -> Unit
): List<ToolbarButton> = listOf(
    ToolbarButton(
        id = "visibility",
        icon = if (uiState.canvasVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
        contentDescription = if (uiState.canvasVisible) stringResource(R.string.hide_canvas) else stringResource(R.string.show_canvas),
        onClick = onCanvasVisibilityToggle
    ),
    ToolbarButton(
        id = "undo",
        icon = Icons.AutoMirrored.Filled.Undo,
        contentDescription = stringResource(R.string.undo),
        isEnabled = uiState.canvasVisible && canUndo,
        onClick = onUndo
    ),
    ToolbarButton(
        id = "clear",
        icon = if (canClearCanvas) Icons.Filled.Delete else Icons.Outlined.Delete,
        contentDescription = stringResource(R.string.clear_canvas),
        isEnabled = uiState.canvasVisible && canClearCanvas,
        onClick = onClearCanvas
    ),
    ToolbarButton(
        id = "tool_controls",
        icon = uiState.currentPenType.icon,
        contentDescription = stringResource(R.string.tool_controls),
        popupPages = listOf(
            { PenTypeSelector(currentPenType = uiState.currentPenType, onPenTypeSwitch = onPenTypeSwitch) },
            { PenControls(penConfig = uiState.currentPenConfig, onStrokeWidthChange = onStrokeWidthChange, onAlphaChange = onAlphaChange, alphaEnabled = !uiState.currentPenType.isEraser) }
        )
    ),
    ToolbarButton(
        id = "color_picker",
        icon = Icons.Default.Palette,
        color = if (!uiState.currentPenType.isEraser) uiState.currentPenConfig.color else null,
        contentDescription = stringResource(R.string.color_picker),
        isEnabled = !uiState.currentPenType.isEraser,
        popupPages = listOf(
            { ColorPicker(
                selectedColor = uiState.currentPenConfig.color,
                onColorSelected = onColorChange,
                onPresetSelected = onPresetColorChange,
                recentColors = uiState.recentColors
            ) }
        )
    ),
    ToolbarButton(
        id = "zoom_lock",
        icon = if (lockMode == LockMode.NONE) Icons.Default.LockOpen else Icons.Default.Lock,
        iconContent = if (lockMode == LockMode.ZOOM) {{ LockZoomIcon() }} else null,
        contentDescription = when (lockMode) {
            LockMode.NONE -> stringResource(R.string.lock_zoom)
            LockMode.ZOOM -> stringResource(R.string.lock_all)
            LockMode.ALL -> stringResource(R.string.unlock_all)
        },
        isEnabled = uiState.canvasVisible,
        onClick = onCycleLockMode
    ),
    ToolbarButton(
        id = "passthrough",
        icon = if (uiState.canvasPassthrough) Icons.Default.DoNotTouch else Icons.Default.TouchApp,
        contentDescription = if (uiState.canvasPassthrough) stringResource(R.string.disable_passthrough) else stringResource(R.string.enable_passthrough),
        isEnabled = uiState.canvasVisible,
        onClick = onCanvasPassthroughToggle
    ),
    ToolbarButton(
        id = "redo",
        icon = Icons.AutoMirrored.Filled.Redo,
        contentDescription = stringResource(R.string.redo),
        isEnabled = uiState.canvasVisible && canRedo,
        onClick = onRedo
    ),
    ToolbarButton(
        id = "minimize",
        icon = Icons.Default.CloseFullscreen,
        contentDescription = stringResource(R.string.minimize_toolbar),
        onClick = { onChangeToolbarMinimized(true) }
    ),
    ToolbarButton(
        id = "settings",
        icon = Icons.Default.Tune,
        contentDescription = stringResource(R.string.settings),
        popupPages = listOf(
            { ToolbarControls(
                currentOrientation = uiState.toolbarOrientation,
                onChangeOrientation = onChangeOrientation,
                autoClearCanvas = uiState.autoClearCanvas,
                onChangeAutoClearCanvas = onChangeAutoClearCanvas,
                visibleOnStart = uiState.visibleOnStart,
                onChangeVisibleOnStart = onChangeVisibleOnStart,
                fingerDrawingEnabled = fingerDrawingEnabled,
                onChangeFingerDrawingEnabled = onChangeFingerDrawingEnabled,
                stylusButtonScheme = stylusButtonScheme,
                onChangeStylusButtonScheme = onChangeStylusButtonScheme,
                stylusPrimaryButtonAction = stylusPrimaryButtonAction,
                onChangeStylusPrimaryButtonAction = onChangeStylusPrimaryButtonAction,
                stylusSecondaryButtonAction = stylusSecondaryButtonAction,
                onChangeStylusSecondaryButtonAction = onChangeStylusSecondaryButtonAction,
                pressureEraserEnabled = pressureEraserEnabled,
                onChangePressureEraserEnabled = onChangePressureEraserEnabled,
                pressureEraserThreshold = pressureEraserThreshold,
                onChangePressureEraserThreshold = onChangePressureEraserThreshold,
                onQuitApplication = onQuitApplication
            ) },
            { AboutScreen() }
        )
    )
)
