package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBackgroundDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.ThreatRed

@Composable
fun RadarShield(
    isActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarPulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha"
    )

    val activeColor = ShieldGreen
    val inactiveColor = ThreatRed

    Box(
        modifier = modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing radar ring if active
        if (isActive) {
            Canvas(modifier = Modifier.size(180.dp)) {
                drawCircle(
                    color = activeColor.copy(alpha = pulseAlpha),
                    radius = (size.minDimension / 2f) * pulseScale,
                    style = Stroke(width = 3.5f)
                )
                drawCircle(
                    color = CyberCyan.copy(alpha = (pulseAlpha * 0.7f)),
                    radius = (size.minDimension / 2f) * (pulseScale * 0.85f),
                    style = Stroke(width = 2f)
                )
            }
        }

        // Inner circular button
        Box(
            modifier = Modifier
                .size(136.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (isActive) {
                            listOf(Color(0xFF003828), Color(0xFF061E1A), CyberBackgroundDark)
                        } else {
                            listOf(Color(0xFF381014), Color(0xFF1E0A0C), CyberBackgroundDark)
                        }
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = if (isActive) ShieldGreen else CyberCyan),
                    onClick = onToggle
                )
                .testTag("shield_toggle_button"),
            contentAlignment = Alignment.Center
        ) {
            // Border ring
            Canvas(modifier = Modifier.size(136.dp)) {
                drawCircle(
                    color = if (isActive) ShieldGreen.copy(alpha = 0.8f) else ThreatRed.copy(alpha = 0.6f),
                    style = Stroke(width = 3f)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (isActive) Icons.Default.Shield else Icons.Default.LockOpen,
                    contentDescription = if (isActive) "Shield Active" else "Shield Offline",
                    tint = if (isActive) ShieldGreen else ThreatRed,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isActive) "PROTECTED" else "OFFLINE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = if (isActive) ShieldGreen else ThreatRed
                )
                Text(
                    text = if (isActive) "Tap to pause" else "Tap to guard",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = TextMuted
                )
            }
        }
    }
}
