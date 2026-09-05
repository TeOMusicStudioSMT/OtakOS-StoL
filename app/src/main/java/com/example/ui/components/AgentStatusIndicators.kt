package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ArtifactEntity
import com.example.ui.model.OtakAgent
import com.example.ui.model.OtakDepartment

/**
 * Dynamiczny wskaźnik statusu na karcie zadania obrazujący:
 * 1. Który agent AI jest aktualnie przypisany jako opiekun zadania (Assigned Agent)
 * 2. Który agent AI aktualnie aktywnie recenzuje zadanie (Reviewing Agent)
 * z pulsującym dynamicznym wskaźnikiem na żywo oraz interaktywną zmianą przydziału/recenzji.
 */
@Composable
fun TaskAgentStatusIndicator(
    artifact: ArtifactEntity,
    onAssignAgent: (String) -> Unit,
    onRequestReview: (String, String) -> Unit,
    onCompleteReview: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var showAssignDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }

    val assignedAgent = OtakDepartment.getAgentByName(artifact.effectiveAssignedAgent)
    val reviewingAgent = artifact.reviewingAgent?.let { OtakDepartment.getAgentByName(it) }

    // Animacja pulsowania dla aktywnego recenzenta
    val infiniteTransition = rememberInfiniteTransition(label = "review_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val radarScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_scale"
    )

    Column(
        modifier = modifier
            .testTag("task_agent_status_indicator_${artifact.id}")
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDark) Color(0x600F172A) else Color(0x50F8FAFC)
            )
            .border(
                1.dp,
                if (isDark) Color(0x30334155) else Color(0x40CBD5E1),
                RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        // Górny pasek etykiety
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STATUS PRZYDZIAŁU I RECENZJI AI",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )

            // Akcja zmiany przypisania / recenzji
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clickable { showAssignDialog = true }
                        .testTag("reassign_agent_btn_${artifact.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Zmień przypisanego agenta",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Przydział",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.2f else 0.12f),
                    modifier = Modifier
                        .clickable { showReviewDialog = true }
                        .testTag("request_review_btn_${artifact.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RateReview,
                            contentDescription = "Poproś o recenzję",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Recenzja",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Rząd 1: Przypisany Opiekun Zadania (Assigned Agent)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    assignedAgent.primaryColor.copy(alpha = if (isDark) 0.14f else 0.08f)
                )
                .border(
                    0.8.dp,
                    assignedAgent.primaryColor.copy(alpha = if (isDark) 0.35f else 0.25f),
                    RoundedCornerShape(8.dp)
                )
                .clickable { showAssignDialog = true }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Awatar z symbolem agenta
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                assignedAgent.primaryColor.copy(alpha = 0.9f),
                                assignedAgent.accentColor.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            ) {
                Text(
                    text = assignedAgent.avatarSymbol,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PRZYPISANY OPIEKUN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = assignedAgent.primaryColor
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(assignedAgent.primaryColor)
                    )
                }
                Text(
                    text = assignedAgent.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = assignedAgent.role,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Rząd 2: Dynamiczny status recenzji / Aktywny Recenzent AI
        if (artifact.status == "ZRATYFIKOWANE") {
            // Dzieło zratyfikowane
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF059669).copy(alpha = if (isDark) 0.16f else 0.10f))
                    .border(0.8.dp, Color(0xFF059669).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Zratyfikowane",
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "RECENZJA ZAKOŃCZONA • ZRATYFIKOWANO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    )
                    Text(
                        text = "Oficjalna pieczęć Rady Katedry OtakOS",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else if (reviewingAgent != null) {
            // Aktywny recenzent prowadzi recenzję
            val reviewerColor = reviewingAgent.primaryColor
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        reviewerColor.copy(alpha = if (isDark) 0.18f else 0.12f)
                    )
                    .border(
                        1.dp,
                        reviewerColor.copy(alpha = pulseAlpha * 0.7f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsujący awatar aktywnego recenzenta z radarem
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(26.dp)
                        .drawBehind {
                            // Pulsujący pierścień radaru
                            drawCircle(
                                color = reviewerColor.copy(alpha = (1.4f - radarScale).coerceIn(0f, 0.5f)),
                                radius = size.minDimension * 0.5f * radarScale
                            )
                        }
                        .clip(CircleShape)
                        .background(reviewerColor)
                        .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                ) {
                    Text(
                        text = reviewingAgent.avatarSymbol,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Żywy pulsujący wskaźnik
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(reviewerColor.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AKTYWNA RECENZJA AI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = reviewerColor
                            )
                        )
                    }
                    Text(
                        text = "${reviewingAgent.name}: ${artifact.reviewStatus ?: "Recenzja i optymalizacja"}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Szybka akcja zatwierdzenia recenzji
                if (onCompleteReview != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = reviewerColor.copy(alpha = 0.2f),
                        modifier = Modifier
                            .clickable { onCompleteReview(true) }
                            .testTag("approve_review_btn_${artifact.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Zatwierdź recenzję",
                                tint = reviewerColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Zatwierdź",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = reviewerColor
                            )
                        }
                    }
                }
            }
        } else {
            // Brak aktywnego recenzenta — zaproszenie do recenzji
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isDark) Color(0x30334155) else Color(0x30E2E8F0)
                    )
                    .border(
                        0.8.dp,
                        if (isDark) Color(0x2064748B) else Color(0x40CBD5E1),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { showReviewDialog = true }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Zadanie oczekuje na recenzję agenta AI",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Wyznacz recenzenta ➔",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // Dialog zmiany przypisanego agenta (Lead)
    if (showAssignDialog) {
        AssignAgentDialog(
            currentAgent = assignedAgent,
            onDismiss = { showAssignDialog = false },
            onSelectAgent = { agent ->
                onAssignAgent(agent.name)
                showAssignDialog = false
            }
        )
    }

    // Dialog wyznaczenia aktywnego recenzenta
    if (showReviewDialog) {
        RequestReviewDialog(
            currentReviewer = reviewingAgent,
            assignedAgent = assignedAgent,
            onDismiss = { showReviewDialog = false },
            onRequest = { agent, reviewType ->
                onRequestReview(agent.name, reviewType)
                showReviewDialog = false
            }
        )
    }
}

