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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ActionLogEntity
import com.example.data.entity.ArtifactEntity
import com.example.ui.model.OtakAgent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Okno inspekcji bieżącej aktywności agenta Katedry OtakOS,
 * wywoływane po kliknięciu świecącego awatara na perłowym stole.
 */
@Composable
fun AgentActivityDialog(
    agent: OtakAgent,
    artifacts: List<ArtifactEntity>,
    actionLogs: List<ActionLogEntity>,
    onDismiss: () -> Unit,
    onFilterByAgent: (String) -> Unit,
    onTriggerCollaboration: (ArtifactEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Filter relevant data for this agent
    val agentLogs = actionLogs
        .filter { it.agentName.equals(agent.name, ignoreCase = true) }
        .take(5)

    val participatingArtifacts = artifacts.filter {
        it.collaboratingAgents.contains(agent.name, ignoreCase = true) ||
                it.proposingAgent.equals(agent.name, ignoreCase = true)
    }

    val activeWorkArtifact = participatingArtifacts.firstOrNull { it.status == "W_OPRACOWANIU" }
        ?: participatingArtifacts.firstOrNull { it.status == "NA_STOLE" }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .testTag("agent_activity_dialog"),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = if (isDark) Color(0xF2121A28) else Color(0xFBF8FAFC),
                modifier = modifier
                    .fillMaxWidth()
                    .shadow(32.dp, RoundedCornerShape(26.dp), spotColor = agent.primaryColor)
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                agent.accentColor,
                                agent.primaryColor.copy(alpha = 0.5f),
                                Color(0xFFFBBF24).copy(alpha = 0.3f)
                            )
                        ),
                        RoundedCornerShape(26.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    // Header: Glowing Avatar, Name, Role & Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Circular glowing avatar with aura
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    agent.accentColor.copy(alpha = 0.6f),
                                                    agent.primaryColor.copy(alpha = 0.2f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDark) Color(0xFF1E293B) else Color.White,
                                    shadowElevation = 8.dp,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .border(
                                            2.dp,
                                            Brush.sweepGradient(listOf(agent.accentColor, agent.primaryColor, agent.accentColor)),
                                            CircleShape
                                        )
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = agent.avatarSymbol,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = agent.primaryColor
                                        )
                                    }
                                }
                                // Online beacon dot
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                        .border(2.dp, if (isDark) Color(0xFF121A28) else Color.White, CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = agent.name,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = agent.primaryColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "ONLINE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = agent.primaryColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = agent.role,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = agent.accentColor
                                )
                                Text(
                                    text = agent.specialty,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("agent_activity_close_button")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Zamknij")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Current status / activity description banner
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = agent.primaryColor.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, agent.primaryColor.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = agent.primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "BIEŻĄCA AKTYWNOŚĆ PRZY STOLE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = agent.primaryColor
                                )
                                Text(
                                    text = agent.statusText,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Recent actions section
                    Text(
                        text = "OSTATNIE DZIAŁANIA W KATEDRZE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (agentLogs.isEmpty()) {
                        Text(
                            text = "Brak zarejestrowanych zdarzeń w bieżącej sesji stołu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            agentLogs.forEach { log ->
                                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDark) Color(0xFF1E2838) else Color(0xFFF1F5F9),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = timeFormat.format(Date(log.timestamp)),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp
                                            ),
                                            color = agent.primaryColor
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = log.description,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Associated Artifacts summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ZADANIA NA STOLE (${participatingArtifacts.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    if (participatingArtifacts.isEmpty()) {
                        Text(
                            text = "Agent nie ma aktualnie przypisanych zadań na stole.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            participatingArtifacts.take(2).forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) Color(0x991E293B) else Color(0x99EDF2F7),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${item.consensusScore}%",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                onFilterByAgent(agent.name)
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("agent_filter_button")
                        ) {
                            Icon(imageVector = Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Filtruj zadania", fontSize = 11.5.sp)
                        }

                        if (activeWorkArtifact != null) {
                            ElevatedButton(
                                onClick = {
                                    onTriggerCollaboration(activeWorkArtifact)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = agent.primaryColor,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("agent_iterate_button")
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Iteruj z agentem", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
