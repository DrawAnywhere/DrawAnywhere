package com.shezik.drawanywhere.view.toolbar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.shezik.drawanywhere.R
import com.shezik.drawanywhere.ui.theme.Spacing

@Composable
internal fun RenderButton(button: ToolbarButton, popupAlignment: Alignment, modifier: Modifier = Modifier) {
    if (button.hasPopup) {
        PopupToolbarButton(modifier = modifier, button = button, popupAlignment = popupAlignment)
    } else {
        AnimatedToolbarButton(modifier = modifier, button = button)
    }
}

@Composable
internal fun ToolbarExpandButton(
    modifier: Modifier,
    isExpanded: Boolean,
    onClick: () -> Unit,
    orientation: ToolbarOrientation
) {
    val targetAngles = when (orientation) {
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
            .background(
                color = if (isExpanded) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = if (isExpanded) stringResource(R.string.collapse_toolbar) else stringResource(R.string.expand_toolbar),
            tint = if (isExpanded) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.graphicsLayer { rotationZ = rotationAngle }
        )
    }
}

@Composable
internal fun AnimatedToolbarButton(modifier: Modifier, button: ToolbarButton) {
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
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), shape = CircleShape)
    ) {
        button.iconContent?.invoke()
            ?: Icon(
                imageVector = button.icon,
                contentDescription = button.contentDescription,
                tint = if (button.isEnabled) iconColor else iconColor.copy(alpha = 0.4f)
            )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PopupToolbarButton(
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
                color = if (isPopupOpen) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = CircleShape
            )
        ) {
            button.iconContent?.invoke()
                ?: Icon(
                    imageVector = button.icon,
                    contentDescription = button.contentDescription,
                    tint = if (isPopupOpen) button.color ?: MaterialTheme.colorScheme.onPrimaryContainer
                    else if (button.isEnabled) button.color ?: MaterialTheme.colorScheme.onSurface
                    else (button.color ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.4f)
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
                    modifier = Modifier.wrapContentSize().width(200.dp).heightIn(max = 420.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(Spacing.lg),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.animateContentSize(),
                            verticalAlignment = Alignment.Top
                        ) { page -> button.popupPages[page].invoke() }
                        if (button.popupPages.size > 1) {
                            Row(
                                Modifier.fillMaxWidth(),
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
