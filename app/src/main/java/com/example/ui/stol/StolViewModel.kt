package com.example.ui.stol

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.most.AkcjaStolu
import com.example.most.Gatunek
import com.example.most.KartaStolu
import com.example.most.KontaktyTost
import com.example.most.MostKlient
import com.example.most.NowaKarta
import com.example.most.OczekujacaKatedra
import com.example.most.StanRejestru
import com.example.most.PublikacjaYouTube
import com.example.most.PamiecKatedry
import com.example.most.Rozmowca
import com.example.most.RozmowaUi
import com.example.most.Warsztat
import com.example.most.WiadomoscTost
import com.example.most.Wypowiedz
import com.example.most.ZdarzenieSzyny
import com.example.most.zapowiedziStolu
import com.example.ui.katedra.PolaczenieStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Stół ratyfikacji na telefonie — prawdziwe karty z mostu (`/api/stol`), nie atrapa z AI Studio.
 * Zakładki: 0 Stół · 1 Izba Akceptacji · 2 Historia (szyna Katedry) · 3 Agenci (TeOgochi + rozmowa przez Delegata
 * + pamięć Katedry) · 4 Katedra · 5 TOST (rozmowy z innymi Katedrami).
 * Połączenie (adres, klucz, token) dzieli z zakładką Katedra — parowanie robi się tam.
 */
data class StolUiState(
    val zakladka: Int = 4,
    val sparowany: Boolean = false,
    val karty: List<KartaStolu> = emptyList(),
    val zdarzenia: List<ZdarzenieSzyny> = emptyList(),
    val gatunki: List<Gatunek> = emptyList(),
    /** Most choć raz odpowiedział — do tej pory pusty stół znaczy „nie wiem", nie „pusto". */
    val wczytano: Boolean = false,
    val wczytuje: Boolean = false,
    val blad: String? = null,
    /** Otwarta karta (pełna treść i Biblia) albo null. */
    val otwarta: KartaStolu? = null,
    /** Decyzja albo nowa karta czeka na odpowiedź mostu. */
    val pracuje: Boolean = false,
    val bladAkcji: String? = null,
    val formularz: Boolean = false,
    val komunikat: String? = null,
    /** Zapowiedzi głosowe Stołu (gotowy projekt, utknięty, moduły oddały zlecenia). */
    val glos: Boolean = true,
    /** Delegat: z kim można rozmawiać (`/api/delegat/wszyscy`) i otwarta rozmowa. */
    val rozmowcy: List<Rozmowca> = emptyList(),
    val rozmowa: RozmowaUi? = null,
    /** Pamięć Katedry — widok w zakładce Agenci. */
    val pamiec: PamiecKatedry? = null,
    val pamiecOtwarta: Boolean = false,
    val pamiecWczytuje: Boolean = false,
    val pamiecBlad: String? = null,
    /** 💬 TOST między Katedrami: kontakty z rejestru otakos.wtf, otwarty wątek (nick) i jego wiadomości. */
    val tost: KontaktyTost? = null,
    val tostWczytuje: Boolean = false,
    val tostBlad: String? = null,
    val tostZ: String? = null,
    val tostWatek: List<WiadomoscTost> = emptyList(),
    val tostWysyla: Boolean = false,
    /** 🏛️ Zatwierdzanie Katedr (tylko gdy ta Katedra jest zarządcą rejestru otakos.wtf) — w Izbie Akceptacji. */
    val rejestr: StanRejestru? = null,
    val pracujeRejestr: Boolean = false,
    /** 📺 Publikacje YouTube od Kronikarza — „do akceptacji” w Izbie. */
    val publikacjeYT: List<PublikacjaYouTube> = emptyList(),
    val pracujeYT: Boolean = false,
)


class StolViewModel(application: Application) : AndroidViewModel(application) {
    private val store = PolaczenieStore(application)
    private val ustawienia = application.getSharedPreferences("stol_zapowiedzi", Context.MODE_PRIVATE)
    private val _ui = MutableStateFlow(StolUiState(sparowany = store.wczytaj() != null, glos = ustawienia.getBoolean("glos", true)))
    val ui: StateFlow<StolUiState> = _ui.asStateFlow()

