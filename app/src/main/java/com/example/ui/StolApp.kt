package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.most.KartaStolu
import com.example.ui.components.MarbleBackground
import com.example.ui.components.TableLightingMode
import com.example.ui.katedra.KatedraView
import com.example.ui.katedra.KatedraViewModel
import com.example.ui.stol.AgenciStada
import com.example.ui.stol.FormularzKarty
import com.example.ui.stol.HistoriaKatedry
import com.example.ui.stol.IzbaAkceptacji
import com.example.ui.stol.KartaOtwarta
import com.example.ui.stol.NieSparowany
import com.example.ui.stol.PamiecKatedryEkran
import com.example.ui.stol.RozmowaDelegata
import com.example.ui.stol.StolEkran
import com.example.ui.stol.StolViewModel
import com.example.ui.stol.TostKontakty
import com.example.ui.stol.TostWatek
import kotlinx.coroutines.delay

/**
 * StoL: Stół ratyfikacji Katedry na marmurze z AI Studio — ale na prawdziwych danych z mostu.
 * 0 Stół · 1 Izba Akceptacji · 2 Historia · 3 Agenci (rozmowa przez Delegata, pamięć Katedry) · 4 Katedra (parowanie, stado, świat klocków)
 * · 5 TOST (rozmowy z innymi Katedrami przez własną Katedrę).
 */
