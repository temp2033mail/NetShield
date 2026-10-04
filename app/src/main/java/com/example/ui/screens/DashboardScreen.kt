package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.UiState
import com.example.ui.components.DomainCheckDialog
import com.example.ui.components.RadarShield
import com.example.ui.components.SpeedSparkline
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

@Composable
fun DashboardScreen(
    uiState: UiState,
    onToggleProtection: () -> Unit,
    onSimulateBlock: (String) -> Unit,
    onNavigateToFirewall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showInspectorDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NETSHIELD",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = CyberCyan
                )
                Text(
                    text = "Network Traffic & Threat Defense",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Network Type Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberSurfaceElevated)
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.speed.currentNetworkType == "WIFI") Icons.Default.Wifi else Icons.Default.CellTower,
                        contentDescription = null,
                        tint = if (uiState.speed.currentNetworkType == "WIFI") CyberElectric else AlertOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.speed.currentNetworkType,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Shield Radar Hero Component
        RadarShield(
            isActive = uiState.isVpnActive,
            onToggle = onToggleProtection
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Real-Time Bandwidth Speedometer & Live Throughput Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CyberCardBorder, CyberCyan.copy(alpha = 0.3f))))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Real-Time Throughput",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = if (uiState.isVpnActive) "LIVE MONITORING" else "NETWORK ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (uiState.isVpnActive) ShieldGreen else TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Download & Upload Speed Gauges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Download Gauge
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurfaceElevated)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Download",
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DOWNLOAD",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp),
                                color = TextSecondary
                            )
                            Text(
                                text = formatSpeed(uiState.speed.downloadSpeedBps),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Upload Gauge
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurfaceElevated)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyberElectric.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Upload",
                                tint = CyberElectric,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "UPLOAD",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp),
                                color = TextSecondary
                            )
                            Text(
                                text = formatSpeed(uiState.speed.uploadSpeedBps),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live animated throughput wave/chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberBackgroundDark.copy(alpha = 0.6f))
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    SpeedSparkline(
                        dataPoints = uiState.downloadHistory,
                        lineColor = CyberCyan,
                        gradientStartColor = CyberCyan.copy(alpha = 0.25f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Wi-Fi vs Cellular breakdown bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🛜 Wi-Fi: ${formatBytes(uiState.speed.totalWifiBytes)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = CyberElectric
                    )
                    Text(
                        text = "📶 Cellular: ${formatBytes(uiState.speed.totalCellularBytes)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = AlertOrange
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Threat Protection Summary Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Threats Blocked",
                value = uiState.securityReport.totalThreatsBlocked.toString(),
                subtitle = "Phishing & C2",
                icon = Icons.Default.Block,
                iconTint = ThreatRed,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Trackers Deflected",
                value = uiState.securityReport.totalTrackersBlocked.toString(),
                subtitle = "Spyware & Ads",
                icon = Icons.Default.Visibility,
                iconTint = AlertOrange,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Data Saved",
                value = formatBytes(uiState.securityReport.estimatedDataSavedBytes),
                subtitle = "Payload Filter",
                icon = Icons.Default.DataUsage,
                iconTint = ShieldGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Security Posture Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CyberCardBorder, CyberCyan.copy(alpha = 0.2f))))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score Dial
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (uiState.securityReport.securityScore >= 85) ShieldGreen.copy(alpha = 0.15f) else AlertOrange.copy(alpha = 0.15f)
                        )
                        .border(
                            2.dp,
                            if (uiState.securityReport.securityScore >= 85) ShieldGreen else AlertOrange,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.securityReport.securityScore}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = if (uiState.securityReport.securityScore >= 85) ShieldGreen else AlertOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Security Posture: Grade ${uiState.securityReport.securityGrade}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = if (uiState.isVpnActive)
                            "Sentinel loopback active. Malicious domains and covert beacons filtered."
                        else
                            "Shield disabled. Real-time blocking inactive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Actions Row: Test Threat Interception & Firewall Quick Access
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showInspectorDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("test_threat_scan_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(CyberCardBorder, CyberCyan)))
            ) {
                Icon(Icons.Default.BugReport, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Test Domain Threat", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
            }

            Button(
                onClick = onNavigateToFirewall,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("quick_firewall_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF061E1A), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("App Firewall", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF061E1A))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showInspectorDialog) {
        DomainCheckDialog(
            initialDomain = "malware-test-beacon.org",
            onDismiss = { showInspectorDialog = false },
            onSimulateBlock = onSimulateBlock
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CyberCardBorder, iconTint.copy(alpha = 0.25f))))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = TextPrimary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextSecondary,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}

fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
        bytesPerSec >= 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
        else -> "$bytesPerSec B/s"
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
