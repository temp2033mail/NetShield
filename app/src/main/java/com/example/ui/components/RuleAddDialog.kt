package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSurfaceDark
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThreatRed

@Composable
fun RuleAddDialog(
    onDismiss: () -> Unit,
    onAddRule: (domain: String, isBlocked: Boolean, category: String, notes: String) -> Unit
) {
    var domain by remember { mutableStateOf("") }
    var isBlocked by remember { mutableStateOf(true) } // true = Block, false = Whitelist
    var notes by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurfaceDark,
        title = {
            Text(
                text = "Add Custom Filter Rule",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Add a domain to permanently block or whitelist from the Sentinel firewall.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row {
                    FilterChip(
                        selected = isBlocked,
                        onClick = { isBlocked = true },
                        label = { Text("Block (Blacklist)") },
                        leadingIcon = {
                            Icon(Icons.Default.Block, contentDescription = null, tint = ThreatRed)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ThreatRed.copy(alpha = 0.2f),
                            selectedLabelColor = ThreatRed
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = !isBlocked,
                        onClick = { isBlocked = false },
                        label = { Text("Whitelist") },
                        leadingIcon = {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ShieldGreen)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ShieldGreen.copy(alpha = 0.2f),
                            selectedLabelColor = ShieldGreen
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = domain,
                    onValueChange = {
                        domain = it
                        errorText = ""
                    },
                    label = { Text("Domain name (e.g. ads.example.com)") },
                    singleLine = true,
                    isError = errorText.isNotEmpty(),
                    supportingText = if (errorText.isNotEmpty()) {
                        { Text(errorText, color = ThreatRed) }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_domain_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isBlocked) ThreatRed else ShieldGreen,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = CyberCyan
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Reason (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = CyberCyan
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = domain.trim().lowercase().removePrefix("http://").removePrefix("https://")
                    if (clean.isBlank() || !clean.contains(".")) {
                        errorText = "Please enter a valid domain (e.g. example.com)"
                        return@Button
                    }
                    onAddRule(clean, isBlocked, if (isBlocked) "Custom Block" else "Custom Whitelist", notes)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBlocked) ThreatRed else ShieldGreen
                ),
                modifier = Modifier.testTag("save_rule_button")
            ) {
                Text(if (isBlocked) "Block Domain" else "Allow Domain", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
