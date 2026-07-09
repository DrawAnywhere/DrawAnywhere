package com.shezik.drawanywhere.view.toolbar

import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoNotTouch
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.shezik.drawanywhere.DrawViewModel
import com.shezik.drawanywhere.R
import com.shezik.drawanywhere.UiState
import com.shezik.drawanywhere.model.PenConfig
import com.shezik.drawanywhere.model.PenType
import com.shezik.drawanywhere.ui.theme.DrawAnywhereTheme
import kotlin.math.abs
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.ColorPalette
import top.yukonga.miuix.kmp.basic.FloatingToolbar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class MainTool(
    val icon: ImageVector,
    val accent: Color,
) {
    Pen(PenNib24Px, Color(0xFF7FB1FF)),
    Eraser(InkEraser24Px, Color(0xFFFF8A80)),
    Shape(Icons.Default.CropSquare, Color(0xFF8FC4FF)),
    Laser(Icons.Default.FlashOn, Color(0xFF69D2E7)),
}

private val toolbarColors = listOf(
    Color(0xFF000000),
    Color(0xFF2238A8),
    Color(0xFFE31D31),
    Color(0xFF8E5C5E),
    Color(0xFF7A58E4),
    Color(0xFF5165CC),
    Color(0xFF5BC3DB),
    Color(0xFFFFD03B),
    Color(0xFFFB681C),
    Color(0xFFF2F2F2),
)

private val penWidthsMm = listOf(0.10f, 0.30f, 0.50f)
private val shapeWidthsMm = listOf(0.30f, 0.50f, 0.80f)
private val laserWidthsMm = listOf(0.10f, 0.30f, 0.50f)
private val eraserSizesMm = listOf(3f, 6f, 10f)

