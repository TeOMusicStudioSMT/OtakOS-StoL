package com.example.ui.stol

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.most.AkcjaStolu
import com.example.most.Gatunek
import com.example.most.KartaStolu
import com.example.most.NowaKarta
import com.example.most.OczekujacaKatedra
import com.example.most.StanRejestru
import com.example.most.PublikacjaYouTube
import com.example.most.Rozmowca
import com.example.most.Warsztat
import com.example.most.ZdarzenieSzyny
import com.example.most.czasIso
import com.example.most.ileTemu

/*
 * 🪑 Ekrany Stołu ratyfikacji: stół z kartami, Izba Akceptacji, Historia (szyna Katedry),
 * Agenci (stado TeOgochi + rozmowa przez Delegata i pamięć Katedry — DelegatEkrany.kt), karta z decyzjami
 * i formularz nowej propozycji.
 * Wygląd z AI Studio (karty na marmurze), dane z mostu. Sam Compose + rdzeń com.example.most —
 * bez importów z Androida, więc kompiluje się i daje obejrzeć także poza telefonem.
 */

private val KOLOR_ETAPU = mapOf(
    "na_stole" to Color(0xFF0284C7),
    "opracowuje" to Color(0xFF7C3AED),
    "do_akceptacji" to Color(0xFFD97706),
    "utknela" to Color(0xFFDC2626),
    "zratyfikowane" to Color(0xFF059669),
    "odrzucona" to Color(0xFF64748B),
)
private val MODUL = mapOf("merch" to "🛒 Marketplace", "muzyka" to "🎵 Muzyka", "model3d" to "🧊 Assety3D", "wideo" to "🎬 Wideo")
private val ZNAK_STANU = mapOf("czeka" to "○", "trwa" to "◐", "gotowe" to "●", "blad" to "✕", "przerwane" to "◌")
private val CZERWONY = Color(0xFFDC2626)

private fun kolor(etap: String) = KOLOR_ETAPU[etap] ?: Color(0xFF64748B)

/** „12 min temu" z czasu ISO mostu; pusty, gdy most nie podał czasu. */
fun kiedy(iso: String?, teraz: Long = System.currentTimeMillis()): String =
    iso?.let(::czasIso)?.let { ileTemu(((teraz - it) / 1000).coerceAtLeast(0)) }.orEmpty()

@Composable
private fun ZnaczekEtapu(etap: String) {
    val k = kolor(etap)
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(k.copy(alpha = 0.14f))
            .border(0.8.dp, k.copy(alpha = 0.45f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
    ) { Text(KartaStolu.NAZWA_ETAPU[etap] ?: etap, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = k) }
}

@Composable
internal fun Marmur(modifier: Modifier = Modifier, akcent: Color? = null, onClick: (() -> Unit)? = null, tresc: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .let { if (akcent != null) it.border(1.dp, akcent.copy(alpha = 0.35f), RoundedCornerShape(18.dp)) else it },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { tresc() } }
}

