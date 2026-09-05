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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.ArtifactEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RatificationView(
    artifacts: List<ArtifactEntity>,
    onPreview: (ArtifactEntity) -> Unit,
    onOpenRatificationAction: (ArtifactEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingRatification = artifacts.filter { it.status == "DO_AKCEPTACJI" }
    val ratifiedWorks = artifacts.filter { it.status == "ZRATYFIKOWANE" }

    LazyColumn(
        modifier = modifier
            .testTag("ratification_view")
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Chamber Header Banner
        item {
            RatificationHeaderBanner(
                pendingCount = pendingRatification.size,
                ratifiedCount = ratifiedWorks.size
            )
        }

        // Section: Pending Ratification
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OCZEKUJĄCE NA AKCEPTACJĘ KATEDRY (${pendingRatification.size})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFFD97706)
                )
            }
        }

        if (pendingRatification.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Brak dzieł oczekujących",
                    subtitle = "Gdy agenci zakończą procesy syntezy i osiągną wysoki konsensus, gotowe prace pojawią się w tym miejscu do akceptacji."
                )
            }
        } else {
            items(pendingRatification, key = { it.id }) { artifact ->
                PendingRatificationCard(
                    artifact = artifact,
                    onPreview = { onPreview(artifact) },
                    onDecide = { onOpenRatificationAction(artifact) }
                )
            }
        }

        // Section: Ratified Masterpieces
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ZRATYFIKOWANY KANON DZIEŁ KATEDRY (${ratifiedWorks.size})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFF059669)
                )
            }
        }

        if (ratifiedWorks.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Brak zratyfikowanych dzieł",
                    subtitle = "Zaakceptowane oficjalnie dzieła będą archiwizowane z pieczęcią Katedry OtakOS."
                )
            }
        } else {
            items(ratifiedWorks, key = { it.id }) { artifact ->
                RatifiedMasterpieceCard(
                    artifact = artifact,
                    onPreview = { onPreview(artifact) }
                )
            }
        }
    }
}