    /**
     * 🔊 Głos Stołu: syntezator mowy telefonu (lokalny, bez chmury). Mówi o PRZEJŚCIACH kart między
     * odświeżeniami (zapowiedziStolu) — pierwsze wczytanie milczy. Działa, gdy StoL jest na ekranie.
     */
    @Volatile private var mowaGotowa = false
    private val mowa: TextToSpeech = TextToSpeech(application) { status ->
        mowaGotowa = status == TextToSpeech.SUCCESS
        if (mowaGotowa) mowaPoPolsku()
    }
    private fun mowaPoPolsku() { runCatching { mowa.language = Locale("pl", "PL") } }
    /** Karty z poprzedniego udanego odczytu — z nimi porównujemy, co się zmieniło. */
    @Volatile private var poprzednieKarty: List<KartaStolu>? = null

    fun przelaczGlos() {
        val nowy = !_ui.value.glos
        ustawienia.edit().putBoolean("glos", nowy).apply()
        _ui.update { it.copy(glos = nowy) }
        if (nowy) powiedz("Zapowiedzi Stołu włączone.") else mowa.stop()
    }

    private fun powiedz(tekst: String) {
        if (!_ui.value.glos || !mowaGotowa) return
        mowa.speak(tekst, TextToSpeech.QUEUE_ADD, null, "stol-${System.nanoTime()}")
    }

    override fun onCleared() {
        mowa.stop()
        mowa.shutdown()
    }

    fun wybierz(zakladka: Int) = _ui.update { it.copy(zakladka = zakladka, otwarta = null, formularz = false) }