/**
 * Kompaktowy wskaźnik statusu agenta dla przestrzennych kart na marmurowym stole (SpatialMarbleTable).
 */
@Composable
fun CompactSpatialAgentIndicator(
    artifact: ArtifactEntity,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val assignedAgent = OtakDepartment.getAgentByName(artifact.effectiveAssignedAgent)
    val reviewingAgent = artifact.reviewingAgent?.let { OtakDepartment.getAgentByName(it) }

    val infiniteTransition = rememberInfiniteTransition(label = "compact_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = modifier
            .testTag("compact_agent_indicator_${artifact.id}")
            .clip(RoundedCornerShape(6.dp))
            .background(if (isDark) Color(0x600F172A) else Color(0x60F1F5F9))
            .border(
                0.8.dp,
                if (isDark) Color(0x35334155) else Color(0x50CBD5E1),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Awatar przypisanego opiekuna
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(assignedAgent.primaryColor)
        ) {
            Text(
                text = assignedAgent.avatarSymbol,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Text(
            text = assignedAgent.name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )

        // Jeśli aktywny recenzent jest w trakcie pracy
        if (reviewingAgent != null && artifact.status != "ZRATYFIKOWANE") {
            Spacer(modifier = Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(reviewingAgent.primaryColor.copy(alpha = pulseAlpha))
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(reviewingAgent.primaryColor)
            ) {
                Text(
                    text = reviewingAgent.avatarSymbol,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 7.5.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Text(
                text = "Recenzja",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = reviewingAgent.primaryColor
                )
            )
        }
    }
}

/**
 * Dialog wyboru nowego opiekuna (Assigned Agent) dla zadania.
 */
@Composable
private fun AssignAgentDialog(
    currentAgent: OtakAgent,
    onDismiss: () -> Unit,
    onSelectAgent: (OtakAgent) -> Unit
) {
    val isDark = isSystemInDarkTheme()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AssignmentInd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Wyznacz Opiekuna Zadania",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Wybierz agenta AI z Katedry OtakOS odpowiedzialnego za prowadzenie tego zadania:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                OtakDepartment.agents.forEach { agent ->
                    val isCurrent = agent.id == currentAgent.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isCurrent) agent.primaryColor.copy(alpha = if (isDark) 0.25f else 0.15f)
                                else if (isDark) Color(0x301E293B) else Color(0x30F1F5F9)
                            )
                            .border(
                                width = if (isCurrent) 1.5.dp else 0.8.dp,
                                color = if (isCurrent) agent.primaryColor else if (isDark) Color(0x2064748B) else Color(0x30CBD5E1),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectAgent(agent) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(agent.primaryColor)
                        ) {
                            Text(
                                text = agent.avatarSymbol,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = agent.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isCurrent) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• Aktualny",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = agent.primaryColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                            Text(
                                text = "${agent.role} • ${agent.specialty}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

/**
 * Dialog wyznaczenia aktywnego recenzenta (Reviewing Agent) i rodzaju audytu.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RequestReviewDialog(
    currentReviewer: OtakAgent?,
    assignedAgent: OtakAgent,
    onDismiss: () -> Unit,
    onRequest: (OtakAgent, String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var selectedAgent by remember {
        mutableStateOf(
            currentReviewer
                ?: OtakDepartment.agents.firstOrNull { it.id != assignedAgent.id }
                ?: OtakDepartment.agents.first()
        )
    }

    val reviewTypes = listOf(
        "Audyt Architektury" to "Sprawdzenie modularności i spójności logicznej z jądrem OtakOS",
        "Optymalizacja i Kod" to "Weryfikacja wydajności struktur tensorowych i operacji pamięci",
        "Synteza Teoretyczna" to "Redakcja manifestu, klarowność założeń i styl Katedry",
        "Weryfikacja Konsensusu" to "Test zgodności z regułami Rady Katedry przed ratyfikacją"
    )

    var selectedReviewType by remember { mutableStateOf(reviewTypes.first().first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.RateReview,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Zleć Aktywną Recenzję AI",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Wybierz recenzenta AI oraz cel przeglądu zadania:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "1. Wybierz Agenta Recenzującego:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Lista agentów w siatce / wierszach
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OtakDepartment.agents.forEach { agent ->
                        val isSelected = agent.id == selectedAgent.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) agent.primaryColor.copy(alpha = if (isDark) 0.25f else 0.15f)
                                    else if (isDark) Color(0x301E293B) else Color(0x30F1F5F9)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.8.dp,
                                    color = if (isSelected) agent.primaryColor else if (isDark) Color(0x2064748B) else Color(0x30CBD5E1),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedAgent = agent }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(agent.primaryColor)
                            ) {
                                Text(
                                    text = agent.avatarSymbol,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = agent.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = agent.role,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "2. Zakres Recenzji:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    reviewTypes.forEach { (type, description) ->
                        val isSelected = selectedReviewType == type
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.2f else 0.12f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = if (isSelected) 1.2.dp else 0.6.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else if (isDark) Color(0x2064748B) else Color(0x30CBD5E1),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedReviewType = type }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = type,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onRequest(selectedAgent, selectedReviewType) },
                colors = ButtonDefaults.buttonColors(containerColor = selectedAgent.primaryColor)
            ) {
                Text("Rozpocznij recenzję")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