@Composable
fun DrawToolbar(
    viewModel: DrawViewModel,
    canRedo: Boolean,
    onRedo: () -> Unit,
    passthroughEnabled: Boolean,
    onTogglePassthrough: () -> Unit,
    onSaveTransparent: () -> Unit,
    onSaveWithBackdrop: () -> Unit,
    onSaveWithScreenBackdrop: () -> Unit,
    onOpenSettings: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = LocalHapticFeedback.current
    val metrics = LocalContext.current.resources.displayMetrics

    var colorPopupFor by remember { mutableStateOf<MainTool?>(null) }
    var widthPopupFor by remember { mutableStateOf<MainTool?>(null) }
    var expandedWidthPx by remember { mutableStateOf(0) }
    val collapsedWidthPx = with(LocalDensity.current) { 64.dp.roundToPx() }
    var lastToolbarMinimized by remember { mutableStateOf(uiState.toolbarMinimized) }

    if (uiState.toolbarMinimized != lastToolbarMinimized) {
        val widthDelta = (expandedWidthPx - collapsedWidthPx).coerceAtLeast(0)
        if (widthDelta > 0) {
            val deltaX = if (uiState.toolbarMinimized) widthDelta.toFloat() else -widthDelta.toFloat()
            viewModel.updateToolbarPosition(Offset(deltaX, 0f))
            viewModel.saveToolbarPosition()
        }
        lastToolbarMinimized = uiState.toolbarMinimized
    }

    DrawAnywhereTheme {
        BoxWithConstraints {
            DraggableToolbarCard(
                modifier = modifier
                    .widthIn(max = maxWidth)
                    .padding(0.dp),
                haptics = haptics,
                onPositionChange = viewModel::updateToolbarPosition,
                onPositionSaved = viewModel::saveToolbarPosition,
                onToolbarInteracted = viewModel::resetToolbarTimer,
            ) {
                ToolbarShell {
                    if (uiState.toolbarMinimized) {
                        CollapsedToolbar(
                            uiState = uiState,
                            haptics = haptics,
                            onPositionChange = viewModel::updateToolbarPosition,
                            onPositionSaved = viewModel::saveToolbarPosition,
                        ) {
                            viewModel.setToolbarMinimized(false)
                        }
                    } else {
                        val selectMainTool: (MainTool) -> Unit = { tool ->
                            when (tool) {
                                MainTool.Pen -> viewModel.switchToPen(PenType.Pen)
                                MainTool.Eraser -> viewModel.switchToLastEraser()
                                MainTool.Shape -> {
                                    if (uiState.currentPenType != PenType.Ellipse) viewModel.switchToPen(PenType.Rectangle)
                                    else viewModel.switchToPen(PenType.Ellipse)
                                }
                                MainTool.Laser -> viewModel.switchToPen(PenType.Laser)
                            }
                        }
                        val detail: @Composable (Boolean) -> Unit = { vertical ->
                            ToolDetailArea(
                                uiState = uiState,
                                metrics = metrics,
                                vertical = vertical,
                                onColorSelected = viewModel::setPresetColor,
                                onStrokeEraserSelected = { viewModel.switchToPen(PenType.StrokeEraser) },
                                onPixelEraserSelected = { viewModel.switchToPen(PenType.PixelEraser) },
                                onRectangleSelected = { viewModel.switchToPen(PenType.Rectangle) },
                                onEllipseSelected = { viewModel.switchToPen(PenType.Ellipse) },
                                onWidthSelected = { mm -> viewModel.setStrokeWidth(mmToPx(mm, metrics)) },
                                onClearCanvas = viewModel::clearCanvas,
                                onOpenColorPopup = { colorPopupFor = it },
                                onOpenWidthPopup = { widthPopupFor = it },
                            )
                        }
                        val actions: @Composable () -> Unit = {
                            ToolbarActionSegment(
                                canRedo = canRedo,
                                onRedo = onRedo,
                                canvasVisible = uiState.canvasVisible,
                                onToggleCanvasVisibility = viewModel::toggleCanvasVisibility,
                                passthroughEnabled = passthroughEnabled,
                                onTogglePassthrough = onTogglePassthrough,
                                onSaveTransparent = onSaveTransparent,
                                onSaveWithBackdrop = onSaveWithBackdrop,
                                onSaveWithScreenBackdrop = onSaveWithScreenBackdrop,
                                onOpenSettings = onOpenSettings,
                                onQuit = onQuit,
                                onToggleOrientation = viewModel::toggleToolbarOrientation,
                                vertical = uiState.toolbarOrientation == ToolbarOrientation.VERTICAL,
                            )
                        }
                        if (uiState.toolbarOrientation == ToolbarOrientation.VERTICAL) {
                            Column(
                                modifier = Modifier
                                    .onSizeChanged { expandedWidthPx = it.width }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                MainToolColumn(uiState = uiState, onSelectMainTool = selectMainTool)
                                HorizontalSeparator()
                                detail(true)
                                DragCollapseHandle(
                                    haptics = haptics,
                                    onPositionChange = viewModel::updateToolbarPosition,
                                    onPositionSaved = viewModel::saveToolbarPosition,
                                    onTap = { viewModel.setToolbarMinimized(true) },
                                    compact = true,
                                )
                                HorizontalSeparator()
                                actions()
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .onSizeChanged { expandedWidthPx = it.width }
                                    .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    MainToolRow(uiState = uiState, onSelectMainTool = selectMainTool)
                                    VerticalSeparator()
                                    detail(false)
                                    DragCollapseHandle(
                                        haptics = haptics,
                                        onPositionChange = viewModel::updateToolbarPosition,
                                        onPositionSaved = viewModel::saveToolbarPosition,
                                        onTap = { viewModel.setToolbarMinimized(true) },
                                    )
                                }
                                HorizontalSeparator(width = 260)
                                actions()
                            }
                        }
                    }

                    colorPopupFor?.let { tool ->
                        SmallColorPopup(
                            tool = tool,
                            currentColor = colorForTool(uiState, tool).copy(alpha = alphaForTool(uiState, tool)),
                            savedCustomColors = uiState.savedCustomColors,
                            onDismiss = { colorPopupFor = null },
                            onApply = { color ->
                                switchToolIfNeeded(viewModel, uiState, tool)
                                viewModel.setPenColor(color.copy(alpha = 1f))
                                viewModel.setStrokeAlpha(color.alpha)
                            },
                            onSaveCustom = { color ->
                                switchToolIfNeeded(viewModel, uiState, tool)
                                viewModel.setPenColor(color.copy(alpha = 1f))
                                viewModel.setStrokeAlpha(color.alpha)
                                viewModel.saveCurrentCustomColor(color)
                            },
                        )
                    }

                    widthPopupFor?.let { tool ->
                        SmallWidthPopup(
                            tool = tool,
                            currentWidthPx = currentWidthForTool(uiState, tool),
                            savedCustomWidths = savedWidthsForTool(uiState, tool),
                            metrics = metrics,
                            onDismiss = { widthPopupFor = null },
                            onApply = { px ->
                                switchToolIfNeeded(viewModel, uiState, tool)
                                viewModel.setStrokeWidth(px)
                            },
                            onSaveCurrent = {
                                switchToolIfNeeded(viewModel, uiState, tool)
                                viewModel.saveCurrentWidthPreset()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarShell(
    content: @Composable () -> Unit,
) {
    FloatingToolbar(
        color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.16f),
        cornerRadius = 18.dp,
        outSidePadding = PaddingValues(0.dp),
        showDivider = false,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.24f),
                            MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.14f),
                        )
                    )
                ),
        ) { content() }
    }
}

@Composable
private fun CollapsedToolbar(
    uiState: UiState,
    modifier: Modifier = Modifier,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onPositionChange: (Offset) -> Unit,
    onPositionSaved: () -> Unit,
    onExpand: () -> Unit,
) {
    Row(
        modifier = modifier
            .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MainToolButton(
            tool = mainToolForPenType(uiState.currentPenType),
            selected = true,
            accent = colorForTool(uiState, mainToolForPenType(uiState.currentPenType)),
            compact = true,
            onClick = onExpand,
        )
        DragCollapseHandle(
            haptics = haptics,
            onPositionChange = onPositionChange,
            onPositionSaved = onPositionSaved,
            onTap = onExpand,
            compact = true,
        )
    }
}

