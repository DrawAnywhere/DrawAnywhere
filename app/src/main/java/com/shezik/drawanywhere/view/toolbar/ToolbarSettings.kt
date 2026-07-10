package com.shezik.drawanywhere.view.toolbar

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shezik.drawanywhere.BuildConfig
import com.shezik.drawanywhere.R
import com.shezik.drawanywhere.ui.theme.Spacing

@Composable
internal fun ToolbarControls(
    currentOrientation: ToolbarOrientation,
    onChangeOrientation: (ToolbarOrientation) -> Unit,
    autoClearCanvas: Boolean,
    onChangeAutoClearCanvas: (Boolean) -> Unit,
    visibleOnStart: Boolean,
    onChangeVisibleOnStart: (Boolean) -> Unit,
    fingerDrawingEnabled: Boolean,
    onChangeFingerDrawingEnabled: (Boolean) -> Unit,
    exportTreeUri: String?,
    onChooseSaveLocation: () -> Unit,
    onResetSaveLocation: () -> Unit,
    onQuitApplication: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
            val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                     else MaterialTheme.colorScheme.surface
            val fg = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                     else MaterialTheme.colorScheme.onSurface
            Button(
                onClick = { onChangeOrientation(orientation) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg),
                shape = RoundedCornerShape(Spacing.sm)
            ) { Text(text = label, style = MaterialTheme.typography.bodyMedium) }
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
        CheckboxControl(
            label = stringResource(R.string.finger_drawing),
            isChecked = fingerDrawingEnabled,
            onCheckedChange = onChangeFingerDrawingEnabled
        )
        SaveLocationControls(
            exportTreeUri = exportTreeUri,
            onChooseSaveLocation = onChooseSaveLocation,
            onResetSaveLocation = onResetSaveLocation
        )
        Button(
            onClick = onQuitApplication,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(8.dp)
        ) { Text(text = stringResource(R.string.quit), style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
internal fun SaveControls(
    onSaveTransparent: () -> Unit,
    onSaveWithBackdrop: () -> Unit,
    onSaveWithScreenBackdrop: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = stringResource(R.string.save_drawing),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Button(
            onClick = onSaveTransparent,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Spacing.sm)
        ) {
            Text(text = stringResource(R.string.save_transparent_png))
        }
        Button(
            onClick = onSaveWithBackdrop,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Spacing.sm)
        ) {
            Text(text = stringResource(R.string.save_backdrop_png))
        }
        Button(
            onClick = onSaveWithScreenBackdrop,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Spacing.sm)
        ) {
            Text(text = stringResource(R.string.save_screen_backdrop_png))
        }
    }
}

@Composable
private fun SaveLocationControls(
    exportTreeUri: String?,
    onChooseSaveLocation: () -> Unit,
    onResetSaveLocation: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.save_location),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = exportTreeUri?.toSaveLocationSummary()
                ?: stringResource(R.string.save_location_default_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(
                onClick = onChooseSaveLocation,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Spacing.sm)
            ) {
                Text(text = stringResource(R.string.choose_folder))
            }
            OutlinedButton(
                onClick = onResetSaveLocation,
                modifier = Modifier.fillMaxWidth(),
                enabled = exportTreeUri != null,
                shape = RoundedCornerShape(Spacing.sm)
            ) {
                Text(text = stringResource(R.string.restore_default))
            }
        }
    }
}

@Composable
internal fun AboutScreen() {
    Box(modifier = Modifier.padding(Spacing.md)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
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
            Spacer(modifier = Modifier.height(Spacing.xl))
            Text(
                text = stringResource(R.string.copyright),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
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
internal fun CheckboxControl(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
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
            modifier = Modifier.width(24.dp).height(24.dp),
            colors = CheckboxDefaults.colors(
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

private fun String.toSaveLocationSummary(): String =
    runCatching {
        Uri.parse(this).lastPathSegment
            ?.substringAfterLast(':')
            ?.ifBlank { null }
    }.getOrNull() ?: this
