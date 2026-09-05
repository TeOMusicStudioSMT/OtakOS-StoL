package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ArtifactEntity
import com.example.ui.model.OtakAgent
import com.example.ui.model.OtakDepartment
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Model danych wycinka wykresu kołowego dla kategorii zadania.
 */
data class CategoryPieSlice(
    val category: String,
    val count: Int,
    val percentage: Float,
    val color: Color,
    val startAngle: Float,
    val sweepAngle: Float,
    val tasks: List<ArtifactEntity>
)

/**
 * Paleta kolorów dla kategorii zadań Katedry OtakOS (styl D3 / Recharts).
 */
fun getCategoryChartColor(category: String): Color {
    val normalized = category.trim().lowercase()
    return when {
        normalized.contains("architekt") -> Color(0xFF0284C7) // Sky Blue
        normalized.contains("algorytm") || normalized.contains("kwant") -> Color(0xFF0D9488) // Teal
        normalized.contains("kod") || normalized.contains("otakos") -> Color(0xFF10B981) // Emerald
        normalized.contains("teoria") || normalized.contains("katedr") -> Color(0xFF8B5CF6) // Violet
        normalized.contains("syntez") || normalized.contains("dan") -> Color(0xFFF59E0B) // Amber
        normalized.contains("design") || normalized.contains("ux") || normalized.contains("wizual") -> Color(0xFFEC4899) // Pink
        normalized.contains("research") || normalized.contains("nauk") || normalized.contains("audyt") -> Color(0xFF6366F1) // Indigo
        else -> Color(0xFF64748B) // Slate
    }
}

