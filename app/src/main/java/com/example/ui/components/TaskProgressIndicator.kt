package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ArtifactEntity
import kotlin.math.roundToInt

/**
 * Model reprezentujący postęp ukończenia zadania / artefaktu Katedry OtakOS.
 */
data class TaskProgress(
    val percentage: Int, // 0 - 100
    val fraction: Float, // 0.0f - 1.0f
    val stageTitle: String,
    val stageSubtitle: String,
    val stageIndex: Int, // 0: Inicjacja, 1: Kooperacja, 2: Akceptacja, 3: Ratyfikacja
    val progressColor: Color,
    val isCompleted: Boolean
)

/**
 * Oblicza dynamiczny postęp ukończenia zadania na podstawie statusu,
 * interakcji agentów (liczba iteracji, liczba współpracujących agentów)
 * oraz wskaźnika konsensusu Katedry.
 */
fun calculateTaskProgress(artifact: ArtifactEntity): TaskProgress {
    val collaboratingCount = artifact.collaboratingAgents
        .split(",")
        .map { it.trim() }
        .count { it.isNotEmpty() }

    return when (artifact.status) {
        "ZRATYFIKOWANE" -> TaskProgress(
            percentage = 100,
            fraction = 1.0f,
            stageTitle = "Zratyfikowane",
            stageSubtitle = "Dzieło ukończone z oficjalną pieczęcią Katedry OtakOS",
            stageIndex = 3,
            progressColor = Color(0xFF059669),
            isCompleted = true
        )
        "DO_AKCEPTACJI" -> {
            // Faza gotowości do zatwierdzenia: 86% - 98%
            val consensusBonus = ((artifact.consensusScore.coerceIn(85, 100) - 85) * 0.75f).roundToInt()
            val pct = (88 + consensusBonus).coerceIn(88, 98)
            TaskProgress(
                percentage = pct,
                fraction = pct / 100f,
                stageTitle = "Gotowe do akceptacji",
                stageSubtitle = "Konsensus Katedry: ${artifact.consensusScore}% • Oczekuje na ratyfikację",
                stageIndex = 2,
                progressColor = Color(0xFFD97706),
                isCompleted = false
            )
        }
        "W_OPRACOWANIU" -> {
            // Kooperacja agentów: 30% - 84% zależnie od iteracji i współpracowników
            val iterationBonus = (artifact.iterationsCount * 8).coerceAtMost(32)
            val agentBonus = (collaboratingCount * 6).coerceAtMost(18)
            val consensusContribution = ((artifact.consensusScore - 40).coerceAtLeast(0) * 0.35f).roundToInt()
            val pct = (28 + iterationBonus + agentBonus + consensusContribution).coerceIn(32, 84)

            val agentsDesc = if (collaboratingCount > 0) {
                "$collaboratingCount współpracujących agentów"
            } else {
                "Koordynacja pętli myślowej"
            }

            TaskProgress(
                percentage = pct,
                fraction = pct / 100f,
                stageTitle = "Kooperacja agentów",
                stageSubtitle = "Iteracja #${artifact.iterationsCount} • $agentsDesc • Konsensus ${artifact.consensusScore}%",
                stageIndex = 1,
                progressColor = Color(0xFFA855F7),
                isCompleted = false
            )
        }
        else -> {
            // "NA_STOLE" - Inicjalna propozycja
            val iterExtra = ((artifact.iterationsCount - 1) * 4).coerceAtMost(10)
            val pct = (12 + iterExtra + (artifact.consensusScore / 15)).coerceIn(12, 26)
            TaskProgress(
                percentage = pct,
                fraction = pct / 100f,
                stageTitle = "Inicjacja zadania",
                stageSubtitle = "Położono na stole • Propozycja: ${artifact.proposingAgent}",
                stageIndex = 0,
                progressColor = Color(0xFF0284C7),
                isCompleted = false
            )
        }
    }
}

/**
 * Pełnowymiarowy, wysoce estetyczny wskaźnik postępu ukończenia zadania
 * przeznaczony dla głównych kart artefaktów (ArtifactCard) oraz okna szczegółów.
 */
