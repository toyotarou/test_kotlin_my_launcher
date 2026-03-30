package com.example.test_kotlin_my_launcher

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.content.pm.LauncherApps
import android.os.Process
import com.example.test_kotlin_my_launcher.ui.screen.LauncherScreen

class MainActivity : ComponentActivity() {
    private val _pinTrigger = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()   // super より前に呼ぶ必要あり
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        acceptPinShortcut(intent)
        setContent { MaterialTheme { LauncherScreen(pinTrigger = _pinTrigger.intValue) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        acceptPinShortcut(intent)
    }

    // Chrome などから「ホーム画面に追加」されたときに呼ばれる
    private fun acceptPinShortcut(intent: Intent?) {
        if (intent?.action != "android.content.pm.action.CONFIRM_PIN_SHORTCUT") return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val la = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            la.getPinItemRequest(intent)
                ?.takeIf { it.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT }
                ?.accept()
        }
        _pinTrigger.intValue++
    }
}
