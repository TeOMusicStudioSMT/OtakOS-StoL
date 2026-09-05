package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ArtifactEntity
import com.example.ui.model.OtakDepartment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ArtifactPreviewDialog(
    artifact: ArtifactEntity,
    onDismiss: () -> Unit,
    onSimulateAgentIteration: (agentName: String) -> Unit,
    onSubmitToRatification: () -> Unit,
    onOpenRatificationAction: () -> Unit,
    onUpdatePayload: (newPayload: String) -> Unit,
    onDelete: () -> Unit,
    onAssignAgent: ((String) -> Unit)? = null,
    onRequestReview: ((String, String) -> Unit)? = null,
    onCompleteReview: ((Boolean) -> Unit)? = null,
    onToggleTimer: (() -> Unit)? = null,
    onResetTimer: (() -> Unit)? = null,
    onAdjustTime: ((Long) -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()

    var isEditingPayload by remember { mutableStateOf(false) }
    var editedPayload by remember(artifact.digitalPayload) { mutableStateOf(artifact.digitalPayload) }
    var selectedAgentForWork by remember { mutableStateOf("Otak-Alpha") }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault()) }
    val formattedDate = dateFormat.format(Date(artifact.updatedTimestamp))

    val statusColor = when (artifact.status) {
        "NA_STOLE" -> Color(0xFF0284C7)
        "W_OPRACOWANIU" -> Color(0xFF7C3AED)
        "DO_AKCEPTACJI" -> Color(0xFFD97706)
        "ZRATYFIKOWANE" -> Color(0xFF059669)
        else -> MaterialTheme.colorScheme.primary
    }
    val priorityInfo = TaskPriority.fromString(artifact.priority)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF131A26) else Color(0xFFFAFBFD)
            ),
            modifier = Modifier
                .testTag("artifact_preview_dialog")
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .border(
                    1.5.dp,
                    if (isDark) Color(0xFF2E3D52) else Color(0xFFCBD5E1),
                    RoundedCornerShape(24.dp)
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header Row: Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = artifact.status.replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = statusColor
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            TaskPriorityBadge(
                                priority = priorityInfo,
                                compact = true,
                                fontSize = 10.sp,
                                iconSize = 10.dp,
                                testTagSuffix = "preview_${artifact.id}"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val categoryColor = getCategoryChartColor(artifact.category)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(categoryColor.copy(alpha = 0.15f))
                                    .border(0.8.dp, categoryColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(categoryColor)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = artifact.category,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        ),
                                        color = categoryColor
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = artifact.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_preview_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Zamknij")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    // Description
                    Text(
                        text = "Kontekst zadania Katedry OtakOS:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = artifact.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Metadata Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF1B2332) else Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Agent Inicjator", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(artifact.proposingAgent, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("Konsensus Katedry", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${artifact.consensusScore}% zgody", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = statusColor))
                                }
                                Column {
                                    Text("Cykl Iteracji", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("#${artifact.iterationsCount}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("Priorytet", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = priorityInfo.label,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = priorityInfo.color
                                        )
                                    )
                                }
                            }

                            if (artifact.collaboratingAgents.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Aktywni współpracownicy: ${artifact.collaboratingAgents}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ostatnia aktualizacja na stole: $formattedDate",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Task Agent Status Indicator (Assigned Lead & Active Reviewing AI Agent)
                    TaskAgentStatusIndicator(
                        artifact = artifact,
                        onAssignAgent = { onAssignAgent?.invoke(it) },
                        onRequestReview = { agent, type -> onRequestReview?.invoke(agent, type) },
                        onCompleteReview = onCompleteReview
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Task Completion Progress Section
                    TaskCardProgressSection(artifact = artifact)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Detailed Task Velocity & Timer Section
                    DetailedTaskVelocityTimerPanel(
                        artifact = artifact,
                        onToggleTimer = { onToggleTimer?.invoke() },
                        onResetTimer = { onResetTimer?.invoke() },
                        onAdjustTime = onAdjustTime
                    )

                    // If Ratified, show Ratification Certificate Box
                    if (artifact.status == "ZRATYFIKOWANE") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF059669).copy(alpha = 0.12f))
                                .border(1.5.dp, Color(0xFF059669), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "OFICJALNIE ZRATYFIKOWANE DZIEŁO KATEDRY",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = Color(0xFF059669)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = artifact.ratificationNotes ?: "Dzieło spełniło wszystkie kryteria estetyczne i logiczne.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Digital Payload Box (Header + Content)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cyfrowa Zawartość na Stole (Payload):",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = {
                                if (isEditingPayload) {
                                    onUpdatePayload(editedPayload)
                                    isEditingPayload = false
                                } else {
                                    isEditingPayload = true
                                }
                            },
                            modifier = Modifier.testTag("toggle_payload_edit")
                        ) {
                            Icon(
                                imageVector = if (isEditingPayload) Icons.Default.CheckCircle else Icons.Default.Edit,
                                contentDescription = if (isEditingPayload) "Zapisz" else "Edytuj",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isEditingPayload) {
                        OutlinedTextField(
                            value = editedPayload,
                            onValueChange = { editedPayload = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .testTag("edit_payload_field"),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF0A0E14) else Color(0xFFF1F5F9))
                                .border(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = artifact.digitalPayload,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Agent Collaboration Box
                    if (artifact.status != "ZRATYFIKOWANE") {
                        Text(
                            text = "Wspólna Praca Agentów przy Stole:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF1A2234) else Color(0xFFF8FAFC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (isDark) Color(0xFF2E3D52) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Wybierz Agenta Katedry do kolejnej iteracji:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val candidateAgents = listOf("Otak-Alpha", "Vektor-9", "Kaliope-AI", "Neuro-Marmur")
                                    candidateAgents.forEach { ag ->
                                        val isChosen = selectedAgentForWork == ag
                                        OutlinedButton(
                                            onClick = { selectedAgentForWork = ag },
                                            colors = if (isChosen) ButtonDefaults.outlinedButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            ) else ButtonDefaults.outlinedButtonColors(),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text(ag, fontSize = 11.sp, fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                ElevatedButton(
                                    onClick = { onSimulateAgentIteration(selectedAgentForWork) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("run_agent_cycle_button")
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Niech $selectedAgentForWork udoskonali dzieło")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_artifact_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Usuń ze stołu", tint = Color(0xFFEF4444))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (artifact.status == "DO_AKCEPTACJI") {
                            ElevatedButton(
                                onClick = onOpenRatificationAction,
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = Color(0xFFD97706),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("dialog_open_ratification_button")
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Decyzja Ratyfikacyjna", fontWeight = FontWeight.Bold)
                            }
                        } else if (artifact.status != "ZRATYFIKOWANE") {
                            FilledTonalButton(
                                onClick = onSubmitToRatification,
                                modifier = Modifier.testTag("dialog_submit_ratification_button")
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Przekaż do Akceptacji")
                            }
                        }

                        OutlinedButton(onClick = onDismiss) {
                            Text("Zamknij")
                        }
                    }
                }
            }
        }
    }
}
