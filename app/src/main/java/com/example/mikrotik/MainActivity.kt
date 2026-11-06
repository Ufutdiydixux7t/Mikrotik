package com.example.mikrotik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.mikrotik.presentation.dashboard.DashboardViewModel
import com.example.mikrotik.presentation.diagnostics.DiagnosticsViewModel
import com.example.mikrotik.presentation.home.HomeViewModel
import com.example.mikrotik.presentation.main.MainScreen
import com.example.mikrotik.presentation.voucher.VoucherViewModel
import com.example.mikrotik.ui.theme.MikrotikTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val diagnosticsViewModel: DiagnosticsViewModel by viewModels()
    private val voucherViewModel: VoucherViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = true
        }
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MikrotikTheme {
                    MainScreen(
                        homeViewModel = homeViewModel,
                        diagnosticsViewModel = diagnosticsViewModel,
                        voucherViewModel = voucherViewModel,
                        dashboardViewModel = dashboardViewModel
                    )
                }
            }
        }
    }
}
