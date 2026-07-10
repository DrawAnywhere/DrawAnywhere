package com.shezik.drawanywhere

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class ScreenCapturePermissionActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val serviceIntent = Intent(this, MainService::class.java).apply {
            action = MainService.ACTION_SCREEN_CAPTURE_PERMISSION_RESULT
            putExtra(MainService.EXTRA_SCREEN_CAPTURE_RESULT_CODE, result.resultCode)
            putExtra(MainService.EXTRA_SCREEN_CAPTURE_DATA, result.data)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        finishWithoutTransition()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
        permissionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun finishWithoutTransition() {
        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
