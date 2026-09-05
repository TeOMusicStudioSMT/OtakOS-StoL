package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class TaskPriority(
    val level: Int,
    val label: String, // "High", "Medium", "Low"
    val localizedLabel: String, // "Wysoki", "Średni", "Niski"
    val color: Color,
    val containerColor: Color,
    val borderColor: Color,
    val icon: ImageVector,
    val urgentFocusTitle: String
) {
    HIGH(
        level = 3,
        label = "High",
        localizedLabel = "Wysoki",
        color = Color(0xFFEF4444),
        containerColor = Color(0x28EF4444),
        borderColor = Color(0x70EF4444),
        icon = Icons.Default.ArrowUpward,
        urgentFocusTitle = "Pilny fokus dla agentów AI (High Priority)"
    ),
    MEDIUM(
        level = 2,
        label = "Medium",
        localizedLabel = "Średni",
        color = Color(0xFFF59E0B),
        containerColor = Color(0x24F59E0B),
        borderColor = Color(0x60F59E0B),
        icon = Icons.Default.Remove,
        urgentFocusTitle = "Standardowy priorytet prac"
    ),
    LOW(
        level = 1,
        label = "Low",
        localizedLabel = "Niski",
        color = Color(0xFF64748B),
        containerColor = Color(0x1F64748B),
        borderColor = Color(0x5064748B),
        icon = Icons.Default.ArrowDownward,
        urgentFocusTitle = "Niski priorytet - kolejka pomocnicza"
    );

    val isUrgent: Boolean
        get() = this == HIGH

    companion object {
        fun fromString(raw: String?): TaskPriority {
            val lower = raw?.lowercase()?.trim() ?: return MEDIUM
            return when {
                lower.contains("high") || lower.contains("wyso") || lower.contains("kluczow") ||
                lower.contains("piln") || lower.contains("urgent") || lower.contains("krytycz") ||
                lower.contains("critic") -> HIGH

                lower.contains("low") || lower.contains("nisk") || lower.contains("minor") ||
                lower.contains("drobn") || lower.contains("drugorzęd") || lower.contains("drugorzed") -> LOW

                else -> MEDIUM // "standardowy", "eksperymentalny", "medium", "normal", "średni"
            }
        }
    }
}

/**
 * Visual badge indicator for task priority (High, Medium, Low)
 * Designed with distinct colors, border accents, and icons to help AI agents focus on urgent tasks first.
 */
@Composable
fun TaskPriorityBadge(
    priority: TaskPriority,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    fontSize: TextUnit = 10.5.sp,
    iconSize: Dp = 11.dp,
    showIcon: Boolean = true,
    testTagSuffix: String = ""
) {
    val tag = if (testTagSuffix.isNotBlank()) "task_priority_tag_${testTagSuffix}" else "task_priority_tag_${priority.label.lowercase()}"
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(priority.containerColor)
            .border(0.8.dp, priority.borderColor, RoundedCornerShape(6.dp))
            .padding(
                horizontal = if (compact) 5.dp else 7.dp,
                vertical = if (compact) 2.dp else 3.dp
            )
            .testTag(tag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.Center)
        ) {
            if (showIcon) {
                Icon(
                    imageVector = priority.icon,
                    contentDescription = "Priorytet ${priority.label}",
                    tint = priority.color,
                    modifier = Modifier.size(iconSize)
                )
                Spacer(modifier = Modifier.width(3.5.dp))
            }
            Text(
                text = priority.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    letterSpacing = 0.3.sp
                ),
                color = priority.color
            )
        }
    }
}

/**
 * Urgent callout banner displayed for HIGH priority tasks to draw immediate attention of AI agents and council members.
 */
@Composable
fun UrgentAgentFocusBanner(
    modifier: Modifier = Modifier,
    label: String = "Pilny fokus dla agentów AI (High Priority)"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEF4444).copy(alpha = 0.12f))
            .border(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .testTag("urgent_agent_focus_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    ),
                    color = Color(0xFFEF4444)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFEF4444))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "URGENT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 8.5.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}
