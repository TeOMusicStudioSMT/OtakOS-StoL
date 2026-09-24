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
)

class KatedraViewModel(application: Application) : AndroidViewModel(application) {
    private val store = PolaczenieStore(application)
    private val _ui = MutableStateFlow(KatedraUiState())
    val ui: StateFlow<KatedraUiState> = _ui.asStateFlow()
    private var obserwacja: Job? = null

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
                    odswiez()
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

    /** Odświeżanie co 20 s, dopóki ekran Katedry jest widoczny. */
    fun zacznijObserwacje() {
        if (obserwacja?.isActive == true) return
        obserwacja = viewModelScope.launch {
            while (isActive) {
                if (_ui.value.sparowany) odswiez()
                delay(20_000)
            }
        }
    }

    fun zatrzymajObserwacje() { obserwacja?.cancel(); obserwacja = null }

    fun rozparuj() {
        zatrzymajObserwacje()
        store.zapomnij()
        _ui.value = KatedraUiState()
    }
}
