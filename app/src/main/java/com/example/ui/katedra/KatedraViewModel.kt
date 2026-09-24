package com.example.ui.katedra

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.most.LinkParowania
import com.example.most.MostKlient
import com.example.most.StanStada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.HttpURLConnection
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Okno na własną Katedrę: parowanie z mostem i obserwacja stada TeOgochi.
 * Tylko ODCZYT — StoL niczego w Katedrze nie zmienia.
 */
data class KatedraUiState(
    val sparowany: Boolean = false,
    val adres: String? = null,
    val stan: StanStada? = null,
    val blad: String? = null,
    val pracuje: Boolean = false,
    /** Kiedy (ms) ostatnio udało się odczytać most. */
    val odczytano: Long? = null,
    /** Strumień z mostu jest otwarty — zdarzenia przychodzą na żywo. */
    val naZywo: Boolean = false,
)

class KatedraViewModel(application: Application) : AndroidViewModel(application) {
    private val store = PolaczenieStore(application)
    private val _ui = MutableStateFlow(KatedraUiState())
    val ui: StateFlow<KatedraUiState> = _ui.asStateFlow()
    private var obserwacja: Job? = null
    @Volatile private var polaczenie: HttpURLConnection? = null

    init {
        store.wczytaj()?.let { _ui.value = KatedraUiState(sparowany = true, adres = it.adres) }
    }

    /** Link z QR / schowka / intencji `otakos-stol://paruj?…`. */
    fun sparuj(tekstLinku: String) {
        val link = LinkParowania.zTekstu(tekstLinku)
        if (link == null) {
            _ui.value = _ui.value.copy(blad = "To nie jest link parowania StoL. Wygeneruj go w Katedrze: Dashboard → karta „StoL”.")
            return
        }
        _ui.value = _ui.value.copy(pracuje = true, blad = null)
        viewModelScope.launch {
            val nazwa = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(40)
            val wynik = withContext(Dispatchers.IO) { MostKlient(link.adres, link.klucz).paruj(link.kod, nazwa) }
            when (wynik) {
                is MostKlient.Wynik.Ok -> {
                    store.zapisz(Polaczenie(link.adres, link.klucz, wynik.wartosc))
                    _ui.value = KatedraUiState(sparowany = true, adres = link.adres, pracuje = true)
                    zacznijObserwacje()
                }
                is MostKlient.Wynik.Blad -> {
                    _ui.value = _ui.value.copy(pracuje = false, blad = wynik.opis)
                }
            }
        }
    }

    fun odswiez() {
        val p = store.wczytaj() ?: return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(pracuje = true)
            val wynik = withContext(Dispatchers.IO) { MostKlient(p.adres, p.klucz).stan(p.token) }
            _ui.value = when (wynik) {
                is MostKlient.Wynik.Ok -> _ui.value.copy(stan = wynik.wartosc, blad = null, pracuje = false, odczytano = System.currentTimeMillis())
                is MostKlient.Wynik.Blad -> {
                    if (wynik.rozparowany) {
                        store.zapomnij()
                        KatedraUiState(blad = wynik.opis)
                    } else _ui.value.copy(blad = wynik.opis, pracuje = false)   // stary stan zostaje, z ostrzeżeniem
                }
            }
        }
    }

    /**
     * Strumień stada (SSE), dopóki ekran Katedry jest widoczny. Zamiast odpytywania co 20 s:
     * most sam przysyła stan i każde zdarzenie. Zerwane połączenie wznawiamy z rosnącą
     * przerwą (2 s → 30 s); odłączenie w Katedrze kończy obserwację i każe sparować od nowa.
     */
    fun zacznijObserwacje() {
        if (obserwacja?.isActive == true) return
        obserwacja = viewModelScope.launch(Dispatchers.IO) {
            var przerwa = 2_000L
            while (isActive) {
                val p = store.wczytaj() ?: break
                val wynik = MostKlient(p.adres, p.klucz).strumien(
                    token = p.token,
                    naStan = { s ->
                        przerwa = 2_000L
                        _ui.update { it.copy(stan = s, blad = null, pracuje = false, naZywo = true, odczytano = System.currentTimeMillis()) }
                    },
                    naZdarzenie = { z ->
                        _ui.update { u -> u.copy(stan = u.stan?.zZdarzeniem(z), odczytano = System.currentTimeMillis()) }
                    },
                    czyDalej = { isActive },
                    naPolaczenie = { polaczenie = it },
                )
                polaczenie = null
                if (!isActive) break
                if (wynik is MostKlient.Wynik.Blad && wynik.rozparowany) {
                    store.zapomnij()
                    _ui.value = KatedraUiState(blad = wynik.opis)
                    break
                }
                val opis = (wynik as? MostKlient.Wynik.Blad)?.opis ?: "Most zamknął strumień."
                _ui.update { it.copy(naZywo = false, pracuje = false, blad = "$opis Łączę ponownie za ${przerwa / 1000} s…") }
                delay(przerwa)
                przerwa = (przerwa * 2).coerceAtMost(30_000L)
            }
        }
    }

    fun zatrzymajObserwacje() {
        obserwacja?.cancel(); obserwacja = null
        polaczenie?.disconnect(); polaczenie = null   // przerywa blokujące readLine od razu
        _ui.update { it.copy(naZywo = false) }
    }

    fun rozparuj() {
        zatrzymajObserwacje()
        store.zapomnij()
        _ui.value = KatedraUiState()
    }
}
