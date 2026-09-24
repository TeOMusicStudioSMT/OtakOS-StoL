package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ViewAgenda
import com.example.ui.components.TaskPriority
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ActionLogEntity
import com.example.data.entity.ArtifactEntity
import com.example.ui.components.AgentActivityDialog
import com.example.ui.components.AgentCategoryChartDialog
import com.example.ui.components.getCategoryChartColor
import com.example.ui.components.AgentStatusBar
import com.example.ui.components.AgentsView
import com.example.ui.components.ArtifactCard
import com.example.ui.components.ArtifactPreviewDialog
import com.example.ui.components.HistoryView
import com.example.ui.components.MarbleBackground
import com.example.ui.components.NewProposalDialog
import com.example.ui.components.RatificationActionDialog
import com.example.ui.components.RatificationView
import com.example.ui.components.SpatialMarbleTable
import com.example.ui.katedra.KatedraView
import com.example.ui.katedra.KatedraViewModel
import com.example.ui.components.TableLightingMode
import com.example.ui.model.OtakAgent
import com.example.ui.model.OtakDepartment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StolApp(
    viewModel: StolViewModel,
    katedraViewModel: KatedraViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val displayedArtifacts by viewModel.displayedArtifacts.collectAsStateWithLifecycle()
    val allArtifacts by viewModel.allArtifacts.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()

    val isDark = isSystemInDarkTheme()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dynamic interactive lighting mode for the marble table
    var currentLightingMode by remember { mutableStateOf(TableLightingMode.PEARL_OPAL) }
    var isCategoryChartDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.notificationMessage) {
        uiState.notificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissNotification()
        }
    }

    val pendingRatificationCount = allArtifacts.count { it.status == "DO_AKCEPTACJI" }

    MarbleBackground(
        modifier = modifier,
        lightingMode = currentLightingMode
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                StolTopHeader(
                    allArtifacts = allArtifacts,
                    searchQuery = uiState.searchQuery,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    currentLightingMode = currentLightingMode,
                    onLightingModeChange = { currentLightingMode = it },
                    onOpenCategoryChart = { isCategoryChartDialogOpen = true }
                )
            },
            bottomBar = {
                StolNavigationBar(
                    selectedTab = uiState.selectedTab,
                    pendingRatificationCount = pendingRatificationCount,
                    onSelectTab = { viewModel.selectTab(it) }
                )
            },
            floatingActionButton = {
                if (uiState.selectedTab == 0) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.openProposalDialog() },
                        icon = {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        },
                        text = {
                            Text(
                                text = "Połóż na stół",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier
                            .testTag("fab_propose_task")
                            .navigationBarsPadding()
                            .shadow(12.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFF0284C7))
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (uiState.selectedTab) {
                    0 -> TableMainScreen(
                        artifacts = displayedArtifacts,
                        selectedCategory = uiState.selectedCategory,
                        selectedStatus = uiState.selectedStatus,
                        tableLayoutMode = uiState.tableLayoutMode,
                        cardPositions = uiState.cardPositions,
                        onSelectCategory = { viewModel.setCategoryFilter(it) },
                        onSelectStatus = { viewModel.setStatusFilter(it) },
                        onSetTableLayoutMode = { viewModel.setTableLayoutMode(it) },
                        onUpdateCardPosition = { id, x, y -> viewModel.updateCardPosition(id, x, y) },
                        onMoveToZone = { artifact, status -> viewModel.moveArtifactToStatusZone(artifact, status) },
                        onAutoArrange = { w, h -> viewModel.autoArrangeCards(w, h) },
                        onCreateTaskAtPosition = { title, priority, x, y ->
                            viewModel.proposeNewArtifactAtPosition(title = title, priority = priority, x = x, y = y)
                        },
                        actionLogs = allLogs,
                        onFilterByAgent = { agentName -> viewModel.setSearchQuery(agentName) },
                        onPreview = { viewModel.openPreview(it) },
                        onCollaborate = { viewModel.collaborateOnArtifact(it) },
                        onDirectRatify = { viewModel.openRatificationAction(it) },
                        onAssignAgent = { artifact, agentName -> viewModel.assignAgentToTask(artifact, agentName) },
                        onRequestReview = { artifact, reviewer, type -> viewModel.requestAgentReview(artifact, reviewer, type) },
                        onCompleteReview = { artifact, approved -> viewModel.completeAgentReview(artifact, approved) },
                        onToggleTimer = { artifact -> viewModel.toggleTaskTimer(artifact) },
                        onResetTimer = { artifact -> viewModel.resetTaskTimer(artifact) }
                    )
                    1 -> RatificationView(
                        artifacts = allArtifacts,
                        onPreview = { viewModel.openPreview(it) },
                        onOpenRatificationAction = { viewModel.openRatificationAction(it) }
                    )
                    2 -> HistoryView(logs = allLogs)
                    4 -> KatedraView(viewModel = katedraViewModel)
                    3 -> AgentsView(
                        artifacts = allArtifacts,
                        onSelectAgentArtifacts = { agentName ->
                            viewModel.setSearchQuery(agentName)
                            viewModel.selectTab(0)
                        },
                        onSelectArtifact = { viewModel.openPreview(it) }
                    )
                }
            }
        }

        // Modals & Dialogs
        uiState.previewArtifact?.let { artifact ->
            ArtifactPreviewDialog(
                artifact = artifact,
                onDismiss = { viewModel.closePreview() },
                onSimulateAgentIteration = { agentName ->
                    viewModel.simulateAgentIteration(artifact, agentName)
                },
                onSubmitToRatification = {
                    viewModel.submitArtifactToRatification(artifact)
                },
                onOpenRatificationAction = {
                    viewModel.openRatificationAction(artifact)
                },
                onUpdatePayload = { newPayload ->
                    viewModel.updateArtifactPayload(artifact, newPayload)
                },
                onDelete = {
                    viewModel.deleteArtifact(artifact.id)
                },
                onAssignAgent = { agentName ->
                    viewModel.assignAgentToTask(artifact, agentName)
                },
                onRequestReview = { reviewer, type ->
                    viewModel.requestAgentReview(artifact, reviewer, type)
                },
                onCompleteReview = { approved ->
                    viewModel.completeAgentReview(artifact, approved)
                },
                onToggleTimer = {
                    viewModel.toggleTaskTimer(artifact)
                },
                onResetTimer = {
                    viewModel.resetTaskTimer(artifact)
                },
                onAdjustTime = { deltaSeconds ->
                    viewModel.adjustTaskTime(artifact, deltaSeconds)
                }
            )
        }

        if (uiState.isProposalDialogOpen) {
            NewProposalDialog(
                onDismiss = { viewModel.closeProposalDialog() },
                onSubmit = { title, category, agent, priority, desc, payload ->
                    viewModel.proposeNewArtifact(title, category, agent, priority, desc, payload)
                }
            )
        }

        uiState.artifactForRatificationAction?.let { artifact ->
            RatificationActionDialog(
                artifact = artifact,
                onDismiss = { viewModel.closeRatificationAction() },
                onRatify = { notes ->
                    viewModel.ratifyArtifact(artifact, notes)
                },
                onReject = { feedback ->
                    viewModel.requestRevision(artifact, feedback)
                }
            )
        }

        // Dialog z wykresem D3/Recharts rozkładu kategorii zadań agentów AI
        if (isCategoryChartDialogOpen) {
            AgentCategoryChartDialog(
                artifacts = allArtifacts,
                onDismiss = { isCategoryChartDialogOpen = false },
                onSelectArtifact = { viewModel.openPreview(it) }
            )
        }
    }
}

