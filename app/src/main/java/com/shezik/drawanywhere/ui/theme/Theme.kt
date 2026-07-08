/*
DrawAnywhere: An Android application that lets you draw on top of other apps.
Copyright (C) 2025-2026 shezik

This program is free software: you can redistribute it and/or modify it under the
terms of the GNU Affero General Public License as published by the Free Software
Foundation, either version 3 of the License, or any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along
with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.shezik.drawanywhere.ui.theme

import android.app.Activity
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.OnBackInvokedDefaultInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme as MiuixRootTheme
import top.yukonga.miuix.kmp.theme.ThemeController

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = Teal80,
    tertiary = Rose80,
    background = Slate950,
    surface = Slate900,
    surfaceContainer = Slate800,
    surfaceContainerHigh = Slate700,
    surfaceContainerHighest = Color(0xFF323A44),
    primaryContainer = Color(0xFF294A70),
    secondaryContainer = Color(0xFF2A4B4A),
    tertiaryContainer = Color(0xFF653B36),
    outline = Color(0xFF76808A),
    onBackground = Cloud50,
    onSurface = Cloud50,
    onSurfaceVariant = Color(0xFFBEC7D2),
    onPrimary = Color(0xFF0E1B2D),
    onPrimaryContainer = Cloud50,
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = Teal40,
    tertiary = Rose40,
    background = Cloud50,
    surface = Color.White,
    surfaceContainer = Cloud100,
    surfaceContainerHigh = Cloud200,
    surfaceContainerHighest = Color(0xFFD6DEE8),
    primaryContainer = Color(0xFFD8E2FF),
    secondaryContainer = Color(0xFFD3ECEB),
    tertiaryContainer = Color(0xFFFFDAD4),
    outline = Color(0xFF6E7882),
    onBackground = Slate950,
    onSurface = Slate950,
    onSurfaceVariant = Slate500,
    onPrimary = Color.White,
    onPrimaryContainer = Slate950,
)

@Composable
fun DrawAnywhereTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme || dynamicColor) DarkColorScheme else LightColorScheme
    val miuixController = remember {
        ThemeController(
            ColorSchemeMode.System,
            keyColor = Color(0xFF3482FF),
        )
    }
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val navigationEventDispatcher = remember(activity) {
        NavigationEventDispatcher {
            (activity as? Activity)?.finish()
        }
    }
    DisposableEffect(navigationEventDispatcher, activity) {
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val input = OnBackInvokedDefaultInput(activity.onBackInvokedDispatcher)
            navigationEventDispatcher.addInput(input)
            onDispose { navigationEventDispatcher.removeInput(input) }
        } else {
            onDispose { }
        }
    }
    val navigationEventDispatcherOwner = remember(navigationEventDispatcher) {
        object : androidx.navigationevent.NavigationEventDispatcherOwner {
            override val navigationEventDispatcher = navigationEventDispatcher
        }
    }

    CompositionLocalProvider(
        LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner,
    ) {
        MiuixRootTheme(controller = miuixController) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                content = content
            )
        }
    }
}