@Composable
private fun ToolbarActionSegment(
    canRedo: Boolean,
    onRedo: () -> Unit,
    canvasVisible: Boolean,
    onToggleCanvasVisibility: () -> Unit,
    passthroughEnabled: Boolean,
    onTogglePassthrough: () -> Unit,
    onSaveTransparent: () -> Unit,
    onSaveWithBackdrop: () -> Unit,
    onSaveWithScreenBackdrop: () -> Unit,
    onOpenSettings: () -> Unit,
    onQuit: () -> Unit,
    onToggleOrientation: () -> Unit,
    vertical: Boolean,
) {
    var saveMenuExpanded by remember { mutableStateOf(false) }
    val passiveBg = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
    val activeBg = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
    val iconColor = MiuixTheme.colorScheme.onSurfaceContainer.copy(alpha = 0.88f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val primaryActions: @Composable () -> Unit = {
            CompactActionButton(
                icon = Icons.AutoMirrored.Filled.Redo,
                contentDescription = stringResource(R.string.redo),
                enabled = canRedo,
                backgroundColor = passiveBg,
                tint = if (canRedo) iconColor else iconColor.copy(alpha = 0.34f),
                onClick = onRedo,
            )
            CompactActionButton(
                icon = Icons.Default.SaveAlt,
                contentDescription = stringResource(R.string.save_drawing),
                selected = saveMenuExpanded,
                backgroundColor = if (saveMenuExpanded) activeBg else passiveBg,
                tint = if (saveMenuExpanded) MiuixTheme.colorScheme.onPrimaryContainer else iconColor,
                onClick = { saveMenuExpanded = !saveMenuExpanded },
            )
            CompactActionButton(
                icon = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                backgroundColor = passiveBg,
                tint = iconColor,
                onClick = onOpenSettings,
            )
        }
        val secondaryActions: @Composable () -> Unit = {
            CompactActionButton(
                icon = Icons.Default.ScreenRotation,
                contentDescription = stringResource(R.string.toggle_toolbar_orientation),
                backgroundColor = passiveBg,
                tint = iconColor,
                onClick = onToggleOrientation,
            )
            CompactActionButton(
                icon = if (canvasVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = if (canvasVisible) {
                    stringResource(R.string.hide_canvas)
                } else {
                    stringResource(R.string.show_canvas)
                },
                selected = !canvasVisible,
                backgroundColor = if (canvasVisible) passiveBg else activeBg,
                tint = if (canvasVisible) iconColor else MiuixTheme.colorScheme.onPrimaryContainer,
                onClick = onToggleCanvasVisibility,
            )
            CompactActionButton(
                icon = if (passthroughEnabled) Icons.Default.DoNotTouch else Icons.Default.TouchApp,
                contentDescription = if (passthroughEnabled) {
                    stringResource(R.string.disable_passthrough)
                } else {
                    stringResource(R.string.enable_passthrough)
                },
                selected = passthroughEnabled,
                backgroundColor = if (passthroughEnabled) activeBg else passiveBg,
                tint = if (passthroughEnabled) MiuixTheme.colorScheme.onPrimaryContainer else iconColor,
                onClick = onTogglePassthrough,
            )
            CompactActionButton(
                icon = Icons.Default.Close,
                contentDescription = stringResource(R.string.quit),
                backgroundColor = passiveBg.copy(alpha = 0.18f),
                tint = iconColor.copy(alpha = 0.76f),
                onClick = onQuit,
            )
        }

        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.20f))
                .padding(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (vertical) {
                primaryActions()
                HorizontalSeparator()
                secondaryActions()
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    primaryActions()
                    secondaryActions()
                }
            }
        }

        if (saveMenuExpanded) {
            SaveMenuCard(
                onSaveTransparent = {
                    saveMenuExpanded = false
                    onSaveTransparent()
                },
                onSaveWithBackdrop = {
                    saveMenuExpanded = false
                    onSaveWithBackdrop()
                },
                onSaveWithScreenBackdrop = {
                    saveMenuExpanded = false
                    onSaveWithScreenBackdrop()
                },
            )
        }
    }
}

@Composable
private fun SaveMenuCard(
    onSaveTransparent: () -> Unit,
    onSaveWithBackdrop: () -> Unit,
    onSaveWithScreenBackdrop: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.56f),
        contentColor = MiuixTheme.colorScheme.onSurfaceContainerHighest,
        shadowElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            SaveMenuItem(text = stringResource(R.string.save_transparent_png), onClick = onSaveTransparent)
            SaveMenuItem(text = stringResource(R.string.save_backdrop_png), onClick = onSaveWithBackdrop)
            SaveMenuItem(text = stringResource(R.string.save_screen_backdrop_png), onClick = onSaveWithScreenBackdrop)
        }
    }
}

