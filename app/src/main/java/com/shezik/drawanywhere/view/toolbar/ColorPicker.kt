package com.shezik.drawanywhere.view.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import top.yukonga.miuix.kmp.basic.ColorPalette

@Composable
fun ColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    onPresetSelected: (Color) -> Unit = onColorSelected,
    recentColors: List<Color> = emptyList(),
) {
    var paletteColor by remember(selectedColor) { mutableStateOf(selectedColor) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SectionLabel(stringResource(R.string.color))
        PresetRows(
            selectedColor = paletteColor,
            onPresetSelected = { color ->
                val colorWithCurrentAlpha = color.copy(alpha = paletteColor.alpha)
                paletteColor = colorWithCurrentAlpha
                onPresetSelected(colorWithCurrentAlpha)
            },
        )

        if (recentColors.isNotEmpty()) {
            SectionLabel(stringResource(R.string.recent), subdued = true)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                recentColors.take(6).forEach { color ->
                    key(color.toArgb()) {
                        ColorSwatchButton(
                            color = color,
                            isSelected = color.toArgb() == paletteColor.toArgb(),
                            onClick = {
                                paletteColor = color
                                onColorSelected(color)
                            },
                        )
                    }
                }
            }
        }

        ColorPalette(
            color = paletteColor,
            onColorChanged = { paletteColor = it },
            modifier = Modifier.fillMaxWidth(),
            rows = 6,
            hueColumns = 12,
            showPreview = true,
        )

        Button(
            onClick = { onColorSelected(paletteColor) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = paletteColor),
        ) {
            Text(stringResource(R.string.apply_color))
        }
    }
}

@Composable
private fun PresetRows(
    selectedColor: Color,
    onPresetSelected: (Color) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        PRESET_COLORS.chunked(6).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                row.forEach { color ->
                    ColorSwatchButton(
                        color = color,
                        isSelected = color.rgbEquals(selectedColor),
                        onClick = { onPresetSelected(color) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                },
                shape = CircleShape,
            )
            .clickable { onClick() },
    )
}

@Composable
private fun SectionLabel(
    text: String,
    subdued: Boolean = false,
) {
    Text(
        text = text,
        modifier = Modifier.padding(top = if (subdued) 0.dp else 2.dp),
        style = if (subdued) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = if (subdued) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
    )
}
