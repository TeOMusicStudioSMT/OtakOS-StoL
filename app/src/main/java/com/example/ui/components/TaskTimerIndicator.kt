package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ArtifactEntity
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Poziom prędkości realizacji zadania (Team Velocity Rating).
 * Pomaga zarządzać przepustowością zespołu agentów AI.
 */
enum class TaskVelocityRating(
    val label: String,
    val localizedLabel: String,
    val color: Color,
    val badgeColor: Color,
    val iconVector: ImageVector
) {
    HIGH(
        label = "High Velocity",
        localizedLabel = "Wysoka prędkość",
        color = Color(0xFF10B981), // Emerald
        badgeColor = Color(0xFF059669),
        iconVector = Icons.Default.Bolt
    ),
    OPTIMAL(
        label = "Optimal Pace",
        localizedLabel = "Optymalne tempo",
        color = Color(0xFF0284C7), // Sky / Cyan
        badgeColor = Color(0xFF0369A1),
        iconVector = Icons.Default.Speed
    ),
    EXTENDED(
        label = "Extended Work",
        localizedLabel = "Wydłużony czas",
        color = Color(0xFFF59E0B), // Amber
        badgeColor = Color(0xFFD97706),
        iconVector = Icons.Default.HourglassBottom
    )
}

/**
 * Metryki czasu i prędkości zespołu dla danego zadania AI.
 */
data class TaskVelocityMetrics(
    val totalSeconds: Long,
    val formattedDuration: String,
    val isRunning: Boolean,
    val activeAgentName: String,
    val velocityRating: TaskVelocityRating,
    val averageSecondsPerIteration: Long,
    val formattedPace: String,
    val velocityStatusText: String,
    val velocityBadgeText: String
)

/**
 * Oblicza metryki velocity i formatuje czas dla zadania.
 */
fun computeTaskVelocity(artifact: ArtifactEntity, currentElapsedSeconds: Long = artifact.currentEffectiveSeconds): TaskVelocityMetrics {
    val totalSec = currentElapsedSeconds.coerceAtLeast(0L)
    val formatted = formatTimerDuration(totalSec)
    val iterations = artifact.iterationsCount.coerceAtLeast(1)
    val avgPerIteration = totalSec / iterations
    val avgPaceFormatted = if (avgPerIteration < 60) {
        "${avgPerIteration}s / iterację"
    } else {
        "${avgPerIteration / 60}m ${(avgPerIteration % 60)}s / iterację"
    }

    val priority = TaskPriority.fromString(artifact.priority)
    val benchmarkSeconds = when (priority) {
        TaskPriority.HIGH -> 3600L // 60 min
        TaskPriority.MEDIUM -> 2400L // 40 min
        TaskPriority.LOW -> 1200L // 20 min
    }

    val velocityRating = when {
        totalSec <= 0L -> TaskVelocityRating.OPTIMAL
        totalSec <= (benchmarkSeconds * 0.65).toLong() -> TaskVelocityRating.HIGH
        totalSec <= (benchmarkSeconds * 1.25).toLong() -> TaskVelocityRating.OPTIMAL
        else -> TaskVelocityRating.EXTENDED
    }

    val (statusText, badgeText) = when (velocityRating) {
        TaskVelocityRating.HIGH -> "Agenci pracują z wysoką efektywnością (+35% vs benchmark)." to "⚡ Szybkie tempo"
        TaskVelocityRating.OPTIMAL -> "Zadanie realizowane w zrównoważonym oknie velocity zespołu." to "⏱️ Optymalne tempo"
        TaskVelocityRating.EXTENDED -> "Wydłużony czas pracy – zadanie wymaga uwagi lub wsparcia kolejnego agenta." to "⚠️ Wydłużony czas"
    }

    return TaskVelocityMetrics(
        totalSeconds = totalSec,
        formattedDuration = formatted,
        isRunning = artifact.isTimerRunning,
        activeAgentName = artifact.activeTrackingAgent,
        velocityRating = velocityRating,
        averageSecondsPerIteration = avgPerIteration,
        formattedPace = avgPaceFormatted,
        velocityStatusText = statusText,
        velocityBadgeText = badgeText
    )
}

