package com.example.mikrotik.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mikrotik.presentation.dashboard.DashboardScreen
import com.example.mikrotik.presentation.dashboard.DashboardViewModel
import com.example.mikrotik.presentation.diagnostics.DiagnosticsScreen
import com.example.mikrotik.presentation.diagnostics.DiagnosticsViewModel
import com.example.mikrotik.presentation.home.HomeScreen
import com.example.mikrotik.presentation.home.HomeViewModel
import com.example.mikrotik.presentation.ui.components.AppBackground
import com.example.mikrotik.presentation.ui.components.AppTopBar
import com.example.mikrotik.presentation.ui.theme.AppIcons
import com.example.mikrotik.presentation.voucher.VoucherScreen
import com.example.mikrotik.presentation.voucher.VoucherViewModel
import com.example.mikrotik.ui.theme.AppPalette

private val DeepGreenHeader get() = AppPalette.TextNavy
private val RichGreenButton get() = AppPalette.Teal
private val SeamlessCardBg get() = AppPalette.Card
private val WatermarkLineColor get() = AppPalette.Border
private val InactiveColor get() = AppPalette.TextNavy
private val BottomBarBg get() = AppPalette.Card

enum class BottomTab(val title: String, val icon: ImageVector) {
    HOME("الرئيسية", AppIcons.Home),
    CREATE_VOUCHER("إنشاء بطاقة", AppIcons.CreditCard),
    DASHBOARD("لوحة التحكم", AppIcons.BarChart),
    SETTINGS("الإعدادات", AppIcons.SettingsGear)
}

@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    diagnosticsViewModel: DiagnosticsViewModel,
    voucherViewModel: VoucherViewModel,
    dashboardViewModel: DashboardViewModel,
    isDarkModeEnabled: Boolean,
    onDarkModeToggle: (Boolean) -> Unit
) {
    var currentTab by remember { mutableStateOf(BottomTab.HOME) }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                // Floating Rounded Bottom Navigation Bar with margins
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 8.dp,
                        shape = RoundedCornerShape(36.dp),
                        color = BottomBarBg
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(72.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BottomTab.entries.forEach { tab ->
                                val isSelected = currentTab == tab
                                val contentColor = if (isSelected) RichGreenButton else InactiveColor

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { currentTab = tab }
                                        .padding(horizontal = 2.dp, vertical = 5.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (tab == BottomTab.HOME && isSelected) {
                                            AppIcons.HomeFilled
                                        } else {
                                            tab.icon
                                        },
                                        contentDescription = tab.title,
                                        tint = contentColor,
                                        modifier = Modifier.size(30.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tab.title,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = contentColor,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(30.dp)
                                            .height(4.dp)
                                            .background(
                                                if (isSelected) RichGreenButton else Color.Transparent,
                                                shape = RoundedCornerShape(3.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    BottomTab.HOME -> {
                        HomeScreen(
                            viewModel = homeViewModel,
                            onOpenDiagnostics = { currentTab = BottomTab.SETTINGS }
                        )
                    }
                    BottomTab.CREATE_VOUCHER -> {
                        VoucherScreen(viewModel = voucherViewModel)
                    }
                    BottomTab.DASHBOARD -> {
                        DashboardScreen(viewModel = dashboardViewModel)
                    }
                    BottomTab.SETTINGS -> {
                        DiagnosticsScreen(
                            viewModel = diagnosticsViewModel,
                            isDarkModeEnabled = isDarkModeEnabled,
                            onDarkModeToggle = onDarkModeToggle
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceholderTabScreen(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AppTopBar(title = title, subtitle = "AL RABDI NET", startIcon = AppIcons.Info)

        // Content Area with Seamless Card
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WatermarkLineColor, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenHeader
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
