package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_domain_rules")
data class CustomDomainRule(
    @PrimaryKey
    val domain: String,
    val isBlocked: Boolean = true, // true = Blacklist, false = Whitelist
    val category: String = "Custom Filter",
    val notes: String = "",
    val addedAt: Long = System.currentTimeMillis()
)
