package com.shezik.drawanywhere

import android.graphics.Color
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import com.shezik.drawanywhere.ui.theme.DrawAnywhereTheme
import com.shezik.drawanywhere.view.toolbar.SettingsScreen

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val viewModel = DrawSessionBridge.viewModel
        if (viewModel == null) {
            finish()
            return
        }

        setContentView(
            ComposeView(this).apply {
                setContent {
                    DrawAnywhereTheme {
                        SettingsScreen(
                            viewModel = viewModel,
                            onChooseSaveLocation = {
                                startActivity(Intent(this@SettingsActivity, SaveLocationPickerActivity::class.java))
                            },
                        )
                    }
                }
            }
        )
    }
}
