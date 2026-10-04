package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.threat.ThreatCheckResult
import com.example.threat.ThreatIntelligence
import com.example.ui.theme.CyberBackgroundDark
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThreatRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DomainCheckDialog(
    initialDomain: String = "",
    onDismiss: () -> Unit,
    onSimulateBlock: (String) -> Unit
) {
    var domainInput by remember { mutableStateOf(initialDomain) }
    var result by remember {
        mutableStateOf<ThreatCheckResult?>(
            if (initialDomain.isNotEmpty()) ThreatIntelligence.checkDomain(initialDomain) else null
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Domain Threat Inspector",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter any domain or test known attack vectors against the NetShield intelligence engine.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = domainInput,
                    onValueChange = {
                        domainInput = it
                        if (it.isNotBlank()) {
                            result = ThreatIntelligence.checkDomain(it)
                        } else {
                            result = null
                        }
                    },
                    label = { Text("Domain (e.g. malware-site.com)") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (domainInput.isNotBlank()) {
                                    result = ThreatIntelligence.checkDomain(domainInput)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Scan", tint = CyberCyan)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("domain_check_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = CyberCyan
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Quick Test Vectors:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ThreatIntelligence.SAMPLE_TEST_DOMAINS.take(5).forEach { sample ->
                        SuggestionChip(
                            onClick = {
                                domainInput = sample
                                result = ThreatIntelligence.checkDomain(sample)
                            },
                            label = { Text(sample, fontSize = 11.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = CyberSurfaceElevated,
                                labelColor = CyberCyan
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = CyberCardBorder
                            )
                        )
                    }
                }

                result?.let { res ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (res.isThreat) Color(0xFF3B1014) else Color(0xFF0C2E20))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (res.isThreat) Icons.Default.Dangerous else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (res.isThreat) ThreatRed else ShieldGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (res.isThreat) "THREAT DETECTED: ${res.threatName}" else "VERIFIED SAFE DOMAIN",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (res.isThreat) ThreatRed else ShieldGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Category: ${res.category.displayName} (${res.level.label})",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = res.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (result?.isThreat == true && domainInput.isNotBlank()) {
                Button(
                    onClick = {
                        onSimulateBlock(domainInput)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThreatRed),
                    modifier = Modifier.testTag("test_block_threat_button")
                ) {
                    Text("Trigger Intercept Test", color = Color.White)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Done", color = Color(0xFF061E1A))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}
