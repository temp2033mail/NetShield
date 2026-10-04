package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_firewall_rules")
data class AppFirewallRule(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val blockWifi: Boolean = false,
    val blockCellular: Boolean = false,
    val isSystemApp: Boolean = false,
    val uid: Int = 0,
    val totalBytesWifi: Long = 0L,
    val totalBytesCellular: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
)
