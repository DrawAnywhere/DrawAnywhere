package com.shezik.drawanywhere.view.toolbar

import android.app.Activity
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shezik.drawanywhere.BuildConfig
import com.shezik.drawanywhere.DrawViewModel
import com.shezik.drawanywhere.R
import com.shezik.drawanywhere.model.PRESET_COLORS
import com.shezik.drawanywhere.model.StylusButtonAction
import com.shezik.drawanywhere.model.StylusButtonScheme
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.ColorPalette
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val pressureThresholdOptions = listOf(0.65f, 0.75f, 0.85f, 0.95f)

@Composable
fun SettingsScreen(
    viewModel: DrawViewModel,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    onChooseSaveLocation: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as? Activity
    val close: () -> Unit = onClose ?: { activity?.finish() }

    val backdropBackground = MiuixTheme.colorScheme.surface.copy(alpha = 0.56f)
    val backdrop = rememberLayerBackdrop {
        drawRect(backdropBackground)
        drawContent()
    }

    CompositionLocalProvider(
        LocalMiuixBlurBackdrop provides backdrop,
        LocalMiuixBlurEnabled provides true,
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = backdropBackground,
            topBar = {
                MiuixBlurTopAppBar(
                    title = stringResource(R.string.settings),
                    color = MiuixTheme.colorScheme.surface,
                    navigationIcon = {
                        IconButton(onClick = close) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.back),
                                tint = MiuixTheme.colorScheme.onBackground,
                            )
                        }
                    },
                )
            },
            content = { paddingValues ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().layerBackdrop(backdrop),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = paddingValues.calculateTopPadding() + 8.dp,
                        end = 12.dp,
                        bottom = paddingValues.calculateBottomPadding() + 20.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                item {
                    PreferenceSection(title = stringResource(R.string.settings_toolbar_section)) {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.toolbar_orientation_mode),
                            items = listOf(
                                stringResource(R.string.horizontal),
                                stringResource(R.string.vertical),
                            ),
                            selectedIndex = selectedIndexOf(
                                value = uiState.toolbarOrientationMode,
                                values = toolbarOrientationValues,
                            ),
                            onSelectedIndexChange = { index ->
                                viewModel.setToolbarOrientationMode(toolbarOrientationValues[index])
                            },
                        )
                        HorizontalDivider()
                        SwitchPreference(
                            title = stringResource(R.string.canvas_visible_on_start),
                            checked = uiState.visibleOnStart,
                            onCheckedChange = viewModel::setVisibleOnStart,
                        )
                        HorizontalDivider()
                        SwitchPreference(
                            title = stringResource(R.string.clear_on_hiding_canvas),
                            checked = uiState.autoClearCanvas,
                            onCheckedChange = viewModel::setAutoClearCanvas,
                        )
                        HorizontalDivider()
                        SaveLocationPreference(
                            exportTreeUri = uiState.exportTreeUri,
                            onChooseSaveLocation = onChooseSaveLocation,
                            onReset = viewModel::resetExportLocation,
                        )
                    }
                }

                item {
                    PreferenceSection(title = stringResource(R.string.settings_drawing_section)) {
                        SwitchPreference(
                            title = stringResource(R.string.finger_drawing),
                            checked = uiState.fingerDrawingEnabled,
                            onCheckedChange = viewModel::setFingerDrawingEnabled,
                        )
                    }
                }

                item {
                    PreferenceSection(title = stringResource(R.string.stylus_settings)) {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.stylus_button_scheme),
                            items = stylusSchemeLabels(),
                            selectedIndex = selectedIndexOf(
                                value = uiState.stylusButtonScheme,
                                values = StylusButtonScheme.entries,
                            ),
                            onSelectedIndexChange = { index ->
                                viewModel.setStylusButtonScheme(StylusButtonScheme.entries[index])
                            },
                        )

                        if (uiState.stylusButtonScheme != StylusButtonScheme.Disabled) {
                            HorizontalDivider()
                            OverlayDropdownPreference(
                                title = stringResource(R.string.stylus_primary_button_action),
                                items = stylusActionLabels(),
                                selectedIndex = selectedIndexOf(
                                    value = uiState.stylusPrimaryButtonAction,
                                    values = StylusButtonAction.entries,
                                ),
                                onSelectedIndexChange = { index ->
                                    viewModel.setStylusPrimaryButtonAction(StylusButtonAction.entries[index])
                                },
                            )
                            HorizontalDivider()
                            OverlayDropdownPreference(
                                title = stringResource(R.string.stylus_secondary_button_action),
                                items = stylusActionLabels(),
                                selectedIndex = selectedIndexOf(
                                    value = uiState.stylusSecondaryButtonAction,
                                    values = StylusButtonAction.entries,
                                ),
                                onSelectedIndexChange = { index ->
                                    viewModel.setStylusSecondaryButtonAction(StylusButtonAction.entries[index])
                                },
                            )
                        }

                        HorizontalDivider()
                        SwitchPreference(
                            title = stringResource(R.string.pressure_eraser),
                            checked = uiState.pressureEraserEnabled,
                            onCheckedChange = viewModel::setPressureEraserEnabled,
                        )

                        if (uiState.pressureEraserEnabled) {
                            HorizontalDivider()
                            OverlayDropdownPreference(
                                title = stringResource(R.string.pressure_eraser_threshold),
                                items = pressureThresholdLabels(),
                                selectedIndex = selectedIndexOf(
                                    value = pressureThresholdOption(uiState.pressureEraserThreshold),
                                    values = pressureThresholdOptions,
                                ),
                                onSelectedIndexChange = { index ->
                                    viewModel.setPressureEraserThreshold(pressureThresholdOptions[index])
                                },
                            )
                        }

                        HorizontalDivider()
                        StylusCycleColorEditor(
                            colors = uiState.stylusCycleColors,
                            onAddColor = viewModel::addStylusCycleColor,
                            onRemoveColor = viewModel::removeStylusCycleColor,
                            onReset = viewModel::resetStylusCycleColors,
                        )
                    }
                }

                item {
                    PreferenceSection(title = stringResource(R.string.app_name)) {
                        AboutContent()
                    }
                }
                }
            },
        )
    }
}

