package com.example.ui.katedra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.most.Gatunek
import com.example.most.NowyProjekt
import com.example.most.ProjektStada

/*
 * 🧩 Projekty stada na telefonie: karta projektu i natywny formularz nowego projektu.
 * Bez importów z Androida (sam Compose + rdzeń com.example.most) — da się to skompilować
 * i obejrzeć także poza telefonem.
 */

private val ZNAK_STANU = mapOf("czeka" to "○", "trwa" to "◐", "gotowe" to "●", "blad" to "✕", "przerwane" to "◌")
private val MODUL = mapOf("merch" to "🛒 Marketplace", "muzyka" to "🎵 Muzyka", "model3d" to "🧊 Assety3D", "wideo" to "🎬 Wideo")
private val STAN_PROJEKTU = mapOf("trwa" to "stado pracuje", "gotowe" to "gotowe", "czesciowo" to "gotowe z brakami", "blad" to "nie wyszło", "przerwany" to "przerwany (restart Katedry)")

@Composable
internal fun KartaProjektu(p: ProjektStada) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(p.nazwa, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                buildString {
                    append(STAN_PROJEKTU[p.stan] ?: p.stan).append(" · ").append("${p.gotowe}/${p.razem} wkładów")
                    if (p.zlecenia.isNotEmpty()) append(" · zlecenia ${p.zleceniaGotowe}/${p.zlecenia.size}")
                    p.zalozyl?.let { append(" · z „$it”") }
                },
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                p.kroki.joinToString("  ") { "${ZNAK_STANU[it.stan] ?: "·"} ${it.imie}${if (it.fala == 4) " (scala)" else ""}" },
                fontSize = 13.sp,
            )
            p.zlecenia.forEach { z ->
                Text(
                    "${ZNAK_STANU[z.stan] ?: "·"} ${MODUL[z.modul] ?: z.modul} — ${z.imie}: ${z.opis}",
                    fontSize = 12.sp,
                    color = if (z.stan == "blad" || z.stan == "przerwane") Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * Natywny formularz projektu: nazwa, wizja, kto bierze udział, czy wkłady same zlecają moduły.
 * Braki (jak w moście) widać od razu pod przyciskiem; odmowę mostu (np. „stado pracuje już
 * nad innym projektem") — jego własnymi słowami, a wpisany tekst zostaje.
 */
@Composable
internal fun FormularzProjektu(
    wyklute: List<Gatunek>,
    zakladanie: Boolean,
    blad: String?,
    onZaloz: (NowyProjekt) -> Unit,
    onAnuluj: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var nazwa by remember { mutableStateOf("") }
    var wizja by remember { mutableStateOf("") }
    var wybrani by remember { mutableStateOf(wyklute.map { it.id }.toSet()) }
    var samoZlecanie by remember { mutableStateOf(true) }
    val projekt = NowyProjekt(nazwa, wizja, wyklute.map { it.id }.filter { it in wybrani }, samoZlecanie)
    val brak = projekt.brak()

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onAnuluj, enabled = !zakladanie) { Text("← Wróć") }
        Text("Nowy wspólny projekt", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Każdy TeOgochi wniesie swoją dziedzinę, na swoim modelu. Na końcu scalenie w Biblię projektu.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = nazwa, onValueChange = { nazwa = it.take(NowyProjekt.MAX_NAZWY) },
            label = { Text("Nazwa, np. Uniwersum Teterhia") }, singleLine = true, enabled = !zakladanie,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = wizja, onValueChange = { wizja = it.take(NowyProjekt.MAX_WIZJI) },
            label = { Text("Twoja wizja: świat, klimat, co ma powstać") }, minLines = 4, enabled = !zakladanie,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Kto bierze udział", fontWeight = FontWeight.SemiBold)
        wyklute.forEach { g ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = g.id in wybrani, enabled = !zakladanie,
                    onCheckedChange = { wybrani = if (it) wybrani + g.id else wybrani - g.id },
                )
                Text("${g.forma} ${g.imie}", fontSize = 15.sp)
                Spacer(Modifier.width(6.dp))
                Text(g.dziedzina, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Wkłady same zlecają moduły Katedry", fontSize = 15.sp)
                Text("merch → Marketplace, muzyka, bryły 3D, wideo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = samoZlecanie, onCheckedChange = { samoZlecanie = it }, enabled = !zakladanie)
        }
        Button(onClick = { onZaloz(projekt) }, enabled = !zakladanie && brak == null, modifier = Modifier.fillMaxWidth()) {
            Text(if (zakladanie) "Wysyłam do Katedry…" else "Zacznijcie razem")
        }
        brak?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        blad?.let { Text("⚠️ $it", fontSize = 13.sp, color = Color(0xFFDC2626)) }
    }
}
