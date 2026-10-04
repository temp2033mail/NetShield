package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomDomainRule
import com.example.data.model.SecurityRecommendation
import com.example.data.model.SecurityReport
import com.example.data.model.ThreatCategory
import com.example.data.model.ThreatLevel
import com.example.ui.components.DomainCheckDialog
import com.example.ui.components.RuleAddDialog
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
fun SecurityReportsScreen(
    report: SecurityReport,
    customRules: List<CustomDomainRule>,
    isVpnActive: Boolean,
    onAddCustomRule: (domain: String, isBlocked: Boolean, category: String, notes: String) -> Unit,
    onRemoveCustomRule: (domain: String) -> Unit,
    onSimulateBlock: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showInspectorDialog by remember { mutableStateOf(false) }
    var showAddRuleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Security Audit & Reports",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Comprehensive threat report & posture score",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = {
                    val shareText = buildString {
                        appendLine("🛡️ NetShield Security Audit Report")
                        appendLine("Security Score: ${report.securityScore}/100 (Grade ${report.securityGrade})")
                        appendLine("Threats Blocked: ${report.totalThreatsBlocked}")
                        appendLine("Trackers Intercepted: ${report.totalTrackersBlocked}")
                        appendLine("Protected Queries Scanned: ${report.totalQueriesInspected}")
                        appendLine("Estimated Data Saved: ${formatBytes(report.estimatedDataSavedBytes)}")
                        appendLine("Cellular Firewall Protected Apps: ${report.blockedAppsCellularCount}")
                    }
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Security Report"))
                },
                modifier = Modifier.testTag("share_report_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Report",
                    tint = CyberCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Security Health Score Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(CyberCardBorder, CyberCyan.copy(alpha = 0.35f)))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Big Circular Health Dial
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        if (report.securityScore >= 85) ShieldGreen.copy(alpha = 0.25f) else AlertOrange.copy(alpha = 0.25f),
                                        CyberSurfaceElevated
                                    )
                                )
                            )
                            .border(
                                3.dp,
                                if (report.securityScore >= 85) ShieldGreen else AlertOrange,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${report.securityScore}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp
                                ),
                                color = if (report.securityScore >= 85) ShieldGreen else AlertOrange
                            )
                            Text(
                                text = "GRADE ${report.securityGrade}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Overall Security Posture",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isVpnActive)
                                "Real-time threat interception is active. Malicious domains, cryptominers, and adware are blocked."
                            else
                                "Protection shield paused. Activate Sentinel to enforce DNS blocking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Posture Sub-factors
                PostureFactorRow(title = "Phishing & Fraud Blocker", progress = 0.95f, color = ThreatRed)
                Spacer(modifier = Modifier.height(6.dp))
                PostureFactorRow(title = "Cryptojacking Defense", progress = 0.90f, color = AlertOrange)
                Spacer(modifier = Modifier.height(6.dp))
                PostureFactorRow(title = "Telemetry & Adware Filter", progress = 0.85f, color = CyberElectric)
                Spacer(modifier = Modifier.height(6.dp))
                PostureFactorRow(
                    title = "App Cellular Firewall",
                    progress = if (report.blockedAppsCellularCount > 0) 0.90f else 0.50f,
                    color = ShieldGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Threat Distribution Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(CyberCardBorder, CyberCyan.copy(alpha = 0.2f)))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = ThreatRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Threat Categories Blocked",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                ThreatCategoryItem(
                    title = "Malware & Ransomware C2",
                    count = report.threatCategoryCounts[ThreatCategory.MALWARE.name] ?: 0,
                    color = ThreatRed
                )
                ThreatCategoryItem(
                    title = "Phishing & Credential Theft",
                    count = report.threatCategoryCounts[ThreatCategory.PHISHING.name] ?: 0,
                    color = AlertOrange
                )
                ThreatCategoryItem(
                    title = "Cryptomining Scripts",
                    count = report.threatCategoryCounts[ThreatCategory.CRYPTOMINER.name] ?: 0,
                    color = CyberCyan
                )
                ThreatCategoryItem(
                    title = "Invasive Adware & Redirects",
                    count = report.threatCategoryCounts[ThreatCategory.ADWARE_TRACKER.name] ?: 0,
                    color = CyberElectric
                )
                ThreatCategoryItem(
                    title = "Covert Telemetry & Spyware",
                    count = report.threatCategoryCounts[ThreatCategory.TELEMETRY.name] ?: 0,
                    color = Color(0xFFB388FF)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Top Blocked Domains List
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(CyberCardBorder, CyberCyan.copy(alpha = 0.2f)))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = ThreatRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Top Intercepted Domains",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (report.topBlockedDomains.isEmpty()) {
                    Text(
                        text = "No malicious domains intercepted yet. Your network is currently clean.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    report.topBlockedDomains.take(5).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.domain,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = TextMuted
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ThreatRed.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${item.count} blocks",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ThreatRed
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Security Recommendations
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(CyberCardBorder, ShieldGreen.copy(alpha = 0.2f)))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Policy, contentDescription = null, tint = ShieldGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Security Audit Findings",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                report.recommendations.forEach { rec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (rec.isResolved) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (rec.isResolved) ShieldGreen else AlertOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rec.title,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = rec.description,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Custom Domain Blacklist / Whitelist Management
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(CyberCardBorder, CyberCyan.copy(alpha = 0.2f)))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Custom Rules (${customRules.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    Button(
                        onClick = { showAddRuleDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_custom_rule_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 11.sp, color = CyberCyan)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (customRules.isEmpty()) {
                    Text(
                        text = "No custom rules configured yet. You can add specific domains to always block or allow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    customRules.forEach { rule ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (rule.isBlocked) ThreatRed.copy(alpha = 0.2f) else ShieldGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (rule.isBlocked) "BLOCK" else "ALLOW",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (rule.isBlocked) ThreatRed else ShieldGreen
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = rule.domain,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                            }

                            IconButton(
                                onClick = { onRemoveCustomRule(rule.domain) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Domain Threat Checker Tool Trigger Card
        Button(
            onClick = { showInspectorDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("open_domain_inspector_button"),
            colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(CyberCardBorder, CyberCyan)))
        ) {
            Icon(Icons.Default.BugReport, contentDescription = null, tint = CyberCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Domain Threat Inspector & Simulator", color = TextPrimary)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showInspectorDialog) {
        DomainCheckDialog(
            initialDomain = "paypal-security-update.com",
            onDismiss = { showInspectorDialog = false },
            onSimulateBlock = onSimulateBlock
        )
    }

    if (showAddRuleDialog) {
        RuleAddDialog(
            onDismiss = { showAddRuleDialog = false },
            onAddRule = onAddCustomRule
        )
    }
}

@Composable
private fun PostureFactorRow(
    title: String,
    progress: Float,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = CyberSurfaceElevated
        )
    }
}

@Composable
private fun ThreatCategoryItem(
    title: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        }
        Text(
            text = "$count intercepted",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (count > 0) color else TextMuted
        )
    }
}
