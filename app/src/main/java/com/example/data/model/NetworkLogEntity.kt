package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_logs")
data class NetworkLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val domain: String,
    val ipAddress: String = "",
    val appName: String = "System / Network",
    val packageName: String = "android",
    val protocol: String = "DNS",
    val networkType: String = "WIFI", // "WIFI" or "CELLULAR"
    val bytesTransferred: Long = 64L,
    val isBlocked: Boolean = false,
    val blockReason: String = "",
    val threatCategory: String = ThreatCategory.NONE.name,
    val threatLevel: String = ThreatLevel.SAFE.name
)
