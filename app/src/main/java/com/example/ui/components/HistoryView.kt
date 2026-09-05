package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ActionLogEntity
import com.example.ui.model.OtakDepartment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryView(
    logs: List<ActionLogEntity>,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val dateFormat = remember { SimpleDateFormat("dd.MM, HH:mm", Locale.getDefault()) }

    var selectedActionTypeFilter by remember { mutableStateOf<String?>(null) }

    val filterOptions = listOf(
        "Wszystkie" to null,
        "Ratyfikacje" to "RATYFIKACJA_KATEDRY",
        "Na stole" to "POŁOŻENIE_NA_STOLE",
        "Iteracje" to "ITERACJA_AGENTÓW",
        "Akceptacje" to "ZGŁOSZENIE_DO_AKCEPTACJI"
    )

    val filteredLogs = logs.filter { log ->
        selectedActionTypeFilter == null || log.actionType == selectedActionTypeFilter
    }

    Column(
        modifier = modifier
            .testTag("history_view")
            .fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "HISTORIA DZIAŁAŃ PRZY STOLE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Lokalny rejestr interakcji, konsensusu i ratyfikacji agentów OtakOS",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { (label, value) ->
                val isSelected = selectedActionTypeFilter == value
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedActionTypeFilter = value },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Timeline List
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (filteredLogs.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1A2234) else Color(0xFFF1F5F9)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Brak wpisów w historii", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Wszelkie działania podejmowane na stole są tutaj automatycznie rejestrowane.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredLogs, key = { it.id }) { log ->
                    ActionLogCard(log = log, dateFormat = dateFormat)
                }
            }
        }
    }
}

@Composable
private fun ActionLogCard(
    log: ActionLogEntity,
    dateFormat: SimpleDateFormat
) {
    val isDark = isSystemInDarkTheme()
    val agent = OtakDepartment.getAgentByName(log.agentName)

    val (badgeColor, badgeLabel) = when (log.actionType) {
        "POŁOŻENIE_NA_STOLE" -> Color(0xFF0284C7) to "NOWE NA STOLE"
        "ITERACJA_AGENTÓW" -> Color(0xFF7C3AED) to "PRACA AGENTA"
        "MODYFIKACJA_KODU" -> Color(0xFF0D9488) to "MODYFIKACJA"
        "ZGŁOSZENIE_DO_AKCEPTACJI" -> Color(0xFFD97706) to "DO AKCEPTACJI"
        "RATYFIKACJA_KATEDRY" -> Color(0xFF059669) to "RATYFIKACJA"
        "ZWROT_DO_POPRAWY" -> Color(0xFFDC2626) to "POPRAWKI"
        else -> MaterialTheme.colorScheme.primary to log.actionType
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF161E2C) else Color(0xFFFFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (log.actionType == "RATYFIKACJA_KATEDRY") Color(0xFF059669).copy(alpha = 0.5f)
                else if (isDark) Color(0xFF263346) else Color(0xFFE2E8F0),
                RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Agent Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(agent.primaryColor.copy(alpha = 0.18f))
                    .border(1.dp, agent.accentColor, CircleShape)
            ) {
                Text(
                    text = agent.avatarSymbol,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = agent.primaryColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = log.agentName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor.copy(alpha = 0.14f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = badgeColor
                            )
                        }
                    }

                    Text(
                        text = dateFormat.format(Date(log.timestamp)),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!log.artifactTitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dotyczy: ${log.artifactTitle}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = log.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