/**
 * Formatuje liczbę sekund do czytelnej postaci HH:mm:ss lub mm:ss.
 */
fun formatTimerDuration(totalSeconds: Long): String {
    val sec = totalSeconds.coerceAtLeast(0L)
    val hours = sec / 3600
    val minutes = (sec % 3600) / 60
    val seconds = sec % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

/**
 * Sekcja stopera czasu pracy agentów zintegrowana w karcie zadania (ArtifactCard).
 * Zawiera zegar HUD, wskaźnik aktywnego agenta, wskaźnik velocity i przyciski sterowania.
 */
@Composable
fun TaskCardTimerSection(
    artifact: ArtifactEntity,
    onToggleTimer: () -> Unit,
    onResetTimer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Lokalny zegar dla płynnego odświeżania co sekundę bez ciągłych zapisów do bazy
    var liveSeconds by remember(artifact.id, artifact.timeSpentSeconds, artifact.isTimerRunning, artifact.timerStartedTimestamp) {
        mutableLongStateOf(artifact.currentEffectiveSeconds)
    }

    LaunchedEffect(artifact.isTimerRunning, artifact.timerStartedTimestamp) {
        if (artifact.isTimerRunning && artifact.timerStartedTimestamp != null) {
            while (true) {
                delay(1000)
                val elapsedSinceStart = ((System.currentTimeMillis() - artifact.timerStartedTimestamp) / 1000).coerceAtLeast(0)
                liveSeconds = artifact.timeSpentSeconds + elapsedSinceStart
            }
        } else {
            liveSeconds = artifact.timeSpentSeconds
        }
    }

    val velocity = computeTaskVelocity(artifact, liveSeconds)

    val infiniteTransition = rememberInfiniteTransition(label = "timer_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val hudBackground = if (isDark) {
        if (artifact.isTimerRunning) Color(0x350284C7) else Color(0x200F172A)
    } else {
        if (artifact.isTimerRunning) Color(0x180284C7) else Color(0x10F1F5F9)
    }

    val hudBorder = if (artifact.isTimerRunning) {
        Color(0xFF0284C7).copy(alpha = if (isDark) 0.6f else 0.45f)
    } else {
        if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFCBD5E1).copy(alpha = 0.6f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(hudBackground)
            .border(1.dp, hudBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("task_timer_section_${artifact.id}")
    ) {
        Column {
            // Górny wiersz: Etykieta statusu stopera + Wskaźnik prędkości zespołu (Velocity)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (artifact.isTimerRunning) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = pulseAlpha))
                        )
                        Text(
                            text = "AI PRACUJE: ${velocity.activeAgentName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                letterSpacing = 0.4.sp
                            ),
                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF64748B))
                        )
                        Text(
                            text = if (liveSeconds > 0) "CZAS ZAPISANY (${velocity.activeAgentName})" else "CZASOMIERZ ZADANIA",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Team Velocity Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(velocity.velocityRating.color.copy(alpha = 0.15f))
                        .border(0.8.dp, velocity.velocityRating.color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("velocity_badge_${artifact.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = velocity.velocityRating.iconVector,
                            contentDescription = null,
                            tint = velocity.velocityRating.color,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = velocity.velocityBadgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp
                            ),
                            color = velocity.velocityRating.color
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Główny wiersz: Cyfry czasu (Monospace HUD) + Pace + Przyciski sterowania
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cyfrowy licznik HUD
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF0B121F) else Color(0xFFE2E8F0))
                            .border(1.dp, if (artifact.isTimerRunning) Color(0xFF0284C7) else Color.Transparent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = velocity.formattedDuration,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = if (artifact.isTimerRunning) {
                                if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            letterSpacing = 1.sp,
                            modifier = Modifier.testTag("timer_display_${artifact.id}")
                        )
                    }

                    Column {
                        Text(
                            text = "Tempo: ${velocity.formattedPace}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (artifact.isTimerRunning) "Rejestracja w toku" else "${artifact.iterationsCount} iteracji",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (artifact.isTimerRunning) Color(0xFF10B981) else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Przyciski akcji stopera
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onResetTimer != null && liveSeconds > 0) {
                        IconButton(
                            onClick = onResetTimer,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("task_timer_reset_${artifact.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Zresetuj czas",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Główny przycisk Start / Pauza
                    FilledTonalButton(
                        onClick = onToggleTimer,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (artifact.isTimerRunning) {
                                Color(0xFFF59E0B).copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            contentColor = if (artifact.isTimerRunning) {
                                Color(0xFFF59E0B)
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        ),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("task_timer_toggle_${artifact.id}"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = if (artifact.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (artifact.isTimerRunning) "Wstrzymaj stoper" else "Uruchom stoper",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (artifact.isTimerRunning) "Wstrzymaj" else if (liveSeconds > 0) "Wznów" else "Start",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Kompaktowy pasek stopera dla karty przestrzennej (DraggableSpatialCard na stole).
 */
@Composable
fun CompactSpatialCardTimer(
    artifact: ArtifactEntity,
    onToggleTimer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    var liveSeconds by remember(artifact.id, artifact.timeSpentSeconds, artifact.isTimerRunning, artifact.timerStartedTimestamp) {
        mutableLongStateOf(artifact.currentEffectiveSeconds)
    }

    LaunchedEffect(artifact.isTimerRunning, artifact.timerStartedTimestamp) {
        if (artifact.isTimerRunning && artifact.timerStartedTimestamp != null) {
            while (true) {
                delay(1000)
                val elapsedSinceStart = ((System.currentTimeMillis() - artifact.timerStartedTimestamp) / 1000).coerceAtLeast(0)
                liveSeconds = artifact.timeSpentSeconds + elapsedSinceStart
            }
        } else {
            liveSeconds = artifact.timeSpentSeconds
        }
    }

    val velocity = computeTaskVelocity(artifact, liveSeconds)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (artifact.isTimerRunning) Color(0x250284C7) else if (isDark) Color(0x1F000000) else Color(0x15F1F5F9))
            .border(
                0.8.dp,
                if (artifact.isTimerRunning) Color(0xFF0284C7).copy(alpha = 0.5f) else Color(0x2564748B),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("spatial_timer_row_${artifact.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (artifact.isTimerRunning) Icons.Default.Timer else Icons.Default.HourglassBottom,
                contentDescription = null,
                tint = if (artifact.isTimerRunning) Color(0xFF0284C7) else Color(0xFF94A3B8),
                modifier = Modifier.size(12.dp)
            )

            Text(
                text = velocity.formattedDuration,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (artifact.isTimerRunning) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("spatial_timer_text_${artifact.id}")
            )

            Text(
                text = "• ${velocity.velocityBadgeText}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                color = velocity.velocityRating.color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (onToggleTimer != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onToggleTimer)
                    .background(if (artifact.isTimerRunning) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFF0284C7).copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .testTag("spatial_timer_toggle_${artifact.id}")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (artifact.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (artifact.isTimerRunning) Color(0xFFF59E0B) else Color(0xFF0284C7),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (artifact.isTimerRunning) "Pauza" else "Czas",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                        color = if (artifact.isTimerRunning) Color(0xFFF59E0B) else Color(0xFF0284C7)
                    )
                }
            }
        }
    }
}

/**
 * Rozbudowany panel czasu i prędkości zespołu dla dialogu podglądu zadania (ArtifactPreviewDialog).
 */
@Composable
fun DetailedTaskVelocityTimerPanel(
    artifact: ArtifactEntity,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onAdjustTime: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    var liveSeconds by remember(artifact.id, artifact.timeSpentSeconds, artifact.isTimerRunning, artifact.timerStartedTimestamp) {
        mutableLongStateOf(artifact.currentEffectiveSeconds)
    }

    LaunchedEffect(artifact.isTimerRunning, artifact.timerStartedTimestamp) {
        if (artifact.isTimerRunning && artifact.timerStartedTimestamp != null) {
            while (true) {
                delay(1000)
                val elapsedSinceStart = ((System.currentTimeMillis() - artifact.timerStartedTimestamp) / 1000).coerceAtLeast(0)
                liveSeconds = artifact.timeSpentSeconds + elapsedSinceStart
            }
        } else {
            liveSeconds = artifact.timeSpentSeconds
        }
    }

    val velocity = computeTaskVelocity(artifact, liveSeconds)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF0E1726) else Color(0xFFF8FAFC))
            .border(1.2.dp, if (artifact.isTimerRunning) Color(0xFF0284C7) else Color(0xFF334155).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("detailed_timer_panel_${artifact.id}")
    ) {
        // Nagłówek sekcji
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REJESTRATOR CZASU & VELOCITY ZESPOŁU",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(velocity.velocityRating.color.copy(alpha = 0.15f))
                    .border(1.dp, velocity.velocityRating.color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = velocity.velocityRating.iconVector,
                        contentDescription = null,
                        tint = velocity.velocityRating.color,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = velocity.velocityRating.localizedLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = velocity.velocityRating.color
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Duży zegar HUD + przycisk główny
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = velocity.formattedDuration,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    color = if (artifact.isTimerRunning) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = if (artifact.isTimerRunning) "Aktywny agent: ${velocity.activeAgentName}" else "Ostatni opiekun: ${velocity.activeAgentName}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (liveSeconds > 0) {
                    OutlinedButton(
                        onClick = onResetTimer,
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("detailed_timer_reset_${artifact.id}"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 12.sp)
                    }
                }

                ElevatedButton(
                    onClick = onToggleTimer,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = if (artifact.isTimerRunning) Color(0xFFF59E0B) else Color(0xFF0284C7),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("detailed_timer_toggle_${artifact.id}"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = if (artifact.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (artifact.isTimerRunning) "Wstrzymaj" else "Uruchom",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Siatka metryk velocity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0xFF131D2E) else Color(0xFFEDF2F7))
                    .padding(8.dp)
            ) {
                Column {
                    Text("ŚREDNIE TEMPO", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.outline)
                    Text(velocity.formattedPace, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0xFF131D2E) else Color(0xFFEDF2F7))
                    .padding(8.dp)
            ) {
                Column {
                    Text("STATUS VELOCITY", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.outline)
                    Text(velocity.velocityBadgeText, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = velocity.velocityRating.color)
                }
            }
        }

        if (onAdjustTime != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Szybka korekta:",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onAdjustTime(300L) }
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("+5 min", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onAdjustTime(900L) }
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("+15 min", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = velocity.velocityStatusText,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Podsumowanie czasu realizacji i prędkości dla karty w Izbie Akceptacji (RatificationView).
 */
@Composable
fun RatificationTimerSummary(
    artifact: ArtifactEntity,
    modifier: Modifier = Modifier
) {
    val velocity = computeTaskVelocity(artifact)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0284C7).copy(alpha = 0.08f))
            .border(0.8.dp, Color(0xFF0284C7).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("ratification_timer_summary_${artifact.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = Color(0xFF0284C7),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Czas realizacji: ${velocity.formattedDuration}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = velocity.velocityRating.iconVector,
                contentDescription = null,
                tint = velocity.velocityRating.color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = velocity.velocityRating.localizedLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = velocity.velocityRating.color
            )
        }
    }
}