@Composable
private fun SaveMenuItem(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 172.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
        )
    }
}

@Composable
private fun CompactActionButton(
    icon: ImageVector,
    contentDescription: String,
    backgroundColor: Color,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    MiuixIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(34.dp),
        backgroundColor = backgroundColor,
        holdDownState = selected,
        cornerRadius = 13.dp,
        minWidth = 34.dp,
        minHeight = 34.dp,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun MainToolRow(
    uiState: UiState,
    onSelectMainTool: (MainTool) -> Unit,
) {
    val selectedMainTool = mainToolForPenType(uiState.currentPenType)
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(MainTool.Pen, MainTool.Eraser, MainTool.Shape, MainTool.Laser).forEach { tool ->
            MainToolButton(
                tool = tool,
                selected = selectedMainTool == tool,
                accent = colorForTool(uiState, tool),
                compact = false,
                vertical = false,
                onClick = { onSelectMainTool(tool) },
            )
        }
    }
}

@Composable
private fun MainToolColumn(
    uiState: UiState,
    onSelectMainTool: (MainTool) -> Unit,
) {
    val selectedMainTool = mainToolForPenType(uiState.currentPenType)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(MainTool.Pen, MainTool.Eraser, MainTool.Shape, MainTool.Laser).forEach { tool ->
            MainToolButton(
                tool = tool,
                selected = selectedMainTool == tool,
                accent = colorForTool(uiState, tool),
                compact = false,
                vertical = true,
                onClick = { onSelectMainTool(tool) },
            )
        }
    }
}

@Composable
private fun MainToolButton(
    tool: MainTool,
    selected: Boolean,
    accent: Color,
    compact: Boolean,
    vertical: Boolean = false,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    val width = when {
        compact -> 30.dp
        vertical -> 58.dp
        else -> 28.dp
    }
    val height = when {
        compact -> 48.dp
        vertical -> 28.dp
        else -> 58.dp
    }
    val lift by animateFloatAsState(
        targetValue = if (selected && !compact) -10f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "tool_lift",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.16f else 1f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "tool_scale",
    )
    val tint = if (selected) accent else MiuixTheme.colorScheme.onBackground.copy(alpha = 0.82f)

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .graphicsLayer {
                if (vertical) {
                    translationX = with(density) { lift.dp.toPx() }
                } else {
                    translationY = with(density) { lift.dp.toPx() }
                }
                scaleX = scale
                scaleY = scale
            },
    ) {
        MiuixIconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            backgroundColor = if (selected) {
                MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.88f)
            } else {
                Color.Transparent
            },
            cornerRadius = 14.dp,
            minWidth = width,
            minHeight = height,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = stringResource(tool.labelRes()),
                    tint = tint,
                    modifier = Modifier.size(if (compact) 20.dp else 22.dp),
                )
            }
        }
    }
}

@Composable
private fun ToolDetailArea(
    uiState: UiState,
    metrics: DisplayMetrics,
    vertical: Boolean,
    onColorSelected: (Color) -> Unit,
    onStrokeEraserSelected: () -> Unit,
    onPixelEraserSelected: () -> Unit,
    onRectangleSelected: () -> Unit,
    onEllipseSelected: () -> Unit,
    onWidthSelected: (Float) -> Unit,
    onClearCanvas: () -> Unit,
    onOpenColorPopup: (MainTool) -> Unit,
    onOpenWidthPopup: (MainTool) -> Unit,
) {
    when (mainToolForPenType(uiState.currentPenType)) {
        MainTool.Pen -> PenLikeArea(
            tool = MainTool.Pen,
            selectedColor = uiState.penConfigs[PenType.Pen]?.color ?: uiState.currentPenConfig.color,
            currentWidthPx = uiState.penConfigs[PenType.Pen]?.width ?: uiState.currentPenConfig.width,
            widthValuesMm = penWidthsMm,
            metrics = metrics,
            vertical = vertical,
            onColorSelected = onColorSelected,
            onWidthSelected = onWidthSelected,
            onOpenColorPopup = onOpenColorPopup,
            onOpenWidthPopup = onOpenWidthPopup,
        )
        MainTool.Laser -> PenLikeArea(
            tool = MainTool.Laser,
            selectedColor = uiState.penConfigs[PenType.Laser]?.color ?: uiState.currentPenConfig.color,
            currentWidthPx = uiState.penConfigs[PenType.Laser]?.width ?: uiState.currentPenConfig.width,
            widthValuesMm = laserWidthsMm,
            metrics = metrics,
            vertical = vertical,
            onColorSelected = onColorSelected,
            onWidthSelected = onWidthSelected,
            onOpenColorPopup = onOpenColorPopup,
            onOpenWidthPopup = onOpenWidthPopup,
        )
        MainTool.Eraser -> EraserArea(
            currentPenType = uiState.currentPenType,
            currentWidthPx = uiState.currentPenConfig.width,
            metrics = metrics,
            vertical = vertical,
            onStrokeEraserSelected = onStrokeEraserSelected,
            onPixelEraserSelected = onPixelEraserSelected,
            onClearCanvas = onClearCanvas,
            onWidthSelected = onWidthSelected,
            onOpenWidthPopup = onOpenWidthPopup,
        )
        MainTool.Shape -> ShapeArea(
            currentPenType = uiState.currentPenType,
            selectedColor = uiState.currentPenConfig.color,
            currentWidthPx = uiState.currentPenConfig.width,
            metrics = metrics,
            vertical = vertical,
            onRectangleSelected = onRectangleSelected,
            onEllipseSelected = onEllipseSelected,
            onColorSelected = onColorSelected,
            onWidthSelected = onWidthSelected,
            onOpenColorPopup = onOpenColorPopup,
            onOpenWidthPopup = onOpenWidthPopup,
        )
    }
}

