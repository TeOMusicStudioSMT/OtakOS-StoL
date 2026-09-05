package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.entity.ActionLogEntity
import com.example.data.entity.ArtifactEntity
import com.example.ui.model.OtakAgent
import com.example.ui.model.OtakDepartment
import kotlin.math.roundToInt

/**
 * Interaktywny stół z perłowego marmuru umożliwiający swobodne przeciąganie (Drag & Drop)
 * i przestrzenne rozmieszczanie zadań przez agentów Katedry OtakOS.
 */
@Composable
fun SpatialMarbleTable(
    artifacts: List<ArtifactEntity>,
    savedPositions: Map<Long, Pair<Float, Float>>,
    onUpdatePosition: (artifactId: Long, x: Float, y: Float) -> Unit,
    onMoveToZone: (artifact: ArtifactEntity, newStatus: String) -> Unit,
    onPreview: (ArtifactEntity) -> Unit,
    onCollaborate: (ArtifactEntity) -> Unit,
    onDirectRatify: (ArtifactEntity) -> Unit,
    onAutoArrange: (width: Float, height: Float) -> Unit,
    onCreateTaskAtPosition: (title: String, priority: String, x: Float, y: Float) -> Unit = { _, _, _, _ -> },
    actionLogs: List<ActionLogEntity> = emptyList(),
    onFilterByAgent: (String) -> Unit = {},
    onToggleTimer: ((ArtifactEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    BoxWithConstraints(
        modifier = modifier
            .testTag("spatial_marble_table")
            .fillMaxSize()
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        // Local drag, context-menu, and agent inspector state
        val activePositions = remember { mutableStateMapOf<Long, Pair<Float, Float>>() }
        var draggingCardId by remember { mutableStateOf<Long?>(null) }
        var highlightedZone by remember { mutableStateOf<String?>(null) }
        var contextMenuTargetOffset by remember { mutableStateOf<Offset?>(null) }
        var selectedAgentForActivity by remember { mutableStateOf<OtakAgent?>(null) }

        // Sync with external saved positions or compute initial spatial layout
        LaunchedEffect(artifacts, savedPositions, containerWidth, containerHeight) {
            if (containerWidth > 0 && containerHeight > 0) {
                artifacts.forEachIndexed { index, artifact ->
                    val saved = savedPositions[artifact.id]
                    if (saved != null) {
                        activePositions[artifact.id] = saved
                    } else if (!activePositions.containsKey(artifact.id)) {
                        // Place organically in zone based on status
                        val defaultPos = when (artifact.status) {
                            "NA_STOLE" -> {
                                val offsetIndex = index % 3
                                Pair(20f + offsetIndex * 24f, 130f + offsetIndex * 50f)
                            }
                            "W_OPRACOWANIU" -> {
                                val centerX = (containerWidth / 2f) - 140f
                                Pair(centerX.coerceAtLeast(16f), 285f + (index % 3) * 60f)
                            }
                            "DO_AKCEPTACJI", "ZRATYFIKOWANE" -> {
                                val rightX = (containerWidth - 310f).coerceAtLeast(16f)
                                Pair(rightX, 495f + (index % 3) * 60f)
                            }
                            else -> Pair(30f + (index * 20f), 140f + (index * 40f))
                        }
                        activePositions[artifact.id] = defaultPos
                        onUpdatePosition(artifact.id, defaultPos.first, defaultPos.second)
                    }
                }
            }
        }

        // 1. Etched Spatial Organization Zones on the Marble Table Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            contextMenuTargetOffset = offset
                        }
                    )
                }
                .drawBehind {
                    val w = size.width
                    val h = size.height

                    // Zone 1: Strefa Inicjacji (Top / Left)
                    val zone1Color = if (highlightedZone == "NA_STOLE") Color(0xFF38BDF8) else Color(0x300284C7)
                    drawRoundRect(
                        color = zone1Color.copy(alpha = if (highlightedZone == "NA_STOLE") 0.18f else 0.07f),
                        topLeft = Offset(12f, 120f),
                        size = Size(w * 0.94f, 130f),
                        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                    )
                    drawRoundRect(
                        color = zone1Color,
                        topLeft = Offset(12f, 120f),
                        size = Size(w * 0.94f, 130f),
                        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                        style = Stroke(width = if (highlightedZone == "NA_STOLE") 2.5f else 1f)
                    )

                    // Zone 2: Krąg Aktywnej Współpracy Agentów (Center)
                    val zone2Color = if (highlightedZone == "W_OPRACOWANIU") Color(0xFFA855F7) else Color(0x357C3AED)
                    drawRoundRect(
                        color = zone2Color.copy(alpha = if (highlightedZone == "W_OPRACOWANIU") 0.20f else 0.08f),
                        topLeft = Offset(12f, 270f),
                        size = Size(w * 0.94f, 190f),
                        cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                    )
                    drawRoundRect(
                        color = zone2Color,
                        topLeft = Offset(12f, 270f),
                        size = Size(w * 0.94f, 190f),
                        cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                        style = Stroke(width = if (highlightedZone == "W_OPRACOWANIU") 2.5f else 1.2f)
                    )

                    // Zone 3: Izba Ratyfikacji i Akceptacji (Bottom)
                    val zone3Color = if (highlightedZone == "DO_AKCEPTACJI") Color(0xFFFBBF24) else Color(0x35D97706)
                    drawRoundRect(
                        color = zone3Color.copy(alpha = if (highlightedZone == "DO_AKCEPTACJI") 0.20f else 0.08f),
                        topLeft = Offset(12f, 480f),
                        size = Size(w * 0.94f, 170f),
                        cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                    )
                    drawRoundRect(
                        color = zone3Color,
                        topLeft = Offset(12f, 480f),
                        size = Size(w * 0.94f, 170f),
                        cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                        style = Stroke(width = if (highlightedZone == "DO_AKCEPTACJI") 2.5f else 1.2f)
                    )

                    // Long-press target beacon on the marble
                    contextMenuTargetOffset?.let { target ->
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFBBF24), Color(0xFF38BDF8), Color.Transparent),
                                center = target,
                                radius = 60f
                            ),
                            center = target,
                            radius = 60f,
                            blendMode = BlendMode.Plus
                        )
                        drawCircle(
                            color = Color(0xFFFBBF24),
                            center = target,
                            radius = 16f,
                            style = Stroke(width = 2f)
                        )
                        drawCircle(
                            color = Color.White,
                            center = target,
                            radius = 5f
                        )
                    }
                }
        )

        // Zone Watermarks & Titles (Engraved in Marble)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 128.dp)
        ) {
            Text(
                text = "1. STREFA INICJACJI • NOWE NA STOLE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                ),
                color = Color(0xFF0284C7).copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(130.dp))

            Text(
                text = "2. KRĄG KOOPERACJI AGENTÓW • W OPRACOWANIU",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                ),
                color = Color(0xFF7C3AED).copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(190.dp))

            Text(
                text = "3. IZBA RATYFIKACJI • DO AKCEPTACJI KATEDRY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                ),
                color = Color(0xFFD97706).copy(alpha = 0.75f)
            )
        }

        // Top Spatial Console: Action Bar & Circular Glowing Agent Avatars
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .zIndex(100f)
        ) {
            // Action bar with layout hints
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xD8141C2A) else Color(0xEEFFFFFF),
                    modifier = Modifier.border(1.dp, if (isDark) Color(0x3394A3B8) else Color(0x44CBD5E1), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Przesuwaj karty • Dotknij awatara agenta, aby podejrzeć aktywność",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ElevatedButton(
                    onClick = { onAutoArrange(containerWidth, containerHeight) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .testTag("auto_arrange_button")
                        .height(30.dp)
                ) {
                    Icon(imageVector = Icons.Default.FilterCenterFocus, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Uporządkuj", fontSize = 10.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Circular Glowing Agent Avatars on Marble Surface
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xCC0E1624) else Color(0xDDF8FAFC),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                Color(0x5538BDF8),
                                Color(0x55FBBF24),
                                Color(0x55A855F7),
                                Color(0x5534D399)
                            )
                        ),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OtakDepartment.agents.forEach { agent ->
                        AgentTableAvatar(
                            agent = agent,
                            isSelected = selectedAgentForActivity?.id == agent.id,
                            onClick = { selectedAgentForActivity = agent }
                        )
                    }
                }
            }
        }

        // 2. Render Draggable Spatial Task Cards
        artifacts.forEach { artifact ->
            val pos = activePositions[artifact.id] ?: Pair(30f, 140f)
            val isBeingDragged = draggingCardId == artifact.id

            DraggableSpatialCard(
                artifact = artifact,
                position = pos,
                isBeingDragged = isBeingDragged,
                onDragStart = {
                    draggingCardId = artifact.id
                },
                onDragDelta = { deltaX, deltaY ->
                    val newX = (pos.first + deltaX).coerceIn(8f, (containerWidth - 280f).coerceAtLeast(8f))
                    val newY = (pos.second + deltaY).coerceIn(110f, (containerHeight - 200f).coerceAtLeast(110f))
                    activePositions[artifact.id] = Pair(newX, newY)

                    // Detect which zone card is hovering over
                    highlightedZone = when {
                        newY < 265f -> "NA_STOLE"
                        newY < 470f -> "W_OPRACOWANIU"
                        else -> "DO_AKCEPTACJI"
                    }
                },
                onDragEnd = {
                    draggingCardId = null
                    val currentY = pos.second
                    val targetStatus = when {
                        currentY < 265f -> "NA_STOLE"
                        currentY < 470f -> "W_OPRACOWANIU"
                        else -> "DO_AKCEPTACJI"
                    }
                    highlightedZone = null
                    onUpdatePosition(artifact.id, pos.first, pos.second)
                    if (targetStatus != artifact.status && targetStatus != "NA_STOLE") {
                        onMoveToZone(artifact, targetStatus)
                    }
                },
                onPreview = { onPreview(artifact) },
                onCollaborate = { onCollaborate(artifact) },
                onDirectRatify = { onDirectRatify(artifact) },
                onToggleTimer = { onToggleTimer?.invoke(artifact) }
            )
        }

        // 3. Context-sensitive menu triggered by long-pressing empty space on the marble table
        contextMenuTargetOffset?.let { touchOffset ->
            TableContextMenu(
                touchOffset = touchOffset,
                onDismiss = { contextMenuTargetOffset = null },
                onCreateTask = { title, priority ->
                    onCreateTaskAtPosition(title, priority, touchOffset.x, touchOffset.y)
                    contextMenuTargetOffset = null
                }
            )
        }

        // 4. Agent Current Activity Inspector Dialog
        selectedAgentForActivity?.let { agent ->
            AgentActivityDialog(
                agent = agent,
                artifacts = artifacts,
                actionLogs = actionLogs,
                onDismiss = { selectedAgentForActivity = null },
                onFilterByAgent = { agentName ->
                    onFilterByAgent(agentName)
                    selectedAgentForActivity = null
                },
                onTriggerCollaboration = { artifact ->
                    onCollaborate(artifact)
                    selectedAgentForActivity = null
                }
            )
        }
    }
}

