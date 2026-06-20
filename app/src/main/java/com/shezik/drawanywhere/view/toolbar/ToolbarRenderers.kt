package com.shezik.drawanywhere.view.toolbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.shezik.drawanywhere.R
import com.shezik.drawanywhere.ui.theme.Spacing

@Composable
internal fun RenderButton(
    button: ToolbarButton,
    onTogglePopup: (String) -> Unit,
    activePopupId: String?,
    modifier: Modifier = Modifier
) {
    if (button.hasPopup) {
        PopupToolbarButton(
            modifier = modifier,
            button = button,
            isOpen = activePopupId == button.id,
            onTogglePopup = { onTogglePopup(button.id) }
        )
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

@Composable
internal fun PopupToolbarButton(
    modifier: Modifier,
    button: ToolbarButton,
    isOpen: Boolean,
    onTogglePopup: () -> Unit
) {
    IconButton(
        onClick = onTogglePopup,
        enabled = button.isEnabled,
        modifier = modifier.background(
            color = if (isOpen) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            shape = CircleShape
        )
    ) {
        button.iconContent?.invoke()
            ?: Icon(
                imageVector = button.icon,
                contentDescription = button.contentDescription,
                tint = if (isOpen) button.color ?: MaterialTheme.colorScheme.onPrimaryContainer
                else if (button.isEnabled) button.color ?: MaterialTheme.colorScheme.onSurface
                else (button.color ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.4f)
            )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PopupOverlay(
    popupPages: List<@Composable () -> Unit>,
    toolbarPosition: Offset,
    orientation: ToolbarOrientation,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val pagerState = rememberPagerState(initialPage = 0) { popupPages.size }

    val toolbarOffsetPx = with(density) { 48.dp.roundToPx() }
    val cardWidthPx = cardSize.width
    val cardHeightPx = cardSize.height

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
    ) {
        val screenWidthPx = constraints.maxWidth
        val screenHeightPx = constraints.maxHeight

        val rawX = when (orientation) {
            ToolbarOrientation.HORIZONTAL -> toolbarPosition.x.toInt()
            ToolbarOrientation.VERTICAL -> toolbarPosition.x.toInt() + toolbarOffsetPx
        }
        val rawY = when (orientation) {
            ToolbarOrientation.HORIZONTAL -> toolbarPosition.y.toInt() + toolbarOffsetPx
            ToolbarOrientation.VERTICAL -> toolbarPosition.y.toInt()
        }

        val xPx = rawX.coerceIn(0, (screenWidthPx - cardWidthPx).coerceAtLeast(0))
        val yPx = rawY.coerceIn(0, (screenHeightPx - cardHeightPx).coerceAtLeast(0))

        Card(
            modifier = Modifier
                .wrapContentSize()
                .width(200.dp)
                .onSizeChanged { cardSize = it }
                .offset { IntOffset(xPx, yPx) },
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(Spacing.lg),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.animateContentSize(),
                    verticalAlignment = Alignment.Top
                ) { page -> popupPages[page].invoke() }
                if (popupPages.size > 1) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(popupPages.size) { index ->
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