package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.model.OtakDepartment

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewProposalDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        category: String,
        agent: String,
        priority: String,
        description: String,
        payload: String
    ) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Architektura Neuronowa") }
    var selectedAgent by remember { mutableStateOf("Przewodniczący Katedry") }
    var selectedPriority by remember { mutableStateOf("High") }
    var description by remember { mutableStateOf("") }
    var payload by remember {
        mutableStateOf(
            """
            // Cyfrowy zarys nowego zadania dla Katedry OtakOS
            fun executeOtakTask(context: TableContext) {
                // Inicjalizacja procedury
            }
            """.trimIndent()
        )
    }

    val categories = listOf(
        "Architektura Neuronowa",
        "Algorytmy Kwantowe",
        "Kod OtakOS",
        "Teoria Katedry",
        "Research",
        "Design & UX",
        "Synteza Danych"
    )

    val priorities = listOf("High", "Medium", "Low")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF131A26) else Color(0xFFFAFBFD)
            ),
            modifier = Modifier
                .testTag("new_proposal_dialog")
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .border(
                    1.5.dp,
                    if (isDark) Color(0xFF2E3D52) else Color(0xFFCBD5E1),
                    RoundedCornerShape(24.dp)
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "POŁÓŻ NOWE ZADANIE NA STOLE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Katedra OtakOS • Inicjacja wspólnej pracy agentów",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Anuluj")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    // Quick Templates
                    Text(
                        text = "Szybkie szablony propozycji:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                title = "Refaktoryzacja Bufora Pamięci Tensorowej"
                                selectedCategory = "Kod OtakOS"
                                selectedAgent = "Neuro-Marmur"
                                selectedPriority = "Kluczowy"
                                description = "Przebudowa lokalnej pamięci podręcznej celem minimalizacji opóźnień między zapytaniami agentów."
                                payload = """
                                    // Memory Buffer Refactor for OtakOS
                                    class LocalTensorPool(val capacity: Int) {
                                        private val buffer = ByteBuffer.allocateDirect(capacity)
                                        fun acquireSlice(): MemorySlice = synchronized(this) { ... }
                                    }
                                """.trimIndent()
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Kod Silnika", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                title = "Kwantowy Model Dyfuzji Konceptów"
                                selectedCategory = "Algorytmy Kwantowe"
                                selectedAgent = "Vektor-9"
                                selectedPriority = "Wysoki"
                                description = "Opracowanie probabilistycznego modelu przechodzenia pojęć semantycznych pomiędzy domenami wiedzy."
                                payload = """
                                    // Quantum Semantic Diffusion Function
                                    fun diffuseConcepts(nodes: Array<Matrix3D>): DiffusionTensor {
                                        return laplacianEigenmap(nodes, damping = 0.05f)
                                    }
                                """.trimIndent()
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Algorytm", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                title = "Kanon Marmurowego Interfejsu Myśli"
                                selectedCategory = "Teoria Katedry"
                                selectedAgent = "Kaliope-AI"
                                selectedPriority = "Standardowy"
                                description = "Formalizacja zasad według których agenci AI prezentują swoje syntezy na fizyczno-wirtualnym stole roboczym."
                                payload = """
                                    # Zasada 1: Czystość Perłowego Stołu
                                    Każdy agent zobowiązany jest do usunięcia niepotrzebnego szumu przed złożeniem propozycji.
                                """.trimIndent()
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Manifest", fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tytuł zadania / propozycji") },
                        placeholder = { Text("np. Nowa synteza architektury...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("proposal_title_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category
                    Text(
                        text = "Kategoria dzieła:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categories.forEach { cat ->
                            val catColor = getCategoryChartColor(cat)
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = catColor.copy(alpha = 0.22f),
                                    selectedLabelColor = catColor
                                ),
                                modifier = Modifier.testTag("proposal_category_chip_$cat")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Proposing Agent
                    Text(
                        text = "Agent wprowadzający na stół:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OtakDepartment.agents.take(3).forEach { ag ->
                            FilterChip(
                                selected = selectedAgent == ag.name,
                                onClick = { selectedAgent = ag.name },
                                label = { Text(ag.name, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OtakDepartment.agents.drop(3).forEach { ag ->
                            FilterChip(
                                selected = selectedAgent == ag.name,
                                onClick = { selectedAgent = ag.name },
                                label = { Text(ag.name, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Priority
                    Text(
                        text = "Priorytet zadania (wskazuje pilność dla agentów AI):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        priorities.forEach { prio ->
                            val pEnum = TaskPriority.fromString(prio)
                            val isSel = selectedPriority == prio
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedPriority = prio },
                                leadingIcon = {
                                    Icon(
                                        imageVector = pEnum.icon,
                                        contentDescription = null,
                                        tint = if (isSel) pEnum.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "${pEnum.label} (${pEnum.localizedLabel})",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = pEnum.containerColor,
                                    selectedLabelColor = pEnum.color
                                ),
                                modifier = Modifier.testTag("priority_chip_${prio.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Opis i cel zadania dla agentów") },
                        placeholder = { Text("Wyjaśnij cel i wymagania do wspólnego opracowania...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .testTag("proposal_description_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Digital Payload (Source / Formula / Text)
                    OutlinedTextField(
                        value = payload,
                        onValueChange = { payload = it },
                        label = { Text("Cyfrowa zawartość (kod, schemat, formuła)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("proposal_payload_input"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Anuluj")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    ElevatedButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSubmit(
                                    title.trim(),
                                    selectedCategory,
                                    selectedAgent,
                                    selectedPriority,
                                    description.ifBlank { "Propozycja zadania położona na stole do analizy." },
                                    payload.ifBlank { "// Pusty zarys cyfrowy" }
                                )
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("submit_proposal_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Połóż na Stole")
                    }
                }
            }
        }
    }
}