@Composable
fun FloatingSettingsWindow(
    viewModel: DrawViewModel,
    onChooseSaveLocation: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val windowWidth = minOf(configuration.screenWidthDp.dp * 0.92f, 520.dp)
    val windowHeight = minOf(configuration.screenHeightDp.dp * 0.86f, 720.dp)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .size(width = windowWidth, height = windowHeight)
                .clip(RoundedCornerShape(24.dp)),
            cornerRadius = 24.dp,
            colors = CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.18f),
                contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
            ),
        ) {
            SettingsScreen(
                viewModel = viewModel,
                onClose = onClose,
                onChooseSaveLocation = onChooseSaveLocation,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PreferenceSection(
    title: String,
    content: @Composable () -> Unit,
) {
    SmallTitle(text = title)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.34f),
            contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
        ),
    ) {
        content()
    }
}

@Composable
private fun SaveLocationPreference(
    exportTreeUri: String?,
    onChooseSaveLocation: () -> Unit,
    onReset: () -> Unit,
) {
    ArrowPreference(
        title = stringResource(R.string.save_location),
        summary = exportTreeUri?.toSaveLocationSummary()
            ?: stringResource(R.string.save_location_default_summary),
        onClick = onChooseSaveLocation,
        bottomAction = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    text = stringResource(R.string.choose_folder),
                    onClick = onChooseSaveLocation,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = stringResource(R.string.restore_default),
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    )
}

@Composable
private fun StylusCycleColorEditor(
    colors: List<Color>,
    onAddColor: (Color) -> Unit,
    onRemoveColor: (Color) -> Unit,
    onReset: () -> Unit,
) {
    var selectedColor by remember { mutableStateOf(colors.firstOrNull() ?: PRESET_COLORS.first()) }
    var colorInput by remember { mutableStateOf(selectedColor.toHexString(includeAlpha = false)) }
    val parsedColor = remember(colorInput) { parseColorInput(colorInput) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.stylus_cycle_colors),
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.stylus_cycle_colors_desc),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.chunked(8).forEach { rowColors ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowColors.forEach { color ->
                        ColorToken(
                            color = color,
                            selected = color.toArgb() == selectedColor.toArgb(),
                            onClick = {
                                selectedColor = color
                                colorInput = color.toHexString(includeAlpha = false)
                            },
                            onRemove = { onRemoveColor(color) },
                        )
                    }
                }
            }
        }
        ColorPalette(
            color = selectedColor,
            onColorChanged = {
                selectedColor = it.copy(alpha = 1f)
                colorInput = selectedColor.toHexString(includeAlpha = false)
            },
            modifier = Modifier.fillMaxWidth(),
            rows = 4,
            hueColumns = 10,
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
        if (colorInput.isNotBlank() && parsedColor == null) {
            Text(
                text = stringResource(R.string.color_value_invalid),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.error,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { onAddColor(parsedColor ?: selectedColor.copy(alpha = 1f)) },
                modifier = Modifier.weight(1f),
                enabled = colorInput.isBlank() || parsedColor != null,
            ) {
                Text(stringResource(R.string.add_color))
            }
            TextButton(
                text = stringResource(R.string.restore_default),
                onClick = onReset,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColorToken(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.outline.copy(alpha = 0.42f),
                shape = CircleShape,
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onRemove,
            ),
    )
}

@Composable
private fun AboutContent() {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_icon),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.size(72.dp),
            colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.primary),
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.SemiBold,
            color = MiuixTheme.colorScheme.onBackground,
        )
        Text(
            text = "${BuildConfig.VERSION_NAME}${if (BuildConfig.DEBUG) "-dev" else ""} (${BuildConfig.VERSION_CODE})",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.copyright),
            style = MiuixTheme.textStyles.body2,
            textAlign = TextAlign.Center,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Text(
            text = stringResource(R.string.licenses),
            style = MiuixTheme.textStyles.body2,
            textAlign = TextAlign.Center,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun stylusSchemeLabels(): List<String> = StylusButtonScheme.entries.map {
    when (it) {
        StylusButtonScheme.Disabled -> stringResource(R.string.stylus_button_scheme_disabled)
        StylusButtonScheme.XiaomiSmartPen -> stringResource(R.string.stylus_button_scheme_xiaomi)
    }
}

@Composable
private fun stylusActionLabels(): List<String> = StylusButtonAction.entries.map {
    when (it) {
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
}

@Composable
private fun pressureThresholdLabels(): List<String> = pressureThresholdOptions.map {
    stringResource(R.string.pressure_eraser_threshold_value, (it * 100).toInt())
}

private val toolbarOrientationValues = listOf(
    ToolbarOrientationMode.HORIZONTAL,
    ToolbarOrientationMode.VERTICAL,
)

private fun <T> selectedIndexOf(value: T, values: List<T>): Int =
    values.indexOf(value).takeIf { it >= 0 } ?: 0

private fun pressureThresholdOption(value: Float): Float =
    pressureThresholdOptions.minBy { kotlin.math.abs(it - value) }

private fun String.toSaveLocationSummary(): String =
    runCatching {
        Uri.parse(this).lastPathSegment
            ?.substringAfterLast(':')
            ?.ifBlank { null }
    }.getOrNull() ?: this
