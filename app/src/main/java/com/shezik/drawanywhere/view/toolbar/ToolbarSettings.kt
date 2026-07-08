package com.shezik.drawanywhere.view.toolbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.shezik.drawanywhere.model.StylusButtonAction
import com.shezik.drawanywhere.model.StylusButtonScheme
import com.shezik.drawanywhere.ui.theme.Spacing

@Composable
internal fun ToolbarControls(
    currentOrientationMode: ToolbarOrientationMode,
    onChangeOrientationMode: (ToolbarOrientationMode) -> Unit,
    autoClearCanvas: Boolean,
    onChangeAutoClearCanvas: (Boolean) -> Unit,
    visibleOnStart: Boolean,
    onChangeVisibleOnStart: (Boolean) -> Unit,
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
    onQuitApplication: () -> Unit
) {
    var page by remember { mutableStateOf(SettingsPage.Main) }
    val schemeOptions = StylusButtonScheme.entries.map { it to stylusButtonSchemeLabel(it) }
    val actionOptions = StylusButtonAction.entries.map { it to stylusButtonActionLabel(it) }
    val pressureOptions = PRESSURE_THRESHOLD_OPTIONS.map {
        it to stringResource(R.string.pressure_eraser_threshold_value, (it * 100).toInt())
    }

    when (page) {
        SettingsPage.Main -> SettingsMainPage(
            currentOrientationMode = currentOrientationMode,
            onChangeOrientationMode = onChangeOrientationMode,
            autoClearCanvas = autoClearCanvas,
            onChangeAutoClearCanvas = onChangeAutoClearCanvas,
            visibleOnStart = visibleOnStart,
            onChangeVisibleOnStart = onChangeVisibleOnStart,
            fingerDrawingEnabled = fingerDrawingEnabled,
            onChangeFingerDrawingEnabled = onChangeFingerDrawingEnabled,
            stylusButtonScheme = stylusButtonScheme,
            onStylusSettingsClick = { page = SettingsPage.Stylus },
            onQuitApplication = onQuitApplication,
        )
        SettingsPage.Stylus -> StylusSettingsPage(
            stylusButtonScheme = stylusButtonScheme,
            stylusPrimaryButtonAction = stylusPrimaryButtonAction,
            stylusSecondaryButtonAction = stylusSecondaryButtonAction,
            pressureEraserEnabled = pressureEraserEnabled,
            onChangePressureEraserEnabled = onChangePressureEraserEnabled,
            pressureEraserThreshold = pressureEraserThreshold,
            onBack = { page = SettingsPage.Main },
            onSchemeClick = { page = SettingsPage.Scheme },
            onPrimaryActionClick = { page = SettingsPage.PrimaryAction },
            onSecondaryActionClick = { page = SettingsPage.SecondaryAction },
            onPressureThresholdClick = { page = SettingsPage.PressureThreshold },
        )
        SettingsPage.Scheme -> SelectionPage(
            title = stringResource(R.string.stylus_button_scheme),
            selected = stylusButtonScheme,
            options = schemeOptions,
            onSelected = {
                onChangeStylusButtonScheme(it)
                page = SettingsPage.Stylus
            },
            onBack = { page = SettingsPage.Stylus },
        )
        SettingsPage.PrimaryAction -> SelectionPage(
            title = stringResource(R.string.stylus_primary_button_action),
            selected = stylusPrimaryButtonAction,
            options = actionOptions,
            onSelected = {
                onChangeStylusPrimaryButtonAction(it)
                page = SettingsPage.Stylus
            },
            onBack = { page = SettingsPage.Stylus },
        )
        SettingsPage.SecondaryAction -> SelectionPage(
            title = stringResource(R.string.stylus_secondary_button_action),
            selected = stylusSecondaryButtonAction,
            options = actionOptions,
            onSelected = {
                onChangeStylusSecondaryButtonAction(it)
                page = SettingsPage.Stylus
            },
            onBack = { page = SettingsPage.Stylus },
        )
        SettingsPage.PressureThreshold -> SelectionPage(
            title = stringResource(R.string.pressure_eraser_threshold),
            selected = pressureThresholdOption(pressureEraserThreshold),
            options = pressureOptions,
            onSelected = {
                onChangePressureEraserThreshold(it)
                page = SettingsPage.Stylus
            },
            onBack = { page = SettingsPage.Stylus },
        )
    }
}

private enum class SettingsPage {
    Main,
    Stylus,
    Scheme,
    PrimaryAction,
    SecondaryAction,
    PressureThreshold,
}