@Composable
private fun PenLikeArea(
    tool: MainTool,
    selectedColor: Color,
    currentWidthPx: Float,
    widthValuesMm: List<Float>,
    metrics: DisplayMetrics,
    vertical: Boolean,
    onColorSelected: (Color) -> Unit,
    onWidthSelected: (Float) -> Unit,
    onOpenColorPopup: (MainTool) -> Unit,
    onOpenWidthPopup: (MainTool) -> Unit,
) {
    val content: @Composable () -> Unit = {
        ColorGrid(
            selectedColor = selectedColor,
            vertical = vertical,
            onColorSelected = onColorSelected,
            onMoreClick = { onOpenColorPopup(tool) },
        )
        WidthChoices(
            valuesMm = widthValuesMm,
            currentWidthPx = currentWidthPx,
            metrics = metrics,
            vertical = vertical,
            onSelected = onWidthSelected,
            onMoreClick = { onOpenWidthPopup(tool) },
        )
    }
    if (vertical) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun EraserArea(
    currentPenType: PenType,
    currentWidthPx: Float,
    metrics: DisplayMetrics,
    vertical: Boolean,
    onStrokeEraserSelected: () -> Unit,
    onPixelEraserSelected: () -> Unit,
    onClearCanvas: () -> Unit,
    onWidthSelected: (Float) -> Unit,
    onOpenWidthPopup: (MainTool) -> Unit,
) {
    val content: @Composable () -> Unit = {
        VerticalIconChoices(
            items = listOf(
                IconChoice(InkEraser24Px, currentPenType == PenType.StrokeEraser, stringResource(R.string.stroke_eraser), onStrokeEraserSelected),
                IconChoice(Icons.Default.BlurOn, currentPenType == PenType.PixelEraser, stringResource(R.string.pixel_eraser), onPixelEraserSelected),
            )
        )
        ClearCanvasButton(vertical = vertical, onClick = onClearCanvas)
        WidthChoices(
            valuesMm = eraserSizesMm,
            currentWidthPx = currentWidthPx,
            metrics = metrics,
            vertical = vertical,
            onSelected = onWidthSelected,
            formatter = { "${it.toInt()}" },
            onMoreClick = { onOpenWidthPopup(MainTool.Eraser) },
        )
    }
    if (vertical) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun ClearCanvasButton(
    vertical: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = if (vertical) Modifier.size(width = 66.dp, height = 34.dp)
        else Modifier.size(width = 34.dp, height = 66.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        contentColor = Color(0xFFD96C6C),
        border = BorderStroke(1.dp, Color(0xFFD96C6C).copy(alpha = 0.56f)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = stringResource(R.string.clear_canvas),
                tint = Color(0xFFD96C6C),
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun ShapeArea(
    currentPenType: PenType,
    selectedColor: Color,
    currentWidthPx: Float,
    metrics: DisplayMetrics,
    vertical: Boolean,
    onRectangleSelected: () -> Unit,
    onEllipseSelected: () -> Unit,
    onColorSelected: (Color) -> Unit,
    onWidthSelected: (Float) -> Unit,
    onOpenColorPopup: (MainTool) -> Unit,
    onOpenWidthPopup: (MainTool) -> Unit,
) {
    val content: @Composable () -> Unit = {
        VerticalIconChoices(
            items = listOf(
                IconChoice(Icons.Default.CropSquare, currentPenType == PenType.Rectangle, stringResource(R.string.rectangle), onRectangleSelected),
                IconChoice(Icons.Default.RadioButtonUnchecked, currentPenType == PenType.Ellipse, stringResource(R.string.ellipse), onEllipseSelected),
            )
        )
        ColorGrid(
            selectedColor = selectedColor,
            vertical = vertical,
            onColorSelected = onColorSelected,
            onMoreClick = { onOpenColorPopup(MainTool.Shape) },
        )
        WidthChoices(
            valuesMm = shapeWidthsMm,
            currentWidthPx = currentWidthPx,
            metrics = metrics,
            vertical = vertical,
            onSelected = onWidthSelected,
            onMoreClick = { onOpenWidthPopup(MainTool.Shape) },
        )
    }
    if (vertical) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

private data class IconChoice(
    val icon: ImageVector,
    val selected: Boolean,
    val label: String,
    val onClick: () -> Unit,
    val tint: Color = Color.Unspecified,
)

@Composable
private fun VerticalIconChoices(items: List<IconChoice>) {
    val defaultTint = MiuixTheme.colorScheme.onSurfaceContainer
    val selectedColor = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
    val selectedBorder = MiuixTheme.colorScheme.primary.copy(alpha = 0.64f)
    val normalBorder = MiuixTheme.colorScheme.outline.copy(alpha = 0.42f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            val tint = if (item.tint == Color.Unspecified) defaultTint else item.tint
            Surface(
                onClick = item.onClick,
                modifier = Modifier.size(30.dp),
                shape = RoundedCornerShape(9.dp),
                color = if (item.selected) selectedColor else Color.Transparent,
                contentColor = tint,
                border = BorderStroke(
                    1.dp,
                    if (item.selected) selectedBorder else normalBorder
                ),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = tint.copy(alpha = if (item.selected) 0.96f else 0.78f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorGrid(
    selectedColor: Color,
    vertical: Boolean,
    onColorSelected: (Color) -> Unit,
    onMoreClick: () -> Unit,
) {
    val colorCells: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            toolbarColors.chunked(if (vertical) 2 else 5).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    row.forEach { color ->
                        ColorCell(
                            color = color,
                            selected = color.toArgb() == selectedColor.toArgb(),
                            onClick = { onColorSelected(color) },
                        )
                    }
                }
            }
        }
    }
    if (vertical) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            colorCells()
            SmallActionButton(
                icon = Icons.Default.Palette,
                contentDescription = stringResource(R.string.color_picker),
                onClick = onMoreClick,
            )
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            colorCells()
            SmallActionButton(
                icon = Icons.Default.Palette,
                contentDescription = stringResource(R.string.color_picker),
                onClick = onMoreClick,
            )
        }
    }
}

@Composable
private fun WidthChoices(
    valuesMm: List<Float>,
    currentWidthPx: Float,
    metrics: DisplayMetrics,
    vertical: Boolean,
    onSelected: (Float) -> Unit,
    formatter: (Float) -> String = { String.format("%.2f", it) },
    onMoreClick: () -> Unit,
) {
    val textColor = MiuixTheme.colorScheme.onSurfaceContainer
    val normalBorder = MiuixTheme.colorScheme.outline.copy(alpha = 0.48f)
    val widthButtons: @Composable () -> Unit = {
        valuesMm.forEach { mm ->
            val selected = abs(currentWidthPx - mmToPx(mm, metrics)) < 1.6f
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    onClick = { onSelected(mm) },
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(
                        2.dp,
                        if (selected) Color(0xFF1A94FF) else normalBorder
                    ),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size((mm * 7f).coerceIn(3f, 10f).dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A94FF))
                        )
                    }
                }
                Text(
                    text = formatter(mm),
                    color = textColor.copy(alpha = 0.92f),
                    style = MiuixTheme.textStyles.body2,
                )
            }
        }
    }
    if (vertical) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            widthButtons()
            SmallActionButton(
                icon = Icons.Default.Tune,
                contentDescription = stringResource(R.string.width),
                onClick = onMoreClick,
            )
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            widthButtons()
            SmallActionButton(
                icon = Icons.Default.Tune,
                contentDescription = stringResource(R.string.width),
                onClick = onMoreClick,
            )
        }
    }
}

