package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppFirewallRule
import com.example.data.model.CustomDomainRule
import com.example.data.model.NetworkLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetShieldDao {

    // --- Network Logs ---
    @Query("SELECT * FROM network_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentLogs(): Flow<List<NetworkLogEntity>>

    @Query("SELECT * FROM network_logs WHERE isBlocked = 1 ORDER BY timestamp DESC LIMIT 200")
    fun getBlockedThreatLogs(): Flow<List<NetworkLogEntity>>

    @Query("SELECT COUNT(*) FROM network_logs")
    fun getTotalLogsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM network_logs WHERE isBlocked = 1")
    fun getBlockedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: NetworkLogEntity): Long

    @Query("DELETE FROM network_logs")
    suspend fun clearAllLogs()

    @Query("SELECT threatCategory, COUNT(*) as count FROM network_logs WHERE isBlocked = 1 GROUP BY threatCategory")
    suspend fun getThreatCategoryBreakdown(): List<CategoryCountTuple>

    @Query("SELECT domain, COUNT(*) as count, threatCategory FROM network_logs WHERE isBlocked = 1 GROUP BY domain ORDER BY count DESC LIMIT 10")
    suspend fun getTopBlockedDomains(): List<TopDomainTuple>

    // --- App Firewall Rules ---
    @Query("SELECT * FROM app_firewall_rules ORDER BY appName ASC")
    fun getAllFirewallRules(): Flow<List<AppFirewallRule>>

    @Query("SELECT * FROM app_firewall_rules WHERE packageName = :pkg LIMIT 1")
    suspend fun getRuleForPackage(pkg: String): AppFirewallRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateFirewallRule(rule: AppFirewallRule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFirewallRules(rules: List<AppFirewallRule>)

    @Query("UPDATE app_firewall_rules SET blockCellular = :block WHERE isSystemApp = 0")
    suspend fun setAllUserAppsCellularBlocked(block: Boolean)

    @Query("UPDATE app_firewall_rules SET blockWifi = :block WHERE isSystemApp = 0")
    suspend fun setAllUserAppsWifiBlocked(block: Boolean)

    @Query("UPDATE app_firewall_rules SET blockCellular = 0, blockWifi = 0")
    suspend fun resetAllFirewallRules()

    // --- Custom Domain Rules ---
    @Query("SELECT * FROM custom_domain_rules ORDER BY addedAt DESC")
    fun getAllCustomDomainRules(): Flow<List<CustomDomainRule>>

    @Query("SELECT * FROM custom_domain_rules WHERE domain = :domain LIMIT 1")
    suspend fun getCustomDomainRule(domain: String): CustomDomainRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomDomainRule(rule: CustomDomainRule)

    @Query("DELETE FROM custom_domain_rules WHERE domain = :domain")
    suspend fun deleteCustomDomainRule(domain: String)
}

data class CategoryCountTuple(
    val threatCategory: String,
    val count: Int
)

data class TopDomainTuple(
    val domain: String,
    val count: Int,
    val threatCategory: String
)
