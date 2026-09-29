package com.example.ui.stol

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.most.PamiecKatedry
import com.example.most.ProcesKatedry
import com.example.most.Rozmowca
import com.example.most.RozmowaUi

/*
 * 🕊️ Delegat w StoL-u: rozmowa z dowolnym TeOgochi i pamięć Katedry.
 * Suweren (2026-09-29): „połączyć StoL z Delegatem… na stole można wybrać każdego z dostępnych" oraz
 * „python coś trzyma… by można było to zrobić z poziomu Katedry i Stołu". Sam Compose + rdzeń most —
 * bez importów z Androida (jak StolEkrany.kt).
 */

/** Pasek u góry zakładki Agenci: z kim porozmawiać + wejście do pamięci Katedry. */
@OptIn(ExperimentalMaterial3Api::class)   // FilterChip: stabilny w material3 1.3, eksperymentalny we wcześniejszych
@Composable
fun DelegatPasek(rozmowcy: List<Rozmowca>, onRozmowa: (Rozmowca) -> Unit, onPamiec: () -> Unit) {
    Marmur(akcent = Color(0xFF7C3AED)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("💬 Porozmawiaj z TeOgochi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    if (rozmowcy.isEmpty()) "Most nie podał jeszcze rozmówców — zaktualizuj Katedrę albo poczekaj na odświeżenie."
                    else "Delegat: każdy zna projekty, Stół i pamięć Katedry. Pełne profile mają też swoje narzędzia.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onPamiec, modifier = Modifier.testTag("btn_pamiec_katedry")) { Text("🧠 Pamięć") }
        }
        if (rozmowcy.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(top = 8.dp)) {
                items(rozmowcy, key = { it.id }) { r ->
                    FilterChip(
                        selected = r.pelny, onClick = { onRozmowa(r) },
                        label = { Text("${r.emoji} ${r.imie}", fontSize = 12.sp) },
                        modifier = Modifier.testTag("rozmowca_${r.id}"),
                    )
                }
            }
        }
    }
}

/** Rozmowa z jednym TeOgochi. Odpowiedź przychodzi po pętli narzędzi — „myśli…" do tego czasu. */
@Composable
fun RozmowaDelegata(
    rozmowa: RozmowaUi,
    onWyslij: (String) -> Unit,
    onZamknij: () -> Unit,
) {
    var tekst by remember(rozmowa.z.id) { mutableStateOf("") }
    val lista = rememberLazyListState()
    LaunchedEffect(rozmowa.wypowiedzi.size, rozmowa.mysli) {
        val ile = rozmowa.wypowiedzi.size + (if (rozmowa.mysli) 1 else 0)
        if (ile > 0) lista.animateScrollToItem(ile - 1)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onZamknij) { Text("← Agenci") }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text("${rozmowa.z.emoji} ${rozmowa.z.imie}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                if (rozmowa.z.dziedzina.isNotBlank()) Text(rozmowa.z.dziedzina, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        LazyColumn(state = lista, modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            if (rozmowa.wypowiedzi.isEmpty() && !rozmowa.mysli) item {
                Pusto("${rozmowa.z.imie} słucha", "Np. „co zrobione w projektach?”, „jak idzie umeblowanie?”, „co trzyma pamięć Katedry?”.")
            }
            items(rozmowa.wypowiedzi) { w -> Dymek(w.kto == "suweren", w.tekst) }
            if (rozmowa.mysli) item { Dymek(false, "${rozmowa.z.imie} myśli… (model lokalny, pętla narzędzi — bywa minuta)") }
        }
        rozmowa.blad?.let { Text(it, color = Color(0xFFDC2626), fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp)) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = tekst, onValueChange = { tekst = it }, modifier = Modifier.weight(1f).testTag("pole_rozmowy"),
                placeholder = { Text("Twoja wiadomość…") }, maxLines = 4, enabled = !rozmowa.mysli,
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onWyslij(tekst); tekst = "" }, enabled = tekst.isNotBlank() && !rozmowa.mysli,
                modifier = Modifier.testTag("btn_wyslij_rozmowe"),
            ) { Text("Wyślij") }
        }
    }
}

@Composable
private fun Dymek(moje: Boolean, tekst: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (moje) Alignment.CenterEnd else Alignment.CenterStart) {
        Text(
            tekst, fontSize = 14.sp,
            modifier = Modifier.widthIn(max = 320.dp).clip(RoundedCornerShape(14.dp))
                .background(if (moje) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/**
 * Pamięć Katedry: wolny RAM i procesy z opisem (python → ComfyUI / Kuźnia / pip…). Zamknięcie po PID z potwierdzeniem;
 * 🔒 chronione (systemowe, most, „Memory Compression") bez przycisku. Most sam sprawdza wszystko jeszcze raz.
 */
@Composable
fun PamiecKatedryEkran(
    pamiec: PamiecKatedry?,
    wczytuje: Boolean,
    blad: String?,
    onOdswiez: () -> Unit,
    onZamknijProces: (Int) -> Unit,
    onWroc: () -> Unit,
) {
    var doZamkniecia by remember { mutableStateOf<ProcesKatedry?>(null) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onWroc) { Text("← Agenci") }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onOdswiez, enabled = !wczytuje) { Text(if (wczytuje) "⟳ …" else "↻ Odśwież") }
            }
            Text("🧠 Pamięć Katedry", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            if (pamiec != null) Text("Wolne ${pamiec.wolneGB} GB z ${pamiec.razemGB} GB", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("„Memory Compression” to RAM ściśnięty przez Windows — nie zamyka się go; zmaleje sam, gdy zamkniesz to, co pamięć zjada.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            blad?.let { Text(it, color = Color(0xFFDC2626), fontSize = 12.sp) }
            pamiec?.blad?.let { Text(it, color = Color(0xFFD97706), fontSize = 12.sp) }
        }
        if (pamiec == null && !wczytuje && blad == null) item { Pusto("Brak odczytu", "Stuknij „Odśwież”.") }
        items(pamiec?.procesy.orEmpty(), key = { it.pid }) { p ->
            Marmur(akcent = if (p.chroniony) null else Color(0xFFD97706)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${if (p.chroniony) "🔒 " else ""}${p.nazwa}  #${p.pid}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        val opis = listOfNotNull(p.opis.takeIf { it.isNotBlank() }, p.skrypt?.let { "($it)" }).joinToString(" ")
                        if (opis.isNotBlank()) Text(opis, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        p.uwaga?.let { Text(it, fontSize = 11.sp, color = Color(0xFFD97706)) }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${p.mb} MB", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (!p.chroniony) TextButton(onClick = { doZamkniecia = p }, enabled = !wczytuje, modifier = Modifier.testTag("btn_zamknij_${p.pid}")) { Text("Zamknij") }
                    }
                }
            }
        }
    }
    doZamkniecia?.let { p ->
        AlertDialog(
            onDismissRequest = { doZamkniecia = null },
            title = { Text("Zamknąć ${p.nazwa} #${p.pid}?") },
            text = { Text(listOfNotNull(p.opis.takeIf { it.isNotBlank() }, p.uwaga, "Niezapisana praca w tym programie przepadnie.").joinToString("\n")) },
            confirmButton = { Button(onClick = { onZamknijProces(p.pid); doZamkniecia = null }) { Text("Zamknij") } },
            dismissButton = { TextButton(onClick = { doZamkniecia = null }) { Text("Zostaw") } },
        )
    }
}
