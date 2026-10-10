package com.example.ui.tlumacz

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.most.FiltrTlumacza
import com.example.most.JEZYKI_KATEDRY
import com.example.most.MostKlient
import com.example.most.SlownikTlumacza
import com.example.ui.katedra.PolaczenieStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 🌍 StoL w języku Suwerena — ten sam Tłumacz co Hub i apki Pierścienia (most: services/Tlumacz.js).
 *
 * Napisy interfejsu idą przez `tl("…")`: jest tłumaczenie w słowniku → tłumaczenie, nie ma → polski od razu
 * i napis trafia do kolejki; paczki (≤ 30) lecą w tle do mostu Katedry przez tunel, a po powrocie Compose
 * przerysowuje napisy. Słownik jest wspólny z Hubem (`GET /api/tlumacz/:jezyk`) i zostaje w telefonie
 * (SharedPreferences `stol_tlumacz`), więc kolejne uruchomienie nie czeka na model.
 *
 * Język: wybór w zakładce Katedra; pierwszy start = język telefonu, jeśli Katedra go zna, inaczej polski.
 * Uczciwie: bez sparowanej Katedry (albo gdy tunel milczy) StoL zostaje po polsku; jakość = lokalny model Katedry.
 * Treść (rozmowy z agentami, karty Stołu, Biblie, TOST) NIE idzie przez Tłumacza — to nie interfejs.
 */
object TlumaczStoL {
    private const val PREFS = "stol_tlumacz"
    private const val ODPOCZYNEK_MS = 30_000L   // most milczy → nie pukamy przy każdym przerysowaniu

    private var app: Context? = null
    private val zakres = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Volatile private var slownik = SlownikTlumacza("pl")
    private val planowane = AtomicBoolean(false)
    private val pobiera = AtomicBoolean(false)
    @Volatile private var odpoczynekDo = 0L

    /** Wybrany język (kod jak w Katedrze: pl, en, de…). */
    var jezyk by mutableStateOf("pl")
        private set
    /** Ile napisów czeka na model (do linijki stanu pod wyborem). */
    var czeka by mutableIntStateOf(0)
        private set
    /** Ostatni błąd mostu/modelu albo null. */
    var blad by mutableStateOf<String?>(null)
        private set
    /** Rośnie po każdej porcji tłumaczeń — `tl()` ją czyta, więc Compose przerysowuje napisy. */
    private var wersja by mutableIntStateOf(0)

    /** Raz, z MainActivity: język z pamięci (albo telefonu) i słownik z pamięci telefonu. */
    fun start(context: Context) {
        if (app != null) return
        app = context.applicationContext
        val zapisany = FiltrTlumacza.kodJezyka(prefs()?.getString("jezyk", null))
        val telefonu = Locale.getDefault().language.takeIf { k -> JEZYKI_KATEDRY.any { it.first == k } }
        ustaw(zapisany ?: telefonu ?: "pl", zapamietaj = false)
    }

    /** Zmień język StoL-a (`pl` = oryginał). */
    fun ustaw(j: String, zapamietaj: Boolean = true) {
        val nowy = FiltrTlumacza.kodJezyka(j) ?: "pl"
        if (zapamietaj) prefs()?.edit()?.putString("jezyk", nowy)?.apply()
        val s = SlownikTlumacza(nowy)
        s.dolacz(SlownikTlumacza.zJson(prefs()?.getString("slownik_$nowy", null)))
        slownik = s
        jezyk = nowy
        blad = null
        czeka = 0
        odpoczynekDo = 0L
        wersja++
        if (nowy != "pl") zakres.launch { dociagnijSlownik(s) }
    }

    /** Napis interfejsu w języku StoL-a (bez tłumaczenia jeszcze — polski, a tłumaczenie dojdzie). */
    fun tl(tekst: String): String {
        @Suppress("UNUSED_VARIABLE") val w = wersja   // odczyt stanu = przerysowanie po nowych tłumaczeniach
        val wynik = slownik.przetlumacz(tekst)
        if (wynik == null) planuj()
        return wynik ?: tekst
    }

    private fun prefs() = app?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun polaczenie() = app?.let { PolaczenieStore(it).wczytaj() }

    private fun planuj() {
        if (app == null || System.currentTimeMillis() < odpoczynekDo) return
        if (!planowane.compareAndSet(false, true)) return
        zakres.launch {
            delay(400)   // zbierz napisy całego ekranu w jedną paczkę
            planowane.set(false)
            pobierz()
        }
    }

    private suspend fun dociagnijSlownik(s: SlownikTlumacza) {
        val p = polaczenie() ?: return
        val w = withContext(Dispatchers.IO) { MostKlient(p.adres, p.klucz).slownikTlumacza(s.jezyk) }
        if (w is MostKlient.Wynik.Ok && slownik === s) {
            s.dolacz(w.wartosc)
            zapamietaj(s)
            wersja++
        }
    }

    private suspend fun pobierz() {
        if (!pobiera.compareAndSet(false, true)) return
        try {
            while (true) {
                val s = slownik
                if (s.jezyk == "pl") break
                val paczka = s.paczka()
                if (paczka.isEmpty()) break
                czeka = s.czeka()
                val p = polaczenie()
                if (p == null) { s.nieDoszla(paczka, naStale = false); odpoczynekDo = System.currentTimeMillis() + ODPOCZYNEK_MS; break }
                val w = withContext(Dispatchers.IO) { MostKlient(p.adres, p.klucz).tlumacz(s.jezyk, paczka) }
                if (slownik !== s) break   // w międzyczasie zmiana języka
                when (w) {
                    is MostKlient.Wynik.Ok -> {
                        s.przyjmij(paczka, w.wartosc)
                        blad = w.wartosc.blad
                        zapamietaj(s)
                        wersja++
                    }
                    is MostKlient.Wynik.Blad -> {
                        // Tunel milczy / telefon rozparowany — spróbujemy później; inny błąd mostu = polski zostaje.
                        val chwilowy = w.rozparowany || w.opis.startsWith("Most milczy")
                        s.nieDoszla(paczka, naStale = !chwilowy)
                        blad = "Tłumaczenie nie doszło: ${w.opis}"
                        if (chwilowy) { odpoczynekDo = System.currentTimeMillis() + ODPOCZYNEK_MS; break }
                    }
                }
            }
        } finally {
            pobiera.set(false)
            czeka = slownik.czeka()
        }
    }

    private fun zapamietaj(s: SlownikTlumacza) {
        prefs()?.edit()?.putString("slownik_${s.jezyk}", s.doJson())?.apply()
    }
}

/** Napis interfejsu StoL-a w wybranym języku. Tylko stałe napisy — nie treść od agentów i ludzi. */
fun tl(tekst: String): String = TlumaczStoL.tl(tekst)