@Composable
private fun DraggableSpatialCard(
    artifact: ArtifactEntity,
    position: Pair<Float, Float>,
    isBeingDragged: Boolean,
    onDragStart: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit,
    onPreview: () -> Unit,
    onCollaborate: () -> Unit,
    onDirectRatify: () -> Unit,
    onToggleTimer: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val agent = OtakDepartment.getAgentByName(artifact.proposingAgent)

    // Physics animations for lifting off the marble table
    val animatedScale by animateFloatAsState(
        targetValue = if (isBeingDragged) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "card_scale"
    )

    val animatedRotation by animateFloatAsState(
        targetValue = if (isBeingDragged) -2.5f else 0.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_tilt"
    )

    val animatedShadowElevation by animateFloatAsState(
        targetValue = if (isBeingDragged) 22f else 5f,
        label = "card_elevation"
    )

    val (statusColor, statusLabel) = when (artifact.status) {
        "NA_STOLE" -> Color(0xFF0284C7) to "NA STOLE"
        "W_OPRACOWANIU" -> Color(0xFF7C3AED) to "W OPRACOWANIU"
        "DO_AKCEPTACJI" -> Color(0xFFD97706) to "DO AKCEPTACJI"
        "ZRATYFIKOWANE" -> Color(0xFF059669) to "ZRATYFIKOWANE"
        else -> MaterialTheme.colorScheme.primary to artifact.status
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(position.first.roundToInt(), position.second.roundToInt()) }
            .width(280.dp)
            .zIndex(if (isBeingDragged) 60f else 10f)
            .scale(animatedScale)
            .rotate(animatedRotation)
            // Advanced Multi-Layer Dragging Shadow onto the Marble Slab
            .drawBehind {
                val cardW = size.width
                val cardH = size.height

                // Dynamic Cast Shadow on the marble table surface
                val shadowOffset = if (isBeingDragged) Offset(8f, 22f) else Offset(2f, 6f)
                val shadowColor = if (isBeingDragged) {
                    if (isDark) Color(0xB0000000) else Color(0x55000000)
                } else {
                    if (isDark) Color(0x60000000) else Color(0x25000000)
                }

                drawRoundRect(
                    color = shadowColor,
                    topLeft = shadowOffset,
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )

                // Colored Caustic Glow on the marble under the card
                val causticBrush = Brush.radialGradient(
                    colors = listOf(
                        statusColor.copy(alpha = if (isBeingDragged) 0.35f else 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(cardW * 0.5f, cardH * 0.5f),
                    radius = cardW * 0.65f
                )
                drawRoundRect(
                    brush = causticBrush,
                    topLeft = Offset(-6f, if (isBeingDragged) 10f else 2f),
                    size = Size(cardW + 12f, cardH + 12f),
                    cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                    blendMode = BlendMode.Plus
                )
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = if (isDark) listOf(
                        Color(0xF5182234),
                        Color(0xEE121927),
                        Color(0xF5162030)
                    ) else listOf(
                        Color(0xFDFDFDFD),
                        Color(0xF8F8FAFC),
                        Color(0xF2F1F5F9)
                    )
                )
            )
            .border(
                width = if (isBeingDragged) 2.dp else 1.2.dp,
                brush = Brush.linearGradient(
                    colors = if (isBeingDragged) listOf(
                        Color(0xFF38BDF8),
                        statusColor,
                        Color(0xFFFBBF24)
                    ) else listOf(
                        if (isDark) Color(0x80FFFFFF) else Color(0xD0FFFFFF),
                        statusColor.copy(alpha = 0.5f),
                        if (isDark) Color(0x20000000) else Color(0x20000000)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .pointerInput(artifact.id) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
            .clickable(onClick = onPreview)
            .testTag("spatial_card_${artifact.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            val priorityInfo = TaskPriority.fromString(artifact.priority)

            // Drag Grip Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Przeciągnij",
                        tint = if (isBeingDragged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = artifact.category,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    TaskPriorityBadge(
                        priority = priorityInfo,
                        compact = true,
                        fontSize = 9.sp,
                        iconSize = 9.dp,
                        testTagSuffix = "spatial_${artifact.id}"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = statusColor
                        )
                    }
                }
            }

            if (priorityInfo == TaskPriority.HIGH) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                        .border(0.6.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Pilny fokus AI (High Priority)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFEF4444)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = artifact.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = artifact.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Agent Assignment & Review Status Row + Consensus
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactSpatialAgentIndicator(artifact = artifact)

                Text(
                    text = "${artifact.consensusScore}% zgody",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic Task Progress Indicator on Spatial Card
            CompactTaskProgressIndicator(artifact = artifact)

            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic Task Timer & Velocity Indicator on Spatial Card
            CompactSpatialCardTimer(
                artifact = artifact,
                onToggleTimer = { onToggleTimer?.invoke() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Actions on Spatial Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    modifier = Modifier.height(28.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Otwórz", fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                if (artifact.status == "DO_AKCEPTACJI") {
                    ElevatedButton(
                        onClick = onDirectRatify,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFFD97706),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(28.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zatwierdź", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (artifact.status != "ZRATYFIKOWANE") {
                    FilledTonalButton(
                        onClick = onCollaborate,
                        modifier = Modifier.height(28.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Iteruj", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
