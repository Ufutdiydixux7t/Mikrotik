package com.example.mikrotik

import android.os.Bundle
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.mikrotik.presentation.dashboard.DashboardViewModel
import com.example.mikrotik.presentation.diagnostics.DiagnosticsViewModel
import com.example.mikrotik.presentation.home.HomeViewModel
import com.example.mikrotik.presentation.main.MainScreen
import com.example.mikrotik.presentation.ui.localization.AppLanguage
import com.example.mikrotik.presentation.ui.localization.LocalAppLanguage
import com.example.mikrotik.presentation.voucher.VoucherViewModel
import com.example.mikrotik.security.AppLockGateScreen
import com.example.mikrotik.security.AppLockViewModel
import com.example.mikrotik.ui.theme.MikrotikTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val diagnosticsViewModel: DiagnosticsViewModel by viewModels()
    private val voucherViewModel: VoucherViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val appLockViewModel: AppLockViewModel by viewModels()
    private var screenOffReceiverRegistered = false
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) appLockViewModel.onScreenLocked()
        }
    }

    override fun onStart() {
        super.onStart()
        appLockViewModel.onAppForeground()
    }

    override fun onStop() {
        val keyguard = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
        if (!isChangingConfigurations) {
            appLockViewModel.onAppBackground(isScreenLocked = keyguard?.isKeyguardLocked == true)
        }
        super.onStop()
    }

    override fun onDestroy() {
        if (screenOffReceiverRegistered) {
            unregisterReceiver(screenOffReceiver)
            screenOffReceiverRegistered = false
        }
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        screenOffReceiverRegistered = true
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = true
        }
        setContent {
            val appLockConfig by appLockViewModel.config.collectAsState()
            val appIsLocked by appLockViewModel.isLocked.collectAsState()
            val preferences = remember {
                getSharedPreferences("mikrotik_ui_preferences", android.content.Context.MODE_PRIVATE)
            }
            val darkMode = remember {
                mutableStateOf(preferences.getBoolean("dark_mode_enabled", false))
            }
            val language = remember {
                mutableStateOf(AppLanguage.fromCode(preferences.getString("ui_language", "ar")))
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = !darkMode.value
                }
                if (appLockConfig.enabled) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
            CompositionLocalProvider(
                LocalAppLanguage provides language.value,
                LocalLayoutDirection provides (
                    if (language.value == AppLanguage.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr
                )
            ) {
                MikrotikTheme(darkTheme = darkMode.value) {
                    if (appLockConfig.enabled && appIsLocked) {
                        AppLockGateScreen(viewModel = appLockViewModel)
                    } else {
                        MainScreen(
                            homeViewModel = homeViewModel,
                            diagnosticsViewModel = diagnosticsViewModel,
                            voucherViewModel = voucherViewModel,
                            dashboardViewModel = dashboardViewModel,
                            appLockViewModel = appLockViewModel,
                            isDarkModeEnabled = darkMode.value,
                            onDarkModeToggle = { enabled ->
                                darkMode.value = enabled
                                preferences.edit().putBoolean("dark_mode_enabled", enabled).apply()
                            },
                            language = language.value,
                            onLanguageChange = { selectedLanguage ->
                                language.value = selectedLanguage
                                preferences.edit().putString("ui_language", selectedLanguage.code).apply()
                            }
                        )
                    }
                }
            }
        }
    }
}
