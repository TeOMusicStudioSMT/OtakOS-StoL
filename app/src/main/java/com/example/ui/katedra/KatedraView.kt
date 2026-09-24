package com.example.ui.katedra

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
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
import com.example.most.ileTemu

/**
 * 🏛️ Katedra na żywo — co robią TeOgochi Twojej lokalnej Katedry.
 *
 * Tylko obserwacja. Wszystko na tym ekranie pochodzi z mostu (migawka stada z Domu
 * TeOgochi + fakty z szyny zdarzeń). Gdy agent milczy, ekran mówi „cisza" — nie zmyśla.
 */
@Composable
fun KatedraView(viewModel: KatedraViewModel, modifier: Modifier = Modifier) {
    val ui by viewModel.ui.collectAsState()

    DisposableEffect(Unit) {
        viewModel.zacznijObserwacje()
        onDispose { viewModel.zatrzymajObserwacje() }
    }

    if (!ui.sparowany) {
        Parowanie(blad = ui.blad, pracuje = ui.pracuje, onSparuj = viewModel::sparuj, modifier = modifier)
        return
    }

    val stan = ui.stan
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Katedra na żywo", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (ui.naZywo) "● na żywo" else "○ łączę…",
                        fontSize = 12.sp,
                        color = if (ui.naZywo) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    buildString {
                        append(ui.adres?.removePrefix("https://") ?: "")
                        stan?.urzadzenie?.let { append(" · ten telefon: $it") }
                    },
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val wiek = stan?.wiekMigawkiSekund
                Text(
                    when {
                        stan == null && ui.pracuje -> "Pukam do mostu…"
                        wiek == null -> stan?.powod ?: "Katedra jeszcze nic nie opublikowała."
                        else -> "Migawka stada: ${ileTemu(wiek)}" + if (wiek > 3600) " — otwórz Dom TeOgochi w Katedrze, żeby odświeżyć" else ""
                    },
                    fontSize = 12.sp,
                    color = if (wiek != null && wiek > 3600) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ui.blad?.let { Text("⚠️ $it", fontSize = 12.sp, color = Color(0xFFDC2626)) }
            }
        }

        val wyklute = stan?.wyklute.orEmpty()
        val jaja = stan?.gatunki.orEmpty().filter { !it.wyklute }
        items(wyklute, key = { it.id }) { KartaTeogochi(it) }

        if (jaja.isNotEmpty()) {
            item {
                Text(
                    "W jajach (${jaja.size}): " + jaja.joinToString(" ") { "${it.forma} ${it.imie}" },
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }

        val aktywnosc = stan?.aktywnosc.orEmpty().take(12)
        if (aktywnosc.isNotEmpty()) {
            item { Text("Ostatnie ślady na szynie", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp)) }
            items(aktywnosc, key = { "${it.kto}-${it.ts}" }) { a ->
                Text("• ${a.kto}: ${a.tresc ?: a.rodzaj ?: "…"}", fontSize = 13.sp)
            }
        }

        item {
            TextButton(onClick = viewModel::rozparuj, modifier = Modifier.padding(vertical = 16.dp)) {
                Text("Odłącz ten telefon od Katedry")
            }
        }
    }
}

@Composable
private fun KartaTeogochi(g: Gatunek) {
    val kolor = remember(g.kolor) { kolorZHex(g.kolor) }
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, kolor.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = kolor.copy(alpha = 0.08f)),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(kolor.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(g.forma, fontSize = 26.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(g.imie, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("${g.etap} · ${g.xp} XP", fontSize = 12.sp, color = kolor)
                }
                Text(g.dziedzina, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    g.robi ?: "cisza — brak śladu na szynie",
                    fontSize = 13.sp,
                    color = if (g.robi != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Parowanie(blad: String?, pracuje: Boolean, onSparuj: (String) -> Unit, modifier: Modifier = Modifier) {
    var link by remember { mutableStateOf("") }
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("Połącz StoL z Katedrą", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "W Katedrze na komputerze otwórz Dashboard → karta „StoL” i wygeneruj link parowania. " +
                "Zeskanuj kod QR aparatem (otworzy StoL sam) albo skopiuj link i wklej go tutaj. Link działa 5 minut i tylko raz.",
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = link,
            onValueChange = { link = it },
            label = { Text("otakos-stol://paruj?…") },
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { onSparuj(link) }, enabled = !pracuje && link.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text(if (pracuje) "Paruję…" else "Sparuj")
        }
        blad?.let { Text("⚠️ $it", fontSize = 13.sp, color = Color(0xFFDC2626)) }
        Text(
            "StoL tylko patrzy: niczego w Katedrze nie zmienia. Odłączyć telefon można tu albo w Katedrze.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun kolorZHex(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: IllegalArgumentException) {
    Color(0xFF94A3B8)
}