@Composable
private fun SettingsMainPage(
    currentOrientationMode: ToolbarOrientationMode,
    onChangeOrientationMode: (ToolbarOrientationMode) -> Unit,
    autoClearCanvas: Boolean,
    onChangeAutoClearCanvas: (Boolean) -> Unit,
    visibleOnStart: Boolean,
    onChangeVisibleOnStart: (Boolean) -> Unit,
    fingerDrawingEnabled: Boolean,
    onChangeFingerDrawingEnabled: (Boolean) -> Unit,
    stylusButtonScheme: StylusButtonScheme,
    onStylusSettingsClick: () -> Unit,
    onQuitApplication: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        PageHeader(title = stringResource(R.string.settings))
        val orientations = listOf(
            ToolbarOrientationMode.HORIZONTAL to stringResource(R.string.horizontal),
            ToolbarOrientationMode.VERTICAL to stringResource(R.string.vertical)
        )
        orientations.forEach { (orientationMode, label) ->
            val isSelected = currentOrientationMode == orientationMode
            val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                     else MaterialTheme.colorScheme.surface
            val fg = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                     else MaterialTheme.colorScheme.onSurface
            Button(
                onClick = { onChangeOrientationMode(orientationMode) },
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
        NavigationControl(
            label = stringResource(R.string.stylus_settings),
            value = stylusButtonSchemeLabel(stylusButtonScheme),
            onClick = onStylusSettingsClick
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
private fun StylusSettingsPage(
    stylusButtonScheme: StylusButtonScheme,
    stylusPrimaryButtonAction: StylusButtonAction,
    stylusSecondaryButtonAction: StylusButtonAction,
    pressureEraserEnabled: Boolean,
    onChangePressureEraserEnabled: (Boolean) -> Unit,
    pressureEraserThreshold: Float,
    onBack: () -> Unit,
    onSchemeClick: () -> Unit,
    onPrimaryActionClick: () -> Unit,
    onSecondaryActionClick: () -> Unit,
    onPressureThresholdClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        PageHeader(title = stringResource(R.string.stylus_settings), onBack = onBack)
        NavigationControl(
            label = stringResource(R.string.stylus_button_scheme),
            value = stylusButtonSchemeLabel(stylusButtonScheme),
            onClick = onSchemeClick
        )
        if (stylusButtonScheme != StylusButtonScheme.Disabled) {
            NavigationControl(
                label = stringResource(R.string.stylus_primary_button_action),
                value = stylusButtonActionLabel(stylusPrimaryButtonAction),
                onClick = onPrimaryActionClick
            )
            NavigationControl(
                label = stringResource(R.string.stylus_secondary_button_action),
                value = stylusButtonActionLabel(stylusSecondaryButtonAction),
                onClick = onSecondaryActionClick
            )
        }
        CheckboxControl(
            label = stringResource(R.string.pressure_eraser),
            isChecked = pressureEraserEnabled,
            onCheckedChange = onChangePressureEraserEnabled
        )
        if (pressureEraserEnabled) {
            NavigationControl(
                label = stringResource(R.string.pressure_eraser_threshold),
                value = stringResource(
                    R.string.pressure_eraser_threshold_value,
                    (pressureThresholdOption(pressureEraserThreshold) * 100).toInt()
                ),
                onClick = onPressureThresholdClick
            )
        }
    }
}

@Composable
private fun <T> SelectionPage(
    title: String,
    selected: T,
    options: List<Pair<T, String>>,
    onSelected: (T) -> Unit,
    onBack: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        PageHeader(title = title, onBack = onBack)
        options.forEach { (value, label) ->
            val isSelected = selected == value
            Button(
                onClick = { onSelected(value) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(Spacing.sm)
            ) {
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PageHeader(title: String, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private val PRESSURE_THRESHOLD_OPTIONS = listOf(0.65f, 0.75f, 0.85f, 0.95f)

private fun pressureThresholdOption(value: Float): Float =
    PRESSURE_THRESHOLD_OPTIONS.minBy { kotlin.math.abs(it - value) }

@Composable
private fun stylusButtonSchemeLabel(scheme: StylusButtonScheme): String =
    when (scheme) {
        StylusButtonScheme.Disabled -> stringResource(R.string.stylus_button_scheme_disabled)
        StylusButtonScheme.XiaomiSmartPen -> stringResource(R.string.stylus_button_scheme_xiaomi)
    }

@Composable
private fun stylusButtonActionLabel(action: StylusButtonAction): String =
    when (action) {
        StylusButtonAction.None -> stringResource(R.string.stylus_action_none)
        StylusButtonAction.CyclePresetColor -> stringResource(R.string.stylus_action_cycle_color)
        StylusButtonAction.ToggleStrokeEraser -> stringResource(R.string.stylus_action_stroke_eraser)
        StylusButtonAction.TogglePixelEraser -> stringResource(R.string.stylus_action_pixel_eraser)
        StylusButtonAction.Undo -> stringResource(R.string.undo)
        StylusButtonAction.Redo -> stringResource(R.string.redo)
        StylusButtonAction.ToggleCanvasVisibility -> stringResource(R.string.stylus_action_toggle_canvas)
        StylusButtonAction.ToggleCanvasPassthrough -> stringResource(R.string.stylus_action_toggle_passthrough)
        StylusButtonAction.ToggleLaser -> stringResource(R.string.laser)
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

@Composable
internal fun NavigationControl(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(Spacing.sm)
    ) {
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}