/** Karta propozycji na stole — to, co widać bez otwierania. */
@Composable
fun KartaNaStole(k: KartaStolu, onOtworz: () -> Unit) {
    Marmur(akcent = kolor(k.etap), onClick = onOtworz) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(k.tytul, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(8.dp))
            ZnaczekEtapu(k.etap)
        }
        Text(
            listOfNotNull(KartaStolu.ZRODLA[k.zrodlo] ?: k.zrodlo, k.zalozyl?.let { "z „$it”" }, kiedy(k.od).ifBlank { null }).joinToString(" · "),
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(k.wizja, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        k.projekt?.let { p ->
            Text(
                buildString {
                    append("Projekt Stada: ${p.gotowe}/${p.razem} wkładów")
                    if (p.rundy > 1) append(" · runda ${p.runda}/${p.rundy}")
                    p.ostatniaOcena?.let { append(" · Sędzia $it/10") }
                    if (p.zlecenia.isNotEmpty()) append(" · zlecenia ${p.zlecenia.count { it.stan == "gotowe" }}/${p.zlecenia.size}")
                },
                fontSize = 12.sp, color = kolor(k.etap),
            )
        }
        if (k.projekt == null && k.sugerowani.isNotEmpty()) {
            Text("Proponowani: ${k.sugerowani.joinToString(", ")}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun Pusto(tytul: String, opis: String) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(tytul, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(opis, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Ostrzezenie(blad: String?) {
    blad?.let { Text("⚠️ $it", fontSize = 13.sp, color = CZERWONY, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) }
}

/** Nagłówek, gdy telefon nie zna jeszcze swojej Katedry. */
@Composable
fun NieSparowany(onDoKatedry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🪑 Stół czeka na Katedrę", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(
            "Karty na stole, decyzje i historia żyją w Twojej Katedrze. Sparuj telefon (QR z karty „StoL” w Katedrze), a stół się zapełni.",
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onDoKatedry) { Text("Przejdź do parowania") }
    }
}

/** Zakładka 0 — cały stół, karty pogrupowane po drodze: na stole → opracowuje → do akceptacji → zratyfikowane. */
@Composable
fun StolEkran(karty: List<KartaStolu>, wczytano: Boolean, blad: String?, onOtworz: (KartaStolu) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Ostrzezenie(blad) }
        if (karty.isEmpty()) {
            item {
                if (wczytano) Pusto("Stół jest pusty", "Połóż propozycję przyciskiem „Połóż na stół”, albo w Katedrze: Podcast Twin → „Na Stół” / „📄 Plik”.")
                else Pusto("Pytam Katedrę…", "Karty przyjdą z mostu przez Kwantowy Tunel.")
            }
        }
        KartaStolu.ETAPY.forEach { (etap, nazwa) ->
            val tu = karty.filter { it.etap == etap }
            if (tu.isNotEmpty()) {
                item(key = "n-$etap") {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(kolor(etap)))
                        Spacer(Modifier.width(8.dp))
                        Text("$nazwa · ${tu.size}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
                items(tu, key = { it.id }) { k -> KartaNaStole(k) { onOtworz(k) } }
            }
        }
    }
}

