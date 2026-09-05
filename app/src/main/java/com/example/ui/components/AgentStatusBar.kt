package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.model.OtakAgent
import com.example.ui.model.OtakDepartment

/**
 * Pasek agentów Katedry OtakOS zasiadających przy perłowym stole.
 */
@Composable
fun AgentStatusBar(
    selectedAgentFilter: String?,
    onSelectAgent: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Text(
                    text = " AGENTÓW PRZY STOLE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            if (selectedAgentFilter != null) {
                Text(
                    text = "Wyczyść filtr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .testTag("clear_agent_filter")
                        .clickable { onSelectAgent(null) }
                        .padding(4.dp)
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(OtakDepartment.agents) { agent ->
                val isSelected = selectedAgentFilter.equals(agent.name, ignoreCase = true)
                AgentPresencePill(
                    agent = agent,
                    isSelected = isSelected,
                    onClick = {
                        if (isSelected) onSelectAgent(null) else onSelectAgent(agent.name)
                    }
                )
            }
        }
    }
}

@Composable
private fun AgentPresencePill(
    agent: OtakAgent,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val bgGradient = if (isSelected) {
        Brush.horizontalGradient(
            colors = listOf(
                agent.primaryColor.copy(alpha = 0.25f),
                agent.accentColor.copy(alpha = 0.15f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                if (isDark) Color(0x991E293B) else Color(0xEEFFFFFF),
                if (isDark) Color(0x660F172A) else Color(0xDDF1F5F9)
            )
        )
    }

    val borderColor = if (isSelected) agent.primaryColor else if (isDark) Color(0x3394A3B8) else Color(0x44CBD5E1)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        modifier = Modifier
            .testTag("agent_pill_${agent.id}")
            .clip(RoundedCornerShape(18.dp))
            .background(bgGradient)
            .border(1.2.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Agent Avatar Symbol
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(agent.primaryColor.copy(alpha = 0.2f))
                    .border(1.dp, agent.accentColor, CircleShape)
            ) {
                Text(
                    text = agent.avatarSymbol,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = agent.primaryColor
                )
            }

            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = agent.name,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = agent.role,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
