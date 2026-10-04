package com.example.tracker

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.TrafficStats
import com.example.data.model.AppFirewallRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val uid: Int,
    val blockWifi: Boolean,
    val blockCellular: Boolean,
    val totalBytes: Long,
    val icon: Drawable? = null
)

class InstalledAppsTracker(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    suspend fun getInstalledAppsWithRules(existingRules: Map<String, AppFirewallRule>): List<InstalledAppItem> {
        return withContext(Dispatchers.IO) {
            val appsList = mutableListOf<InstalledAppItem>()

            try {
                val installedPackages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

                for (appInfo in installedPackages) {
                    // Filter out this app from being locked out
                    if (appInfo.packageName == context.packageName) continue

                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val appName = try {
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        appInfo.packageName
                    }

                    val icon = try {
                        packageManager.getApplicationIcon(appInfo)
                    } catch (e: Exception) {
                        null
                    }

                    val uid = appInfo.uid
                    val rxBytes = TrafficStats.getUidRxBytes(uid)
                    val txBytes = TrafficStats.getUidTxBytes(uid)
                    val totalTraffic = (if (rxBytes != TrafficStats.UNSUPPORTED.toLong()) rxBytes else 0L) +
                            (if (txBytes != TrafficStats.UNSUPPORTED.toLong()) txBytes else 0L)

                    val existingRule = existingRules[appInfo.packageName]

                    appsList.add(
                        InstalledAppItem(
                            packageName = appInfo.packageName,
                            appName = appName,
                            isSystemApp = isSystem,
                            uid = uid,
                            blockWifi = existingRule?.blockWifi ?: false,
                            blockCellular = existingRule?.blockCellular ?: false,
                            totalBytes = totalTraffic,
                            icon = icon
                        )
                    )
                }

                // Sort: non-system apps first, then by highest data consumption, then alphabetical
                appsList.sortWith(
                    compareBy<InstalledAppItem> { it.isSystemApp }
                        .thenByDescending { it.totalBytes }
                        .thenBy { it.appName.lowercase() }
                )
            } catch (e: Exception) {
                // Fallback safe return
            }

            appsList
        }
    }
}