@Composable
fun StolApp(
    viewModel: StolViewModel,
    katedraViewModel: KatedraViewModel,
    modifier: Modifier = Modifier
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var oswietlenie by remember { mutableStateOf(TableLightingMode.PEARL_OPAL) }
    val cyklZycia = LocalLifecycleOwner.current

    // Stół żyje z mostu: co 10 s na zakładkach stołu, co 30 s gdzie indziej (zapowiedzi głosowe muszą
    // słyszeć koniec projektu także, gdy patrzysz na Katedrę) — tylko gdy apka jest na ekranie.
    LaunchedEffect(ui.zakladka) {
        val coIle = if (ui.zakladka in 0..3) 10_000L else 30_000L
        cyklZycia.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.odswiez()
                delay(coIle)
            }
        }
    }
    // TOST: kontakty i otwarty wątek co 5 s, tylko gdy zakładka TOST jest na ekranie.
    LaunchedEffect(ui.zakladka, ui.tostZ) {
        if (ui.zakladka != 5) return@LaunchedEffect
        cyklZycia.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.odswiezTost()
                delay(5_000)
            }
        }
    }
    LaunchedEffect(ui.komunikat) {
        ui.komunikat?.let {
            snackbar.showSnackbar(it)
            viewModel.komunikatPokazany()
        }
    }
    BackHandler(enabled = ui.otwarta != null && !ui.pracuje) { viewModel.zamknij() }
    BackHandler(enabled = ui.formularz && !ui.pracuje) { viewModel.pokazFormularz(false) }
    BackHandler(enabled = ui.zakladka == 3 && ui.rozmowa != null) { viewModel.zamknijRozmowe() }
    BackHandler(enabled = ui.zakladka == 3 && ui.rozmowa == null && ui.pamiecOtwarta) { viewModel.pokazPamiec(false) }
    BackHandler(enabled = ui.zakladka == 5 && ui.tostZ != null) { viewModel.zamknijTost() }

    MarbleBackground(modifier = modifier, lightingMode = oswietlenie) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                if (ui.zakladka != 4 && ui.zakladka != 5) {
                    NaglowekStolu(ui.karty, glos = ui.glos, onGlos = { viewModel.przelaczGlos() }, onOswietlenie = {
                        oswietlenie = TableLightingMode.entries[(oswietlenie.ordinal + 1) % TableLightingMode.entries.size]
                    })
                }
            },
            bottomBar = {
                PasekZakladek(
                    wybrana = ui.zakladka,
                    czeka = ui.karty.count { it.czekaNaSuwerena },
                    tostNowe = ui.tost?.kontakty?.sumOf { it.nieprzeczytane } ?: 0,
                    onWybierz = { viewModel.wybierz(it) }
                )
            },
            floatingActionButton = {
                if (ui.zakladka == 0 && ui.sparowany && ui.otwarta == null && !ui.formularz) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.pokazFormularz(true) },
                        icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                        text = { Text("Połóż na stół", fontWeight = FontWeight.Bold) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_poloz_na_stol")
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                val otwarta = ui.otwarta
                when {
                    ui.zakladka == 4 -> KatedraView(viewModel = katedraViewModel)
                    !ui.sparowany -> NieSparowany(onDoKatedry = { viewModel.wybierz(4) })
                    otwarta != null -> KartaOtwarta(
                        k = otwarta,
                        wyklute = ui.gatunki.filter { it.wyklute },
                        pracuje = ui.pracuje,
                        blad = ui.bladAkcji,
                        onDecyzja = { akcja, uczestnicy, warsztat -> viewModel.decyzja(otwarta, akcja, uczestnicy, warsztat) },
                        onZamknij = { viewModel.zamknij() }
                    )
                    ui.formularz -> FormularzKarty(
                        pracuje = ui.pracuje,
                        blad = ui.bladAkcji,
                        onPoloz = { viewModel.poloz(it) },
                        onAnuluj = { viewModel.pokazFormularz(false) }
                    )
                    ui.zakladka == 5 && ui.tostZ != null -> TostWatek(
                        nick = ui.tostZ!!,
                        wiadomosci = ui.tostWatek,
                        wysyla = ui.tostWysyla,
                        blad = ui.tostBlad,
                        onWyslij = { viewModel.wyslijTost(it) },
                        onZamknij = { viewModel.zamknijTost() }
                    )
                    ui.zakladka == 5 -> TostKontakty(
                        k = ui.tost,
                        wczytuje = ui.tostWczytuje,
                        blad = ui.tostBlad,
                        onOtworz = { viewModel.otworzTost(it) },
                        onOdswiez = { viewModel.odswiezTost() }
                    )
                    ui.zakladka == 0 -> StolEkran(ui.karty, ui.wczytano, ui.blad) { viewModel.otworz(it.id) }
                    ui.zakladka == 1 -> IzbaAkceptacji(ui.karty, ui.wczytano, ui.blad) { viewModel.otworz(it.id) }
                    ui.zakladka == 2 -> HistoriaKatedry(ui.zdarzenia, ui.wczytano, ui.blad)
                    ui.zakladka == 3 && ui.rozmowa != null -> RozmowaDelegata(
                        rozmowa = ui.rozmowa!!,
                        onWyslij = { viewModel.wyslij(it) },
                        onZamknij = { viewModel.zamknijRozmowe() }
                    )
                    ui.zakladka == 3 && ui.pamiecOtwarta -> PamiecKatedryEkran(
                        pamiec = ui.pamiec,
                        wczytuje = ui.pamiecWczytuje,
                        blad = ui.pamiecBlad,
                        onOdswiez = { viewModel.wczytajPamiec() },
                        onZamknijProces = { viewModel.zwolnij(it) },
                        onWroc = { viewModel.pokazPamiec(false) }
                    )
                    ui.zakladka == 3 -> AgenciStada(
                        gatunki = ui.gatunki,
                        wczytano = ui.wczytano,
                        rozmowcy = ui.rozmowcy,
                        onRozmowa = { viewModel.otworzRozmowe(it) },
                        onPamiec = { viewModel.pokazPamiec(true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NaglowekStolu(karty: List<KartaStolu>, glos: Boolean, onGlos: () -> Unit, onOswietlenie: () -> Unit) {
    val ciemny = isSystemInDarkTheme()
    Surface(
        color = if (ciemny) Color(0xD0101724) else Color(0xDCFFFFFF),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .border(0.8.dp, if (ciemny) Color(0x3394A3B8) else Color(0x44CBD5E1))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0F172A))))
                        .border(1.8.dp, Color(0xFFFBBF24), CircleShape)
                ) {
                    Text("S", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("StoL", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp))
                    Text(
                        "Stół ratyfikacji Katedry OtakOS",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onGlos, modifier = Modifier.testTag("btn_glos_stolu")) {
                    Icon(
                        if (glos) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = if (glos) "Zapowiedzi głosowe włączone" else "Zapowiedzi głosowe wyłączone",
                        tint = if (glos) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onOswietlenie) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Światło stołu", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                Licznik("Na stole", karty.count { it.etap == "na_stole" || it.etap == "utknela" }, Color(0xFF0284C7), Modifier.weight(1f))
                Licznik("W pracy", karty.count { it.etap == "opracowuje" }, Color(0xFF7C3AED), Modifier.weight(1f))
                Licznik("Do akcept.", karty.count { it.etap == "do_akceptacji" }, Color(0xFFD97706), Modifier.weight(1f))
                Licznik("Zratyf.", karty.count { it.etap == "zratyfikowane" }, Color(0xFF059669), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Licznik(etykieta: String, ile: Int, kolor: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(8.dp), color = kolor.copy(alpha = 0.12f), modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(etykieta, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = kolor)
            Text("$ile", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = kolor)
        }
    }
}

@Composable
private fun PasekZakladek(wybrana: Int, czeka: Int, tostNowe: Int, onWybierz: (Int) -> Unit) {
    val ciemny = isSystemInDarkTheme()
    NavigationBar(
        containerColor = if (ciemny) Color(0xEE101724) else Color(0xF2FFFFFF),
        tonalElevation = 10.dp,
        modifier = Modifier
            .border(1.dp, if (ciemny) Color(0x2294A3B8) else Color(0x33CBD5E1))
            .navigationBarsPadding()
    ) {
        NavigationBarItem(
            selected = wybrana == 0, onClick = { onWybierz(0) },
            icon = { Icon(Icons.Default.TableRestaurant, contentDescription = "Stół") },
            label = { Text("Stół", fontSize = 11.sp) }, modifier = Modifier.testTag("nav_tab_table")
        )
        NavigationBarItem(
            selected = wybrana == 1, onClick = { onWybierz(1) },
            icon = {
                if (czeka > 0) BadgedBox(badge = { Badge { Text("$czeka") } }) { Icon(Icons.Default.Verified, contentDescription = "Izba Akceptacji") }
                else Icon(Icons.Default.Verified, contentDescription = "Izba Akceptacji")
            },
            label = { Text("Izba", fontSize = 11.sp) }, modifier = Modifier.testTag("nav_tab_ratification")
        )
        NavigationBarItem(
            selected = wybrana == 2, onClick = { onWybierz(2) },
            icon = { Icon(Icons.Default.History, contentDescription = "Historia") },
            label = { Text("Historia", fontSize = 11.sp) }, modifier = Modifier.testTag("nav_tab_history")
        )
        NavigationBarItem(
            selected = wybrana == 3, onClick = { onWybierz(3) },
            icon = { Icon(Icons.Default.Psychology, contentDescription = "Agenci") },
            label = { Text("Agenci", fontSize = 11.sp) }, modifier = Modifier.testTag("nav_tab_agents")
        )
        NavigationBarItem(
            selected = wybrana == 4, onClick = { onWybierz(4) },
            icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Katedra") },
            label = { Text("Katedra", fontSize = 11.sp) }, modifier = Modifier.testTag("nav_tab_katedra")
        )
        NavigationBarItem(
            selected = wybrana == 5, onClick = { onWybierz(5) },
            icon = {
                if (tostNowe > 0) BadgedBox(badge = { Badge { Text("$tostNowe") } }) { Icon(Icons.Default.Forum, contentDescription = "TOST") }
                else Icon(Icons.Default.Forum, contentDescription = "TOST")
            },
            label = { Text("TOST", fontSize = 11.sp) }, modifier = Modifier.testTag("nav_tab_tost")
        )
    }
}