@Composable
private fun RatificationHeaderBanner(
    pendingCount: Int,
    ratifiedCount: Int
) {
    val isDark = isSystemInDarkTheme()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E2638) else Color(0xFFF8FAFC)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFFD97706), Color(0xFF059669), Color(0xFF0284C7))
                ),
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "IZBA AKCEPTACJI KATEDRY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFD97706)
                    )
                    Text(
                        text = "Ratyfikacja Dzieł Agentów OtakOS",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD97706).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFD97706), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Miejsce ostatecznej oceny prac przedłożonych przez współpracujące instancje AI. Zratyfikowane dzieła otrzymują kanoniczną pieczęć Katedry.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFD97706).copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Oczekujące", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD97706))
                        Text("$pendingCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFD97706))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF059669).copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Zratyfikowane", style = MaterialTheme.typography.labelSmall, color = Color(0xFF059669))
                        Text("$ratifiedCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF059669))
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingRatificationCard(
    artifact: ArtifactEntity,
    onPreview: () -> Unit,
    onDecide: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1B2332) else Color(0xFFFFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFFD97706).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val priorityInfo = TaskPriority.fromString(artifact.priority)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFD97706).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "DO ZATWIERDZENIA",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFD97706)
                        )
                    }

                    TaskPriorityBadge(
                        priority = priorityInfo,
                        compact = true,
                        fontSize = 10.sp,
                        iconSize = 10.dp,
                        testTagSuffix = "ratification_${artifact.id}"
                    )
                }

                Text(
                    text = "Konsensus: ${artifact.consensusScore}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFD97706)
                )
            }

            if (priorityInfo == TaskPriority.HIGH) {
                Spacer(modifier = Modifier.height(8.dp))
                UrgentAgentFocusBanner(modifier = Modifier.testTag("urgent_ratification_${artifact.id}"))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = artifact.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = artifact.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Payload glimpse
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0xFF0F141C) else Color(0xFFF1F5F9))
                    .padding(8.dp)
            ) {
                Text(
                    text = artifact.digitalPayload.lines().take(2).joinToString("\n"),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Agent Status Indicator
            CompactSpatialAgentIndicator(artifact = artifact)

            Spacer(modifier = Modifier.height(8.dp))

            // Compact Task Timer & Velocity Indicator
            CompactSpatialCardTimer(
                artifact = artifact,
                onToggleTimer = null
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Zbadaj", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                ElevatedButton(
                    onClick = onDecide,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFFD97706),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .testTag("decide_ratification_${artifact.id}")
                        .height(36.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Icon(imageVector = Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Decyzja Katedry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RatifiedMasterpieceCard(
    artifact: ArtifactEntity,
    onPreview: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val dateStr = artifact.ratifiedTimestamp?.let { dateFormat.format(Date(it)) } ?: "Kanoniczne"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF131F1C) else Color(0xFFF0FDF4)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFF059669).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ZRATYFIKOWANE DZIEŁO",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF059669)
                    )
                }

                Text(
                    text = "Pieczęć z dnia: $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = artifact.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = artifact.ratificationNotes ?: "Dzieło włączone do dziedzictwa cyfrowego Katedry OtakOS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            CompactSpatialAgentIndicator(artifact = artifact)

            Spacer(modifier = Modifier.height(8.dp))

            CompactSpatialCardTimer(
                artifact = artifact,
                onToggleTimer = null
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inicjator: ${artifact.proposingAgent} • Iteracje: #${artifact.iterationsCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FilledTonalButton(
                    onClick = onPreview,
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("Podgląd Dzieła", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun RatificationActionDialog(
    artifact: ArtifactEntity,
    onDismiss: () -> Unit,
    onRatify: (notes: String) -> Unit,
    onReject: (feedback: String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var notes by remember {
        mutableStateOf("Dzieło spełnia najwyższe rygory Katedry OtakOS. Nadaje się status kanoniczny.")
    }
    var feedback by remember {
        mutableStateOf("Wymaga pogłębienia analizy brzegowej i lepszej optymalizacji zużycia pamięci.")
    }
    var isRevisionMode by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF131A26) else Color(0xFFFFFFFF)
            ),
            modifier = Modifier
                .testTag("ratification_action_dialog")
                .fillMaxWidth()
                .padding(8.dp)
                .border(
                    1.5.dp,
                    if (isRevisionMode) Color(0xFFEF4444) else Color(0xFFD97706),
                    RoundedCornerShape(22.dp)
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRevisionMode) Color(0xFFEF4444).copy(alpha = 0.15f)
                                    else Color(0xFFD97706).copy(alpha = 0.15f)
                                )
                        ) {
                            Icon(
                                imageVector = if (isRevisionMode) Icons.Default.Replay else Icons.Default.Gavel,
                                contentDescription = null,
                                tint = if (isRevisionMode) Color(0xFFEF4444) else Color(0xFFD97706)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isRevisionMode) "ZWROT DO POPRAWEK" else "DECYZJA RATYFIKACYJNA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 0.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isRevisionMode) Color(0xFFEF4444) else Color(0xFFD97706)
                            )
                            Text(
                                text = artifact.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Zamknij")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isRevisionMode) {
                    Text(
                        text = "Wpis do Protokołu Ratyfikacyjnego Katedry OtakOS:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("ratification_notes_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chcesz zgłosić zastrzeżenia?",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .testTag("switch_to_revision_mode")
                                .clip(RoundedCornerShape(4.dp))
                                .padding(4.dp)
                        )
                        OutlinedButton(
                            onClick = { isRevisionMode = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Text("Zwróć do poprawy", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    ElevatedButton(
                        onClick = { onRatify(notes) },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFF059669),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .testTag("confirm_ratify_button")
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AssignmentTurnedIn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("UROCZYŚCIE ZRATYFIKUJ DZIEŁO", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "Wskaż agentom co wymaga dopracowania:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = feedback,
                        onValueChange = { feedback = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("revision_feedback_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { isRevisionMode = false }) {
                            Text("Powrót do Ratyfikacji", fontSize = 11.sp)
                        }

                        ElevatedButton(
                            onClick = { onReject(feedback) },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("confirm_reject_button")
                        ) {
                            Text("Odeślij do Agentów")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, subtitle: String) {
    val isDark = isSystemInDarkTheme()
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0x661E293B) else Color(0x99F1F5F9)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
