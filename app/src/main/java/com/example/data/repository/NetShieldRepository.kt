package com.example.data.repository

import android.content.Context
import com.example.data.db.NetShieldDao
import com.example.data.model.AppFirewallRule
import com.example.data.model.BlockedDomainStat
import com.example.data.model.CustomDomainRule
import com.example.data.model.NetworkLogEntity
import com.example.data.model.SecurityRecommendation
import com.example.data.model.SecurityReport
import com.example.data.model.ThreatCategory
import com.example.data.model.ThreatLevel
import com.example.threat.ThreatIntelligence
import com.example.tracker.InstalledAppItem
import com.example.tracker.InstalledAppsTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class NetShieldRepository(
    private val dao: NetShieldDao,
    private val context: Context
) {
    private val appsTracker = InstalledAppsTracker(context)

    // Flow of recent network connection logs
    val recentLogs: Flow<List<NetworkLogEntity>> = dao.getRecentLogs()

    // Flow of blocked malicious threats only
    val blockedThreats: Flow<List<NetworkLogEntity>> = dao.getBlockedThreatLogs()

    // Flow of all active app firewall rules
    val firewallRules: Flow<List<AppFirewallRule>> = dao.getAllFirewallRules()

    // Flow of custom domain rules
    val customDomainRules: Flow<List<CustomDomainRule>> = dao.getAllCustomDomainRules()

    suspend fun getInstalledApps(): List<InstalledAppItem> {
        val existingRulesMap = dao.getAllFirewallRules().first().associateBy { it.packageName }
        return appsTracker.getInstalledAppsWithRules(existingRulesMap)
    }

    suspend fun updateAppFirewallRule(
        packageName: String,
        appName: String,
        isSystemApp: Boolean,
        uid: Int,
        blockWifi: Boolean,
        blockCellular: Boolean
    ) {
        val rule = AppFirewallRule(
            packageName = packageName,
            appName = appName,
            isSystemApp = isSystemApp,
            uid = uid,
            blockWifi = blockWifi,
            blockCellular = blockCellular,
            lastUpdated = System.currentTimeMillis()
        )
        dao.insertOrUpdateFirewallRule(rule)
    }

    suspend fun setAllUserAppsCellularBlocked(block: Boolean) {
        dao.setAllUserAppsCellularBlocked(block)
    }

    suspend fun setAllUserAppsWifiBlocked(block: Boolean) {
        dao.setAllUserAppsWifiBlocked(block)
    }

    suspend fun resetAllFirewallRules() {
        dao.resetAllFirewallRules()
    }

    suspend fun addCustomDomainRule(domain: String, isBlocked: Boolean, category: String, notes: String) {
        dao.insertCustomDomainRule(
            CustomDomainRule(
                domain = domain.trim().lowercase(),
                isBlocked = isBlocked,
                category = category,
                notes = notes
            )
        )
    }

    suspend fun removeCustomDomainRule(domain: String) {
        dao.deleteCustomDomainRule(domain.trim().lowercase())
    }

    suspend fun logNetworkEvent(log: NetworkLogEntity): Long {
        return dao.insertLog(log)
    }

    suspend fun clearLogs() {
        dao.clearAllLogs()
    }

    /**
     * Seeds initial threat intelligence demonstration logs if empty so that the
     * dashboard and security reports immediately show rich, informative data.
     */
    suspend fun seedInitialLogsIfEmpty() {
        withContext(Dispatchers.IO) {
            val count = dao.getTotalLogsCount().first()
            if (count == 0) {
                val now = System.currentTimeMillis()
                val seedData = listOf(
                    NetworkLogEntity(
                        timestamp = now - 120000,
                        domain = "trojan-payload-dropper.biz",
                        ipAddress = "198.51.100.23",
                        appName = "Chrome Browser",
                        packageName = "com.android.chrome",
                        protocol = "DNS",
                        networkType = "WIFI",
                        bytesTransferred = 128,
                        isBlocked = true,
                        blockReason = "Known Trojan Dropper & C2 Infrastructure",
                        threatCategory = ThreatCategory.MALWARE.name,
                        threatLevel = ThreatLevel.CRITICAL.name
                    ),
                    NetworkLogEntity(
                        timestamp = now - 240000,
                        domain = "coinhive.com",
                        ipAddress = "104.244.42.1",
                        appName = "Social App",
                        packageName = "com.social.media",
                        protocol = "DNS",
                        networkType = "CELLULAR",
                        bytesTransferred = 64,
                        isBlocked = true,
                        blockReason = "In-browser Cryptomining JavaScript detected",
                        threatCategory = ThreatCategory.CRYPTOMINER.name,
                        threatLevel = ThreatLevel.HIGH.name
                    ),
                    NetworkLogEntity(
                        timestamp = now - 360000,
                        domain = "paypal-security-update.com",
                        ipAddress = "203.0.113.88",
                        appName = "Email Client",
                        packageName = "com.google.android.gm",
                        protocol = "DNS",
                        networkType = "CELLULAR",
                        bytesTransferred = 88,
                        isBlocked = true,
                        blockReason = "Deceptive Phishing link attempting credential theft",
                        threatCategory = ThreatCategory.PHISHING.name,
                        threatLevel = ThreatLevel.CRITICAL.name
                    ),
                    NetworkLogEntity(
                        timestamp = now - 480000,
                        domain = "telemetry-beacon.analytics-cloud.io",
                        ipAddress = "192.0.2.14",
                        appName = "Utility Tool",
                        packageName = "com.tool.utility",
                        protocol = "DNS",
                        networkType = "WIFI",
                        bytesTransferred = 256,
                        isBlocked = true,
                        blockReason = "Covert device telemetry and fingerprint exfiltration",
                        threatCategory = ThreatCategory.TELEMETRY.name,
                        threatLevel = ThreatLevel.MEDIUM.name
                    ),
                    NetworkLogEntity(
                        timestamp = now - 520000,
                        domain = "api.github.com",
                        ipAddress = "140.82.112.6",
                        appName = "Developer Tools",
                        packageName = "com.dev.app",
                        protocol = "HTTPS",
                        networkType = "WIFI",
                        bytesTransferred = 4096,
                        isBlocked = false,
                        blockReason = "Clean domain",
                        threatCategory = ThreatCategory.NONE.name,
                        threatLevel = ThreatLevel.SAFE.name
                    ),
                    NetworkLogEntity(
                        timestamp = now - 600000,
                        domain = "cloudflare-dns.com",
                        ipAddress = "1.1.1.1",
                        appName = "System DNS",
                        packageName = "android",
                        protocol = "DNS",
                        networkType = "WIFI",
                        bytesTransferred = 512,
                        isBlocked = false,
                        blockReason = "Clean domain",
                        threatCategory = ThreatCategory.NONE.name,
                        threatLevel = ThreatLevel.SAFE.name
                    )
                )

                for (log in seedData) {
                    dao.insertLog(log)
                }

                // Also seed a default custom rule
                dao.insertCustomDomainRule(
                    CustomDomainRule(
                        domain = "ad-click-fraud.net",
                        isBlocked = true,
                        category = "Invasive Adware",
                        notes = "Known clickjacking farm"
                    )
                )
            }
        }
    }

    /**
     * Dynamically compiles a rich SecurityReport based on DB metrics, threat levels,
     * and firewall configuration.
     */
    suspend fun generateSecurityReport(isVpnRunning: Boolean): SecurityReport {
        return withContext(Dispatchers.IO) {
            val categoryTuples = dao.getThreatCategoryBreakdown()
            val categoryCounts = categoryTuples.associate { it.threatCategory to it.count }

            val topDomainsTuples = dao.getTopBlockedDomains()
            val topDomains = topDomainsTuples.map {
                BlockedDomainStat(domain = it.domain, count = it.count, category = it.threatCategory)
            }

            val rules = dao.getAllFirewallRules().first()
            val cellularBlockedCount = rules.count { it.blockCellular }
            val wifiBlockedCount = rules.count { it.blockWifi }

            val totalThreats = categoryCounts.values.sum().toLong()
            val trackerCount = (categoryCounts[ThreatCategory.ADWARE_TRACKER.name] ?: 0) +
                    (categoryCounts[ThreatCategory.TELEMETRY.name] ?: 0)

            val totalLogs = dao.getTotalLogsCount().first().toLong()
            // Estimate data saved by blocking malicious payloads (~250 KB per blocked connection)
            val estimatedDataSaved = totalThreats * 256 * 1024L

            // Calculate Security Health Score
            var score = 70
            if (isVpnRunning) score += 15
            if (cellularBlockedCount > 0) score += 5
            if (totalThreats > 0) score += 5
            if (rules.isNotEmpty()) score += 5
            score = score.coerceIn(40, 99)

            val grade = when {
                score >= 95 -> "A+"
                score >= 88 -> "A"
                score >= 78 -> "B"
                score >= 65 -> "C"
                else -> "D"
            }

            val recommendations = mutableListOf<SecurityRecommendation>()

            if (!isVpnRunning) {
                recommendations.add(
                    SecurityRecommendation(
                        id = "vpn_offline",
                        title = "Protection Shield is Inactive",
                        description = "Enable NetShield Sentinel to actively block malicious domains and cryptominers in real-time.",
                        severity = ThreatLevel.CRITICAL,
                        actionText = "Turn On Shield",
                        isResolved = false
                    )
                )
            } else {
                recommendations.add(
                    SecurityRecommendation(
                        id = "vpn_online",
                        title = "DNS Sentinel Active",
                        description = "All DNS queries are routed through threat filtering and malicious hosts are blackholed.",
                        severity = ThreatLevel.SAFE,
                        actionText = "Protected",
                        isResolved = true
                    )
                )
            }

            if (cellularBlockedCount == 0) {
                recommendations.add(
                    SecurityRecommendation(
                        id = "cellular_saver",
                        title = "All Apps Have Full Cellular Access",
                        description = "Review the App Firewall to prevent background data drain on mobile cellular connections.",
                        severity = ThreatLevel.MEDIUM,
                        actionText = "Review Firewall",
                        isResolved = false
                    )
                )
            } else {
                recommendations.add(
                    SecurityRecommendation(
                        id = "cellular_protected",
                        title = "Cellular Firewall Guarding Data",
                        description = "$cellularBlockedCount apps are restricted from consuming cellular data.",
                        severity = ThreatLevel.SAFE,
                        actionText = "Configured",
                        isResolved = true
                    )
                )
            }

            if (totalThreats > 0) {
                recommendations.add(
                    SecurityRecommendation(
                        id = "threats_intercepted",
                        title = "$totalThreats Malicious Requests Intercepted",
                        description = "Phishing links and malware C2 beacons were prevented from connecting to your device.",
                        severity = ThreatLevel.HIGH,
                        actionText = "View Log",
                        isResolved = true
                    )
                )
            }

            SecurityReport(
                securityScore = score,
                securityGrade = grade,
                totalQueriesInspected = totalLogs,
                totalThreatsBlocked = totalThreats,
                totalTrackersBlocked = trackerCount.toLong(),
                estimatedDataSavedBytes = estimatedDataSaved,
                threatCategoryCounts = categoryCounts,
                topBlockedDomains = topDomains,
                activeFirewallRulesCount = rules.size,
                blockedAppsCellularCount = cellularBlockedCount,
                blockedAppsWifiCount = wifiBlockedCount,
                recommendations = recommendations
            )
        }
    }

    /**
     * Executes a simulated attack / test domain query to verify blocking in real-time.
     */
    suspend fun simulateThreatTest(domain: String) {
        val check = ThreatIntelligence.checkDomain(domain)
        val now = System.currentTimeMillis()
        val log = NetworkLogEntity(
            timestamp = now,
            domain = domain,
            ipAddress = if (check.isThreat) "0.0.0.0 (Intercepted)" else "93.184.216.34",
            appName = "Security Test Tool",
            packageName = context.packageName,
            protocol = "DNS",
            networkType = "WIFI",
            bytesTransferred = 142L,
            isBlocked = check.isThreat,
            blockReason = if (check.isThreat) check.reason else "Passed verified domain check",
            threatCategory = check.category.name,
            threatLevel = check.level.name
        )
        dao.insertLog(log)
    }
}
