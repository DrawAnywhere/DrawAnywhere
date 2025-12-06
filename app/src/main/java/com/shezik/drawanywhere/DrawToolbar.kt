/*
DrawAnywhere: An Android application that lets you draw on top of other apps.
Copyright (C) 2025 shezik

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

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.shezik.drawanywhere.ui.theme.DrawAnywhereTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign

// TODO: Modifiers (esp. size) unification

data class ToolbarButton(
    val id: String,
    val icon: ImageVector,
    val color: Color? = null,
    val contentDescription: String,
    val isEnabled: Boolean = true,
    val onClick: (() -> Unit)? = null,
    val popupPages: List<@Composable () -> Unit> = emptyList()
) {
    val hasPopup: Boolean
        get() = popupPages.isNotEmpty()
}

enum class ToolbarOrientation {
    HORIZONTAL, VERTICAL
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun DrawToolbar(
    viewModel: DrawViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val canClearCanvas by viewModel.canClearCanvas.collectAsState()

    val haptics = LocalHapticFeedback.current
    val hScrollState = rememberScrollState()
    val vScrollState = rememberScrollState()

    val allButtonsMap = createAllToolbarButtons(
        uiState = uiState,
        canUndo = canUndo,
        canRedo = canRedo,
        canClearCanvas = canClearCanvas,
        onCanvasVisibilityToggle = viewModel::toggleCanvasVisibility,
        onCanvasPassthroughToggle = viewModel::toggleCanvasPassthrough,
        onClearCanvas = viewModel::clearCanvas,
        onUndo = viewModel::undo,
        onRedo = viewModel::redo,
        onPenTypeSwitch = viewModel::switchToPen,
        onColorChange = viewModel::setPenColor,
        onStrokeWidthChange = viewModel::setStrokeWidth,
        onAlphaChange = viewModel::setStrokeAlpha,
        onChangeOrientation = viewModel::setToolbarOrientation,
        onChangeAutoClearCanvas = viewModel::setAutoClearCanvas,
        onChangeVisibleOnStart = viewModel::setVisibleOnStart,
        onQuitApplication = viewModel::quitApplication
    ).associateBy { it.id }

    DrawAnywhereTheme {
        // Root composable
        BoxWithConstraints {
            DraggableToolbarCard(
                modifier = modifier
                    .wrapContentSize(unbounded = true)  // Required for animatedContentSize on toolbar expansion
                    .widthIn(max = maxWidth)
                    .heightIn(max = maxHeight)
                    .scrollFadingEdges(hScrollState, false)
                    .scrollFadingEdges(vScrollState, true)
                    .horizontalScroll(hScrollState)
                    .verticalScroll(vScrollState)
                    // Leave space for defaultElevation shadows, should be as small as possible
                    // since user can't start a stroke on the outer padding.
                    .padding(4.dp),
                uiState = uiState,
                haptics = haptics,
                onPositionChange = viewModel::updateToolbarPosition,
                onPositionSaved = viewModel::saveToolbarPosition,
                onToolbarInteracted = viewModel::resetToolbarTimer
            ) {
                ToolbarButtonsContainer(
                    modifier = Modifier.padding(8.dp),
                    uiState = uiState,
                    allButtonsMap = allButtonsMap,
                    onExpandToggleClick = viewModel::toggleSecondDrawer
                )
            }
        }
    }
}

@Composable
private fun DraggableToolbarCard(
    modifier: Modifier = Modifier,
    uiState: UiState,
    haptics: HapticFeedback,
    onPositionChange: (Offset) -> Unit,
    onPositionSaved: () -> Unit,
    onToolbarInteracted: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        onToolbarInteracted()
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onPositionChange(dragAmount)
                    },
                    onDragEnd = {
                        onPositionSaved()
                    }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
        )
    ) {
        content()
    }
}

@Composable
private fun ToolbarButtonsContainer(
    modifier: Modifier = Modifier,
    uiState: UiState,
    allButtonsMap: Map<String, ToolbarButton>,
    onExpandToggleClick: () -> Unit
) {
    val orientation = uiState.toolbarOrientation
    val isFirstDrawerOpen = uiState.firstDrawerOpen
    val isSecondDrawerOpen = uiState.secondDrawerOpen
    val firstDrawerButtonIds = uiState.firstDrawerButtons
    val secondDrawerButtonIds = uiState.secondDrawerButtons
    val secondDrawerPinnedButtons = uiState.secondDrawerPinnedButtons

    val standaloneButtonIds = allButtonsMap.keys.filter {
        it !in firstDrawerButtonIds &&
        it !in secondDrawerButtonIds
    }

    val arrangement = Arrangement.spacedBy(8.dp)
    val popupAlignment = when (orientation) {
        ToolbarOrientation.HORIZONTAL -> Alignment.TopCenter
        ToolbarOrientation.VERTICAL -> Alignment.CenterEnd
    }

    // Animate the size of the container holding the expandable buttons
    val animatedContentSize = Modifier.animateContentSize(
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
    )

    when (orientation) {
        // REMEMBER TO SYNC VERTICAL CODE WITH HORIZONTAL CODE
        // HORIZONTAL CODE IS THE MOST ACCURATE
        ToolbarOrientation.HORIZONTAL -> {
            Row(
                modifier = modifier.then(animatedContentSize),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = arrangement
            ) {
                standaloneButtonIds.forEach { buttonId ->
                    val button = allButtonsMap[buttonId] ?: return@forEach
                    RenderButton(button, popupAlignment)
                }

                firstDrawerButtonIds.forEach { buttonId ->
                    val button = allButtonsMap[buttonId] ?: return@forEach
                    AnimatedVisibility(
                        visible = isFirstDrawerOpen,
                        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.5f),
                        exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.5f)
                    ) {
                        RenderButton(button, popupAlignment)
                    }
                }

                val isDividerVisible = isFirstDrawerOpen && (
                        secondDrawerPinnedButtons.isNotEmpty()
                                || (isSecondDrawerOpen && secondDrawerButtonIds.isNotEmpty())
                        )
                AnimatedVisibility(
                    visible = isDividerVisible,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300))
                ) {
                    VerticalDivider(
                        modifier = Modifier
                            .height(24.dp)
                            .padding(horizontal = 8.dp),
                        thickness = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                secondDrawerButtonIds.forEach { buttonId ->
                    val button = allButtonsMap[buttonId] ?: return@forEach
                    val isVisible =
                        isFirstDrawerOpen && (isSecondDrawerOpen || buttonId in secondDrawerPinnedButtons)

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.5f),
                        exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.5f)
                    ) {
                        RenderButton(button, popupAlignment)
                    }
                }

                val isExpandButtonVisible =
                    isFirstDrawerOpen && secondDrawerButtonIds.isNotEmpty()
                AnimatedVisibility(
                    visible = isExpandButtonVisible,
                    enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.5f),
                    exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.5f)
                ) {
                    ToolbarExpandButton(
                        modifier = Modifier,
                        isExpanded = isSecondDrawerOpen,
                        onClick = onExpandToggleClick,
                        orientation = orientation
                    )
                }
            }
        }

        ToolbarOrientation.VERTICAL -> {
            Column(
                modifier = modifier.then(animatedContentSize),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = arrangement
            ) {
                standaloneButtonIds.forEach { buttonId ->
                    val button = allButtonsMap[buttonId] ?: return@forEach
                    RenderButton(button, popupAlignment)
                }

                firstDrawerButtonIds.forEach { buttonId ->
                    val button = allButtonsMap[buttonId] ?: return@forEach
                    AnimatedVisibility(
                        visible = isFirstDrawerOpen,
                        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.5f),
                        exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.5f)
                    ) {
                        RenderButton(button, popupAlignment)
                    }
                }

                val isDividerVisible = isFirstDrawerOpen && (
                        secondDrawerPinnedButtons.isNotEmpty()
                                || (isSecondDrawerOpen && secondDrawerButtonIds.isNotEmpty())
                        )
                AnimatedVisibility(
                    visible = isDividerVisible,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300))
                ) {
                    HorizontalDivider(
                        modifier = Modifier
                            .width(24.dp)
                            .padding(vertical = 8.dp),
                        thickness = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                secondDrawerButtonIds.forEach { buttonId ->
                    val button = allButtonsMap[buttonId] ?: return@forEach
                    val isVisible =
                        isFirstDrawerOpen && (isSecondDrawerOpen || buttonId in secondDrawerPinnedButtons)

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.5f),
                        exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.5f)
                    ) {
                        RenderButton(button, popupAlignment)
                    }
                }

                val isExpandButtonVisible =
                    isFirstDrawerOpen && secondDrawerButtonIds.isNotEmpty()
                AnimatedVisibility(
                    visible = isExpandButtonVisible,
                    enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.5f),
                    exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.5f)
                ) {
                    ToolbarExpandButton(
                        modifier = Modifier,
                        isExpanded = isSecondDrawerOpen,
                        onClick = onExpandToggleClick,
                        orientation = orientation
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderButton(button: ToolbarButton, popupAlignment: Alignment, modifier: Modifier = Modifier) {
    if (button.hasPopup) {
        PopupToolbarButton(
            modifier = modifier,
            button = button,
            popupAlignment = popupAlignment
        )
    } else {
        AnimatedToolbarButton(
            modifier = modifier,
            button = button
        )
    }
}

@Composable
private fun ToolbarExpandButton(
    modifier: Modifier,
    isExpanded: Boolean,
    onClick: () -> Unit,
    orientation: ToolbarOrientation
) {
    val targetAngles =
        when (orientation) {
            ToolbarOrientation.HORIZONTAL -> 180f to 0f
            ToolbarOrientation.VERTICAL -> 270f to 90f
        }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) targetAngles.first else targetAngles.second,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "toggle_rotation"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier
//            .size(40.dp)
            .background(
                color = if (isExpanded)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = CircleShape  // Apply CircleShape here for background
            )
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = if (isExpanded) stringResource(R.string.collapse_toolbar) else stringResource(R.string.expand_toolbar),
            tint = if (isExpanded)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.graphicsLayer { rotationZ = rotationAngle }
        )
    }
}

@Composable
private fun AnimatedToolbarButton(modifier: Modifier, button: ToolbarButton) {
    val iconColor = button.color ?: MaterialTheme.colorScheme.onSurface

    val scale by animateFloatAsState(
        targetValue = if (button.isEnabled) 1f else 0.9f,
        animationSpec = tween(200),
        label = "button_scale"
    )

    IconButton(
        onClick = button.onClick ?: {},
        enabled = button.isEnabled,
        modifier = modifier
//            .size(40.dp)
            // Apply clip and graphicsLayer after size for correct visual effects
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = button.icon,
            contentDescription = button.contentDescription,
            tint = if (button.isEnabled)
                iconColor
            else
                iconColor.copy(alpha = 0.4f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PopupToolbarButton(
    modifier: Modifier,
    button: ToolbarButton,
    popupAlignment: Alignment
) {
    var isPopupOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { isPopupOpen = !isPopupOpen },
            enabled = button.isEnabled,
            modifier = Modifier.background(
                color = if (isPopupOpen)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = CircleShape
            )
        ) {
            Icon(
                imageVector = button.icon,
                contentDescription = button.contentDescription,
                tint = if (isPopupOpen)
                    button.color ?: MaterialTheme.colorScheme.onPrimaryContainer
                else if (button.isEnabled)
                    button.color ?: MaterialTheme.colorScheme.onSurface
                else
                    (button.color ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.4f)
            )
        }

        if (isPopupOpen && button.popupPages.isNotEmpty()) {
            val pagerState = rememberPagerState(initialPage = 0) { button.popupPages.size }

            Popup(
                alignment = popupAlignment,
                offset = when (popupAlignment) {
                    Alignment.TopCenter -> IntOffset(0, -60)
                    Alignment.CenterEnd -> IntOffset(60, 0)
                    else -> IntOffset(0, 0)
                },
                onDismissRequest = { isPopupOpen = false },
                properties = PopupProperties(focusable = true)
            ) {
                Card(
                    modifier = Modifier
                        .wrapContentSize()
                        .width(200.dp),
//                        .defaultMinSize(minWidth = 200.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .animateContentSize(),
                            verticalAlignment = Alignment.Top
                        ) { page ->
                            button.popupPages[page].invoke()
                        }

                        if (button.popupPages.size > 1) {
                            Row(
                                Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(button.popupPages.size) { index ->
                                    val selected = pagerState.currentPage == index
                                    Box(
                                        modifier = Modifier
                                            .size(if (selected) 10.dp else 6.dp)
                                            .padding(2.dp)
                                            .background(
                                                color = if (selected) MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PenTypeSelector(
    currentPenType: PenType,
    onPenTypeSwitch: (PenType) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
//        modifier = Modifier.width(120.dp)
    ) {
        Text(
            text = stringResource(R.string.tools),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        val penTypes = listOf(
            PenType.Rectangle    to stringResource(R.string.rectangle),
            PenType.Pen          to stringResource(R.string.pen),
            PenType.StrokeEraser to stringResource(R.string.stroke_eraser)
        )

        penTypes.forEach { (penType, label) ->
            val isSelected = currentPenType == penType
            val backgroundColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
            val contentColor = if (isSelected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurface

            Button(
                onClick = { onPenTypeSwitch(penType) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = backgroundColor,
                    contentColor = contentColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ColorSwatchButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "color_button_scale"
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape) // Ensure clipping is applied to the box
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
private fun ColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(
        Color.Red, Color.Blue, Color.Green, Color.Yellow,
        Color.Magenta, Color.Cyan, Color.Black, Color.Gray,
        Color.White, Color(0xFF8BC34A), Color(0xFFFF9800), Color(0xFF9C27B0)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.color),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            colors.chunked(4).forEach { colorRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    colorRow.forEach { color ->
                        ColorSwatchButton(
                            color = color,
                            isSelected = color.toArgb() == selectedColor.toArgb(),
                            onClick = { onColorSelected(color) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun PenControls(
    penConfig: PenConfig,
    onStrokeWidthChange: (Float) -> Unit,
    onAlphaChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.tool_controls),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        SliderControl(
            label = stringResource(R.string.width),
            value = penConfig.width,
            valueRange = 1f..50f,
            onValueChange = onStrokeWidthChange,
            valueDisplay = { "${it.toInt()}px" }
        )

        SliderControl(
            label = stringResource(R.string.opacity),
            value = penConfig.alpha,
            valueRange = 0.1f..1f,
            onValueChange = onAlphaChange,
            valueDisplay = { "${(it * 100).toInt()}%" }
        )
    }
}

@Composable
private fun ToolbarControls(
    currentOrientation: ToolbarOrientation,
    onChangeOrientation: (ToolbarOrientation) -> Unit,
    autoClearCanvas: Boolean,
    onChangeAutoClearCanvas: (Boolean) -> Unit,
    visibleOnStart: Boolean,
    onChangeVisibleOnStart: (Boolean) -> Unit,
    onQuitApplication: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
//        modifier = Modifier.width(120.dp)
    ) {
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        val orientations = listOf(
            ToolbarOrientation.HORIZONTAL to stringResource(R.string.horizontal),
            ToolbarOrientation.VERTICAL to stringResource(R.string.vertical)
        )

        orientations.forEach { (orientation, label) ->
            val isSelected = currentOrientation == orientation
            val backgroundColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
            val contentColor = if (isSelected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurface

            Button(
                onClick = { onChangeOrientation(orientation) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = backgroundColor,
                    contentColor = contentColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        CheckboxControl(
            label = stringResource(R.string.clear_on_hiding_canvas),
            isChecked = autoClearCanvas,
            onCheckedChange = onChangeAutoClearCanvas
        )

        CheckboxControl(
            label = stringResource(R.string.canvas_visible_on_start),
            isChecked = visibleOnStart,
            onCheckedChange = onChangeVisibleOnStart
        )

        Button(
            onClick = onQuitApplication,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = stringResource(R.string.quit),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun AboutScreen() {
    Box(modifier = Modifier.padding(12.dp)) {  // Looks nice.
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(72.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${BuildConfig.VERSION_NAME}${if (BuildConfig.DEBUG) "-dev" else ""} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.ExtraLight,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.copyright),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.licenses),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SliderControl(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueDisplay: (Float) -> String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueDisplay(value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            modifier = Modifier.height(30.dp),
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun CheckboxControl(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.ExtraLight,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier
                .width(24.dp)
                .height(24.dp),
            colors = CheckboxDefaults.colors(
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun createAllToolbarButtons(
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
    onStrokeWidthChange: (Float) -> Unit,
    onAlphaChange: (Float) -> Unit,
    onChangeOrientation: (ToolbarOrientation) -> Unit,
    onChangeAutoClearCanvas: (Boolean) -> Unit,
    onChangeVisibleOnStart: (Boolean) -> Unit,
    onQuitApplication: () -> Unit
): List<ToolbarButton> {
    return listOf(
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
            icon = when (uiState.currentPenType) {
                PenType.Rectangle    -> Icons.Outlined.CropSquare
                PenType.Pen          -> Icons.Default.Edit
                PenType.StrokeEraser -> InkEraser24Px
            },
            contentDescription = stringResource(R.string.tool_controls),
            popupPages = listOf(
            {
                PenTypeSelector(
                currentPenType = uiState.currentPenType,
                onPenTypeSwitch = onPenTypeSwitch
            ) },
            {
            PenControls(
                penConfig = uiState.currentPenConfig,
                onStrokeWidthChange = onStrokeWidthChange,
                onAlphaChange = onAlphaChange
            ) }
        )
    ),

        ToolbarButton(
            id = "color_picker",
            icon = Icons.Default.Palette,
            color = uiState.currentPenConfig.color,
            contentDescription = stringResource(R.string.color_picker),
            popupPages = listOf(
                { ColorPicker(
                    selectedColor = uiState.currentPenConfig.color,
                    onColorSelected = onColorChange
                ) }
            )
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
                    onQuitApplication = onQuitApplication
                ) },
                { AboutScreen() }
            )
        )
    )
}