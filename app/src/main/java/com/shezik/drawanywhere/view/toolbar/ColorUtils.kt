package com.shezik.drawanywhere.view.toolbar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

internal fun Color.rgbEquals(other: Color): Boolean =
    red == other.red && green == other.green && blue == other.blue

internal fun Color.toHexString(includeAlpha: Boolean = alpha < 1f): String {
    val argb = toArgb()
    return if (includeAlpha) {
        "#${argb.toUInt().toString(16).padStart(8, '0').uppercase()}"
    } else {
        "#${(argb and 0x00FFFFFF).toString(16).padStart(6, '0').uppercase()}"
    }
}

internal fun parseColorInput(input: String): Color? {
    val raw = input.trim()
    if (raw.isBlank()) return null

    parseHexColor(raw)?.let { return it }
    parseRgbColor(raw)?.let { return it }
    return null
}

private fun parseHexColor(input: String): Color? {
    val normalized = input
        .removePrefix("#")
        .removePrefix("0x")
        .removePrefix("0X")
        .trim()
    if (normalized.length !in setOf(6, 8)) return null
    if (!normalized.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null

    val argb = normalized.toLong(16).toInt()
    return if (normalized.length == 6) {
        Color(0xFF000000.toInt() or argb)
    } else {
        Color(argb)
    }
}

private fun parseRgbColor(input: String): Color? {
    val normalized = input
        .removePrefix("rgb(")
        .removePrefix("RGB(")
        .removeSuffix(")")
        .replace(';', ',')
    val parts = normalized
        .split(',', ' ')
        .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
    if (parts.size !in 3..4) return null

    val channels = parts.map { it.toFloatOrNull() ?: return null }
    val red = channels[0].toColorChannel() ?: return null
    val green = channels[1].toColorChannel() ?: return null
    val blue = channels[2].toColorChannel() ?: return null
    val alpha = channels.getOrNull(3)?.let {
        if (it <= 1f) it.coerceIn(0f, 1f) else (it / 255f).coerceIn(0f, 1f)
    } ?: 1f

    return Color(red, green, blue, alpha)
}

private fun Float.toColorChannel(): Float? =
    if (this in 0f..255f) this / 255f else null