/** Zakładka 1 — Izba Akceptacji: tylko to, co czeka na Suwerena (przyjąć propozycję albo zratyfikować Biblię). */
@Composable
fun IzbaAkceptacji(
    karty: List<KartaStolu>, wczytano: Boolean, blad: String?, onOtworz: (KartaStolu) -> Unit,
    /** 🏛️ Katedry czekające na zatwierdzenie w sieci otakos.wtf — tylko gdy ta Katedra jest zarządcą rejestru. */
    rejestr: StanRejestru? = null,
    pracujeRejestr: Boolean = false,
    onKatedra: (OczekujacaKatedra, Boolean) -> Unit = { _, _ -> },
    /** 📺 Publikacje YouTube przygotowane przez Kronikarza. */
    publikacjeYT: List<PublikacjaYouTube> = emptyList(),
    pracujeYT: Boolean = false,
    onPublikacja: (PublikacjaYouTube, Boolean) -> Unit = { _, _ -> },
) {
    val czeka = karty.filter { it.czekaNaSuwerena }
    val katedry = rejestr?.takeIf { it.jestZarzadca }?.oczekujace.orEmpty()
    val doYT = publikacjeYT.filter { it.czekaNaSuwerena }
    val prywatne = publikacjeYT.filter { it.etap == "prywatna" || it.etap == "blad" }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Izba Akceptacji", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(
                "Propozycje czekają na przyjęcie (→ Projekt Stada), gotowe Biblie — na ratyfikację. Dopiero ratyfikacja uruchamia moduły Katedry.",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { Ostrzezenie(blad) }
        if (katedry.isNotEmpty()) {
            item { Text("🏛️ Katedry do zatwierdzenia · ${katedry.size}", fontWeight = FontWeight.SemiBold, color = Color(0xFFD97706)) }
            items(katedry, key = { "katedra-" + it.nick + it.klucz }) { k ->
                Marmur(akcent = Color(0xFFD97706)) {
                    Text(k.nick, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    val temu = czasIso(k.kiedy)?.let { ileTemu((System.currentTimeMillis() - it) / 1000) }
                    Text(listOfNotNull(k.powod, temu?.let { "meldunek $it" }).joinToString(" · "), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("klucz: ${k.klucz.takeLast(16)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onKatedra(k, true) }, enabled = !pracujeRejestr) { Text("✓ Zatwierdź na otakos.wtf") }
                        OutlinedButton(onClick = { onKatedra(k, false) }, enabled = !pracujeRejestr) { Text("Odrzuć") }
                    }
                }
            }
        }
        rejestr?.takeIf { it.jestZarzadca }?.blad?.let { b -> item { Text("Rejestr otakos.wtf: $b", fontSize = 12.sp, color = Color(0xFFD97706)) } }
        if (doYT.isNotEmpty()) {
            item { Text("📺 Na YouTube · ${doYT.size}", fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626)) }
            items(doYT, key = { "yt-" + it.id }) { pub ->
                Marmur(akcent = Color(0xFFDC2626)) {
                    Text(pub.tytul, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(pub.nazwa, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    pub.kanalNazwa?.let { Text("→ kanał: $it", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626)) }
                    Text(pub.opis, fontSize = 13.sp, maxLines = 8, overflow = TextOverflow.Ellipsis)
                    if (pub.tagi.isNotEmpty()) Text(pub.tagi.joinToString(" ") { "#$it" }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Kronikarz to napisał. Poprawki tytułu, opisu i kanału — w Hubie (Impresariat). Film pójdzie jako niepubliczny.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onPublikacja(pub, true) }, enabled = !pracujeYT) { Text("✓ Wyślij na YouTube") }
                        OutlinedButton(onClick = { onPublikacja(pub, false) }, enabled = !pracujeYT) { Text("Odrzuć") }
                    }
                }
            }
        }
        if (prywatne.isNotEmpty()) {
            items(prywatne, key = { "ytp-" + it.id }) { pub ->
                Text("📺 ${pub.tytul.ifBlank { pub.nazwa }} — ${pub.opisEtapu}${(pub.blad ?: pub.uwaga)?.let { ": $it" } ?: ""}", fontSize = 12.sp, color = Color(0xFFD97706))
            }
        }
        if (czeka.isEmpty() && katedry.isEmpty() && doYT.isEmpty() && wczytano) item { Pusto("Nic nie czeka", "Wszystkie karty są w pracy stada albo już zdecydowane.") }
        val (ratyfikacja, przyjecie) = czeka.partition { it.moznaRatyfikowac }
        if (ratyfikacja.isNotEmpty()) {
            item { Text("Do ratyfikacji · ${ratyfikacja.size}", fontWeight = FontWeight.SemiBold, color = kolor("do_akceptacji")) }
            items(ratyfikacja, key = { it.id }) { k -> KartaNaStole(k) { onOtworz(k) } }
        }
        if (przyjecie.isNotEmpty()) {
            item { Text("Do przyjęcia · ${przyjecie.size}", fontWeight = FontWeight.SemiBold, color = kolor("na_stole")) }
            items(przyjecie, key = { it.id }) { k -> KartaNaStole(k) { onOtworz(k) } }
        }
    }
}

/** Zakładka 2 — Historia: szyna Katedry (Stół, projekty stada, praca TeOgochi), najnowsze pierwsze. */
@OptIn(ExperimentalMaterial3Api::class)   // FilterChip: stabilny w material3 1.3, eksperymentalny we wcześniejszych
@Composable
fun HistoriaKatedry(zdarzenia: List<ZdarzenieSzyny>, wczytano: Boolean, blad: String?) {
    var tylkoStol by remember { mutableStateOf(true) }
    val widoczne = if (tylkoStol) zdarzenia.filter { it.rodzaj == "stol" || it.rodzaj == "projekt" || it.agent == "Stół" } else zdarzenia
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Historia", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Szyna zdarzeń Katedry — to, co naprawdę się stało.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                FilterChip(selected = tylkoStol, onClick = { tylkoStol = true }, label = { Text("Stół i projekty") })
                FilterChip(selected = !tylkoStol, onClick = { tylkoStol = false }, label = { Text("Cała szyna") })
            }
        }
        item { Ostrzezenie(blad) }
        if (widoczne.isEmpty() && wczytano) item { Pusto("Cisza", "Szyna Katedry nie ma jeszcze takich zdarzeń (po restarcie mostu zaczyna od nowa).") }
        items(widoczne, key = { "${it.id ?: 0}-${it.kiedy}-${it.agent}" }) { z ->
            Marmur {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(z.agent.ifBlank { "Katedra" }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(kiedy(z.kiedy), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(z.tresc.ifBlank { z.rodzaj }, fontSize = 13.sp)
            }
        }
    }
}