    /** Karty, szyna i stado naraz. Stary widok zostaje przy błędzie — z ostrzeżeniem. */
    fun odswiez() {
        val p = store.wczytaj()
        if (p == null) { poprzednieKarty = null; _ui.update { StolUiState(zakladka = it.zakladka, glos = it.glos) }; return }
        if (_ui.value.wczytuje) return
        _ui.update { it.copy(sparowany = true, wczytuje = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val k = MostKlient(p.adres, p.klucz)
            val karty = async { k.stol(p.token) }
            val zdarzenia = async { k.zdarzenia(p.token) }
            val stan = async { k.stan(p.token) }
            // Rozmówcy Delegata zmieniają się rzadko (nowa karta roli w Katedrze) — dociągamy, dopóki lista pusta.
            val rozmowcy = if (_ui.value.rozmowcy.isEmpty()) async { k.rozmowcy(p.token) } else null
            val rejestr = async { k.rejestrStan(p.token) }
            val publikacjeYT = async { k.youtubePublikacje(p.token) }
            val wk = karty.await()
            val wz = zdarzenia.await()
            val ws = stan.await()
            (wk as? MostKlient.Wynik.Ok)?.wartosc?.let { karty ->
                val zapowiedzi = zapowiedziStolu(poprzednieKarty, karty)
                poprzednieKarty = karty
                zapowiedzi.forEach(::powiedz)
                zapowiedzi.lastOrNull()?.let { z -> _ui.update { it.copy(komunikat = z) } }
            }
            _ui.update { u ->
                u.copy(
                    wczytuje = false,
                    wczytano = u.wczytano || wk is MostKlient.Wynik.Ok,
                    karty = (wk as? MostKlient.Wynik.Ok)?.wartosc ?: u.karty,
                    zdarzenia = (wz as? MostKlient.Wynik.Ok)?.wartosc ?: u.zdarzenia,
                    gatunki = (ws as? MostKlient.Wynik.Ok)?.wartosc?.gatunki ?: u.gatunki,
                    rozmowcy = (rozmowcy?.await() as? MostKlient.Wynik.Ok)?.wartosc ?: u.rozmowcy,
                    rejestr = (rejestr.await() as? MostKlient.Wynik.Ok)?.wartosc ?: u.rejestr,
                    publikacjeYT = (publikacjeYT.await() as? MostKlient.Wynik.Ok)?.wartosc ?: u.publikacjeYT,
                    blad = (wk as? MostKlient.Wynik.Blad)?.let(::opisBledu),
                )
            }
            _ui.value.otwarta?.let { otworz(it.id) }   // otwarta karta (pełna treść, Biblia) też idzie naprzód
        }
    }

    fun otworz(id: String) {
        val p = store.wczytaj() ?: return
        _ui.value.karty.find { it.id == id }?.let { skrot -> if (_ui.value.otwarta?.id != id) _ui.update { it.copy(otwarta = skrot, bladAkcji = null) } }
        viewModelScope.launch(Dispatchers.IO) {
            when (val w = MostKlient(p.adres, p.klucz).karta(p.token, id)) {
                is MostKlient.Wynik.Ok -> _ui.update { u -> if (u.otwarta?.id == id) u.copy(otwarta = w.wartosc) else u }
                is MostKlient.Wynik.Blad -> _ui.update { u -> if (u.otwarta?.id == id) u.copy(bladAkcji = opisBledu(w)) else u }
            }
        }
    }

    fun zamknij() = _ui.update { it.copy(otwarta = null, bladAkcji = null) }

    /** Przyjmij / odrzuć / ratyfikuj. Odmowa mostu zostaje na karcie jego słowami. */
    fun decyzja(karta: KartaStolu, akcja: AkcjaStolu, uczestnicy: List<String> = emptyList(), warsztat: Warsztat? = null) {
        val p = store.wczytaj() ?: return
        _ui.update { it.copy(pracuje = true, bladAkcji = null) }
        viewModelScope.launch {
            val w = withContext(Dispatchers.IO) { MostKlient(p.adres, p.klucz).decyzja(p.token, karta.id, akcja, uczestnicy, warsztat) }
            when (w) {
                is MostKlient.Wynik.Ok -> {
                    val zdanie = when (akcja) {
                        AkcjaStolu.PRZYJMIJ -> "„${karta.tytul}” przyjęte — stado zaczyna pracę" +
                            (warsztat?.takeIf { it.rundy > 1 }?.let { " (${it.rundy} rund doskonalenia)" } ?: "") + "."
                        AkcjaStolu.DOSKONAL -> "„${karta.tytul}” wraca do stada — ${warsztat?.rundy ?: 1} ${if ((warsztat?.rundy ?: 1) == 1) "runda" else "rundy"} doskonalenia."
                        AkcjaStolu.NOCNA -> "„${karta.tytul}” na Nocnej Zmianie: ${warsztat?.powtorzenia ?: 1} × ${warsztat?.rundy ?: 1} rund" +
                            (if (karta.nocna?.wlaczona == false) " — włącz Zmianę w Katedrze, inaczej poczeka." else ".")
                        AkcjaStolu.ODRZUC -> "„${karta.tytul}” odłożone ze stołu."
                        AkcjaStolu.RATYFIKUJ ->
                            if (w.wartosc > 0) "„${karta.tytul}” zratyfikowane — stado zleca ${w.wartosc} zadań modułom Katedry."
                            else "„${karta.tytul}” zratyfikowane — wkłady nie miały zleceń dla modułów."
                    }
                    _ui.update { it.copy(pracuje = false, komunikat = zdanie) }
                    odswiez()
                    otworz(karta.id)   // nowy etap karty od razu, nawet gdy odświeżanie stołu jest w toku
                }
                is MostKlient.Wynik.Blad -> _ui.update { it.copy(pracuje = false, bladAkcji = opisBledu(w)) }
            }
        }
    }

    fun pokazFormularz(czy: Boolean) = _ui.update { it.copy(formularz = czy, bladAkcji = null) }

    fun poloz(karta: NowaKarta) {
        val p = store.wczytaj() ?: return
        _ui.update { it.copy(pracuje = true, bladAkcji = null) }
        viewModelScope.launch {
            val w = withContext(Dispatchers.IO) { MostKlient(p.adres, p.klucz).polozNaStol(p.token, karta) }
            when (w) {
                is MostKlient.Wynik.Ok -> {
                    _ui.update { u -> u.copy(pracuje = false, formularz = false, karty = listOf(w.wartosc) + u.karty, komunikat = "„${w.wartosc.tytul}” leży na stole.") }
                    odswiez()
                }
                is MostKlient.Wynik.Blad -> _ui.update { it.copy(pracuje = false, bladAkcji = opisBledu(w)) }
            }
        }
    }

    // ── Delegat: rozmowa z dowolnym TeOgochi ──

    fun otworzRozmowe(r: Rozmowca) = _ui.update { u -> if (u.rozmowa?.z?.id == r.id) u else u.copy(rozmowa = RozmowaUi(r)) }
    fun zamknijRozmowe() = _ui.update { it.copy(rozmowa = null) }

    /** Wyślij wypowiedź; odpowiedź przychodzi po całej pętli narzędzi (bywa minuta i dłużej). Głos czyta ją, gdy włączony. */
    fun wyslij(tekst: String) {
        val p = store.wczytaj() ?: return
        val r = _ui.value.rozmowa ?: return
        if (tekst.isBlank() || r.mysli) return
        _ui.update { it.copy(rozmowa = r.copy(wypowiedzi = r.wypowiedzi + Wypowiedz("suweren", tekst.trim()), mysli = true, blad = null)) }
        viewModelScope.launch {
            val w = withContext(Dispatchers.IO) { MostKlient(p.adres, p.klucz).rozmawiaj(p.token, r.z.id, tekst, r.rozmowaId) }
            _ui.update { u ->
                val teraz = u.rozmowa?.takeIf { it.z.id == r.z.id } ?: return@update u
                when (w) {
                    is MostKlient.Wynik.Ok -> u.copy(rozmowa = teraz.copy(wypowiedzi = teraz.wypowiedzi + Wypowiedz("delegat", w.wartosc.odpowiedz), rozmowaId = w.wartosc.rozmowaId, mysli = false))
                    is MostKlient.Wynik.Blad -> u.copy(rozmowa = teraz.copy(mysli = false, blad = opisBledu(w)))
                }
            }
            (w as? MostKlient.Wynik.Ok)?.let { powiedz(it.wartosc.odpowiedz) }
        }
    }

    // ── Pamięć Katedry ──

    fun pokazPamiec(czy: Boolean) {
        _ui.update { it.copy(pamiecOtwarta = czy) }
        if (czy) wczytajPamiec()
    }

    fun wczytajPamiec() {
        val p = store.wczytaj() ?: return
        _ui.update { it.copy(pamiecWczytuje = true, pamiecBlad = null) }
        viewModelScope.launch {
            val w = withContext(Dispatchers.IO) { MostKlient(p.adres, p.klucz).pamiec(p.token) }
            _ui.update { u ->
                when (w) {
                    is MostKlient.Wynik.Ok -> u.copy(pamiec = w.wartosc, pamiecWczytuje = false)
                    is MostKlient.Wynik.Blad -> u.copy(pamiecWczytuje = false, pamiecBlad = opisBledu(w))
                }
            }
        }
    }

    /** Zamknij proces po PID (potwierdzenie robi ekran). Wynik mostu słowami w pasku, potem świeża lista. */
    fun zwolnij(pid: Int) {
        val p = store.wczytaj() ?: return
        _ui.update { it.copy(pamiecWczytuje = true, pamiecBlad = null) }
        viewModelScope.launch {
            val w = withContext(Dispatchers.IO) { MostKlient(p.adres, p.klucz).zwolnij(p.token, listOf(pid)) }
            when (w) {
                is MostKlient.Wynik.Ok -> _ui.update { it.copy(komunikat = w.wartosc.zdanie) }
                is MostKlient.Wynik.Blad -> _ui.update { it.copy(pamiecBlad = opisBledu(w)) }
            }
            wczytajPamiec()
        }
    }

    // ── 💬 TOST między Katedrami (telefon pisze przez swoją Katedrę; ona szyfruje i niesie kopertę) ──

    /** Kontakty i — gdy wątek otwarty — jego wiadomości. Stary widok zostaje przy błędzie. */
    fun odswiezTost() {
        val p = store.wczytaj() ?: return
        if (_ui.value.tostWczytuje) return
        _ui.update { it.copy(tostWczytuje = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val k = MostKlient(p.adres, p.klucz)
            val z = _ui.value.tostZ
            val kontakty = k.tostKontakty(p.token)
            val watek = z?.let { k.tostRozmowa(p.token, it) }
            _ui.update { u ->
                u.copy(
                    tostWczytuje = false,
                    tost = (kontakty as? MostKlient.Wynik.Ok)?.wartosc ?: u.tost,
                    tostBlad = (kontakty as? MostKlient.Wynik.Blad)?.let(::opisBledu),
                    tostWatek = if (z != null && u.tostZ == z) (watek as? MostKlient.Wynik.Ok)?.wartosc ?: u.tostWatek else u.tostWatek,
                )
            }
        }
    }

    fun otworzTost(nick: String) {
        _ui.update { it.copy(tostZ = nick, tostWatek = emptyList(), tostBlad = null) }
        odswiezTost()
    }

    fun zamknijTost() = _ui.update { it.copy(tostZ = null, tostWatek = emptyList()) }

    /** Wyślij do otwartego wątku. Odbiorca offline → wiadomość „czeka” w Katedrze i wychodzi sama. */
    fun wyslijTost(tekst: String) {
        val p = store.wczytaj() ?: return
        val z = _ui.value.tostZ ?: return
        if (tekst.isBlank() || _ui.value.tostWysyla) return
        _ui.update { it.copy(tostWysyla = true, tostBlad = null) }
        viewModelScope.launch {
            val w = withContext(Dispatchers.IO) { MostKlient(p.adres, p.klucz).tostWyslij(p.token, z, tekst) }
            when (w) {
                is MostKlient.Wynik.Ok -> _ui.update { u -> u.copy(tostWysyla = false, tostWatek = if (u.tostZ == z) u.tostWatek + w.wartosc else u.tostWatek) }
                is MostKlient.Wynik.Blad -> _ui.update { it.copy(tostWysyla = false, tostBlad = opisBledu(w)) }
            }
            odswiezTost()
        }
    }

    /** ✓ / ✕ publikacji YouTube — ✓ oddaje film Impresariatowi (niepubliczny). */
    fun decyzjaPublikacji(pub: PublikacjaYouTube, zatwierdz: Boolean) {
        val p = store.wczytaj() ?: return
        _ui.update { it.copy(pracujeYT = true) }
        viewModelScope.launch {
            val w = withContext(Dispatchers.IO) { MostKlient(p.adres, p.klucz).youtubeDecyzja(p.token, pub.id, zatwierdz) }
            val zdanie = when (w) {
                is MostKlient.Wynik.Ok -> if (zatwierdz) "„${pub.tytul}” idzie na YouTube (niepubliczny)." else "„${pub.tytul}” odrzucona."
                is MostKlient.Wynik.Blad -> opisBledu(w)
            }
            _ui.update { u -> u.copy(pracujeYT = false, komunikat = zdanie, publikacjeYT = if (w is MostKlient.Wynik.Ok) u.publikacjeYT.filter { it.id != pub.id } else u.publikacjeYT) }
            odswiez()
        }
    }

    /** Zatwierdź / odrzuć Katedrę w sieci otakos.wtf — Katedra zarządcy wyśle podpisaną listę do rejestru. */
    fun decyzjaKatedry(k: OczekujacaKatedra, zatwierdz: Boolean) {
        val p = store.wczytaj() ?: return
        _ui.update { it.copy(pracujeRejestr = true) }
        viewModelScope.launch {
            val w = withContext(Dispatchers.IO) { MostKlient(p.adres, p.klucz).rejestrDecyzja(p.token, k, zatwierdz) }
            val zdanie = when (w) {
                is MostKlient.Wynik.Ok -> if (zatwierdz) "„${k.nick}” zatwierdzona — pojawi się na otakos.wtf." else "„${k.nick}” odrzucona."
                is MostKlient.Wynik.Blad -> opisBledu(w)
            }
            _ui.update { u -> u.copy(pracujeRejestr = false, komunikat = zdanie, rejestr = if (w is MostKlient.Wynik.Ok) u.rejestr?.let { r -> r.copy(oczekujace = r.oczekujace.filter { it != k }) } else u.rejestr) }
            odswiez()
        }
    }

    fun komunikatPokazany() = _ui.update { it.copy(komunikat = null) }

    /** Token odrzucony: parowanie jest w zakładce Katedra (tam też telefon zapomina połączenie). */
    private fun opisBledu(w: MostKlient.Wynik.Blad) =
        if (w.rozparowany) "${w.opis} Sparuj telefon w zakładce Katedra." else w.opis
}
