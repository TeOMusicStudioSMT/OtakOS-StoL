package com.example.ui.stol

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.most.WpisKsiegi
import com.example.ui.tlumacz.tl

/*
 * 📖 Żywa Księga w StoL-u (Suweren 2026-10-29: „rób Księgę w StoL… dogrywanie z telefonu”).
 * Iskra z telefonu: parę słów, zdjęcie i film z galerii (nagrywasz aparatem telefonu, potem wybierasz).
 * Słowo Kronikarza i obraz Palety tworzy stado w Katedrze. Film oglądasz w Hubie albo na otakos.wtf (wpis 🌍).
 */

private val PERGAMIN = Color(0xFFF3E4C4)
private val ATRAMENT = Color(0xFF2C1810)
private val SEPIA = Color(0xFF7A5230)
private val OKUCIE = Color(0xFF3B2314)

@Composable
fun KsiegaEkran(vm: KsiegaViewModel, onKomunikat: (String) -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(ui.komunikat) { ui.komunikat?.let { onKomunikat(it); vm.komunikatPokazany() } }

    var tekst by remember { mutableStateOf("") }
    var publiczny by remember { mutableStateOf(true) }
    var zdjecie by remember { mutableStateOf<Uri?>(null) }
    var film by remember { mutableStateOf<Uri?>(null) }
    val wybierzZdjecie = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { if (it != null) zdjecie = it }
    val wybierzFilm = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { if (it != null) film = it }
    // Dołożenie do istniejącego wpisu: najpierw zapamiętaj, do którego, potem galeria.
    var doWpisu by remember { mutableStateOf<Pair<String, String>?>(null) }
    val dolozZGalerii = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val cel = doWpisu
        if (uri != null && cel != null) vm.doloz(cel.first, cel.second, uri)
        doWpisu = null
    }
    val pracuje = ui.wysyla != null

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tl("📖 Żywa Księga"), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(tl("Twoja Iskra — stado dotwarza słowo i obraz w Katedrze"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = { vm.odswiez() }, enabled = !ui.wczytuje) { Text(if (ui.wczytuje) tl("⟳ …") else tl("↻")) }
            }
            ui.blad?.let { Text("⚠️ $it", color = Color(0xFFDC2626), fontSize = 12.sp) }
        }
        item {
            Marmur {
                Text(tl("✨ Wrzutnia Iskry"), fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = tekst, onValueChange = { if (it.length <= WpisKsiegi.MAX_TEKSTU) tekst = it },
                    placeholder = { Text(tl("Parę słów z przepływu…")) }, minLines = 3, maxLines = 8, enabled = !pracuje,
                    modifier = Modifier.fillMaxWidth().testTag("pole_iskry"),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { wybierzZdjecie.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !pracuje) {
                        Text(if (zdjecie != null) tl("📷 zdjęcie ✓") else tl("📷 zdjęcie"))
                    }
                    OutlinedButton(onClick = { wybierzFilm.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }, enabled = !pracuje) {
                        Text(if (film != null) tl("🎥 film ✓") else tl("🎥 film"))
                    }
                }
                if (zdjecie != null || film != null) TextButton(onClick = { zdjecie = null; film = null }, enabled = !pracuje) { Text(tl("✕ bez plików")) }
                Text(tl("Film nagraj aparatem telefonu i wybierz z galerii — do 95 MB (limit tunelu). Zdjęcie idzie bez danych GPS."), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = publiczny, onCheckedChange = { publiczny = it }, enabled = !pracuje)
                    Text(tl("🌍 dla świata (otakos.wtf)"), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Button(
                        onClick = { vm.dodaj(tekst, publiczny, zdjecie, film) { tekst = ""; zdjecie = null; film = null } },
                        enabled = tekst.isNotBlank() && !pracuje, modifier = Modifier.testTag("btn_zostaw_iskre"),
                    ) { Text(tl("📖 Zostaw")) }
                }
                ui.wysyla?.let { Text("⏳ $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary) }
                ui.bladAkcji?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️ $it", color = Color(0xFFDC2626), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.zamknijBlad() }) { Text("✕") }
                    }
                }
            }
        }
        if (ui.wczytano && ui.wpisy.isEmpty()) item { Pusto("Księga czeka", "Pierwsza Iskra może być stąd — parę słów, zdjęcie, film.") }
        items(ui.wpisy, key = { it.id }) { w ->
            KartaWpisuKsiegi(
                w = w, obrazy = ui.obrazy, pracuje = pracuje,
                onObraz = { plik -> vm.wczytajObraz(w, plik) },
                onPubliczny = { vm.przelaczPubliczny(w) },
                onPonow = { vm.ponowStado(w) },
                onDoloz = { rodzaj ->
                    doWpisu = w.id to rodzaj
                    dolozZGalerii.launch(PickVisualMediaRequest(if (rodzaj == "zdjecie") ActivityResultContracts.PickVisualMedia.ImageOnly else ActivityResultContracts.PickVisualMedia.VideoOnly))
                },
            )
        }
    }
}