@Composable
private fun StolTopHeader(
    allArtifacts: List<ArtifactEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    currentLightingMode: TableLightingMode,
    onLightingModeChange: (TableLightingMode) -> Unit,
    onOpenCategoryChart: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var isSearchExpanded by remember { mutableStateOf(false) }

    val activeCount = allArtifacts.count { it.status == "NA_STOLE" || it.status == "W_OPRACOWANIU" }
    val pendingCount = allArtifacts.count { it.status == "DO_AKCEPTACJI" }
    val ratifiedCount = allArtifacts.count { it.status == "ZRATYFIKOWANE" }

    Surface(
        color = if (isDark) Color(0xD0101724) else Color(0xDCFFFFFF),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .border(
                0.8.dp,
                if (isDark) Color(0x3394A3B8) else Color(0x44CBD5E1)
            )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and insignia with marble radial glow
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0F172A))
                                )
                            )
                            .border(1.8.dp, Color(0xFFFBBF24), CircleShape)
                            .shadow(6.dp, CircleShape, spotColor = Color(0xFF38BDF8))
                    ) {
                        Text(
                            text = "S",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "StoL",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                                    .border(0.8.dp, Color(0xFF0284C7).copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "KATEDRA OtakOS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color(0xFF0284C7)
                                )
                            }
                        }
                        Text(
                            text = "Stół z Perłowego Marmuru • Lokalni Agenci AI",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pie Chart Analytics Dialog button
                    IconButton(
                        onClick = onOpenCategoryChart,
                        modifier = Modifier.testTag("btn_category_distribution_chart")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = "Rozkład kategorii zadań",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Search toggle icon
                    IconButton(
                        onClick = {
                            isSearchExpanded = !isSearchExpanded
                            if (!isSearchExpanded) onSearchChange("")
                        },
                        modifier = Modifier.testTag("toggle_search_button")
                    ) {
                        Icon(
                            imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Szukaj na stole",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Search input field when opened
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Filtruj zadania, kod, agentów...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("search_input_field"),
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lighting Mode Selector Ribbon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Światło stołu",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )

                TableLightingMode.values().forEach { mode ->
                    val isSelected = currentLightingMode == mode
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onLightingModeChange(mode) }
                            .border(
                                width = if (isSelected) 1.2.dp else 0.8.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x3394A3B8),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Counter Ribbon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickMetricChip(label = "Na Stole", count = activeCount, color = Color(0xFF0284C7), modifier = Modifier.weight(1f))
                QuickMetricChip(label = "Do Akceptacji", count = pendingCount, color = Color(0xFFD97706), modifier = Modifier.weight(1f))
                QuickMetricChip(label = "Zratyfikowane", count = ratifiedCount, color = Color(0xFF059669), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickMetricChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = color
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
        }
    }
}

@Composable
private fun TableMainScreen(
    artifacts: List<ArtifactEntity>,
    selectedCategory: String?,
    selectedStatus: String?,
    tableLayoutMode: TableLayoutMode,
    cardPositions: Map<Long, Pair<Float, Float>>,
    onSelectCategory: (String?) -> Unit,
    onSelectStatus: (String?) -> Unit,
    onSetTableLayoutMode: (TableLayoutMode) -> Unit,
    onUpdateCardPosition: (artifactId: Long, x: Float, y: Float) -> Unit,
    onMoveToZone: (artifact: ArtifactEntity, newStatus: String) -> Unit,
    onAutoArrange: (width: Float, height: Float) -> Unit,
    onCreateTaskAtPosition: (title: String, priority: String, x: Float, y: Float) -> Unit = { _, _, _, _ -> },
    actionLogs: List<ActionLogEntity> = emptyList(),
    onFilterByAgent: (String) -> Unit = {},
    onPreview: (ArtifactEntity) -> Unit,
    onCollaborate: (ArtifactEntity) -> Unit,
    onDirectRatify: (ArtifactEntity) -> Unit,
    onAssignAgent: (ArtifactEntity, String) -> Unit = { _, _ -> },
    onRequestReview: (ArtifactEntity, String, String) -> Unit = { _, _, _ -> },
    onCompleteReview: (ArtifactEntity, Boolean) -> Unit = { _, _ -> },
    onToggleTimer: (ArtifactEntity) -> Unit = {},
    onResetTimer: (ArtifactEntity) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    var selectedAgentForActivityInList by remember { mutableStateOf<OtakAgent?>(null) }
    var selectedPriorityFilter by remember { mutableStateOf<TaskPriority?>(null) }
    var sortByPriorityFirst by remember { mutableStateOf(false) }

    val processedArtifacts = remember(artifacts, selectedPriorityFilter, sortByPriorityFirst) {
        val filtered = if (selectedPriorityFilter != null) {
            artifacts.filter { TaskPriority.fromString(it.priority) == selectedPriorityFilter }
        } else {
            artifacts
        }
        if (sortByPriorityFirst) {
            filtered.sortedByDescending { TaskPriority.fromString(it.priority).level }
        } else {
            filtered
        }
    }

    val categories = listOf(
        "Wszystkie" to null,
        "Research" to "Research",
        "Kod OtakOS" to "Kod OtakOS",
        "Design & UX" to "Design & UX",
        "Architektura" to "Architektura Neuronowa",
        "Algorytmy" to "Algorytmy Kwantowe",
        "Teoria" to "Teoria Katedry",
        "Synteza" to "Synteza Danych"
    )

    val statuses = listOf(
        "Wszystkie stany" to null,
        "Na Stole" to "NA_STOLE",
        "W Opracowaniu" to "W_OPRACOWANIU",
        "Do Akceptacji" to "DO_AKCEPTACJI",
        "Zratyfikowane" to "ZRATYFIKOWANE"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Mode switch ribbon: Stół Przestrzenny vs Lista
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF1E2838) else Color(0xFFE2E8F0))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (tableLayoutMode == TableLayoutMode.SPATIAL_CANVAS) {
                        MaterialTheme.colorScheme.primary
                    } else Color.Transparent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onSetTableLayoutMode(TableLayoutMode.SPATIAL_CANVAS) }
                        .testTag("mode_spatial_canvas")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = null,
                            tint = if (tableLayoutMode == TableLayoutMode.SPATIAL_CANVAS) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Stół Przestrzenny",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (tableLayoutMode == TableLayoutMode.SPATIAL_CANVAS) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (tableLayoutMode == TableLayoutMode.SPATIAL_CANVAS) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (tableLayoutMode == TableLayoutMode.STRUCTURED_LIST) {
                        MaterialTheme.colorScheme.primary
                    } else Color.Transparent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onSetTableLayoutMode(TableLayoutMode.STRUCTURED_LIST) }
                        .testTag("mode_structured_list")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewAgenda,
                            contentDescription = null,
                            tint = if (tableLayoutMode == TableLayoutMode.STRUCTURED_LIST) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Lista",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (tableLayoutMode == TableLayoutMode.STRUCTURED_LIST) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (tableLayoutMode == TableLayoutMode.STRUCTURED_LIST) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = "${artifacts.size} zadań na stole",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Category Filter Chips
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { (label, value) ->
                    val isSelected = selectedCategory == value
                    val chipColor = if (value != null) getCategoryChartColor(value) else MaterialTheme.colorScheme.primary
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectCategory(value) },
                        leadingIcon = if (value != null) {
                            {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(chipColor)
                                )
                            }
                        } else null,
                        label = { Text(label, fontSize = 11.sp) },
                        colors = if (value != null) {
                            androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipColor.copy(alpha = 0.2f),
                                selectedLabelColor = chipColor
                            )
                        } else androidx.compose.material3.FilterChipDefaults.filterChipColors(),
                        modifier = Modifier.testTag("filter_chip_${label.replace(" ", "_")}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(statuses) { (label, value) ->
                    val isSelected = selectedStatus == value
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectStatus(value) },
                        label = { Text(label, fontSize = 10.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Priority Filter Chips & Quick Urgent Focus Toggle
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedPriorityFilter == null && !sortByPriorityFirst,
                        onClick = {
                            selectedPriorityFilter = null
                            sortByPriorityFirst = false
                        },
                        label = { Text("Wszystkie priorytety", fontSize = 10.sp) },
                        modifier = Modifier.testTag("filter_priority_all")
                    )
                }

                item {
                    FilterChip(
                        selected = sortByPriorityFirst,
                        onClick = { sortByPriorityFirst = !sortByPriorityFirst },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (sortByPriorityFirst) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Pilne pierwsze (High First)",
                                fontSize = 10.sp,
                                fontWeight = if (sortByPriorityFirst) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0x28EF4444),
                            selectedLabelColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier.testTag("filter_urgent_first")
                    )
                }

                items(TaskPriority.values()) { prio ->
                    val isSelected = selectedPriorityFilter == prio
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPriorityFilter = if (isSelected) null else prio },
                        leadingIcon = {
                            Icon(
                                imageVector = prio.icon,
                                contentDescription = null,
                                tint = if (isSelected) prio.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(11.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "${prio.label} (${prio.localizedLabel})",
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = prio.containerColor,
                            selectedLabelColor = prio.color
                        ),
                        modifier = Modifier.testTag("filter_priority_${prio.label.lowercase()}")
                    )
                }
            }
        }

        // View Mode Branch: Spatial Table (Drag & Drop) OR Linear Feed
        if (tableLayoutMode == TableLayoutMode.SPATIAL_CANVAS) {
            SpatialMarbleTable(
                artifacts = processedArtifacts,
                savedPositions = cardPositions,
                onUpdatePosition = onUpdateCardPosition,
                onMoveToZone = onMoveToZone,
                onPreview = onPreview,
                onCollaborate = onCollaborate,
                onDirectRatify = onDirectRatify,
                onAutoArrange = onAutoArrange,
                onCreateTaskAtPosition = onCreateTaskAtPosition,
                actionLogs = actionLogs,
                onFilterByAgent = onFilterByAgent,
                onToggleTimer = onToggleTimer,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 75.dp)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 88.dp),
                modifier = Modifier
                    .testTag("table_main_screen_list")
                    .weight(1f)
            ) {
                // Agent presence row at table
                item {
                    AgentStatusBar(
                        selectedAgentFilter = null,
                        onSelectAgent = { agentName ->
                            if (agentName != null) {
                                selectedAgentForActivityInList = OtakDepartment.getAgentByName(agentName)
                            }
                        },
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (processedArtifacts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xCC1E293B) else Color(0xEEFFFFFF)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Perłowy stół jest pusty w tym widoku",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Użyj przycisku poniżej, aby położyć nową propozycję zadania lub zmień filtry kategorii.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(processedArtifacts, key = { it.id }) { artifact ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)) {
                            ArtifactCard(
                                artifact = artifact,
                                onPreview = { onPreview(artifact) },
                                onAgentCollaborate = { onCollaborate(artifact) },
                                onDirectRatify = { onDirectRatify(artifact) },
                                onAssignAgent = { agentName -> onAssignAgent(artifact, agentName) },
                                onRequestReview = { reviewer, type -> onRequestReview(artifact, reviewer, type) },
                                onCompleteReview = { approved -> onCompleteReview(artifact, approved) },
                                onToggleTimer = { onToggleTimer(artifact) },
                                onResetTimer = { onResetTimer(artifact) }
                            )
                        }
                    }
                }
            }

            // Dialog for agent activity inspection when in list mode
            selectedAgentForActivityInList?.let { agent ->
                AgentActivityDialog(
                    agent = agent,
                    artifacts = artifacts,
                    actionLogs = actionLogs,
                    onDismiss = { selectedAgentForActivityInList = null },
                    onFilterByAgent = { agentName ->
                        onFilterByAgent(agentName)
                        selectedAgentForActivityInList = null
                    },
                    onTriggerCollaboration = { artifact ->
                        onCollaborate(artifact)
                        selectedAgentForActivityInList = null
                    }
                )
            }
        }
    }
}

