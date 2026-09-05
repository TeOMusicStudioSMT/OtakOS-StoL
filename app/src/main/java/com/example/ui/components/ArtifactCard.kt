package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ArtifactEntity
import com.example.ui.model.OtakDepartment

@Composable
fun ArtifactCard(
    artifact: ArtifactEntity,
    onPreview: () -> Unit,
    onAgentCollaborate: () -> Unit,
    onDirectRatify: (() -> Unit)? = null,
    onAssignAgent: ((String) -> Unit)? = null,
    onRequestReview: ((String, String) -> Unit)? = null,
    onCompleteReview: ((Boolean) -> Unit)? = null,
    onToggleTimer: (() -> Unit)? = null,
    onResetTimer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val agent = OtakDepartment.getAgentByName(artifact.proposingAgent)

    val (statusColor, statusLabel) = when (artifact.status) {
        "NA_STOLE" -> Color(0xFF0284C7) to "NA STOLE"
        "W_OPRACOWANIU" -> Color(0xFF7C3AED) to "W OPRACOWANIU"
        "DO_AKCEPTACJI" -> Color(0xFFD97706) to "DO AKCEPTACJI"
        "ZRATYFIKOWANE" -> Color(0xFF059669) to "ZRATYFIKOWANE"
        else -> MaterialTheme.colorScheme.primary to artifact.status
    }

    // Glassmorphic translucent crystal card background
    val cardBackground = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xF0182234),
                Color(0xDC111827),
                Color(0xE8161E2E)
            ),
            start = Offset(0f, 0f),
            end = Offset(800f, 1000f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFAFFFFFF),
                Color(0xF0F8FAFC),
                Color(0xEBF1F5F9)
            ),
            start = Offset(0f, 0f),
            end = Offset(800f, 1000f)
        )
    }

    // Specular border: top-left highlight and bottom-right shadow (efekt szkła optycznego)
    val specularBorderBrush = Brush.linearGradient(
        colors = listOf(
            if (isDark) Color(0x90FFFFFF) else Color(0xD0FFFFFF),
            if (isDark) Color(0x3094A3B8) else Color(0x50CBD5E1),
            statusColor.copy(alpha = 0.45f),
            if (isDark) Color(0x15000000) else Color(0x25000000)
        ),
        start = Offset(0f, 0f),
        end = Offset(800f, 900f)
    )

    Box(
        modifier = modifier
            .testTag("artifact_card_${artifact.id}")
            .fillMaxWidth()
            // Advanced Multi-layer Shadow & Caustic Light Spill onto the marble stone
            .drawBehind {
                val cardWidth = size.width
                val cardHeight = size.height

                // 1. Ambient Occlusion shadow directly underneath
                drawRoundRect(
                    color = if (isDark) Color(0x80000000) else Color(0x30000000),
                    topLeft = Offset(0f, 6f),
                    size = Size(cardWidth, cardHeight),
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                )

                // 2. Directional soft drop shadow from overhead table light
                drawRoundRect(
                    color = if (isDark) Color(0x50020617) else Color(0x1F0F172A),
                    topLeft = Offset(2f, 12f),
                    size = Size(cardWidth, cardHeight),
                    cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
                )

                // 3. Caustic Light Spill (Opalizujący barwny blask rzucany przez artefakt na marmurowy stół)
                val causticBrush = Brush.radialGradient(
                    colors = listOf(
                        statusColor.copy(alpha = if (isDark) 0.28f else 0.16f),
                        statusColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(cardWidth * 0.5f, cardHeight * 0.6f),
                    radius = cardWidth * 0.65f
                )
                drawRoundRect(
                    brush = causticBrush,
                    topLeft = Offset(-10f, 4f),
                    size = Size(cardWidth + 20f, cardHeight + 16f),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                    blendMode = BlendMode.Plus
                )
            }
            .clip(RoundedCornerShape(20.dp))
            .background(cardBackground)
            .border(1.5.dp, specularBorderBrush, RoundedCornerShape(20.dp))
            .clickable(onClick = onPreview)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Top Row: Category + Priority + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category & Agent Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val categoryColor = getCategoryChartColor(artifact.category)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(categoryColor.copy(alpha = 0.15f))
                            .border(0.8.dp, categoryColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                            .testTag("task_category_tag_${artifact.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = artifact.category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp
                                ),
                                color = categoryColor
                            )
                        }
                    }

                    val priorityInfo = TaskPriority.fromString(artifact.priority)
                    TaskPriorityBadge(
                        priority = priorityInfo,
                        testTagSuffix = "${artifact.id}"
                    )
                }

                // Status Badge with Glowing Dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = statusColor
                    )
                }
            }

            // Urgent Agent Focus Banner for High Priority Tasks
            val priorityInfo = TaskPriority.fromString(artifact.priority)
            if (priorityInfo == TaskPriority.HIGH) {
                Spacer(modifier = Modifier.height(10.dp))
                UrgentAgentFocusBanner(modifier = Modifier.testTag("urgent_banner_${artifact.id}"))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = artifact.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
                text = artifact.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Digital Payload Preview Box (Monospace code / formula snippet)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color(0xFF0C111A) else Color(0xFFF1F5F9))
                    .border(1.dp, if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = artifact.digitalPayload.lines().take(3).joinToString("\n"),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF334155),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Status Indicator for Assigned Lead & Active Reviewing AI Agent
            TaskAgentStatusIndicator(
                artifact = artifact,
                onAssignAgent = { onAssignAgent?.invoke(it) },
                onRequestReview = { agentName, type -> onRequestReview?.invoke(agentName, type) },
                onCompleteReview = onCompleteReview
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Task Completion Progress Indicator reflecting agent interactions
            TaskCardProgressSection(artifact = artifact)

            Spacer(modifier = Modifier.height(10.dp))

            // Task Timer & Team Velocity Tracker
            TaskCardTimerSection(
                artifact = artifact,
                onToggleTimer = { onToggleTimer?.invoke() },
                onResetTimer = onResetTimer
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    modifier = Modifier
                        .testTag("preview_button_${artifact.id}")
                        .height(36.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Podgląd",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Podgląd", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (artifact.status == "DO_AKCEPTACJI" && onDirectRatify != null) {
                    ElevatedButton(
                        onClick = onDirectRatify,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFFD97706),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .testTag("ratify_action_${artifact.id}")
                            .height(36.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Ratyfikuj",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Zatwierdź", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (artifact.status != "ZRATYFIKOWANE") {
                    FilledTonalButton(
                        onClick = onAgentCollaborate,
                        modifier = Modifier
                            .testTag("collaborate_button_${artifact.id}")
                            .height(36.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Agent pracuj",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Współpraca", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
