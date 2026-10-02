package com.example.ui.stol

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.most.KontaktyTost
import com.example.most.WiadomoscTost
import com.example.most.linkWizytowek

/*
 * 💬 TOST między Katedrami w StoL-u (Suweren 2026-10-02). Telefon pisze przez SWOJĄ Katedrę, ona szyfruje
 * end-to-end i niesie kopertę do tunelu drugiej Katedry; offline → wiadomość czeka w Katedrze.
 * Sam Compose + rdzeń most — bez importów z Androida (jak StolEkrany.kt, DelegatEkrany.kt).
 */

/** Kontakty: Katedry online z rejestru otakos.wtf (tylko nicki) i ci, z którymi już pisałeś. */
@Composable
fun TostKontakty(
    k: KontaktyTost?,
    wczytuje: Boolean,
    blad: String?,
    onOtworz: (String) -> Unit,
    onOdswiez: () -> Unit,
) {
    val uri = LocalUriHandler.current
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("💬 TOST · Katedry", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(
                        if (k?.ja != null) "Jesteś: ${k.ja} · szyfrowane end-to-end przez Twoją Katedrę" else "Rozmowy z innymi Katedrami w sieci otakos.wtf",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = onOdswiez, enabled = !wczytuje) { Text(if (wczytuje) "⟳ …" else "↻") }
            }
            Spacer(Modifier.padding(top = 4.dp))
            // Wizytówki Katedr na otakos.wtf — link zapamięta w przeglądarce telefonu „moją Katedrę".
            OutlinedButton(onClick = { uri.openUri(linkWizytowek(k?.ja)) }, modifier = Modifier.testTag("btn_wizytowki")) {
                Text("🪪 Wizytówki Katedr na otakos.wtf")
            }
            blad?.let { Text(it, color = Color(0xFFDC2626), fontSize = 12.sp) }
            k?.bladRejestru?.let { Text("Rejestr otakos.wtf: $it", color = Color(0xFFD97706), fontSize = 12.sp) }
        }
        if (k != null && k.ja == null) item {
            Pusto("Katedra bez wizytówki", "W Katedrze: Dashboard → „Wystawa teo.center” → 🪪 Wizytówka — nick, Kwantowy Tunel i „Melduj w sieci”. Bez tego inne Katedry Cię nie widzą.")
        }
        if (k != null && k.ja != null && k.kontakty.isEmpty()) item {
            Pusto("Nikogo online", "Żadna inna zatwierdzona Katedra nie jest teraz w sieci.")
        }
        items(k?.kontakty.orEmpty(), key = { it.nick }) { c ->
            Marmur(akcent = if (c.nieprzeczytane > 0) Color(0xFF059669) else null, onClick = { onOtworz(c.nick) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (c.online) "🟢" else "⚪", fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.nick, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        val pod = c.ostatnia ?: c.motto
                        if (pod.isNotBlank()) Text(pod, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (c.czeka > 0) Text("⏳${c.czeka}", fontSize = 12.sp, color = Color(0xFFD97706))
                    if (c.nieprzeczytane > 0) Text("  ${c.nieprzeczytane} nowe", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                }
            }
        }
    }
}

/** Wątek z jedną Katedrą. Stan wychodzących prawdziwy: czeka / dostarczona / niedostarczona. */
@Composable
fun TostWatek(
    nick: String,
    wiadomosci: List<WiadomoscTost>,
    wysyla: Boolean,
    blad: String?,
    onWyslij: (String) -> Unit,
    onZamknij: () -> Unit,
) {
    var tekst by remember(nick) { mutableStateOf("") }
    val lista = rememberLazyListState()
    LaunchedEffect(wiadomosci.size) { if (wiadomosci.isNotEmpty()) lista.animateScrollToItem(wiadomosci.size - 1) }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onZamknij) { Text("← TOST") }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text("🏛️ $nick", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("szyfrowane end-to-end", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LazyColumn(state = lista, modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            if (wiadomosci.isEmpty()) item { Pusto("Pusto", "Pierwsza wiadomość do $nick. Gdy ta Katedra jest offline, poczeka w Twojej i wyjdzie sama.") }
            items(wiadomosci, key = { it.id }) { m -> DymekTost(m) }
        }
        blad?.let { Text(it, color = Color(0xFFDC2626), fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp)) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = tekst, onValueChange = { if (it.length <= 4000) tekst = it }, modifier = Modifier.weight(1f).testTag("pole_tost"),
                placeholder = { Text("Do $nick…") }, maxLines = 4, enabled = !wysyla,
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onWyslij(tekst); tekst = "" }, enabled = tekst.isNotBlank() && !wysyla, modifier = Modifier.testTag("btn_wyslij_tost")) {
                Text(if (wysyla) "…" else "Wyślij")
            }
        }
    }
}

@Composable
private fun DymekTost(m: WiadomoscTost) {
    val moje = !m.przychodzaca
    Box(Modifier.fillMaxWidth(), contentAlignment = if (moje) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            Modifier.widthIn(max = 320.dp).clip(RoundedCornerShape(14.dp))
                .background(if (moje) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(m.tekst, fontSize = 14.sp)
            val pod = listOf(m.czas.take(16).replace('T', ' '), m.opisStanu).filter { it.isNotBlank() }.joinToString(" · ")
            Text(pod, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