/**
 * Komponent wykresu kołowego / pierścieniowego w stylu D3 / Recharts,
 * prezentujący rozkład kategorii zadań przypisanych poszczególnym agentom AI Katedry OtakOS.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentCategoryDistributionChart(
    artifacts: List<ArtifactEntity>,
    modifier: Modifier = Modifier,
    initialSelectedAgentName: String? = null,
    onSelectArtifact: ((ArtifactEntity) -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()

    // Wybór agenta: null oznacza wszystkich agentów
    var selectedAgentName by remember(initialSelectedAgentName) {
        mutableStateOf(initialSelectedAgentName)
    }

    // Tryb wykresu: Pierścień (Donut) vs Pełne Koło (Pie)
    var isDonutMode by remember { mutableStateOf(true) }

    // Aktualnie podświetlony / dotknięty wycinek (indeks w slices)
    var selectedSliceIndex by remember { mutableStateOf<Int?>(null) }

    // Filtrowanie zadań dla wybranego agenta (lub wszystkich)
    val filteredArtifacts = remember(artifacts, selectedAgentName) {
        if (selectedAgentName == null) {
            artifacts
        } else {
            artifacts.filter {
                it.effectiveAssignedAgent.equals(selectedAgentName, ignoreCase = true) ||
                        it.proposingAgent.equals(selectedAgentName, ignoreCase = true)
            }
        }
    }

    // Obliczenie wycinków wykresu
    val slices = remember(filteredArtifacts) {
        calculateCategorySlices(filteredArtifacts)
    }

    // Reset zaznaczonego wycinka przy zmianie agenta
    LaunchedEffect(selectedAgentName) {
        selectedSliceIndex = null
    }

    // Animacja przejścia kąta wycinków (efekt wejścia / aktualizacji D3)
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(selectedAgentName, isDonutMode) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    val activeAgent = remember(selectedAgentName) {
        selectedAgentName?.let { OtakDepartment.getAgentByName(it) }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF141D2B) else Color(0xFFFFFFFF)
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.2.dp,
                if (activeAgent != null) activeAgent.primaryColor.copy(alpha = 0.45f)
                else if (isDark) Color(0xFF2A3950) else Color(0xFFE2E8F0),
                RoundedCornerShape(20.dp)
            )
            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = activeAgent?.primaryColor ?: Color(0xFF0284C7))
            .testTag("agent_category_distribution_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .animateContentSize()
        ) {
            // Nagłówek sekcji z przełącznikiem Donut / Pie
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                (activeAgent?.primaryColor ?: Color(0xFF0284C7)).copy(alpha = 0.16f)
                            )
                            .border(
                                1.2.dp,
                                (activeAgent?.primaryColor ?: Color(0xFF0284C7)).copy(alpha = 0.4f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDonutMode) Icons.Default.DonutLarge else Icons.Default.PieChart,
                            contentDescription = null,
                            tint = activeAgent?.primaryColor ?: Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ROZKŁAD KATEGORII ZADAŃ",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF0284C7).copy(alpha = 0.14f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "D3 / RECHARTS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color(0xFF0284C7)
                                )
                            }
                        }

                        Text(
                            text = if (activeAgent != null) "Agent: ${activeAgent.name} • ${activeAgent.role}"
                            else "Zbiorczy rozkład wszystkich zadań przy stole",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Przełącznik stylu wykresu: Donut / Pie
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDonutMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier
                            .clickable { isDonutMode = true }
                            .testTag("chart_mode_donut")
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)) {
                            Text(
                                text = "Torus",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isDonutMode) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isDonutMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (!isDonutMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier
                            .clickable { isDonutMode = false }
                            .testTag("chart_mode_pie")
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)) {
                            Text(
                                text = "Koło",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (!isDonutMode) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (!isDonutMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pozioma listwa wyboru Agenta AI (Filter Chips)
            Text(
                text = "Wybierz Agenta AI do analizy rozkładu:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chart_agent_filter_row")
            ) {
                // Opcja: Wszyscy Agenci
                item {
                    val isAllSelected = selectedAgentName == null
                    FilterChip(
                        selected = isAllSelected,
                        onClick = { selectedAgentName = null },
                        label = { Text("Wszyscy Agenci (${artifacts.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("filter_all_agents")
                    )
                }

                // Poszczególni agenci Katedry
                items(OtakDepartment.agents) { agent ->
                    val isSelected = selectedAgentName == agent.name
                    val agentTaskCount = artifacts.count {
                        it.effectiveAssignedAgent.equals(agent.name, ignoreCase = true) ||
                                it.proposingAgent.equals(agent.name, ignoreCase = true)
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedAgentName = agent.name },
                        label = {
                            Text(
                                text = "${agent.avatarSymbol} ${agent.name} ($agentTaskCount)",
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = agent.primaryColor.copy(alpha = 0.2f),
                            selectedLabelColor = agent.primaryColor
                        ),
                        modifier = Modifier.testTag("filter_agent_${agent.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Obszar wizualizacji kołowej (Canvas + Recharts Tooltip)
            if (filteredArtifacts.isEmpty() || slices.isEmpty()) {
                // Pusty stan dla agenta bez zadań
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0x221E293B) else Color(0xFFF8FAFC))
                        .border(1.dp, if (isDark) Color(0x3338BDF8) else Color(0x220284C7), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Brak zadań przypisanych dla: ${selectedAgentName ?: "wybranego filtra"}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Połóż nowe zadanie na stole i przypisz je temu agentowi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Główny panel wykresu: Canvas kołowy + wskaźnik centralny + Recharts Tooltip
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .testTag("pie_chart_canvas_container"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Canvas rysujący wycinki z interakcją dotykową
                        PieChartCanvas(
                            slices = slices,
                            isDonut = isDonutMode,
                            selectedSliceIndex = selectedSliceIndex,
                            animationProgress = animationProgress.value,
                            onSliceTapped = { index ->
                                selectedSliceIndex = if (selectedSliceIndex == index) null else index
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Centralny wskaźnik w trybie Donut (styl Recharts Center Metric)
                        if (isDonutMode) {
                            val activeSlice = selectedSliceIndex?.let { slices.getOrNull(it) }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .padding(8.dp)
                            ) {
                                if (activeSlice != null) {
                                    Text(
                                        text = "${activeSlice.count}",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = activeSlice.color
                                    )
                                    Text(
                                        text = "${String.format("%.1f", activeSlice.percentage)}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = activeSlice.color
                                    )
                                    Text(
                                        text = activeSlice.category,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = "${filteredArtifacts.size}",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = activeAgent?.primaryColor ?: MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "ZADAŃ",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${slices.size} kategorii",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Wskazówka interakcji
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (selectedSliceIndex == null) "Dotknij wycinek lub element legendy, aby wyświetlić szczegóły (D3 Tooltip)"
                            else "Kliknij ponownie lub inne pole, aby odznaczyć",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    // D3/Recharts Interactive Tooltip Card (pojawia się przy zaznaczeniu wycinka)
                    AnimatedVisibility(
                        visible = selectedSliceIndex != null && selectedSliceIndex!! in slices.indices,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut()
                    ) {
                        val activeSlice = selectedSliceIndex?.let { slices.getOrNull(it) }
                        if (activeSlice != null) {
                            RechartsTooltipCard(
                                slice = activeSlice,
                                onDismiss = { selectedSliceIndex = null },
                                onSelectArtifact = onSelectArtifact
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        color = if (isDark) Color(0xFF223046) else Color(0xFFE2E8F0),
                        thickness = 0.8.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Recharts Legend (interaktywna legenda z próbkami kolorów, licznikami i procentami)
                    Text(
                        text = "LEGENDA KATEGORII (RECHARTS STYLE)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        slices.forEachIndexed { index, slice ->
                            val isSelected = selectedSliceIndex == index
                            RechartsLegendChip(
                                slice = slice,
                                isSelected = isSelected,
                                onClick = {
                                    selectedSliceIndex = if (isSelected) null else index
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Rysowanie wycinków Canvas z detekcją kliknięć na kąty wycinków (D3 math).
 */