@Composable
fun TaskCardProgressSection(
    artifact: ArtifactEntity,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val progress = calculateTaskProgress(artifact)

    // Płynna animacja zmiany postępu przy każdej interakcji agenta
    val animatedProgress by animateFloatAsState(
        targetValue = progress.fraction,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "task_progress_fraction"
    )

    val animatedColor by animateColorAsState(
        targetValue = progress.progressColor,
        animationSpec = tween(durationMillis = 500),
        label = "task_progress_color"
    )

    // Delikatna fala światła na pasku postępu dla aktywnych zadań
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_progress")
    val pulseShimmer by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    val stages = listOf(
        ProgressStageStep("Inicjacja", Icons.Default.Lightbulb),
        ProgressStageStep("Kooperacja", Icons.Default.AutoAwesome),
        ProgressStageStep("Akceptacja", Icons.Default.HourglassTop),
        ProgressStageStep("Ratyfikacja", Icons.Default.CheckCircle)
    )

    Column(
        modifier = modifier
            .testTag("task_progress_section_${artifact.id}")
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDark) Color(0x700F172A) else Color(0x60F1F5F9)
            )
            .border(
                1.dp,
                if (isDark) Color(0x35334155) else Color(0x50CBD5E1),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        // Górny wiersz: Tytuł wskaźnika i dynamiczny licznik procentowy
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(animatedColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "POSTĘP UKOŃCZENIA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Procent ukończenia ze stylowym tłem
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = animatedColor.copy(alpha = if (isDark) 0.22f else 0.15f),
                modifier = Modifier.border(
                    0.8.dp,
                    animatedColor.copy(alpha = 0.4f),
                    RoundedCornerShape(8.dp)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${progress.percentage}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = animatedColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Główny zaokrąglony pasek postępu z gradientem i podświetleniem kaustycznym
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                animatedColor.copy(alpha = 0.75f),
                                animatedColor,
                                if (progress.isCompleted) Color(0xFF34D399) else animatedColor.copy(alpha = pulseShimmer)
                            )
                        )
                    )
                    .drawBehind {
                        // Poświata na czole paska
                        if (size.width > 10f) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.4f),
                                radius = size.height * 0.8f,
                                center = Offset(size.width - size.height * 0.5f, size.height * 0.5f)
                            )
                        }
                    }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4-etapowa ścieżka milowych kroków (Inicjacja -> Kooperacja -> Akceptacja -> Ratyfikacja)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            stages.forEachIndexed { index, stage ->
                val isPassed = index < progress.stageIndex
                val isCurrent = index == progress.stageIndex
                val stepColor = when {
                    isPassed -> animatedColor
                    isCurrent -> animatedColor
                    else -> if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = if (index == 0) Arrangement.Start else if (index == stages.lastIndex) Arrangement.End else Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrent) animatedColor.copy(alpha = 0.25f)
                                else if (isPassed) animatedColor.copy(alpha = 0.15f)
                                else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                            )
                            .border(
                                width = if (isCurrent) 1.5.dp else 1.dp,
                                color = stepColor,
                                shape = CircleShape
                            )
                    ) {
                        if (isPassed || (isCurrent && progress.isCompleted)) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = stepColor,
                                modifier = Modifier.size(9.dp)
                            )
                        } else if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(stepColor)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stage.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isCurrent) animatedColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isPassed) 0.9f else 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Dynamiczny opis aktualnego stanu wygenerowany przez współpracę agentów
        Text(
            text = progress.stageSubtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                lineHeight = 13.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            maxLines = 1
        )
    }
}

/**
 * Kompaktowy wskaźnik postępu ukończenia zadania na przestrzennym blacie marmurowym
 * (przeznaczony dla DraggableSpatialCard).
 */
@Composable
fun CompactTaskProgressIndicator(
    artifact: ArtifactEntity,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val progress = calculateTaskProgress(artifact)

    val animatedProgress by animateFloatAsState(
        targetValue = progress.fraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "compact_task_progress"
    )

    val animatedColor by animateColorAsState(
        targetValue = progress.progressColor,
        animationSpec = tween(durationMillis = 400),
        label = "compact_task_color"
    )

    Column(
        modifier = modifier
            .testTag("compact_task_progress_${artifact.id}")
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(animatedColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = progress.stageTitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${progress.percentage}%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = animatedColor
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Pasek postępu z poświatą
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                animatedColor.copy(alpha = 0.8f),
                                animatedColor
                            )
                        )
                    )
            )
        }
    }
}

private data class ProgressStageStep(
    val label: String,
    val icon: ImageVector
)