@Composable
private fun StolNavigationBar(
    selectedTab: Int,
    pendingRatificationCount: Int,
    onSelectTab: (Int) -> Unit
) {
    val isDark = isSystemInDarkTheme()

    NavigationBar(
        containerColor = if (isDark) Color(0xEE101724) else Color(0xF2FFFFFF),
        tonalElevation = 10.dp,
        modifier = Modifier
            .border(
                1.dp,
                if (isDark) Color(0x2294A3B8) else Color(0x33CBD5E1)
            )
            .navigationBarsPadding()
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onSelectTab(0) },
            icon = { Icon(imageVector = Icons.Default.TableRestaurant, contentDescription = "Stół") },
            label = { Text("Stół", fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_tab_table")
        )

        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onSelectTab(1) },
            icon = {
                if (pendingRatificationCount > 0) {
                    BadgedBox(badge = { Badge { Text("$pendingRatificationCount") } }) {
                        Icon(imageVector = Icons.Default.Verified, contentDescription = "Akceptacje")
                    }
                } else {
                    Icon(imageVector = Icons.Default.Verified, contentDescription = "Akceptacje")
                }
            },
            label = { Text("Akceptacje", fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_tab_ratification")
        )

        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onSelectTab(2) },
            icon = { Icon(imageVector = Icons.Default.History, contentDescription = "Historia") },
            label = { Text("Historia", fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_tab_history")
        )

        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onSelectTab(3) },
            icon = { Icon(imageVector = Icons.Default.Psychology, contentDescription = "Agenci") },
            label = { Text("Agenci", fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_tab_agents")
        )

        // Prawdziwe stado z mostu Katedry (parowanie + obserwacja). Pozostałe zakładki to szkic stołu.
        NavigationBarItem(
            selected = selectedTab == 4,
            onClick = { onSelectTab(4) },
            icon = { Icon(imageVector = Icons.Default.AccountBalance, contentDescription = "Katedra") },
            label = { Text("Katedra", fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_tab_katedra")
        )
    }
}
