package com.shezik.drawanywhere.view.toolbar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shezik.drawanywhere.R
import com.shezik.drawanywhere.model.PRESET_COLORS
import com.shezik.drawanywhere.ui.theme.Spacing
import com.godaddy.android.colorpicker.HsvColor
import com.godaddy.android.colorpicker.harmony.ColorHarmonyMode
import com.godaddy.android.colorpicker.harmony.HarmonyColorPicker

@Composable
private fun ColorSwatchButton(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    onPresetSelected: (Color) -> Unit = onColorSelected,
    recentColors: List<Color> = emptyList(),
) {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        HorizontalPager(state = pagerState, verticalAlignment = Alignment.Top) { page ->
            when (page) {
                0 -> PresetsPage(selectedColor, onPresetSelected, onColorSelected, recentColors)
                1 -> HsvPage(selectedColor, onColorSelected)
            }
        }
        // Page dots
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            repeat(pagerState.pageCount) { idx ->
                val selected = pagerState.currentPage == idx
                Box(
                    modifier = Modifier
                        .size(if (selected) 10.dp else 6.dp)
                        .padding(2.dp)
                        .background(
                            if (selected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun PresetsPage(
    selectedColor: Color,
    onPresetSelected: (Color) -> Unit,
    onRecentSelected: (Color) -> Unit,
    recentColors: List<Color>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        // ── Presets ────────────────────────────────────────────
        Text(
            text = stringResource(R.string.color),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        val presets = PRESET_COLORS
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            presets.chunked(6).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally)) {
                    row.forEach { c ->
                        ColorSwatchButton(c, c.toArgb() == selectedColor.toArgb()) { onPresetSelected(c) }
                    }
                }
            }
        }

        // ── Recent ──────────────────────────────────────────────
        if (recentColors.isNotEmpty()) {
            Text(
                text = stringResource(R.string.recent),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally)) {
                recentColors.take(6).forEach { c ->
                    key(c.toArgb()) {
                        ColorSwatchButton(c, c.toArgb() == selectedColor.toArgb()) { onRecentSelected(c) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HsvPage(selectedColor: Color, onColorSelected: (Color) -> Unit) {
    Column {
        SectionLabel(
            stringResource(R.string.hsv),
            modifier = Modifier.padding(bottom = Spacing.sm),
        )
        val initial = remember(selectedColor) { HsvColor.from(selectedColor) }
        var h by remember(selectedColor) { mutableFloatStateOf(initial.hue) }
        var s by remember(selectedColor) { mutableFloatStateOf(initial.saturation) }
        var v by remember(selectedColor) { mutableFloatStateOf(initial.value) }

        val previewColor = HsvColor(h, s, v, 1f).toColor()
        var hexText by remember { mutableStateOf("%06X".format(previewColor.toArgb() and 0xFFFFFF)) }
        LaunchedEffect(previewColor) { hexText = "%06X".format(previewColor.toArgb() and 0xFFFFFF) }
        LaunchedEffect(selectedColor) {
            val hsv = HsvColor.from(selectedColor)
            h = hsv.hue; s = hsv.saturation; v = hsv.value
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(previewColor)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            OutlinedTextField(
                value = hexText,
                onValueChange = { txt ->
                    hexText = txt.take(6).uppercase()
                    if (txt.length == 6) {
                        try {
                            val c = Color(("FF$txt").toLong(16).toInt())
                            val hsv = HsvColor.from(c)
                            h = hsv.hue; s = hsv.saturation; v = hsv.value
                        } catch (_: Exception) {}
                    }
                },
                modifier = Modifier.weight(1f).height(52.dp),
                singleLine = true,
                prefix = { Text("#", style = MaterialTheme.typography.bodyMedium) },
                textStyle = MaterialTheme.typography.bodyMedium,
            )
        }

        // ── Color wheel ──────────────────────────────────────────
        HarmonyColorPicker(
            modifier = Modifier.fillMaxWidth().height(150.dp),
            harmonyMode = ColorHarmonyMode.NONE,
            color = HsvColor(
                hue = h, saturation = s, value = v, alpha = 1f,
            ),
            showBrightnessBar = false,
            onColorChanged = { hsv ->
                h = hsv.hue
                s = hsv.saturation
            },
        )

        HsvSlider("H", h, 0f..360f, { h = it }, { "${it.toInt()}°" })
        HsvSlider("S", s, 0f..1f, { s = it }, { "${(it * 100).toInt()}%" }, modifier = Modifier.padding(top = Spacing.sm))
        HsvSlider("V", v, 0f..1f, { v = it }, { "${(it * 100).toInt()}%" }, modifier = Modifier.padding(top = Spacing.sm))

        val contrastColor = Color(1f - previewColor.red, 1f - previewColor.green, 1f - previewColor.blue)

        Button(
            onClick = { onColorSelected(previewColor) },
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            shape = RoundedCornerShape(Spacing.sm),
            colors = ButtonDefaults.buttonColors(containerColor = previewColor, contentColor = contrastColor)
        ) {
            Text(stringResource(R.string.apply_color))
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
private fun HsvSlider(
    label: String, value: Float, valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit, valueDisplay: (Float) -> String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueDisplay(value), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value, onValueChange = onValueChange, valueRange = valueRange,
            modifier = Modifier.height(18.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            )
        )
    }
}