@Composable
private fun KartaWpisuKsiegi(
    w: WpisKsiegi, obrazy: Map<String, android.graphics.Bitmap>, pracuje: Boolean,
    onObraz: (String) -> Unit, onPubliczny: () -> Unit, onPonow: () -> Unit, onDoloz: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(OKUCIE).padding(4.dp)
            .clip(RoundedCornerShape(10.dp)).background(PERGAMIN).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(kiedyWpisu(w.kiedy), fontSize = 11.sp, color = SEPIA, modifier = Modifier.weight(1f))
            TextButton(onClick = onPubliczny, enabled = !pracuje) { Text(if (w.publiczny) tl("🌍 dla świata") else tl("🔒 tylko ja"), fontSize = 12.sp, color = SEPIA) }
        }
        w.zdjecie?.let { plik -> ObrazWpisu(w, plik, obrazy, onObraz, wysoki = true) }
        Text(w.tekst, fontFamily = FontFamily.Serif, fontSize = 16.sp, color = ATRAMENT)
        if (w.maNagranie) Text("🎥 " + tl("nagranie") + (w.nagranieSekundy?.let { " · ${it.toInt()} s" } ?: "") + " · " + tl("obejrzysz w Hubie albo na otakos.wtf"), fontSize = 12.sp, color = SEPIA)
        if (w.rysunkow > 0) Text("✒️ " + tl("żywa kreska") + " ×${w.rysunkow}", fontSize = 12.sp, color = SEPIA)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFEFDCB4)).border(1.dp, Color(0xFFC4A168), RoundedCornerShape(8.dp)).padding(10.dp),
        ) {
            Text(tl("📜 Kronikarz odpowiada"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SEPIA)
            val s = w.slowo
            if (s?.gotowy == true && s.tekst != null) Text(s.tekst, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 14.sp, color = ATRAMENT)
            else Text(s?.opisStanu?.let { tl(it) } ?: "—", fontSize = 12.sp, color = SEPIA)
        }
        val il = w.ilustracja
        if (il?.gotowy == true && il.plik != null) ObrazWpisu(w, il.plik, obrazy, onObraz, wysoki = false)
        else Text(tl("🎨 Paleta maluje") + ": " + (il?.opisStanu?.let { tl(it) } ?: "—"), fontSize = 12.sp, color = SEPIA)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { onDoloz("zdjecie") }, enabled = !pracuje) { Text(if (w.zdjecie != null) tl("📷 zmień") else tl("📷 dodaj"), fontSize = 12.sp) }
            OutlinedButton(onClick = { onDoloz("nagranie") }, enabled = !pracuje) { Text(if (w.maNagranie) tl("🎥 zmień") else tl("🎥 dograj"), fontSize = 12.sp) }
            if (w.stadoBlad) OutlinedButton(onClick = onPonow, enabled = !pracuje) { Text(tl("↻ stado"), fontSize = 12.sp) }
        }
    }
}

@Composable
private fun ObrazWpisu(w: WpisKsiegi, plik: String, obrazy: Map<String, android.graphics.Bitmap>, onObraz: (String) -> Unit, wysoki: Boolean) {
    val klucz = KsiegaViewModel.kluczObrazu(w, plik)
    LaunchedEffect(klucz) { onObraz(plik) }
    val bmp = obrazy[klucz]
    if (bmp == null) {
        Text(tl("🖼️ wczytuję…"), fontSize = 11.sp, color = SEPIA)
        return
    }
    val obraz = remember(bmp) { bmp.asImageBitmap() }
    Image(
        bitmap = obraz, contentDescription = null, contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxWidth().heightIn(max = if (wysoki) 360.dp else 320.dp).clip(RoundedCornerShape(8.dp)).border(2.dp, Color(0xFF5C3A21), RoundedCornerShape(8.dp)),
    )
    Spacer(Modifier.width(1.dp))
}

/** „2026-10-29T14:35:44.482Z” → „2026-10-29 14:35” (UTC, jak w Historii). */
private fun kiedyWpisu(iso: String) = iso.take(16).replace('T', ' ')
