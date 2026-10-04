package com.example.data.model

data class RealtimeSpeed(
    val downloadSpeedBps: Long = 0L,  // bytes per second
    val uploadSpeedBps: Long = 0L,    // bytes per second
    val currentNetworkType: String = "WIFI", // "WIFI", "CELLULAR", "NONE"
    val isVpnActive: Boolean = false,
    val totalRxSessionBytes: Long = 0L,
    val totalTxSessionBytes: Long = 0L,
    val totalWifiBytes: Long = 0L,
    val totalCellularBytes: Long = 0L
)

data class SecurityReport(
    val securityScore: Int = 92, // 0 - 100
    val securityGrade: String = "A", // A+, A, B, C, F
    val totalQueriesInspected: Long = 0L,
    val totalThreatsBlocked: Long = 0L,
    val totalTrackersBlocked: Long = 0L,
    val estimatedDataSavedBytes: Long = 0L,
    val threatCategoryCounts: Map<String, Int> = emptyMap(),
    val topBlockedDomains: List<BlockedDomainStat> = emptyList(),
    val activeFirewallRulesCount: Int = 0,
    val blockedAppsCellularCount: Int = 0,
    val blockedAppsWifiCount: Int = 0,
    val recommendations: List<SecurityRecommendation> = emptyList()
)

data class BlockedDomainStat(
    val domain: String,
    val count: Int,
    val category: String
)

data class SecurityRecommendation(
    val id: String,
    val title: String,
    val description: String,
    val severity: ThreatLevel,
    val actionText: String = "",
    val isResolved: Boolean = false
)

data class AppTrafficInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val blockWifi: Boolean,
    val blockCellular: Boolean,
    val wifiBytes: Long,
    val cellularBytes: Long,
    val totalBytes: Long
)
