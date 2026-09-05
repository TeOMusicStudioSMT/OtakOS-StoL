package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.model.OtakAgent

/**
 * Okrągły, świecący awatar agenta Katedry OtakOS umieszczony na perłowym stole.
 * Posiada pulsującą poświatę kaustyczną i po dotknięciu otwiera panel aktywności agenta.
 */
@Composable
fun AgentTableAvatar(
    agent: OtakAgent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    val isDark = isSystemInDarkTheme()

    // Breathing pulse animation for agent's presence aura
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_glow_${agent.id}")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .testTag("table_agent_avatar_${agent.id}")
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .drawBehind {
                    val radius = size.minDimension / 2f + (8f * glowPulse)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                agent.accentColor.copy(alpha = 0.55f * glowPulse),
                                agent.primaryColor.copy(alpha = 0.25f * glowPulse),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * 1.5f
                        ),
                        radius = radius * 1.5f,
                        blendMode = BlendMode.Plus
                    )
                }
        ) {
            // Main circular orb with specular marble bevel
            Surface(
                shape = CircleShape,
                color = if (isDark) Color(0xFF131B2A) else Color(0xFFF8FAFC),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(44.dp)
                    .border(
                        width = if (isSelected) 2.5.dp else 1.8.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                agent.accentColor,
                                agent.primaryColor,
                                Color.White.copy(alpha = 0.8f),
                                agent.accentColor
                            )
                        ),
                        shape = CircleShape
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = agent.avatarSymbol,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = agent.primaryColor
                    )
                }
            }

            // Glowing online presence status beacon
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
                    .border(1.5.dp, if (isDark) Color(0xFF101724) else Color.White, CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Agent mini label on stone surface
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isDark) Color(0xCC1A2333) else Color(0xDDFFFFFF),
            modifier = Modifier.border(
                0.6.dp,
                if (isSelected) agent.primaryColor else if (isDark) Color(0x3394A3B8) else Color(0x44CBD5E1),
                RoundedCornerShape(6.dp)
            )
        ) {
            Text(
                text = agent.name.takeWhile { it != '-' },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) agent.primaryColor else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}