@Composable
private fun ColorCell(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(width = 22.dp, height = 20.dp),
        shape = RoundedCornerShape(5.dp),
        color = color,
        contentColor = readableContentColor(color),
        border = if (selected) BorderStroke(2.dp, MiuixTheme.colorScheme.primary) else null,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (selected) {
                Text(
                    text = "✓",
                    color = if (color == Color.White) Color.Black else Color.White,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SmallActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    MiuixIconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp),
        backgroundColor = Color.Transparent,
        cornerRadius = 10.dp,
        minWidth = 28.dp,
        minHeight = 28.dp,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.86f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun VerticalSeparator() {
    Box(
        modifier = Modifier
            .height(64.dp)
            .width(1.dp)
            .background(MiuixTheme.colorScheme.outline.copy(alpha = 0.26f))
    )
}

@Composable
private fun HorizontalSeparator(width: Int = 74) {
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(1.dp)
            .background(MiuixTheme.colorScheme.outline.copy(alpha = 0.26f))
    )
}

@Composable
private fun DragCollapseHandle(
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onPositionChange: (Offset) -> Unit,
    onPositionSaved: () -> Unit,
    onTap: () -> Unit,
    compact: Boolean = false,
) {
    Surface(
        modifier = Modifier
            .width(22.dp)
            .height(if (compact) 52.dp else 74.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: return@awaitEachGesture
                    var dragged = false
                    var keepDragging = true
                    while (keepDragging) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUpIgnoreConsumed()) {
                            keepDragging = false
                        } else {
                            val delta = change.position - change.previousPosition
                            if (delta != Offset.Zero) {
                                if (!dragged) {
                                    dragged = true
                                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                }
                                onPositionChange(delta)
                                change.consume()
                            }
                        }
                    }
                    if (dragged) {
                        onPositionSaved()
                    } else {
                        onTap()
                    }
                }
            },
        shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
        color = Color.Transparent,
        contentColor = MiuixTheme.colorScheme.onBackground,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                repeat(if (compact) 5 else 7) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(2.dp)
                            .background(MiuixTheme.colorScheme.onBackground.copy(alpha = 0.62f), RoundedCornerShape(50))
                            .offset(x = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallColorPopup(
    tool: MainTool,
    currentColor: Color,
    savedCustomColors: List<Color>,
    onDismiss: () -> Unit,
    onApply: (Color) -> Unit,
    onSaveCustom: (Color) -> Unit,
) {
    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(0, -20),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        var selectedColor by remember(currentColor) { mutableStateOf(currentColor) }
        var colorInput by remember(currentColor) { mutableStateOf(currentColor.toHexString()) }
        val parsedInput = remember(colorInput) { parseColorInput(colorInput) }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
            contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.width(240.dp).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = when (tool) {
                        MainTool.Pen -> stringResource(R.string.pen_color)
                        MainTool.Shape -> stringResource(R.string.shape_color)
                        MainTool.Laser -> stringResource(R.string.laser_color)
                        MainTool.Eraser -> stringResource(R.string.color)
                    },
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    toolbarColors.take(5).forEach { color ->
                        ColorCell(color = color, selected = color.rgbEquals(selectedColor)) {
                            onApply(color.copy(alpha = selectedColor.alpha))
                            onDismiss()
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    toolbarColors.drop(5).take(5).forEach { color ->
                        ColorCell(color = color, selected = color.rgbEquals(selectedColor)) {
                            onApply(color.copy(alpha = selectedColor.alpha))
                            onDismiss()
                        }
                    }
                }
                if (savedCustomColors.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        savedCustomColors.take(6).forEach { color ->
                            ColorCell(color = color, selected = color.toArgb() == selectedColor.toArgb()) {
                                onApply(color)
                                onDismiss()
                            }
                        }
                    }
                }
                ColorPalette(
                    color = selectedColor,
                    onColorChanged = {
                        selectedColor = it
                        colorInput = it.toHexString()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    rows = 6,
                    hueColumns = 12,
                    showPreview = true,
                )
                TextField(
                    value = colorInput,
                    onValueChange = {
                        colorInput = it
                        parseColorInput(it)?.let { parsed ->
                            selectedColor = parsed
                        }
                    },
                    label = stringResource(R.string.color_value_input),
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                )
                if (colorInput.isNotBlank() && parsedInput == null) {
                    Text(
                        text = stringResource(R.string.color_value_invalid),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.error,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onSaveCustom(parsedInput ?: selectedColor)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = colorInput.isBlank() || parsedInput != null,
                        colors = ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.secondaryVariant,
                            contentColor = MiuixTheme.colorScheme.onBackground,
                        ),
                    ) {
                        Text(stringResource(R.string.save_custom_color))
                    }
                    Button(
                        onClick = {
                            onApply(parsedInput ?: selectedColor)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = colorInput.isBlank() || parsedInput != null,
                        colors = ButtonDefaults.buttonColors(
                            color = selectedColor,
                            contentColor = readableContentColor(selectedColor),
                        ),
                    ) {
                        Text(stringResource(R.string.apply_color))
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallWidthPopup(
    tool: MainTool,
    currentWidthPx: Float,
    savedCustomWidths: List<Float>,
    metrics: DisplayMetrics,
    onDismiss: () -> Unit,
    onApply: (Float) -> Unit,
    onSaveCurrent: () -> Unit,
) {
    val rangeMm = when (tool) {
        MainTool.Pen -> 0.05f..2f
        MainTool.Shape -> 0.10f..3f
        MainTool.Laser -> 0.05f..1.5f
        MainTool.Eraser -> 1f..18f
    }
    var widthMm by remember(currentWidthPx) { mutableFloatStateOf(pxToMm(currentWidthPx, metrics).coerceIn(rangeMm.start, rangeMm.endInclusive)) }

    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(0, -20),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
            contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.width(220.dp).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = when (tool) {
                        MainTool.Eraser -> stringResource(R.string.custom_eraser_size)
                        else -> stringResource(R.string.custom_width)
                    },
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                )
                if (savedCustomWidths.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        savedCustomWidths.take(4).forEach { widthPx ->
                            val mm = pxToMm(widthPx, metrics)
                            WidthPresetChip(
                                label = if (tool == MainTool.Eraser) mm.toInt().toString() else String.format("%.2f", mm),
                                selected = abs(currentWidthPx - widthPx) < 0.5f,
                                onClick = {
                                    onApply(widthPx)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
                Text(
                    text = if (tool == MainTool.Eraser) "${widthMm.toInt()}" else String.format("%.2f mm", widthMm),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                )
                Slider(
                    value = widthMm,
                    onValueChange = {
                        widthMm = it
                        onApply(mmToPx(it, metrics))
                    },
                    valueRange = rangeMm,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onSaveCurrent()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.primary,
                            contentColor = MiuixTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Text(stringResource(R.string.save_custom_width))
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.secondaryVariant,
                            contentColor = MiuixTheme.colorScheme.onBackground,
                        ),
                    ) {
                        Text(stringResource(R.string.done))
                    }
                }
            }
        }
    }
}

@Composable
private fun WidthPresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MiuixTheme.colorScheme.primaryContainer else MiuixTheme.colorScheme.surfaceContainer,
        contentColor = if (selected) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onSurfaceContainer,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            color = if (selected) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.body2,
        )
    }
}

