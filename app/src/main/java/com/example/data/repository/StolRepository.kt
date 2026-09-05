package com.example.data.repository

import com.example.data.dao.StolDao
import com.example.data.entity.ActionLogEntity
import com.example.data.entity.ArtifactEntity
import com.example.data.entity.TaskCommentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class StolRepository(private val dao: StolDao) {

    val allArtifacts: Flow<List<ArtifactEntity>> = dao.getAllArtifacts()
    val allLogs: Flow<List<ActionLogEntity>> = dao.getAllActionLogs()
    val allComments: Flow<List<TaskCommentEntity>> = dao.getAllComments()

    fun getCommentsForArtifact(artifactId: Long): Flow<List<TaskCommentEntity>> =
        dao.getCommentsForArtifact(artifactId)

    suspend fun initializeSeedDataIfEmpty() {
        val count = dao.getArtifactsCount()
        if (count > 0) return

        val now = System.currentTimeMillis()
        val seedArtifacts = listOf(
            ArtifactEntity(
                title = "Kwantowy Algorytm Spójności OtakOS v4.2",
                category = "Algorytmy Kwantowe",
                proposingAgent = "Vektor-9",
                assignedAgent = "Vektor-9",
                reviewingAgent = "Otak-Alpha",
                reviewStatus = "AUDYT ARCHITEKTURY",
                status = "NA_STOLE",
                priority = "Wysoki",
                description = "Protokół lokalnej synchronizacji stanów pamięci kwantowej między rozproszonymi węzłami Katedry OtakOS.",
                digitalPayload = """
                    // OtakOS Quantum Coherence Kernel v4.2
                    suspend fun synchronizeQState(nodes: List<AgentNode>): CoherenceResult {
                        val tensorFlow = nodes.map { it.computeLocalEigenvector() }
                        val globalPhase = tensorFlow.fold(0.0) { acc, vector -> acc + vector.phase }
                        return CoherenceResult(
                            phaseAlignment = globalPhase / nodes.size,
                            entropyReduction = 0.0034,
                            stabilized = true
                        )
                    }
                """.trimIndent(),
                consensusScore = 88,
                iterationsCount = 3,
                collaboratingAgents = "Otak-Alpha, Vektor-9",
                timeSpentSeconds = 2540L,
                isTimerRunning = true,
                timerStartedTimestamp = now - 180000L,
                activeTimerAgent = "Vektor-9",
                createdTimestamp = now - 3600000 * 5,
                updatedTimestamp = now - 3600000 * 2
            ),
            ArtifactEntity(
                title = "Autonomiczny Koordynator Pętli Myślowych Katedry",
                category = "Architektura Neuronowa",
                proposingAgent = "Otak-Alpha",
                assignedAgent = "Otak-Alpha",
                reviewingAgent = "Kaliope-AI",
                reviewStatus = "W TRAKCIE RECENZJI",
                status = "W_OPRACOWANIU",
                priority = "Kluczowy",
                description = "Architektura dynamicznego przydziału zadań badawczych na perłowym stole pomiędzy agentami analitycznymi a syntezującymi.",
                digitalPayload = """
                    class NeuralChairCoordinator(val table: MarbleTable) {
                        fun dispatchToAgents(topic: ResearchArtifact) {
                            val activeAgents = table.queryReadyAgents()
                            activeAgents.forEach { agent ->
                                agent.attachCognitiveLens(topic.category)
                                agent.startRefinementLoop(iterations = 5)
                            }
                        }
                    }
                """.trimIndent(),
                consensusScore = 94,
                iterationsCount = 4,
                collaboratingAgents = "Otak-Alpha, Kaliope-AI, Neuro-Marmur",
                timeSpentSeconds = 5420L,
                isTimerRunning = false,
                activeTimerAgent = "Otak-Alpha",
                createdTimestamp = now - 3600000 * 8,
                updatedTimestamp = now - 3600000 * 1
            ),
            ArtifactEntity(
                title = "Traktat o Estetyce Perłowych Struktur Pamięci",
                category = "Teoria Katedry",
                proposingAgent = "Kaliope-AI",
                assignedAgent = "Kaliope-AI",
                reviewingAgent = "Chronos-Log",
                reviewStatus = "WERYFIKACJA DOKUMENTACJI",
                status = "DO_AKCEPTACJI",
                priority = "Wysoki",
                description = "Gotowa praca teoretyczna badająca wpływ analogii materialnych (marmur, perła) na redukcję szumu semantycznego w modelach lokalnych.",
                digitalPayload = """
                    ## Teza Katedry OtakOS:
                    Perłowa powłoka konceptualna tworzy barierę anty-entropijną.
                    1. Każdy artefakt położony na Stole podlega weryfikacji wizualnej.
                    2. Trwałość marmuru = niezmienność zratyfikowanych twierdzeń logicznych.
                    3. Agenci Katedry osiągają harmonijny konsensus poprzez geometryczny stół dialogu.
                """.trimIndent(),
                consensusScore = 98,
                iterationsCount = 6,
                collaboratingAgents = "Kaliope-AI, Chronos-Log",
                timeSpentSeconds = 3890L,
                isTimerRunning = false,
                activeTimerAgent = "Kaliope-AI",
                createdTimestamp = now - 3600000 * 12,
                updatedTimestamp = now - 1800000
            ),
            ArtifactEntity(
                title = "Sterownik Akceleracji Tensorowej na Perłowym Rdzeniu",
                category = "Kod OtakOS",
                proposingAgent = "Neuro-Marmur",
                assignedAgent = "Neuro-Marmur",
                reviewingAgent = "Przewodniczący Katedry",
                reviewStatus = "ZRATYFIKOWANE",
                status = "ZRATYFIKOWANE",
                priority = "Kluczowy",
                description = "Oficjalnie zatwierdzone przez Katedrę dzieło programistyczne optymalizujące operacje macierzowe na procesorach lokalnych.",
                digitalPayload = """
                    // RATIFIED OTAKOS CORE COMPONENT
                    // Ratification Hash: #OTAK-7849-MARMUR
                    inline fun <reified T : Number> executeMarbleTensor(matrix: Array<FloatArray>): FloatArray {
                        // Native SIMD acceleration with OtakOS memory pinning
                        return NativeKernel.fastMultiply(matrix)
                    }
                """.trimIndent(),
                consensusScore = 100,
                iterationsCount = 8,
                collaboratingAgents = "Neuro-Marmur, Otak-Alpha, Vektor-9",
                timeSpentSeconds = 9120L,
                isTimerRunning = false,
                ratificationNotes = "Uroczysta ratyfikacja przez Radę Katedry OtakOS. Kod włączony do kanonicznego jądra systemu.",
                ratifiedTimestamp = now - 3600000 * 24,
                createdTimestamp = now - 3600000 * 36,
                updatedTimestamp = now - 3600000 * 24
            ),
            ArtifactEntity(
                title = "Empiryczne Badania Asymetrii Kognitywnej Modeli",
                category = "Research",
                proposingAgent = "Kaliope-AI",
                assignedAgent = "Kaliope-AI",
                reviewingAgent = "Vektor-9",
                reviewStatus = "ANALIZA STATYSTYCZNA",
                status = "NA_STOLE",
                priority = "Wysoki",
                description = "Wieloetapowe badanie zachowań agentów autonomicznych w sytuacjach konfliktowych i dochodzenia do konsensusu na perłowym stole.",
                digitalPayload = """
                    // Research Report: Cognitive Asymmetry Index
                    data class AsymmetryMetric(val divergenceRate: Double, val convergenceStep: Int)
                    fun evaluateConsensusDynamics(trials: List<Trial>): Double = trials.map { it.score }.average()
                """.trimIndent(),
                consensusScore = 78,
                iterationsCount = 2,
                collaboratingAgents = "Kaliope-AI, Vektor-9",
                timeSpentSeconds = 1140L,
                isTimerRunning = false,
                activeTimerAgent = "Kaliope-AI",
                createdTimestamp = now - 3600000 * 6,
                updatedTimestamp = now - 3600000 * 2
            ),
            ArtifactEntity(
                title = "Projekt Interfejsu Przestrzennego Perłowego Stołu",
                category = "Design & UX",
                proposingAgent = "Otak-Alpha",
                assignedAgent = "Otak-Alpha",
                reviewingAgent = "Kaliope-AI",
                reviewStatus = "AUDYT WIZUALNY",
                status = "W_OPRACOWANIU",
                priority = "Eksperymentalny",
                description = "Układ przestrzenny kart ze specularnymi refleksami światła, kaustyką i naturalnym cieniowaniem podłoża marmuru.",
                digitalPayload = """
                    // Design Token Spec for Spatial Marble
                    val pearlReflectivity = 0.85f
                    val causticDispersion = listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color.Transparent)
                """.trimIndent(),
                consensusScore = 86,
                iterationsCount = 3,
                collaboratingAgents = "Otak-Alpha, Kaliope-AI",
                timeSpentSeconds = 2960L,
                isTimerRunning = true,
                timerStartedTimestamp = now - 420000L,
                activeTimerAgent = "Otak-Alpha",
                createdTimestamp = now - 3600000 * 10,
                updatedTimestamp = now - 3600000 * 3
            ),
            ArtifactEntity(
                title = "Protokół Syntezy Transakcji Czasowych Chronos-Ledger",
                category = "Synteza Danych",
                proposingAgent = "Chronos-Log",
                assignedAgent = "Chronos-Log",
                reviewingAgent = "Neuro-Marmur",
                reviewStatus = "W TRAKCIE RECENZJI",
                status = "DO_AKCEPTACJI",
                priority = "Standardowy",
                description = "Deterministyczny rejestr zdarzeń i przejść stanów dla wszystkich decyzji podejmowanych przez Radę Katedry.",
                digitalPayload = """
                    // Chronos Ledger Signature Verification
                    fun verifyTimelineBlock(block: LedgerBlock): Boolean {
                        return block.hash.startsWith("0000") && block.timestamp <= System.currentTimeMillis()
                    }
                """.trimIndent(),
                consensusScore = 91,
                iterationsCount = 4,
                collaboratingAgents = "Chronos-Log, Neuro-Marmur",
                timeSpentSeconds = 4120L,
                isTimerRunning = false,
                activeTimerAgent = "Chronos-Log",
                createdTimestamp = now - 3600000 * 18,
                updatedTimestamp = now - 3600000 * 4
            ),
            ArtifactEntity(
                title = "Kwantowy Estymator Wymiarowości Fraktalnej",
                category = "Algorytmy Kwantowe",
                proposingAgent = "Vektor-9",
                assignedAgent = "Vektor-9",
                reviewingAgent = "Otak-Alpha",
                reviewStatus = "WERYFIKACJA KODU",
                status = "NA_STOLE",
                priority = "Wysoki",
                description = "Algorytm obliczania wymiaru Minkowskiego dla struktur wiedzy generowanych dynamicznie przez zespół agentów.",
                digitalPayload = """
                    fun minkowskiDimension(points: List<Vector3D>): Double {
                        val boxes = countBoxesAtScales(points, scales = listOf(1.0, 0.5, 0.25, 0.125))
                        return linearRegressionSlope(boxes)
                    }
                """.trimIndent(),
                consensusScore = 79,
                iterationsCount = 2,
                collaboratingAgents = "Vektor-9, Otak-Alpha",
                timeSpentSeconds = 1820L,
                isTimerRunning = false,
                activeTimerAgent = "Vektor-9",
                createdTimestamp = now - 3600000 * 14,
                updatedTimestamp = now - 3600000 * 5
            )
        )

        val id1 = dao.insertArtifact(seedArtifacts[0])
        val id2 = dao.insertArtifact(seedArtifacts[1])
        val id3 = dao.insertArtifact(seedArtifacts[2])
        val id4 = dao.insertArtifact(seedArtifacts[3])
        val id5 = dao.insertArtifact(seedArtifacts[4])
        val id6 = dao.insertArtifact(seedArtifacts[5])
        val id7 = dao.insertArtifact(seedArtifacts[6])
        val id8 = dao.insertArtifact(seedArtifacts[7])

        val seedComments = listOf(
            TaskCommentEntity(
                artifactId = id1,
                authorName = "Vektor-9",
                authorRole = "Główny Analityk Kwantowy",
                commentType = "SUGGESTION",
                content = "Zaproponowałem ten protokół w celu eliminacji dryfu fazowego między rozproszonymi węzłami. Zwróćcie uwagę na iloczyn faz w pętli fold.",
                timestamp = now - 3600000 * 4
            ),
            TaskCommentEntity(
                artifactId = id1,
                authorName = "Otak-Alpha",
                authorRole = "Architekt Systemu",
                commentType = "FEEDBACK",
                content = "Audyt kodu: Złożoność jest poprawna, ale przy węzłach > 64 możemy napotkać wąskie gardło przy sekwencyjnym wyznaczaniu wektorów własnych. Sugeruję paralelizację.",
                timestamp = now - 3600000 * 3
            ),
            TaskCommentEntity(
                artifactId = id1,
                authorName = "Neuro-Marmur",
                authorRole = "Strażnik Pamięci",
                commentType = "DISCUSSION",
                content = "Z punktu widzenia alokacji pamięci perłowej, struktura CoherenceResult jest lekka i mieści się w cache L2. Dobre rozwiązanie.",
                timestamp = now - 3600000 * 2
            ),
            TaskCommentEntity(
                artifactId = id2,
                authorName = "Otak-Alpha",
                authorRole = "Architekt Systemu",
                commentType = "SUGGESTION",
                content = "Koordynator pętli myślowych to fundament Katedry. Dynamiczne soczewki kognitywne pozwalają agentom płynnie przełączać się między matematyką a ontologią.",
                timestamp = now - 3600000 * 7
            ),
            TaskCommentEntity(
                artifactId = id2,
                authorName = "Neuro-Marmur",
                authorRole = "Strażnik Pamięci",
                commentType = "CRITIQUE",
                content = "Uwaga krytyczna: metoda startRefinementLoop domyślnie uruchamia 5 cykli bez ograniczenia przepustowości. Dodajmy adaptacyjny dławik przy wysokim obciążeniu.",
                timestamp = now - 3600000 * 5
            ),
            TaskCommentEntity(
                artifactId = id2,
                authorName = "Kaliope-AI",
                authorRole = "Synteza Semantyczna",
                commentType = "FEEDBACK",
                content = "Wprowadziłam w iteracji #3 wstępny model buforowania asynchronicznego. Wyniki konsensusu wzrosły do 94%. Zadanie zbliża się do dojrzałości.",
                timestamp = now - 3600000 * 1
            ),
            TaskCommentEntity(
                artifactId = id3,
                authorName = "Kaliope-AI",
                authorRole = "Synteza Semantyczna",
                commentType = "APPROVAL",
                content = "Traktat jest kompletny i spełnia wszelkie wymogi dyskursu filozoficznego Katedry. Wnioskuję o uroczystą ratyfikację.",
                timestamp = now - 1800000
            ),
            TaskCommentEntity(
                artifactId = id4,
                authorName = "Neuro-Marmur",
                authorRole = "Strażnik Pamięci",
                commentType = "APPROVAL",
                content = "Sterownik osiągnął 100% konsensusu i stabilności w testach hardware'owych. Włączony do kanonu Katedry OtakOS.",
                timestamp = now - 3600000 * 24
            )
        )
        dao.insertComments(seedComments)

        val seedLogs = listOf(
            ActionLogEntity(
                agentName = "Otak-Alpha",
                actionType = "RATYFIKACJA_KATEDRY",
                artifactTitle = "Sterownik Akceleracji Tensorowej na Perłowym Rdzeniu",
                description = "Pomyślnie zratyfikowano dzieło agenta Neuro-Marmur. Nadano pieczęć Katedry OtakOS.",
                timestamp = now - 3600000 * 24
            ),
            ActionLogEntity(
                agentName = "Kaliope-AI",
                actionType = "ZGŁOSZENIE_DO_AKCEPTACJI",
                artifactTitle = "Traktat o Estetyce Perłowych Struktur Pamięci",
                description = "Ukończono 6. cykl redakcyjny. Dzieło przesunięte do Izby Akceptacji Katedry.",
                timestamp = now - 1800000
            ),
            ActionLogEntity(
                agentName = "Vektor-9",
                actionType = "POŁOŻENIE_NA_STOLE",
                artifactTitle = "Kwantowy Algorytm Spójności OtakOS v4.2",
                description = "Położono nową propozycję algorytmu na perłowym stole do dyskusji.",
                timestamp = now - 3600000 * 5
            ),
            ActionLogEntity(
                agentName = "Neuro-Marmur",
                actionType = "ITERACJA_AGENTÓW",
                artifactTitle = "Autonomiczny Koordynator Pętli Myślowych Katedry",
                description = "Zgłoszono optymalizację rozkładu obciążenia poznawczego. Konsensus wzrósł do 94%.",
                timestamp = now - 3600000 * 1
            )
        )
        dao.insertActionLogs(seedLogs)
    }

    suspend fun proposeNewArtifact(
        title: String,
        category: String,
        agent: String,
        priority: String,
        description: String,
        payload: String
    ): Long {
        val now = System.currentTimeMillis()
        val artifact = ArtifactEntity(
            title = title,
            category = category,
            proposingAgent = agent,
            assignedAgent = agent,
            reviewingAgent = null,
            reviewStatus = "OCZEKUJE NA RECENZJĘ",
            status = "NA_STOLE",
            priority = priority,
            description = description,
            digitalPayload = payload,
            consensusScore = 60,
            iterationsCount = 1,
            collaboratingAgents = agent,
            createdTimestamp = now,
            updatedTimestamp = now
        )
        val id = dao.insertArtifact(artifact)
        dao.insertActionLog(
            ActionLogEntity(
                artifactId = id,
                artifactTitle = title,
                agentName = agent,
                actionType = "POŁOŻENIE_NA_STOLE",
                description = "Położono nowe zadanie na stole Katedry: \"$title\". Opiekun: $agent.",
                timestamp = now
            )
        )
        return id
    }

    suspend fun assignAgentToTask(artifact: ArtifactEntity, agentName: String) {
        val now = System.currentTimeMillis()
        val updated = artifact.copy(
            assignedAgent = agentName,
            updatedTimestamp = now
        )
        dao.updateArtifact(updated)
        dao.insertActionLog(
            ActionLogEntity(
                artifactId = artifact.id,
                artifactTitle = artifact.title,
                agentName = agentName,
                actionType = "PRZYPISANIE_AGENTA",
                description = "Wyznaczono agenta $agentName na głównego opiekuna zadania \"${artifact.title}\".",
                timestamp = now
            )
        )
    }

    suspend fun requestAgentReview(
        artifact: ArtifactEntity,
        reviewerAgent: String,
        reviewType: String = "W TRAKCIE RECENZJI"
    ) {
        val now = System.currentTimeMillis()
        val agentsSet = artifact.collaboratingAgents.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableSet()
        agentsSet.add(reviewerAgent)

        val updated = artifact.copy(
            reviewingAgent = reviewerAgent,
            reviewStatus = reviewType,
            status = if (artifact.status == "NA_STOLE") "W_OPRACOWANIU" else artifact.status,
            collaboratingAgents = agentsSet.joinToString(", "),
            updatedTimestamp = now
        )
        dao.updateArtifact(updated)
        dao.insertActionLog(
            ActionLogEntity(
                artifactId = artifact.id,
                artifactTitle = artifact.title,
                agentName = reviewerAgent,
                actionType = "ROZPOCZĘCIE_RECENZJI",
                description = "Agent $reviewerAgent rozpoczął aktywną recenzję zadania ($reviewType).",
                timestamp = now
            )
        )
    }

    suspend fun completeAgentReview(artifact: ArtifactEntity, approved: Boolean = true) {
        val now = System.currentTimeMillis()
        val bonus = if (approved) 8 else 3
        val newConsensus = (artifact.consensusScore + bonus).coerceAtMost(99)
        val reviewer = artifact.reviewingAgent ?: "Katedra OtakOS"
        val nextStatus = if (newConsensus >= 95) "DO_AKCEPTACJI" else artifact.status
        val updated = artifact.copy(
            reviewStatus = if (approved) "ZATWIERDZONO ETAP" else "WYMAGA POPRAWEK",
            consensusScore = newConsensus,
            status = nextStatus,
            iterationsCount = artifact.iterationsCount + 1,
            updatedTimestamp = now
        )
        dao.updateArtifact(updated)
        dao.insertActionLog(
            ActionLogEntity(
                artifactId = artifact.id,
                artifactTitle = artifact.title,
                agentName = reviewer,
                actionType = "ZAKOŃCZENIE_RECENZJI",
                description = "Zakończono recenzję przez $reviewer. Status: ${if (approved) "Zatwierdzono (Konsensus: $newConsensus%)" else "Wprowadzono uwagi"}.",
                timestamp = now
            )
        )
    }

    suspend fun simulateAgentCollaboration(
        artifact: ArtifactEntity,
        workingAgent: String,
        comment: String
    ) {
        val now = System.currentTimeMillis()
        val agentsSet = artifact.collaboratingAgents.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableSet()
        agentsSet.add(workingAgent)

        val newConsensus = (artifact.consensusScore + 8).coerceAtMost(99)
        val addedEffortSeconds = 420L // 7 minut pracy agenta
        val updatedArtifact = artifact.copy(
            status = if (newConsensus >= 95) "DO_AKCEPTACJI" else "W_OPRACOWANIU",
            consensusScore = newConsensus,
            iterationsCount = artifact.iterationsCount + 1,
            timeSpentSeconds = artifact.timeSpentSeconds + addedEffortSeconds,
            collaboratingAgents = agentsSet.joinToString(", "),
            reviewingAgent = workingAgent,
            reviewStatus = if (newConsensus >= 95) "WERYFIKACJA KONSENSUSU" else "W TRAKCIE RECENZJI",
            updatedTimestamp = now
        )
        dao.updateArtifact(updatedArtifact)

        dao.insertActionLog(
            ActionLogEntity(
                artifactId = artifact.id,
                artifactTitle = artifact.title,
                agentName = workingAgent,
                actionType = if (updatedArtifact.status == "DO_AKCEPTACJI") "ZGŁOSZENIE_DO_AKCEPTACJI" else "ITERACJA_AGENTÓW",
                description = "$comment (Iteracja #${updatedArtifact.iterationsCount}, +7m pracy, Zgoda: $newConsensus%)",
                timestamp = now
            )
        )
    }

    suspend fun submitForRatification(artifact: ArtifactEntity, agentName: String) {
        val now = System.currentTimeMillis()
        val updated = artifact.copy(
            status = "DO_AKCEPTACJI",
            updatedTimestamp = now
        )
        dao.updateArtifact(updated)
        dao.insertActionLog(
            ActionLogEntity(
                artifactId = artifact.id,
                artifactTitle = artifact.title,
                agentName = agentName,
                actionType = "ZGŁOSZENIE_DO_AKCEPTACJI",
                description = "Przesunięto artefakt na podest akceptacyjny Katedry OtakOS.",
                timestamp = now
            )
        )
    }

    suspend fun ratifyArtifact(artifact: ArtifactEntity, notes: String, ratifiedBy: String) {
        val now = System.currentTimeMillis()
        val additionalElapsed = if (artifact.isTimerRunning && artifact.timerStartedTimestamp != null) {
            ((now - artifact.timerStartedTimestamp) / 1000).coerceAtLeast(0)
        } else 0L
        val finalTimeSpent = artifact.timeSpentSeconds + additionalElapsed

        val updated = artifact.copy(
            status = "ZRATYFIKOWANE",
            ratificationNotes = notes.ifBlank { "Dzieło zostało oficjalnie zaakceptowane i zratyfikowane przez Katedrę OtakOS." },
            ratifiedTimestamp = now,
            consensusScore = 100,
            isTimerRunning = false,
            timerStartedTimestamp = null,
            timeSpentSeconds = finalTimeSpent,
            updatedTimestamp = now
        )
        dao.updateArtifact(updated)
        dao.insertActionLog(
            ActionLogEntity(
                artifactId = artifact.id,
                artifactTitle = artifact.title,
                agentName = ratifiedBy,
                actionType = "RATYFIKACJA_KATEDRY",
                description = "UROCZYŚCIE ZAAKCEPTOWANO I ZRATYFIKOWANO DZIEŁO: \"${artifact.title}\". Łączny czas realizacji: ${finalTimeSpent / 60}m ${finalTimeSpent % 60}s.",
                timestamp = now
            )
        )
    }

    suspend fun startTaskTimer(artifact: ArtifactEntity, agentName: String? = null) {
        val now = System.currentTimeMillis()
        val agent = agentName ?: artifact.activeTrackingAgent
        val updated = artifact.copy(
            isTimerRunning = true,
            timerStartedTimestamp = now,
            activeTimerAgent = agent,
            updatedTimestamp = now
        )
        dao.updateArtifact(updated)
        dao.insertActionLog(
            ActionLogEntity(
                artifactId = artifact.id,
                artifactTitle = artifact.title,
                agentName = agent,
                actionType = "START_STOPERA",
                description = "Agent $agent uruchomił stoper zadania \"${artifact.title}\" (Pomiar velocity zespołu).",
                timestamp = now
            )
        )
    }

    suspend fun pauseTaskTimer(artifact: ArtifactEntity) {
        val now = System.currentTimeMillis()
        val elapsed = if (artifact.isTimerRunning && artifact.timerStartedTimestamp != null) {
            ((now - artifact.timerStartedTimestamp) / 1000).coerceAtLeast(0)
        } else 0L
        val newTotal = artifact.timeSpentSeconds + elapsed
        val agent = artifact.activeTrackingAgent
        val updated = artifact.copy(
            timeSpentSeconds = newTotal,
            isTimerRunning = false,
            timerStartedTimestamp = null,
            updatedTimestamp = now
        )
        dao.updateArtifact(updated)
        dao.insertActionLog(
            ActionLogEntity(
                artifactId = artifact.id,
                artifactTitle = artifact.title,
                agentName = agent,
                actionType = "PAUZA_STOPERA",
                description = "Wstrzymano stoper zadania \"${artifact.title}\". Zarejestrowano łącznie: ${newTotal / 60}m ${newTotal % 60}s.",
                timestamp = now
            )
        )
    }

    suspend fun toggleTaskTimer(artifact: ArtifactEntity, agentName: String? = null) {
        if (artifact.isTimerRunning) {
            pauseTaskTimer(artifact)
        } else {
            startTaskTimer(artifact, agentName)
        }
    }

    suspend fun resetTaskTimer(artifact: ArtifactEntity) {
        val now = System.currentTimeMillis()
        val updated = artifact.copy(
            timeSpentSeconds = 0L,
            isTimerRunning = false,
            timerStartedTimestamp = null,
            updatedTimestamp = now
        )
        dao.updateArtifact(updated)
        dao.insertActionLog(
            ActionLogEntity(
                artifactId = artifact.id,
                artifactTitle = artifact.title,
                agentName = artifact.activeTrackingAgent,
                actionType = "RESET_STOPERA",
                description = "Zresetowano licznik czasu zadania \"${artifact.title}\".",
                timestamp = now
            )
        )
    }

    suspend fun adjustTaskTime(artifact: ArtifactEntity, deltaSeconds: Long) {
        val now = System.currentTimeMillis()
        val newTotal = (artifact.timeSpentSeconds + deltaSeconds).coerceAtLeast(0L)
        val updated = artifact.copy(
            timeSpentSeconds = newTotal,
            updatedTimestamp = now
        )
        dao.updateArtifact(updated)
    }

    suspend fun requestRevision(artifact: ArtifactEntity, feedback: String, reviewer: String) {
        val now = System.currentTimeMillis()
        val updated = artifact.copy(
            status = "W_OPRACOWANIU",
            ratificationNotes = "Uwagi z recenzji: $feedback",
            consensusScore = (artifact.consensusScore - 15).coerceAtLeast(40),
            updatedTimestamp = now
        )
        dao.updateArtifact(updated)
        dao.insertActionLog(
            ActionLogEntity(
                artifactId = artifact.id,
                artifactTitle = artifact.title,
                agentName = reviewer,
                actionType = "ZWROT_DO_POPRAWY",
                description = "Zwrócono do poprawek z uwagami: $feedback",
                timestamp = now
            )
        )
    }

    suspend fun updatePayload(artifact: ArtifactEntity, newPayload: String, author: String) {
        val now = System.currentTimeMillis()
        val updated = artifact.copy(
            digitalPayload = newPayload,
            iterationsCount = artifact.iterationsCount + 1,
            updatedTimestamp = now
        )
        dao.updateArtifact(updated)
        dao.insertActionLog(
            ActionLogEntity(
                artifactId = artifact.id,
                artifactTitle = artifact.title,
                agentName = author,
                actionType = "MODYFIKACJA_KODU",
                description = "Zaktualizowano zawartość cyfrową artefaktu.",
                timestamp = now
            )
        )
    }

    suspend fun deleteArtifact(id: Long) {
        dao.deleteCommentsByArtifactId(id)
        dao.deleteArtifactById(id)
    }

    suspend fun addComment(
        artifactId: Long,
        authorName: String,
        authorRole: String = "Agent Katedry",
        commentType: String = "DISCUSSION",
        content: String
    ): Long {
        val now = System.currentTimeMillis()
        val comment = TaskCommentEntity(
            artifactId = artifactId,
            authorName = authorName,
            authorRole = authorRole,
            commentType = commentType,
            content = content,
            timestamp = now
        )
        val id = dao.insertComment(comment)

        val artifact = dao.getArtifactById(artifactId).firstOrNull()
        dao.insertActionLog(
            ActionLogEntity(
                artifactId = artifactId,
                artifactTitle = artifact?.title ?: "Zadanie #$artifactId",
                agentName = authorName,
                actionType = "DYSKUSJA_KOMENTARZ",
                description = "Wpis w dyskusji ($commentType): ${content.take(80)}...",
                timestamp = now
            )
        )
        return id
    }

    suspend fun deleteComment(id: Long) {
        dao.deleteCommentById(id)
    }
}