/** Zakładka 3 — Agenci: stado TeOgochi z Katedry (wykluci pierwsi), bez wymyślonych statusów. */
@Composable
fun AgenciStada(
    gatunki: List<Gatunek>,
    wczytano: Boolean,
    rozmowcy: List<Rozmowca> = emptyList(),
    onRozmowa: (Rozmowca) -> Unit = {},
    onPamiec: () -> Unit = {},
) {
    val lista = gatunki.sortedWith(compareByDescending<Gatunek> { it.wyklute }.thenByDescending { it.xp })
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Agenci stada", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("To oni opracowują karty ze stołu — każdy na swoim modelu, w swojej dziedzinie. Stuknij, by porozmawiać.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { DelegatPasek(rozmowcy, onRozmowa, onPamiec) }
        if (lista.isEmpty() && wczytano) item { Pusto("Stado milczy", "Katedra nie opublikowała jeszcze migawki stada.") }
        items(lista, key = { it.id }) { g ->
            // Rozmówca Delegata pasuje po id gatunku albo imieniu — wtedy karta agenta otwiera rozmowę.
            val rozmowca = rozmowcy.find { it.id.equals(g.id, true) || it.imie.equals(g.imie, true) }
            Marmur(akcent = runCatching { Color(kolorHex(g.kolor)) }.getOrNull(), onClick = rozmowca?.let { r -> { onRozmowa(r) } }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(g.forma, fontSize = 26.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.imie, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(g.dziedzina, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text((if (rozmowca != null) "💬 " else "") + (if (g.wyklute) "${g.xp} XP" else "🥚 jajko"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                g.robi?.let { Text("${it}${g.robiOd?.let { t -> " · " + ileTemu(((System.currentTimeMillis() - t) / 1000).coerceAtLeast(0)) } ?: ""}", fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

/** "#a855f7" → ARGB. */
internal fun kolorHex(hex: String): Long {
    val h = hex.trim().removePrefix("#")
    require(h.length == 6) { "kolor $hex" }
    return 0xFF000000L or h.toLong(16)
}

/**
 * Karta otwarta: pełna wizja, uczestnicy do wyboru przy przyjęciu, Biblia przy ratyfikacji,
 * zlecenia modułów po ratyfikacji i zapis decyzji. Przyciski tylko te, na które pozwala etap.
 */
@Composable
fun KartaOtwarta(
    k: KartaStolu,
    wyklute: List<Gatunek>,
    pracuje: Boolean,
    blad: String?,
    onDecyzja: (AkcjaStolu, List<String>, Warsztat?) -> Unit,
    onZamknij: () -> Unit,
) {
    // Przyjęcie: ile rund doskonalenia i ile pętli na punkt planu. Doskonalenie: ile rund jeszcze.
    var warsztat by remember(k.id) { mutableStateOf(Warsztat(rundy = 1, petla = k.projekt?.petla ?: 0)) }
    // Domyślnie: zaproponowani w karcie (po imieniu albo id), a gdy mniej niż dwóch — całe wyklute stado.
    val proponowani = wyklute.filter { g -> k.sugerowani.any { it.equals(g.imie, true) || it.equals(g.id, true) } }.map { it.id }
    var wybrani by remember(k.id, wyklute) { mutableStateOf((if (proponowani.size >= 2) proponowani else wyklute.map { it.id }).toSet()) }
    var calaTresc by remember(k.id) { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onZamknij, enabled = !pracuje) { Text("← Stół") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(k.tytul, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            ZnaczekEtapu(k.etap)
        }
        Text(
            listOfNotNull(KartaStolu.ZRODLA[k.zrodlo] ?: k.zrodlo, k.zalozyl?.let { "z „$it”" }, kiedy(k.od).ifBlank { null }).joinToString(" · "),
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Marmur {
            Text("Wizja", fontWeight = FontWeight.SemiBold)
            Text(k.wizja, fontSize = 14.sp)
            if (k.tresc.isNotBlank()) {
                TextButton(onClick = { calaTresc = !calaTresc }) { Text(if (calaTresc) "Zwiń treść" else "Pokaż całą treść karty") }
                if (calaTresc) Text(k.tresc, fontSize = 13.sp)
            }
        }

        when (k.etap) {
            "na_stole", "utknela" -> {
                if (k.etap == "utknela") Text("Projekt stada padł albo przerwał go restart Katedry — możesz przyjąć kartę od nowa.", fontSize = 13.sp, color = CZERWONY)
                Marmur {
                    Text("Kto opracuje kartę", fontWeight = FontWeight.SemiBold)
                    if (wyklute.isEmpty()) Text("Nie widzę wyklutego stada — most wybierze sam (proponowani z karty albo całe stado).", fontSize = 12.sp)
                    wyklute.forEach { g ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = g.id in wybrani, enabled = !pracuje, onCheckedChange = { wybrani = if (it) wybrani + g.id else wybrani - g.id })
                            Text("${g.forma} ${g.imie}", fontSize = 15.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(g.dziedzina, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    val obcy = k.sugerowani.filter { s -> wyklute.none { it.imie.equals(s, true) || it.id.equals(s, true) } }
                    if (obcy.isNotEmpty() && wyklute.isNotEmpty()) Text("Jeszcze nie wykluci: ${obcy.joinToString(", ")}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Marmur {
                    Text("Jak ma pracować stado", fontWeight = FontWeight.SemiBold)
                    WyborWarsztatu(warsztat, pracuje, zPetla = true) { warsztat = it }
                }
                val zaMalo = wyklute.isNotEmpty() && wybrani.size < 2
                Button(
                    onClick = { onDecyzja(AkcjaStolu.PRZYJMIJ, if (wyklute.isEmpty()) emptyList() else wyklute.map { it.id }.filter { it in wybrani }, warsztat) },
                    enabled = !pracuje && !zaMalo, modifier = Modifier.fillMaxWidth(),
                ) { Text(if (pracuje) "Wysyłam do Katedry…" else "Przyjmij → Projekt Stada") }
                if (zaMalo) Text("Wspólny projekt potrzebuje co najmniej dwóch TeOgochi.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Stado napisze wkłady i Biblię projektu. Moduły (produkty, muzyka, 3D, wideo) ruszą dopiero po Twojej ratyfikacji.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            "opracowuje" -> Marmur(akcent = kolor("opracowuje")) {
                Text("Stado pracuje", fontWeight = FontWeight.SemiBold)
                k.projekt?.let {
                    if (it.rundy > 1) Text("Runda ${it.runda} z ${it.rundy}${if (it.petla > 0) " · pętla kreatywna ×${it.petla}" else ""}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = kolor("opracowuje"))
                    Text("${it.gotowe}/${it.razem} wkładów gotowych w tej rundzie. Gdy powstanie Biblia, karta wróci do Izby Akceptacji.", fontSize = 13.sp)
                }
                k.projekt?.let { OcenySedziego(it.oceny, it.braki) }
            }
            "do_akceptacji" -> {
                Marmur(akcent = kolor("do_akceptacji")) {
                    Text("Biblia projektu", fontWeight = FontWeight.SemiBold)
                    val biblia = k.projekt?.biblia.orEmpty()
                    Text(biblia.ifBlank { "Scalenie nie powstało — projekt skończył się z brakami. Możesz ratyfikować to, co jest, albo odłożyć kartę." }, fontSize = 13.sp)
                    k.projekt?.let { Text("Wkłady: ${it.gotowe}/${it.razem}${if (it.rundy > 1) " · po ${it.runda} z ${it.rundy} rund" else ""}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                k.projekt?.takeIf { it.oceny.isNotEmpty() }?.let { p -> Marmur { OcenySedziego(p.oceny, p.braki) } }
                Button(
                    onClick = { onDecyzja(AkcjaStolu.RATYFIKUJ, emptyList(), null) }, enabled = !pracuje, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = kolor("zratyfikowane")),
                ) { Text(if (pracuje) "Wysyłam do Katedry…" else "Ratyfikuj → moduły Katedry") }
                Text("Ratyfikacja zleca modułom to, co przewidziały wkłady stada (produkty do Marketplace, muzykę, bryły 3D, wideo).", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            "zratyfikowane" -> Marmur(akcent = kolor("zratyfikowane")) {
                Text("Zlecenia modułów", fontWeight = FontWeight.SemiBold)
                val z = k.projekt?.zlecenia.orEmpty()
                if (z.isEmpty()) Text("Wkłady nie miały zleceń dla modułów.", fontSize = 13.sp)
                z.forEach {
                    Text(
                        "${ZNAK_STANU[it.stan] ?: "·"} ${MODUL[it.modul] ?: it.modul}: ${it.opis}", fontSize = 13.sp,
                        color = if (it.stan == "blad" || it.stan == "przerwane") CZERWONY else MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (z.any { it.stan == "blad" || it.stan == "przerwane" }) Text("Nieudane zlecenia ponawia się przy Katedrze (Projekt Stada → ponów).", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (k.moznaNaNoc) WarsztatKarty(k, warsztat, pracuje, onDecyzja) { warsztat = it }

        if (k.moznaOdrzucic) {
            OutlinedButton(onClick = { onDecyzja(AkcjaStolu.ODRZUC, emptyList(), null) }, enabled = !pracuje, modifier = Modifier.fillMaxWidth()) { Text("Odłóż ze stołu") }
        }
        blad?.let { Text("⚠️ $it", fontSize = 13.sp, color = CZERWONY) }

        if (k.decyzje.isNotEmpty()) {
            Text("Decyzje", fontWeight = FontWeight.SemiBold)
            k.decyzje.forEach { d ->
                val co = mapOf("przyjeta" to "przyjął", "odrzucona" to "odłożył", "zratyfikowana" to "zratyfikował", "doskonalona" to "odesłał do doskonalenia", "na_noc" to "dał na Nocną Zmianę")[d.co] ?: d.co
                Text("• ${d.kto} $co · ${kiedy(d.kiedy)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.size(40.dp))
    }
}

/** Oceny Sędziego po rundach (zgodność Biblii z wizją) i braki, na których stado buduje dalej. */
@Composable
private fun OcenySedziego(oceny: List<com.example.most.OcenaRundy>, braki: List<String>) {
    if (oceny.isEmpty()) return
    Text("⚖️ Sędzia: zgodność z wizją", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Text(oceny.joinToString("  →  ") { "R${it.runda}: ${it.ocena?.let { o -> "$o/10" } ?: "—"}" }, fontSize = 13.sp)
    braki.forEach { Text("· $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

/** Liczba z przyciskami − / + (rundy, pętla) — palcem na telefonie łatwiej niż pole tekstowe. */
@Composable
private fun Licznik(etykieta: String, wartosc: Int, zakres: IntRange, wlaczony: Boolean, onZmiana: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(etykieta, fontSize = 13.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = { onZmiana(wartosc - 1) }, enabled = wlaczony && wartosc > zakres.first) { Text("−", fontSize = 18.sp) }
        Text("$wartosc", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
        TextButton(onClick = { onZmiana(wartosc + 1) }, enabled = wlaczony && wartosc < zakres.last) { Text("+", fontSize = 18.sp) }
    }
}

/** Rundy doskonalenia i pętla kreatywna na punkt planu — w granicach mostu. */
@Composable
fun WyborWarsztatu(w: Warsztat, pracuje: Boolean, zPetla: Boolean, onZmiana: (Warsztat) -> Unit) {
    Column {
        Licznik("Rundy doskonalenia", w.rundy, 1..Warsztat.MAX_RUND, !pracuje) { onZmiana(w.copy(rundy = it)) }
        if (zPetla) Licznik("Pętla kreatywna na punkt planu", w.petla, 0..Warsztat.MAX_PETLI, !pracuje) { onZmiana(w.copy(petla = it)) }
        if (w.rundy > 1 || w.petla > 0) Text(
            "Małe lokalne modele potrzebują kilku przejść — każda runda i pętla to kolejne minuty pracy karty graficznej.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun rund(n: Int) = if (n == 1) "runda" else if (n in 2..4) "rundy" else "rund"

/**
 * Warsztat karty: kolejne rundy doskonalenia TERAZ (▶, na żywo — karta pokazuje postęp) albo na Nocną Zmianę
 * ×N (Katedra zrobi je, gdy Suweren śpi). Do akceptacji, po ratyfikacji (wtedy znów do akceptacji) i w trakcie
 * pracy (tylko noc — teraz stado i tak pracuje).
 */
@Composable
fun WarsztatKarty(
    k: KartaStolu,
    w: Warsztat,
    pracuje: Boolean,
    onDecyzja: (AkcjaStolu, List<String>, Warsztat?) -> Unit,
    onZmiana: (Warsztat) -> Unit,
) {
    Marmur(akcent = kolor("opracowuje")) {
        Text(if (k.etap == "do_akceptacji") "Jeszcze nie to? Doskonal" else "Rundy doskonalenia", fontWeight = FontWeight.SemiBold)
        Text(
            "Stado dołoży kolejne cegiełki na brakach Sędziego — każdy na swoim poprzednim wkładzie. Ocena ${Warsztat.CEL_OCENY}/10 kończy wcześniej." +
                if (k.etap == "zratyfikowane") " Po rundach karta znów czeka na Twoją ratyfikację." else "",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        k.nocna?.let { n ->
            n.zadania.forEach { z ->
                Text(
                    "🌙 Na noc: ${z.rundy} ${rund(z.rundy)} × ${z.powtorzenia} · zrobione ${z.wykonane}/${z.powtorzenia}" +
                        when (z.stan) { "trwa" -> " · trwa teraz"; "blad" -> " · padło: ${z.blad ?: "błąd"}"; else -> "" },
                    fontSize = 12.sp, color = if (z.stan == "blad") CZERWONY else kolor("opracowuje"),
                )
            }
            if (!n.wlaczona) Text(
                "Nocna Zmiana jest wyłączona w Katedrze — zadania na noc czekają, aż ją włączysz przy komputerze.",
                fontSize = 12.sp, color = kolor("do_akceptacji"),
            )
        }
        WyborWarsztatu(w, pracuje, zPetla = true, onZmiana = onZmiana)
        if (k.moznaDoskonalic) {
            Button(onClick = { onDecyzja(AkcjaStolu.DOSKONAL, emptyList(), w) }, enabled = !pracuje, modifier = Modifier.fillMaxWidth()) {
                Text(if (pracuje) "Wysyłam do Katedry…" else "▶ Teraz: ${w.rundy} ${rund(w.rundy)}")
            }
        }
        Licznik("Powtórzeń na noc", w.powtorzenia, 1..Warsztat.MAX_POWTORZEN, !pracuje) { onZmiana(w.copy(powtorzenia = it)) }
        OutlinedButton(onClick = { onDecyzja(AkcjaStolu.NOCNA, emptyList(), w) }, enabled = !pracuje, modifier = Modifier.fillMaxWidth()) {
            Text(if (pracuje) "Wysyłam do Katedry…" else "🌙 Na Nocną Zmianę: ${w.powtorzenia} × ${w.rundy} ${rund(w.rundy)}")
        }
    }
}

/** Nowa propozycja z telefonu: tytuł i treść (można wkleić całą rozmowę z „KARTĄ DLA STOŁU"). */
@Composable
fun FormularzKarty(pracuje: Boolean, blad: String?, onPoloz: (NowaKarta) -> Unit, onAnuluj: () -> Unit) {
    var tytul by remember { mutableStateOf("") }
    var tresc by remember { mutableStateOf("") }
    val karta = NowaKarta(tytul, tresc)
    val brak = karta.brak()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onAnuluj, enabled = !pracuje) { Text("← Wróć") }
        Text("Połóż na stół", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Propozycja dla stada. Sekcje „Wizja:” i „Uczestnicy:” w treści Katedra sama odczyta (tak jak z Podcast Twin).",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = tytul, onValueChange = { tytul = it.take(NowaKarta.MAX_TYTULU) }, label = { Text("Tytuł, np. Forge Fashion") },
            singleLine = true, enabled = !pracuje, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = tresc, onValueChange = { tresc = it.take(NowaKarta.MAX_TRESCI) }, label = { Text("Treść propozycji") },
            minLines = 6, enabled = !pracuje, modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { onPoloz(karta) }, enabled = !pracuje && brak == null, modifier = Modifier.fillMaxWidth()) {
            Text(if (pracuje) "Wysyłam do Katedry…" else "Połóż na stół")
        }
        brak?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        blad?.let { Text("⚠️ $it", fontSize = 13.sp, color = CZERWONY) }
    }
}
