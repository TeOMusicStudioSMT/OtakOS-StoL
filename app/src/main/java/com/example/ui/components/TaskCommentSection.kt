package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ArtifactEntity
import com.example.data.entity.TaskCommentEntity
import com.example.ui.model.OtakAgent
import com.example.ui.model.OtakDepartment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sekcja komentarzy i dyskusji agentów wbudowana w kartę zadania (oraz podgląd zadania).
 * Umożliwia agentom AI oraz użytkownikowi wymianę spostrzeżeń, uwag technicznych i feedbacku.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskCommentSection(
    artifact: ArtifactEntity,
    comments: List<TaskCommentEntity>,
    onAddComment: (artifactId: Long, authorName: String, authorRole: String, type: String, content: String) -> Unit,
    onGenerateAgentFeedback: (artifact: ArtifactEntity, agentName: String, userPrompt: String?, type: String?) -> Unit,
    onDeleteComment: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    maxCompactListHeight: Boolean = false
) {
    val isDark = isSystemInDarkTheme()
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    var newCommentText by remember { mutableStateOf("") }
    var selectedCommentType by remember { mutableStateOf("DISCUSSION") }
    var selectedAuthorMode by remember { mutableStateOf("USER") } // "USER" or agent name
    var isAgentPromptDialogOpen by remember { mutableStateOf(false) }

    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "chevron")

    val relevantComments = remember(comments, artifact.id) {
        comments.filter { it.artifactId == artifact.id }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isDark) Color(0xFF131B2A).copy(alpha = 0.6f)
                else Color(0xFFF8FAFC)
            )
            .border(
                width = 1.dp,
                color = if (isDark) Color(0x3338BDF8) else Color(0x220284C7),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(12.dp)
            .testTag("task_comment_section_${artifact.id}")
    ) {
        // Nagłówek sekcji dyskusji (zawsze widoczny)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Dyskusja i Feedback Agentów",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Licznik komentarzy
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (relevantComments.isNotEmpty()) Color(0xFF0284C7).copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${relevantComments.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (relevantComments.isNotEmpty()) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = if (relevantComments.isEmpty()) "Brak wpisów • Kliknij, aby otworzyć debatę"
                        else "Ostatni wpis: ${relevantComments.last().authorName}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Szybki przycisk poproszenia agenta o feedback bez konieczności rozwijania
                IconButton(
                    onClick = {
                        isExpanded = true
                        isAgentPromptDialogOpen = true
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("quick_agent_feedback_btn_${artifact.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Zaproś agenta do oceny",
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Zwiń dyskusję" else "Rozwiń dyskusję",
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }
        }

        // Rozwijana zawartość dyskusji
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                // Lista komentarzy
                if (relevantComments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0x221E293B) else Color(0xFFF1F5F9))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Brak wpisów w dyskusji nad tym zadaniem",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FilledTonalButton(
                                onClick = { isAgentPromptDialogOpen = true },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Poproś agenta o pierwszą opinię", fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    val columnModifier = if (maxCompactListHeight) {
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    } else {
                        Modifier.fillMaxWidth()
                    }

                    Column(
                        modifier = columnModifier,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        relevantComments.forEach { comment ->
                            CommentItemCard(
                                comment = comment,
                                onDelete = onDeleteComment?.let { { it(comment.id) } }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pasek zaproszenia agentów do dyskusji
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0x331E293B) else Color(0xFFF1F5F9))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Zaproś agenta do oceny zadania:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Kliknij agenta poniżej",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFF7C3AED)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OtakDepartment.agents.forEach { agent ->
                            AgentQuickChip(
                                agent = agent,
                                onClick = {
                                    onGenerateAgentFeedback(artifact, agent.name, null, null)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Formularz dodawania własnego komentarza lub ukierunkowanego pytania
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    // Wybór typu wpisu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Typ wpisu:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        listOf(
                            "DISCUSSION" to "Dyskusja",
                            "FEEDBACK" to "Feedback",
                            "SUGGESTION" to "Sugestia",
                            "CRITIQUE" to "Krytyka"
                        ).forEach { (typeKey, typeLabel) ->
                            val isSelected = selectedCommentType == typeKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) getCommentTypeColor(typeKey).copy(alpha = 0.18f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) getCommentTypeColor(typeKey) else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedCommentType = typeKey }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = typeLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) getCommentTypeColor(typeKey) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            placeholder = {
                                Text(
                                    "Zadaj pytanie lub skomentuj stan prac...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("comment_input_${artifact.id}"),
                            shape = RoundedCornerShape(8.dp),
                            maxLines = 3,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0284C7),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Przycisk wysyłki komentarza
                        IconButton(
                            onClick = {
                                if (newCommentText.isNotBlank()) {
                                    val author = if (selectedAuthorMode == "USER") "Przewodniczący Katedry" else selectedAuthorMode
                                    val role = if (selectedAuthorMode == "USER") "Katedra OtakOS"
                                    else OtakDepartment.getAgentByName(selectedAuthorMode).role

                                    onAddComment(
                                        artifact.id,
                                        author,
                                        role,
                                        selectedCommentType,
                                        newCommentText.trim()
                                    )
                                    newCommentText = ""
                                }
                            },
                            enabled = newCommentText.isNotBlank(),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (newCommentText.isNotBlank()) Color(0xFF0284C7)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .testTag("send_comment_btn_${artifact.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Wyślij komentarz",
                                tint = if (newCommentText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal dialog wyboru agenta do dedykowanego feedbacku z opcjonalnym pytaniem użytkownika
    if (isAgentPromptDialogOpen) {
        AgentFeedbackPromptDialog(
            artifact = artifact,
            onDismiss = { isAgentPromptDialogOpen = false },
            onConfirm = { agentName, prompt, type ->
                onGenerateAgentFeedback(artifact, agentName, prompt, type)
                isAgentPromptDialogOpen = false
            }
        )
    }
}

/**
 * Pojedynczy kafelek komentarza w liście dyskusji.
 */
@Composable
private fun CommentItemCard(
    comment: TaskCommentEntity,
    onDelete: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val typeColor = getCommentTypeColor(comment.commentType)
    val agent = OtakDepartment.agents.find { it.name == comment.authorName }
    val authorColor = agent?.primaryColor ?: Color(0xFF0284C7)
    val symbol = agent?.avatarSymbol ?: "✦"

    val timeFormatted = remember(comment.timestamp) {
        val diff = System.currentTimeMillis() - comment.timestamp
        when {
            diff < 60_000 -> "przed chwilą"
            diff < 3600_000 -> "${diff / 60_000} min temu"
            diff < 86400_000 -> "${diff / 3600_000} godz. temu"
            else -> SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(comment.timestamp))
        }
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF182234) else Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.8.dp, typeColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Nagłówek komentarza (autor, odznaka roli, typ wpisu, czas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Awatar autora
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(authorColor.copy(alpha = 0.2f))
                            .border(1.dp, authorColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = symbol,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = authorColor
                        )
                    }

                    Spacer(modifier = Modifier.width(7.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = comment.authorName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Badge typu
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(typeColor.copy(alpha = 0.16f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = getCommentTypeLabel(comment.commentType),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = typeColor
                                )
                            }
                        }

                        Text(
                            text = "${comment.authorRole} • $timeFormatted",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Usuń komentarz",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Treść komentarza
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Chip szybkiego wywołania agenta do opinii.
 */
@Composable
private fun AgentQuickChip(
    agent: OtakAgent,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = agent.primaryColor.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, agent.primaryColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .clickable { onClick() }
            .testTag("agent_feedback_chip_${agent.name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = agent.avatarSymbol,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = agent.primaryColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = agent.name,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = agent.primaryColor
            )
        }
    }
}

/**
 * Dialog wygenerowania dedykowanego feedbacku od wybranego agenta.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgentFeedbackPromptDialog(
    artifact: ArtifactEntity,
    onDismiss: () -> Unit,
    onConfirm: (agentName: String, userPrompt: String?, type: String) -> Unit
) {
    var selectedAgentName by remember { mutableStateOf(artifact.effectiveAssignedAgent) }
    var selectedType by remember { mutableStateOf("FEEDBACK") }
    var customQuestion by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Poproś Agenta o Feedback",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Zamknij")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Zadanie: \"${artifact.title}\"",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Wybierz instancję AI:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OtakDepartment.agents.forEach { agent ->
                        val isSelected = selectedAgentName == agent.name
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAgentName = agent.name },
                            label = { Text("${agent.avatarSymbol} ${agent.name}", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = agent.primaryColor.copy(alpha = 0.2f),
                                selectedLabelColor = agent.primaryColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tryb wypowiedzi:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "FEEDBACK" to "Ocena stanu",
                        "SUGGESTION" to "Sugestia",
                        "CRITIQUE" to "Krytyka",
                        "APPROVAL" to "Aprobata"
                    ).forEach { (key, label) ->
                        val isSelected = selectedType == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) getCommentTypeColor(key).copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) getCommentTypeColor(key) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedType = key }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) getCommentTypeColor(key) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Opcjonalne pytanie / kontekst (opcjonalnie):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = customQuestion,
                    onValueChange = { customQuestion = it },
                    placeholder = { Text("np. Czy ten algorytm jest bezpieczny pod obciążeniem?", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 2,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Anuluj", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledTonalButton(
                        onClick = {
                            onConfirm(
                                selectedAgentName,
                                customQuestion.ifBlank { null },
                                selectedType
                            )
                        }
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generuj Feedback", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun getCommentTypeColor(type: String): Color {
    return when (type) {
        "FEEDBACK" -> Color(0xFF0284C7) // Błękit / Sky
        "SUGGESTION" -> Color(0xFFD97706) // Bursztyn / Amber
        "CRITIQUE" -> Color(0xFFE11D48) // Czerwień / Rose
        "APPROVAL" -> Color(0xFF059669) // Szmaragd / Emerald
        "DISCUSSION" -> Color(0xFF7C3AED) // Fiolet / Violet
        else -> Color(0xFF475569)
    }
}

private fun getCommentTypeLabel(type: String): String {
    return when (type) {
        "FEEDBACK" -> "FEEDBACK"
        "SUGGESTION" -> "SUGESTIA"
        "CRITIQUE" -> "KRYTYKA"
        "APPROVAL" -> "APROBATA"
        "DISCUSSION" -> "DYSKUSJA"
        else -> "WPIS"
    }
}