private fun mainToolForPenType(type: PenType): MainTool =
    when (type) {
        PenType.Pen -> MainTool.Pen
        PenType.StrokeEraser, PenType.PixelEraser -> MainTool.Eraser
        PenType.Rectangle, PenType.Ellipse -> MainTool.Shape
        PenType.Laser -> MainTool.Laser
    }

private fun MainTool.labelRes(): Int =
    when (this) {
        MainTool.Pen -> R.string.pen
        MainTool.Eraser -> R.string.stroke_eraser
        MainTool.Shape -> R.string.rectangle
        MainTool.Laser -> R.string.laser
    }

private fun switchToolIfNeeded(viewModel: DrawViewModel, uiState: UiState, tool: MainTool) {
    when (tool) {
        MainTool.Pen -> if (uiState.currentPenType != PenType.Pen) viewModel.switchToPen(PenType.Pen)
        MainTool.Eraser -> if (uiState.currentPenType != PenType.StrokeEraser && uiState.currentPenType != PenType.PixelEraser) {
            viewModel.switchToPen(PenType.StrokeEraser)
        }
        MainTool.Shape -> if (uiState.currentPenType != PenType.Rectangle && uiState.currentPenType != PenType.Ellipse) {
            viewModel.switchToPen(PenType.Rectangle)
        }
        MainTool.Laser -> if (uiState.currentPenType != PenType.Laser) viewModel.switchToPen(PenType.Laser)
    }
}

