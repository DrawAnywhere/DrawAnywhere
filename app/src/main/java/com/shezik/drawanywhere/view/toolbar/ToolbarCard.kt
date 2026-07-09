package com.shezik.drawanywhere.view.toolbar

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DraggableToolbarCard(
    modifier: Modifier = Modifier,
    haptics: HapticFeedback,
    onPositionChange: (Offset) -> Unit,
    onPositionSaved: () -> Unit,
    onToolbarInteracted: () -> Unit,
    onDragStart: (() -> Unit)? = null,
    onDragPosition: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Boolean)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .testTag("toolbar_card")
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        onToolbarInteracted()
                    }
                }
        },
        shape = RoundedCornerShape(20.dp),
        color = MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.10f),
        contentColor = MiuixTheme.colorScheme.onSurfaceContainerHigh,
        shadowElevation = 0.dp,
    ) {
        content()
    }
}