@Composable
private fun PieChartCanvas(
    slices: List<CategoryPieSlice>,
    isDonut: Boolean,
    selectedSliceIndex: Int?,
    animationProgress: Float,
    onSliceTapped: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(slices, isDonut) {
                detectTapGestures { tapOffset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = tapOffset.x - center.x
                    val dy = tapOffset.y - center.y
                    val distance = sqrt(dx * dx + dy * dy)
                    val minDim = kotlin.math.min(size.width, size.height).toFloat()
                    val radius = minDim / 2f
                    val innerRadius = if (isDonut) radius * 0.55f else 0f

                    // Jeśli kliknięto wewnątrz pierścienia lub poza promieniem koła
                    if (distance in innerRadius..radius) {
                        // Kąt w stopniach (0 na górze: -90 stopni)
                        var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (angleDeg < 0) angleDeg += 360f

                        // Dopasowanie kąta do -90 deg (270 deg)
                        val normalizedAngle = (angleDeg + 90f) % 360f

                        // Znalezienie wycinka odpowiadającego kątowi
                        val clickedIndex = slices.indexOfFirst { slice ->
                            val start = (slice.startAngle + 90f) % 360f
                            val end = start + slice.sweepAngle
                            if (end <= 360f) {
                                normalizedAngle in start..end
                            } else {
                                normalizedAngle >= start || normalizedAngle <= (end % 360f)
                            }
                        }

                        if (clickedIndex != -1) {
                            onSliceTapped(clickedIndex)
                        }
                    }
                }
            }
    ) {
        val diameter = size.minDimension
        val strokeWidth = if (isDonut) diameter * 0.22f else diameter / 2f
        val radius = (diameter - strokeWidth) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        slices.forEachIndexed { index, slice ->
            val isSelected = selectedSliceIndex == index

            // Animowany kąt rozwarcia
            val currentSweep = slice.sweepAngle * animationProgress

            // D3/Recharts "activeShape": przesunięcie promieniowe dla aktywnego wycinka
            val explodeDistance = if (isSelected) 10.dp.toPx() else 0f
            val midAngleRad = Math.toRadians((slice.startAngle + slice.sweepAngle / 2f).toDouble())
            val offsetCenter = if (explodeDistance > 0f) {
                Offset(
                    x = center.x + (cos(midAngleRad) * explodeDistance).toFloat(),
                    y = center.y + (sin(midAngleRad) * explodeDistance).toFloat()
                )
            } else {
                center
            }

            val arcTopLeft = Offset(
                x = offsetCenter.x - radius,
                y = offsetCenter.y - radius
            )
            val arcSize = Size(radius * 2f, radius * 2f)

            if (isDonut) {
                // Rysowanie pierścienia
                drawArc(
                    color = slice.color,
                    startAngle = slice.startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(
                        width = if (isSelected) strokeWidth * 1.08f else strokeWidth,
                        cap = StrokeCap.Butt
                    )
                )

                // Subtelny obrys oddzielający wycinki (jak w Recharts paddingAngle)
                drawArc(
                    color = Color.Black.copy(alpha = 0.25f),
                    startAngle = slice.startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 2.dp.toPx())
                )
            } else {
                // Rysowanie pełnego koła (Pie)
                val fullRadius = if (isSelected) (diameter / 2f) * 1.04f else diameter / 2f
                val pieTopLeft = Offset(
                    x = offsetCenter.x - fullRadius,
                    y = offsetCenter.y - fullRadius
                )
                val pieSize = Size(fullRadius * 2f, fullRadius * 2f)

                drawArc(
                    color = slice.color,
                    startAngle = slice.startAngle,
                    sweepAngle = currentSweep,
                    useCenter = true,
                    topLeft = pieTopLeft,
                    size = pieSize,
                    style = Fill
                )

                // Separator krawędzi
                drawArc(
                    color = Color.Black.copy(alpha = 0.2f),
                    startAngle = slice.startAngle,
                    sweepAngle = currentSweep,
                    useCenter = true,
                    topLeft = pieTopLeft,
                    size = pieSize,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}

/**
 * Kafelek Recharts Tooltip wyświetlany po wybraniu segmentu wykresu.
 */
@Composable
private fun RechartsTooltipCard(
    slice: CategoryPieSlice,
    onDismiss: () -> Unit,
    onSelectArtifact: ((ArtifactEntity) -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, slice.color),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .testTag("recharts_tooltip_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(slice.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = slice.category,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Zamknij podgląd",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Liczba zadań: ${slice.count}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = slice.color
                )

                Text(
                    text = "Udział w puli: ${String.format("%.1f", slice.percentage)}%",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Próbka zadań z danej kategorii
            Text(
                text = "Zadania w tej kategorii (${slice.tasks.size}):",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                slice.tasks.take(3).forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                            .clickable { onSelectArtifact?.invoke(task) }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• ${task.title}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(slice.color.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = task.status.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = slice.color
                            )
                        }
                    }
                }

                if (slice.tasks.size > 3) {
                    Text(
                        text = "+ ${slice.tasks.size - 3} więcej zadań",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Pojedynczy element legendy Recharts (Chip z próbnikiem koloru, liczbą i procentem).
 */
@Composable
private fun RechartsLegendChip(
    slice: CategoryPieSlice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) slice.color.copy(alpha = 0.22f) else slice.color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 0.8.dp,
            color = if (isSelected) slice.color else slice.color.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .clickable { onClick() }
            .testTag("legend_chip_${slice.category}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(slice.color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = slice.category,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${slice.count} (${String.format("%.0f", slice.percentage)}%)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = slice.color
            )
        }
    }
}

/**
 * Oblicza kąty wycinków kołowych dla zgrupowanych kategorii.
 */
private fun calculateCategorySlices(artifacts: List<ArtifactEntity>): List<CategoryPieSlice> {
    if (artifacts.isEmpty()) return emptyList()

    val grouped = artifacts.groupBy { it.category.ifBlank { "Inne" } }
    val totalCount = artifacts.size.toFloat()

    var currentAngle = -90f // Startujemy od góry (godzina 12:00)

    return grouped.entries
        .sortedByDescending { it.value.size }
        .map { (category, tasks) ->
            val count = tasks.size
            val sweep = (count / totalCount) * 360f
            val percentage = (count / totalCount) * 100f
            val color = getCategoryChartColor(category)

            val slice = CategoryPieSlice(
                category = category,
                count = count,
                percentage = percentage,
                color = color,
                startAngle = currentAngle,
                sweepAngle = sweep,
                tasks = tasks
            )
            currentAngle += sweep
            slice
        }
}

/**
 * Pełnoekranowy dialog analityczny z wykresem D3/Recharts rozkładu kategorii,
 * dostępny ze stolika i paska nagłówka.
 */
@Composable
fun AgentCategoryChartDialog(
    artifacts: List<ArtifactEntity>,
    onDismiss: () -> Unit,
    initialSelectedAgent: String? = null,
    onSelectArtifact: ((ArtifactEntity) -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 24.dp)
                .testTag("agent_category_chart_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analityka Katedry OtakOS",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Zamknij")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                AgentCategoryDistributionChart(
                    artifacts = artifacts,
                    initialSelectedAgentName = initialSelectedAgent,
                    onSelectArtifact = {
                        onDismiss()
                        onSelectArtifact?.invoke(it)
                    }
                )
            }
        }
    }
}
