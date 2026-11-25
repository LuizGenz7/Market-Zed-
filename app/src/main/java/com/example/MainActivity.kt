package com.example

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.data.notifications.AppNavigationTarget
import com.example.data.notifications.NotificationTargetParser
import com.example.ui.MarketZedApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingNavigationTarget by mutableStateOf<AppNavigationTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transparentLightBars = SystemBarStyle.light(
            scrim = AndroidColor.TRANSPARENT,
            darkScrim = AndroidColor.TRANSPARENT
        )
        enableEdgeToEdge(
            statusBarStyle = transparentLightBars,
            navigationBarStyle = transparentLightBars
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        pendingNavigationTarget = NotificationTargetParser.parseIntent(intent)

        val appContainer = (application as MarketZedApplication).container
        setContent {
            MarketZedApp(
                container = appContainer,
                pendingNavigationTarget = pendingNavigationTarget,
                onConsumeNavigationTarget = { pendingNavigationTarget = null }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        MarketZedApplication.isForeground = true
        val container = (application as? MarketZedApplication)?.container ?: return
        container.notificationPreferencesRepository.recordAppActive()
        lifecycleScope.launch {
            container.authRepository.validateAndRefreshSession(forceReload = false)
        }
    }

    override fun onStop() {
        MarketZedApplication.isForeground = false
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NotificationTargetParser.parseIntent(intent)?.let {
            pendingNavigationTarget = it
        }
    }
}
