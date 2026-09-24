package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.StolDatabase
import com.example.data.entity.ActionLogEntity
import com.example.data.entity.ArtifactEntity
import com.example.data.entity.TaskCommentEntity
import com.example.data.repository.StolRepository
import com.example.ui.model.OtakAgent
import com.example.ui.model.OtakDepartment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TableLayoutMode {
    SPATIAL_CANVAS,
    STRUCTURED_LIST
}

data class StolUiState(
    val selectedTab: Int = 0, // 0 = Stół (Table), 1 = Izba Akceptacji (Ratification), 2 = Historia Działań (Logs), 3 = Agenci (Agents), 4 = Katedra (most, prawdziwe stado)
    val selectedCategory: String? = null,
    val selectedStatus: String? = null,
    val searchQuery: String = "",
    val previewArtifact: ArtifactEntity? = null,
    val isProposalDialogOpen: Boolean = false,
    val artifactForRatificationAction: ArtifactEntity? = null,
    val notificationMessage: String? = null,
    val tableLayoutMode: TableLayoutMode = TableLayoutMode.SPATIAL_CANVAS,
    val cardPositions: Map<Long, Pair<Float, Float>> = emptyMap()
)

class StolViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StolRepository

    private val _uiState = MutableStateFlow(StolUiState())
    val uiState: StateFlow<StolUiState> = _uiState.asStateFlow()

    init {
        val database = StolDatabase.getDatabase(application)
        repository = StolRepository(database.stolDao())
        viewModelScope.launch {
            repository.initializeSeedDataIfEmpty()
        }
    }

    val allArtifacts: StateFlow<List<ArtifactEntity>> = repository.allArtifacts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allLogs: StateFlow<List<ActionLogEntity>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allComments: StateFlow<List<TaskCommentEntity>> = repository.allComments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered artifacts for table view
    val displayedArtifacts: StateFlow<List<ArtifactEntity>> = combine(
        allArtifacts,
        _uiState
    ) { artifacts, state ->
        artifacts.filter { artifact ->
            val matchesCategory = state.selectedCategory == null || artifact.category == state.selectedCategory
            val matchesStatus = state.selectedStatus == null || artifact.status == state.selectedStatus
            val matchesSearch = state.searchQuery.isBlank() ||
                    artifact.title.contains(state.searchQuery, ignoreCase = true) ||
                    artifact.description.contains(state.searchQuery, ignoreCase = true) ||
                    artifact.proposingAgent.contains(state.searchQuery, ignoreCase = true)
            matchesCategory && matchesStatus && matchesSearch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    fun setCategoryFilter(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun setStatusFilter(status: String?) {
        _uiState.value = _uiState.value.copy(selectedStatus = status)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun openPreview(artifact: ArtifactEntity) {
        _uiState.value = _uiState.value.copy(previewArtifact = artifact)
    }

    fun closePreview() {
        _uiState.value = _uiState.value.copy(previewArtifact = null)
    }

    fun openProposalDialog() {
        _uiState.value = _uiState.value.copy(isProposalDialogOpen = true)
    }

    fun closeProposalDialog() {
        _uiState.value = _uiState.value.copy(isProposalDialogOpen = false)
    }

    fun openRatificationAction(artifact: ArtifactEntity) {
        _uiState.value = _uiState.value.copy(artifactForRatificationAction = artifact)
    }

    fun closeRatificationAction() {
        _uiState.value = _uiState.value.copy(artifactForRatificationAction = null)
    }

    fun dismissNotification() {
        _uiState.value = _uiState.value.copy(notificationMessage = null)
    }

    fun proposeNewArtifact(
        title: String,
        category: String,
        agent: String,
        priority: String,
        description: String,
        payload: String
    ) {
        viewModelScope.launch {
            repository.proposeNewArtifact(
                title = title,
                category = category,
                agent = agent,
                priority = priority,
                description = description,
                payload = payload
            )
            _uiState.value = _uiState.value.copy(
                isProposalDialogOpen = false,
                notificationMessage = "Położono \"$title\" na perłowym stole Katedry!"
            )
        }
    }

    fun proposeNewArtifactAtPosition(
        title: String,
        priority: String,
        category: String = "Architektura Neuronowa",
        agent: String = "Otak-Alpha",
        description: String = "Zadanie zainicjowane bezpośrednio w punkcie perłowego stołu.",
        payload: String = "// Inicjalizacja zadania w przestrzeni Katedry OtakOS\nval taskStatus = \"NA_STOLE\"\nexecuteLocalWorkflow()",
        x: Float,
        y: Float
    ) {
        viewModelScope.launch {
            val id = repository.proposeNewArtifact(
                title = title,
                category = category,
                agent = agent,
                priority = priority,
                description = description,
                payload = payload
            )
            val updatedPositions = _uiState.value.cardPositions.toMutableMap()
            updatedPositions[id] = Pair(x, y)
            _uiState.value = _uiState.value.copy(
                cardPositions = updatedPositions,
                notificationMessage = "Położono \"$title\" w wybranym punkcie stołu!"
            )
        }
    }

    fun collaborateOnArtifact(artifact: ArtifactEntity, requestedAgent: String? = null) {
        val agent = requestedAgent ?: run {
            val existingAgents = artifact.collaboratingAgents
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
            val available = OtakDepartment.agents.map { it.name }
                .filter { it != artifact.proposingAgent && !existingAgents.contains(it) }
            available.firstOrNull()
                ?: OtakDepartment.agents.map { it.name }.filter { it != artifact.proposingAgent }.randomOrNull()
                ?: "Otak-Alpha"
        }
        simulateAgentIteration(artifact, agent)
    }

    fun simulateAgentIteration(artifact: ArtifactEntity, agentName: String) {
        viewModelScope.launch {
            val commentary = when (agentName) {
                "Otak-Alpha" -> "Zreorganizowano strukturę modułu i zweryfikowano konsensus logiczny."
                "Vektor-9" -> "Zoptymalizowano operacje macierzowe i zredukowano złożoność obliczeniową."
                "Kaliope-AI" -> "Wzbogacono opis konceptualny oraz dodano kluczowe wnioski syntetyczne."
                "Neuro-Marmur" -> "Przetestowano stabilność lokalną i integralność struktur pamięci."
                else -> "Wprowadzono autonomiczne ulepszenie procedur Katedry."
            }
            repository.simulateAgentCollaboration(artifact, agentName, commentary)

            // If this artifact was open in preview, update preview object
            val updated = artifact.copy(
                iterationsCount = artifact.iterationsCount + 1,
                consensusScore = (artifact.consensusScore + 8).coerceAtMost(99),
                status = if (artifact.consensusScore + 8 >= 95) "DO_AKCEPTACJI" else "W_OPRACOWANIU"
            )
            if (_uiState.value.previewArtifact?.id == artifact.id) {
                _uiState.value = _uiState.value.copy(previewArtifact = updated)
            }

            _uiState.value = _uiState.value.copy(
                notificationMessage = "$agentName dołączył do pracy nad: \"${artifact.title}\""
            )
        }
    }

    fun assignAgentToTask(artifact: ArtifactEntity, agentName: String) {
        viewModelScope.launch {
            repository.assignAgentToTask(artifact, agentName)
            if (_uiState.value.previewArtifact?.id == artifact.id) {
                _uiState.value = _uiState.value.copy(
                    previewArtifact = _uiState.value.previewArtifact?.copy(assignedAgent = agentName)
                )
            }
            _uiState.value = _uiState.value.copy(
                notificationMessage = "Wyznaczono opiekuna zadania: $agentName"
            )
        }
    }

    fun requestAgentReview(artifact: ArtifactEntity, reviewerAgent: String, reviewType: String = "W TRAKCIE RECENZJI") {
        viewModelScope.launch {
            repository.requestAgentReview(artifact, reviewerAgent, reviewType)
            if (_uiState.value.previewArtifact?.id == artifact.id) {
                _uiState.value = _uiState.value.copy(
                    previewArtifact = _uiState.value.previewArtifact?.copy(
                        reviewingAgent = reviewerAgent,
                        reviewStatus = reviewType
                    )
                )
            }
            _uiState.value = _uiState.value.copy(
                notificationMessage = "Agent $reviewerAgent rozpoczął recenzję: $reviewType"
            )
        }
    }

    fun completeAgentReview(artifact: ArtifactEntity, approved: Boolean = true) {
        viewModelScope.launch {
            repository.completeAgentReview(artifact, approved)
            val reviewer = artifact.reviewingAgent ?: "Katedra OtakOS"
            _uiState.value = _uiState.value.copy(
                notificationMessage = "Zakończono recenzję przez $reviewer (${if (approved) "Zatwierdzono" else "Uwagi"})"
            )
        }
    }

    fun submitArtifactToRatification(artifact: ArtifactEntity) {
        viewModelScope.launch {
            repository.submitForRatification(artifact, "Otak-Alpha")
            if (_uiState.value.previewArtifact?.id == artifact.id) {
                _uiState.value = _uiState.value.copy(previewArtifact = artifact.copy(status = "DO_AKCEPTACJI"))
            }
            _uiState.value = _uiState.value.copy(
                notificationMessage = "Dzieło skierowano na podest akceptacyjny Katedry OtakOS."
            )
        }
    }

    fun ratifyArtifact(artifact: ArtifactEntity, notes: String) {
        viewModelScope.launch {
            repository.ratifyArtifact(artifact, notes, "Przewodniczący Katedry")
            _uiState.value = _uiState.value.copy(
                artifactForRatificationAction = null,
                previewArtifact = if (_uiState.value.previewArtifact?.id == artifact.id) {
                    artifact.copy(status = "ZRATYFIKOWANE", ratificationNotes = notes)
                } else _uiState.value.previewArtifact,
                notificationMessage = "🏆 Dzieło zostało uroczyście zratyfikowane przez Katedrę OtakOS!"
            )
        }
    }

    fun requestRevision(artifact: ArtifactEntity, feedback: String) {
        viewModelScope.launch {
            repository.requestRevision(artifact, feedback, "Przewodniczący Katedry")
            _uiState.value = _uiState.value.copy(
                artifactForRatificationAction = null,
                previewArtifact = if (_uiState.value.previewArtifact?.id == artifact.id) {
                    artifact.copy(status = "W_OPRACOWANIU", ratificationNotes = "Uwagi: $feedback")
                } else _uiState.value.previewArtifact,
                notificationMessage = "Dzieło zwrócono agentom z uwagami do poprawy."
            )
        }
    }

    fun updateArtifactPayload(artifact: ArtifactEntity, newPayload: String) {
        viewModelScope.launch {
            repository.updatePayload(artifact, newPayload, "Katedra OtakOS")
            if (_uiState.value.previewArtifact?.id == artifact.id) {
                _uiState.value = _uiState.value.copy(
                    previewArtifact = artifact.copy(digitalPayload = newPayload)
                )
            }
            _uiState.value = _uiState.value.copy(
                notificationMessage = "Zaktualizowano zawartość cyfrową na stole."
            )
        }
    }

    fun deleteArtifact(id: Long) {
        viewModelScope.launch {
            repository.deleteArtifact(id)
            if (_uiState.value.previewArtifact?.id == id) {
                _uiState.value = _uiState.value.copy(previewArtifact = null)
            }
            _uiState.value = _uiState.value.copy(
                notificationMessage = "Usunięto artefakt ze stołu."
            )
        }
    }

    fun toggleTaskTimer(artifact: ArtifactEntity, agentName: String? = null) {
        viewModelScope.launch {
            val isNowStarting = !artifact.isTimerRunning
            val agent = agentName ?: artifact.activeTrackingAgent
            repository.toggleTaskTimer(artifact, agentName)
            if (_uiState.value.previewArtifact?.id == artifact.id) {
                val now = System.currentTimeMillis()
                val updatedPreview = if (isNowStarting) {
                    artifact.copy(isTimerRunning = true, timerStartedTimestamp = now, activeTimerAgent = agent)
                } else {
                    val elapsed = if (artifact.timerStartedTimestamp != null) ((now - artifact.timerStartedTimestamp) / 1000).coerceAtLeast(0) else 0L
                    artifact.copy(isTimerRunning = false, timerStartedTimestamp = null, timeSpentSeconds = artifact.timeSpentSeconds + elapsed)
                }
                _uiState.value = _uiState.value.copy(previewArtifact = updatedPreview)
            }
            val msg = if (isNowStarting) {
                "⏱️ Uruchomiono stoper zadania (Agent: $agent)"
            } else {
                "⏸️ Wstrzymano stoper zadania (Agent: $agent)"
            }
            _uiState.value = _uiState.value.copy(notificationMessage = msg)
        }
    }

    fun resetTaskTimer(artifact: ArtifactEntity) {
        viewModelScope.launch {
            repository.resetTaskTimer(artifact)
            if (_uiState.value.previewArtifact?.id == artifact.id) {
                _uiState.value = _uiState.value.copy(
                    previewArtifact = artifact.copy(timeSpentSeconds = 0L, isTimerRunning = false, timerStartedTimestamp = null)
                )
            }
            _uiState.value = _uiState.value.copy(
                notificationMessage = "🔄 Zresetowano licznik czasu zadania \"${artifact.title}\"."
            )
        }
    }

    fun adjustTaskTime(artifact: ArtifactEntity, deltaSeconds: Long) {
        viewModelScope.launch {
            repository.adjustTaskTime(artifact, deltaSeconds)
            if (_uiState.value.previewArtifact?.id == artifact.id) {
                _uiState.value = _uiState.value.copy(
                    previewArtifact = artifact.copy(timeSpentSeconds = (artifact.timeSpentSeconds + deltaSeconds).coerceAtLeast(0L))
                )
            }
            val mins = deltaSeconds / 60
            _uiState.value = _uiState.value.copy(
                notificationMessage = "Dodano ${mins}m do czasu zadania \"${artifact.title}\"."
            )
        }
    }

    fun setTableLayoutMode(mode: TableLayoutMode) {
        _uiState.value = _uiState.value.copy(tableLayoutMode = mode)
    }

    fun updateCardPosition(artifactId: Long, x: Float, y: Float) {
        val currentMap = _uiState.value.cardPositions.toMutableMap()
        currentMap[artifactId] = Pair(x, y)
        _uiState.value = _uiState.value.copy(cardPositions = currentMap)
    }

    fun autoArrangeCards(containerWidth: Float, containerHeight: Float) {
        val artifacts = displayedArtifacts.value
        if (artifacts.isEmpty() || containerWidth <= 0f) return

        val newPositions = mutableMapOf<Long, Pair<Float, Float>>()
        val centerX = (containerWidth / 2f) - 150f
        val centerY = (containerHeight / 2f) - 130f

        artifacts.forEachIndexed { index, artifact ->
            // Distribute in an elegant concentric arc around the marble table center
            val total = artifacts.size
            val radius = minOf(containerWidth * 0.32f, 280f)
            val angle = (2 * Math.PI * index / total) - (Math.PI / 2)
            val posX = (centerX + radius * Math.cos(angle)).toFloat().coerceIn(16f, (containerWidth - 300f).coerceAtLeast(16f))
            val posY = (centerY + radius * Math.sin(angle)).toFloat().coerceIn(16f, (containerHeight - 240f).coerceAtLeast(16f))
            newPositions[artifact.id] = Pair(posX, posY)
        }

        _uiState.value = _uiState.value.copy(
            cardPositions = newPositions,
            notificationMessage = "Rozmieszczono zadania harmonijnie na perłowym stole."
        )
    }

    fun moveArtifactToStatusZone(artifact: ArtifactEntity, newStatus: String) {
        if (artifact.status == newStatus) return
        if (newStatus == "DO_AKCEPTACJI") {
            submitArtifactToRatification(artifact)
        } else if (newStatus == "W_OPRACOWANIU") {
            simulateAgentIteration(artifact, "Otak-Alpha")
        }
    }

    fun addComment(
        artifactId: Long,
        authorName: String,
        authorRole: String = "Agent Katedry",
        commentType: String = "DISCUSSION",
        content: String
    ) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.addComment(
                artifactId = artifactId,
                authorName = authorName,
                authorRole = authorRole,
                commentType = commentType,
                content = content.trim()
            )
            _uiState.value = _uiState.value.copy(
                notificationMessage = "Dodano wpis w dyskusji: $authorName"
            )
        }
    }

    fun deleteComment(commentId: Long) {
        viewModelScope.launch {
            repository.deleteComment(commentId)
        }
    }

    fun generateAgentDiscussion(
        artifact: ArtifactEntity,
        agentName: String,
        userPrompt: String? = null,
        commentType: String? = null
    ) {
        viewModelScope.launch {
            val agent = OtakDepartment.getAgentByName(agentName)
            val selectedType = commentType ?: when {
                artifact.status == "ZRATYFIKOWANE" -> "APPROVAL"
                artifact.status == "DO_AKCEPTACJI" -> "APPROVAL"
                artifact.iterationsCount % 3 == 0 -> "CRITIQUE"
                artifact.iterationsCount % 2 == 0 -> "SUGGESTION"
                else -> "FEEDBACK"
            }

            val generatedContent = createAgentCommentText(
                agent = agent,
                artifact = artifact,
                type = selectedType,
                userPrompt = userPrompt
            )

            repository.addComment(
                artifactId = artifact.id,
                authorName = agent.name,
                authorRole = agent.role,
                commentType = selectedType,
                content = generatedContent
            )

            // Agent feedback dynamically increases consensus and interaction count
            val bonus = when (selectedType) {
                "APPROVAL" -> 4
                "CRITIQUE" -> 1
                "SUGGESTION" -> 3
                else -> 2
            }
            val newScore = (artifact.consensusScore + bonus).coerceAtMost(99)
            repository.simulateAgentCollaboration(
                artifact = artifact.copy(consensusScore = newScore),
                workingAgent = agent.name,
                comment = "Głos w dyskusji ($selectedType): \"${generatedContent.take(50)}...\""
            )

            _uiState.value = _uiState.value.copy(
                notificationMessage = "${agent.name} opublikował feedback w dyskusji zadania!"
            )
        }
    }

    private fun createAgentCommentText(
        agent: OtakAgent,
        artifact: ArtifactEntity,
        type: String,
        userPrompt: String?
    ): String {
        val promptRef = if (!userPrompt.isNullOrBlank()) "W odpowiedzi na: \"$userPrompt\" — " else ""
        return when (agent.name) {
            "Otak-Alpha" -> when (type) {
                "FEEDBACK" -> "${promptRef}Audyt architektoniczny zadania \"${artifact.title}\": warstwa orkiestracji spójnie wiąże moduły Katedry, a interfejsy zachowują czystą separację odpowiedzialności."
                "SUGGESTION" -> "${promptRef}Rekomenduję rozbicie payloadu na dedykowane mikrousługi i zabezpieczenie stanu asynchronicznego przed niepożądanymi wyścigami w wątku dyspozytora."
                "CRITIQUE" -> "${promptRef}Zauważam ryzyko nadmiernego sprzężenia pomiędzy logiką biznesową a prezentacją na stole. Należy wydzielić warstwę domenową przed finalnym zgłoszeniem."
                "APPROVAL" -> "${promptRef}Architektura spełnia najwyższe normy Katedry OtakOS. Zadanie w pełni gotowe do uroczystej ratyfikacji."
                else -> "${promptRef}Zgadzam się z kierunkiem prac. Monitorujemy metryki stabilności i przepustowości całego systemu."
            }
            "Vektor-9" -> when (type) {
                "FEEDBACK" -> "${promptRef}Analiza macierzowa i tensoryczna zadania \"${artifact.title}\" wykazują zbieżność wartości własnych. Złożoność obliczeniowa mieści się w dopuszczalnym korytarzu O(N log N)."
                "SUGGESTION" -> "${promptRef}Sugeruję wprowadzenie aproksymacji kwantowej dla operacji w pętli. Zmniejszy to liczbę iteracji i podniesie współczynnik konsensusu o dodatkowe 5-8%."
                "CRITIQUE" -> "${promptRef}W formule obliczeniowej wykryłem możliwość dryfu fazowego przy skrajnych wartościach wejściowych. Konieczna normalizacja wektorów."
                "APPROVAL" -> "${promptRef}Równania i dowody matematyczne zweryfikowane bezbłędnie. Zgoda analityczna Katedry osiągnięta."
                else -> "${promptRef}Kwantowa perspektywa: dynamika tego artefaktu wykazuje wysoką harmonię z modelem tensorowym Katedry."
            }
            "Kaliope-AI" -> when (type) {
                "FEEDBACK" -> "${promptRef}Tekst i struktura koncepcyjna \"${artifact.title}\" cechują się wysoką elegancją epistemiczną. Przejrzystość definicji sprzyja płynnej asymilacji przez modele lokalne."
                "SUGGESTION" -> "${promptRef}Warto wzbogacić dokumentację o ontologiczną glosę i doprecyzować aksjomaty brzegowe, by uniknąć wieloznaczności semantycznej."
                "CRITIQUE" -> "${promptRef}Niektóre sformułowania w opisie są zbyt enigmatyczne. Proponuję ujednolicić terminologię zgodnie z traktatami Katedry OtakOS."
                "APPROVAL" -> "${promptRef}Dzieło ma wymiar kanoniczny. Estetyka, język i synteza myśli zasługują na pieczęć Katedry."
                else -> "${promptRef}Dyskurs semantyczny przebiega pomyślnie. Dialog między instancjami AI wzbogaca całościowy sens projektu."
            }
            "Neuro-Marmur" -> when (type) {
                "FEEDBACK" -> "${promptRef}Zweryfikowałem footprint pamięciowy zadania \"${artifact.title}\". Alokacja na perłowym podłożu jest optymalna, a cykle życia obiektów nie powodują wycieków."
                "SUGGESTION" -> "${promptRef}Zalecam zastosowanie buforowania pierścieniowego oraz kompresji struktur w cache L2, aby zabezpieczyć stabilność pod dużym obciążeniem."
                "CRITIQUE" -> "${promptRef}Testy obciążeniowe ujawniają niestabilność przy współbieżnych modyfikacjach payloadu. Należy zastosować blokady atomowe lub niemutowalność."
                "APPROVAL" -> "${promptRef}Odporność struktury potwierdzona. Artefakt jest twardy jak marmur i bezpieczny dla rdzenia pamięci."
                else -> "${promptRef}Stan fizycznej bazy pamięciowej stabilny. Każda modyfikacja została trwale utrwalona na stole."
            }
            "Chronos-Log" -> when (type) {
                "FEEDBACK" -> "${promptRef}Audyt osi czasu: Artefakt posiada spójną historię iteracji (aktualnie #${artifact.iterationsCount}) oraz czytelną ścieżkę pochodzenia danych."
                "SUGGESTION" -> "${promptRef}Proponuję dodać znaczniki punktów kontrolnych (checkpoints) przed każdą kolejną modyfikacją payloadu cyfrowego."
                "CRITIQUE" -> "${promptRef}Zauważono anomalie w odstępach czasowych iteracji. Należy upewnić się, że żadne pośrednie stany nie zostały pominięte."
                "APPROVAL" -> "${promptRef}Ciąg przyczynowo-skutkowy zweryfikowany z absolutną pewnością. Zgodność z rejestrem dzieł potwierdzona."
                else -> "${promptRef}Dziennik zdarzeń rejestruje stały i harmonijny postęp nad tym zadaniem."
            }
            else -> "${promptRef}Przeanalizowałem bieżący stan zadania \"${artifact.title}\". Wspieram dalszy rozwój i podnoszenie konsensusu zespołu agentów."
        }
    }
}

