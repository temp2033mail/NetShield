package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.NetShieldDatabase
import com.example.data.model.CustomDomainRule
import com.example.data.model.NetworkLogEntity
import com.example.data.model.RealtimeSpeed
import com.example.data.model.SecurityReport
import com.example.data.repository.NetShieldRepository
import com.example.threat.ThreatCheckResult
import com.example.threat.ThreatIntelligence
import com.example.tracker.InstalledAppItem
import com.example.tracker.NetworkSpeedTracker
import com.example.vpn.NetShieldVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenTab(val title: String) {
    DASHBOARD("Dashboard"),
    TRAFFIC("Traffic"),
    FIREWALL("Firewall"),
    SECURITY("Security")
}

enum class LogFilter(val label: String) {
    ALL("All Requests"),
    BLOCKED("Blocked Threats"),
    ALLOWED("Allowed Clean")
}

enum class FirewallFilter(val label: String) {
    ALL("All Apps"),
    USER_ONLY("User Apps"),
    CELLULAR_BLOCKED("Cellular Blocked"),
    WIFI_BLOCKED("Wi-Fi Blocked"),
    SYSTEM_ONLY("System Apps")
}

data class UiState(
    val currentTab: ScreenTab = ScreenTab.DASHBOARD,
    val isVpnActive: Boolean = false,
    val speed: RealtimeSpeed = RealtimeSpeed(),
    val downloadHistory: List<Float> = emptyList(),
    val uploadHistory: List<Float> = emptyList(),
    val securityReport: SecurityReport = SecurityReport(),
    val logFilter: LogFilter = LogFilter.ALL,
    val logSearchQuery: String = "",
    val firewallFilter: FirewallFilter = FirewallFilter.ALL,
    val firewallSearchQuery: String = "",
    val isLoadingApps: Boolean = false,
    val lastTestResult: ThreatCheckResult? = null,
    val domainCheckInput: String = ""
)

class NetShieldViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NetShieldDatabase.getInstance(application)
    private val repository = NetShieldRepository(database.netShieldDao(), application)
    private val speedTracker = NetworkSpeedTracker(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val recentLogs: StateFlow<List<NetworkLogEntity>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customRules: StateFlow<List<CustomDomainRule>> = repository.customDomainRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    init {
        // Start real-time network throughput tracker
        speedTracker.startTracking(viewModelScope)

        // Listen to speed tracker changes
        viewModelScope.launch {
            speedTracker.speedState.collect { speed ->
                _uiState.value = _uiState.value.copy(speed = speed)
            }
        }
        viewModelScope.launch {
            speedTracker.downloadHistory.collect { history ->
                _uiState.value = _uiState.value.copy(downloadHistory = history)
            }
        }
        viewModelScope.launch {
            speedTracker.uploadHistory.collect { history ->
                _uiState.value = _uiState.value.copy(uploadHistory = history)
            }
        }

        // Listen to VPN status
        viewModelScope.launch {
            NetShieldVpnService.isVpnActive.collect { active ->
                _uiState.value = _uiState.value.copy(isVpnActive = active)
                refreshSecurityReport()
            }
        }

        // Seed rich initial data for first-time launch so charts and threat reports are populated
        viewModelScope.launch {
            repository.seedInitialLogsIfEmpty()
            refreshSecurityReport()
            refreshAppsList()
        }
    }

    fun selectTab(tab: ScreenTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
        if (tab == ScreenTab.FIREWALL && _installedApps.value.isEmpty()) {
            refreshAppsList()
        }
        if (tab == ScreenTab.SECURITY) {
            refreshSecurityReport()
        }
    }

    fun setLogFilter(filter: LogFilter) {
        _uiState.value = _uiState.value.copy(logFilter = filter)
    }

    fun setLogSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(logSearchQuery = query)
    }

    fun setFirewallFilter(filter: FirewallFilter) {
        _uiState.value = _uiState.value.copy(firewallFilter = filter)
    }

    fun setFirewallSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(firewallSearchQuery = query)
    }

    fun refreshAppsList() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingApps = true)
            val apps = repository.getInstalledApps()
            _installedApps.value = apps
            _uiState.value = _uiState.value.copy(isLoadingApps = false)
            refreshSecurityReport()
        }
    }

    fun toggleAppWifi(app: InstalledAppItem) {
        viewModelScope.launch {
            val newBlockWifi = !app.blockWifi
            repository.updateAppFirewallRule(
                packageName = app.packageName,
                appName = app.appName,
                isSystemApp = app.isSystemApp,
                uid = app.uid,
                blockWifi = newBlockWifi,
                blockCellular = app.blockCellular
            )
            // Update local memory list for immediate UI responsiveness
            _installedApps.value = _installedApps.value.map {
                if (it.packageName == app.packageName) it.copy(blockWifi = newBlockWifi) else it
            }
            refreshSecurityReport()
        }
    }

    fun toggleAppCellular(app: InstalledAppItem) {
        viewModelScope.launch {
            val newBlockCellular = !app.blockCellular
            repository.updateAppFirewallRule(
                packageName = app.packageName,
                appName = app.appName,
                isSystemApp = app.isSystemApp,
                uid = app.uid,
                blockWifi = app.blockWifi,
                blockCellular = newBlockCellular
            )
            _installedApps.value = _installedApps.value.map {
                if (it.packageName == app.packageName) it.copy(blockCellular = newBlockCellular) else it
            }
            refreshSecurityReport()
        }
    }

    fun blockAllUserAppsCellular() {
        viewModelScope.launch {
            repository.setAllUserAppsCellularBlocked(true)
            refreshAppsList()
        }
    }

    fun allowAllUserAppsCellular() {
        viewModelScope.launch {
            repository.setAllUserAppsCellularBlocked(false)
            refreshAppsList()
        }
    }

    fun resetAllFirewallRules() {
        viewModelScope.launch {
            repository.resetAllFirewallRules()
            refreshAppsList()
        }
    }

    fun refreshSecurityReport() {
        viewModelScope.launch {
            val report = repository.generateSecurityReport(_uiState.value.isVpnActive)
            _uiState.value = _uiState.value.copy(securityReport = report)
        }
    }

    fun testDomainSecurity(domain: String) {
        val result = ThreatIntelligence.checkDomain(domain)
        _uiState.value = _uiState.value.copy(
            lastTestResult = result,
            domainCheckInput = domain
        )
    }

    fun simulateThreatEvent(domain: String) {
        viewModelScope.launch {
            repository.simulateThreatTest(domain)
            refreshSecurityReport()
        }
    }

    fun addCustomRule(domain: String, isBlocked: Boolean, category: String, notes: String) {
        viewModelScope.launch {
            repository.addCustomDomainRule(domain, isBlocked, category, notes)
            refreshSecurityReport()
        }
    }

    fun removeCustomRule(domain: String) {
        viewModelScope.launch {
            repository.removeCustomDomainRule(domain)
            refreshSecurityReport()
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            refreshSecurityReport()
        }
    }

    fun toggleProtection(onVpnPermissionNeeded: () -> Unit) {
        val context = getApplication<Application>()
        if (_uiState.value.isVpnActive) {
            NetShieldVpnService.stopService(context)
        } else {
            onVpnPermissionNeeded()
        }
    }

    fun startVpnService() {
        val context = getApplication<Application>()
        NetShieldVpnService.startService(context)
    }
}
