package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.tracker.InstalledAppItem
import com.example.ui.FirewallFilter
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.CyberBackgroundDark
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberElectric
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThreatRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FirewallScreen(
    apps: List<InstalledAppItem>,
    isLoading: Boolean,
    currentFilter: FirewallFilter,
    searchQuery: String,
    onFilterChange: (FirewallFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleWifi: (InstalledAppItem) -> Unit,
    onToggleCellular: (InstalledAppItem) -> Unit,
    onBlockAllCellular: () -> Unit,
    onAllowAllCellular: () -> Unit,
    onResetAll: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cellularBlockedCount = remember(apps) { apps.count { it.blockCellular } }
    val wifiBlockedCount = remember(apps) { apps.count { it.blockWifi } }

    val filteredApps = remember(apps, currentFilter, searchQuery) {
        apps.filter { app ->
            val matchesFilter = when (currentFilter) {
                FirewallFilter.ALL -> true
                FirewallFilter.USER_ONLY -> !app.isSystemApp
                FirewallFilter.CELLULAR_BLOCKED -> app.blockCellular
                FirewallFilter.WIFI_BLOCKED -> app.blockWifi
                FirewallFilter.SYSTEM_ONLY -> app.isSystemApp
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                app.appName.contains(searchQuery, ignoreCase = true) ||
                        app.packageName.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackgroundDark)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Firewall Summary Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "App Network Firewall",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Control cellular and Wi-Fi access per application",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("refresh_apps_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh apps",
                    tint = CyberCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Metrics Banner: Cellular Blocked vs Wi-Fi Blocked
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurfaceDark)
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AlertOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CellTower,
                            contentDescription = null,
                            tint = AlertOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "$cellularBlockedCount Apps",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (cellularBlockedCount > 0) ThreatRed else TextPrimary
                        )
                        Text(
                            text = "Cellular Blocked",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurfaceDark)
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CyberElectric.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = CyberElectric,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "$wifiBlockedCount Apps",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (wifiBlockedCount > 0) ThreatRed else TextPrimary
                        )
                        Text(
                            text = "Wi-Fi Blocked",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Batch Operations
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onBlockAllCellular,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("block_all_cellular_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Block All Cellular", fontSize = 11.sp, color = AlertOrange)
            }
            Button(
                onClick = onAllowAllCellular,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("allow_all_cellular_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Allow All Data", fontSize = 11.sp, color = ShieldGreen)
            }
            Button(
                onClick = onResetAll,
                modifier = Modifier
                    .height(38.dp)
                    .testTag("reset_firewall_rules_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search installed application...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("firewall_search_input"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = CyberCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = CyberCyan,
                focusedContainerColor = CyberSurfaceDark,
                unfocusedContainerColor = CyberSurfaceDark
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filters FlowRow
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FirewallFilter.entries.forEach { filter ->
                FilterChip(
                    selected = currentFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                        selectedLabelColor = CyberCyan
                    ),
                    modifier = Modifier.testTag("firewall_filter_${filter.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyberCyan)
            }
        } else if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No applications found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppFirewallRow(
                        app = app,
                        onToggleWifi = { onToggleWifi(app) },
                        onToggleCellular = { onToggleCellular(app) }
                    )
                }
            }
        }
    }
}

@Composable
fun AppFirewallRow(
    app: InstalledAppItem,
    onToggleWifi: () -> Unit,
    onToggleCellular: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .testTag("app_row_${app.packageName}"),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (app.blockCellular || app.blockWifi) AlertOrange.copy(alpha = 0.35f) else CyberCardBorder
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                val bmp = remember(app.icon) {
                    try {
                        app.icon?.toBitmap(96, 96, Bitmap.Config.ARGB_8888)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = app.appName,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Text(
                        text = app.appName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CyberCyan
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // App Label & Package
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = if (app.totalBytes > 0) formatBytes(app.totalBytes) + " consumed" else app.packageName,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextSecondary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Cellular Toggle Button (Red = Blocked, Grey/Green = Allowed)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (app.blockCellular) ThreatRed.copy(alpha = 0.2f) else CyberSurfaceElevated
                    )
                    .border(
                        1.dp,
                        if (app.blockCellular) ThreatRed else CyberCardBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onToggleCellular)
                    .testTag("toggle_cellular_${app.packageName}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CellTower,
                    contentDescription = if (app.blockCellular) "Cellular Blocked" else "Cellular Allowed",
                    tint = if (app.blockCellular) ThreatRed else AlertOrange,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Wi-Fi Toggle Button (Red = Blocked, Grey/Cyan = Allowed)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (app.blockWifi) ThreatRed.copy(alpha = 0.2f) else CyberSurfaceElevated
                    )
                    .border(
                        1.dp,
                        if (app.blockWifi) ThreatRed else CyberCardBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onToggleWifi)
                    .testTag("toggle_wifi_${app.packageName}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = if (app.blockWifi) "Wi-Fi Blocked" else "Wi-Fi Allowed",
                    tint = if (app.blockWifi) ThreatRed else CyberElectric,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
