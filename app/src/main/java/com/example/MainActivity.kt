package com.example

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.NetShieldViewModel
import com.example.ui.ScreenTab
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FirewallScreen
import com.example.ui.screens.SecurityReportsScreen
import com.example.ui.screens.TrafficScreen
import com.example.ui.theme.CyberBackgroundDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.NetShieldTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    private val viewModel: NetShieldViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NetShieldTheme(darkTheme = true) {
                // VPN Permission Launcher
                val vpnLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        viewModel.startVpnService()
                        Toast.makeText(this, "NetShield Sentinel Activated", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "VPN permission needed to block threats", Toast.LENGTH_SHORT).show()
                    }
                }

                // Notification Permission for Android 13+
                val notificationLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Ignored, optional */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val onToggleProtection = {
                    viewModel.toggleProtection {
                        val prepareIntent = VpnService.prepare(this)
                        if (prepareIntent != null) {
                            vpnLauncher.launch(prepareIntent)
                        } else {
                            viewModel.startVpnService()
                            Toast.makeText(this, "NetShield Sentinel Activated", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                NetShieldApp(
                    viewModel = viewModel,
                    onToggleProtection = onToggleProtection
                )
            }
        }
    }
}

@Composable
fun NetShieldApp(
    viewModel: NetShieldViewModel,
    onToggleProtection: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val customRules by viewModel.customRules.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav"),
                containerColor = CyberSurfaceDark,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = uiState.currentTab == ScreenTab.DASHBOARD,
                    onClick = { viewModel.selectTab(ScreenTab.DASHBOARD) },
                    icon = { Icon(Icons.Default.Shield, contentDescription = "Dashboard") },
                    label = { Text(ScreenTab.DASHBOARD.title, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = CyberCyan.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == ScreenTab.TRAFFIC,
                    onClick = { viewModel.selectTab(ScreenTab.TRAFFIC) },
                    icon = { Icon(Icons.Default.Language, contentDescription = "Traffic") },
                    label = { Text(ScreenTab.TRAFFIC.title, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = CyberCyan.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_traffic")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == ScreenTab.FIREWALL,
                    onClick = { viewModel.selectTab(ScreenTab.FIREWALL) },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Firewall") },
                    label = { Text(ScreenTab.FIREWALL.title, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = CyberCyan.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_firewall")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == ScreenTab.SECURITY,
                    onClick = { viewModel.selectTab(ScreenTab.SECURITY) },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                    label = { Text(ScreenTab.SECURITY.title, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = CyberCyan.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_security")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBackgroundDark)
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                ScreenTab.DASHBOARD -> DashboardScreen(
                    uiState = uiState,
                    onToggleProtection = onToggleProtection,
                    onSimulateBlock = { domain -> viewModel.simulateThreatEvent(domain) },
                    onNavigateToFirewall = { viewModel.selectTab(ScreenTab.FIREWALL) }
                )
                ScreenTab.TRAFFIC -> TrafficScreen(
                    logs = recentLogs,
                    currentFilter = uiState.logFilter,
                    searchQuery = uiState.logSearchQuery,
                    onFilterChange = { viewModel.setLogFilter(it) },
                    onSearchChange = { viewModel.setLogSearchQuery(it) },
                    onClearLogs = { viewModel.clearAllLogs() },
                    onSimulateBlock = { domain -> viewModel.simulateThreatEvent(domain) }
                )
                ScreenTab.FIREWALL -> FirewallScreen(
                    apps = installedApps,
                    isLoading = uiState.isLoadingApps,
                    currentFilter = uiState.firewallFilter,
                    searchQuery = uiState.firewallSearchQuery,
                    onFilterChange = { viewModel.setFirewallFilter(it) },
                    onSearchChange = { viewModel.setFirewallSearchQuery(it) },
                    onToggleWifi = { app -> viewModel.toggleAppWifi(app) },
                    onToggleCellular = { app -> viewModel.toggleAppCellular(app) },
                    onBlockAllCellular = { viewModel.blockAllUserAppsCellular() },
                    onAllowAllCellular = { viewModel.allowAllUserAppsCellular() },
                    onResetAll = { viewModel.resetAllFirewallRules() },
                    onRefresh = { viewModel.refreshAppsList() }
                )
                ScreenTab.SECURITY -> SecurityReportsScreen(
                    report = uiState.securityReport,
                    customRules = customRules,
                    isVpnActive = uiState.isVpnActive,
                    onAddCustomRule = { domain, isBlocked, category, notes ->
                        viewModel.addCustomRule(domain, isBlocked, category, notes)
                    },
                    onRemoveCustomRule = { domain ->
                        viewModel.removeCustomRule(domain)
                    },
                    onSimulateBlock = { domain -> viewModel.simulateThreatEvent(domain) }
                )
            }
        }
    }
}