private fun colorForTool(uiState: UiState, tool: MainTool): Color =
    when (tool) {
        MainTool.Pen -> uiState.penConfigs[PenType.Pen]?.color ?: MainTool.Pen.accent
        MainTool.Laser -> uiState.penConfigs[PenType.Laser]?.color ?: MainTool.Laser.accent
        MainTool.Shape -> uiState.penConfigs[
            uiState.currentPenType.takeIf { it == PenType.Rectangle || it == PenType.Ellipse } ?: PenType.Rectangle
        ]?.color ?: MainTool.Shape.accent
        MainTool.Eraser -> MainTool.Eraser.accent
    }

private fun currentWidthForTool(uiState: UiState, tool: MainTool): Float =
    when (tool) {
        MainTool.Pen -> uiState.penConfigs[PenType.Pen]?.width ?: 0f
        MainTool.Shape -> uiState.penConfigs[uiState.currentPenType.takeIf { it == PenType.Rectangle || it == PenType.Ellipse } ?: PenType.Rectangle]?.width ?: 0f
        MainTool.Laser -> uiState.penConfigs[PenType.Laser]?.width ?: 0f
        MainTool.Eraser -> uiState.currentPenConfig.width
    }

private fun savedWidthsForTool(uiState: UiState, tool: MainTool): List<Float> =
    when (tool) {
        MainTool.Pen -> uiState.savedPenWidths
        MainTool.Shape -> uiState.savedShapeWidths
        MainTool.Laser -> uiState.savedLaserWidths
        MainTool.Eraser -> uiState.savedEraserSizes
    }

private fun alphaForTool(uiState: UiState, tool: MainTool): Float =
    when (tool) {
        MainTool.Pen -> uiState.penConfigs[PenType.Pen]?.alpha ?: 1f
        MainTool.Shape -> uiState.penConfigs[
            uiState.currentPenType.takeIf { it == PenType.Rectangle || it == PenType.Ellipse } ?: PenType.Rectangle
        ]?.alpha ?: 1f
        MainTool.Laser -> uiState.penConfigs[PenType.Laser]?.alpha ?: 1f
        MainTool.Eraser -> 1f
    }

private fun mmToPx(mm: Float, metrics: DisplayMetrics): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, mm, metrics)

private fun pxToMm(px: Float, metrics: DisplayMetrics): Float =
    px / TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 1f, metrics)

private fun readableContentColor(background: Color): Color {
    val luminance = background.red * 0.299f + background.green * 0.587f + background.blue * 0.114f
    return if (luminance > 0.58f) Color.Black else Color.White
}
