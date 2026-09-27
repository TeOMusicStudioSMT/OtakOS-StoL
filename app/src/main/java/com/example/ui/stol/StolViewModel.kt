package com.example.ui.stol

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.most.AkcjaStolu
import com.example.most.Gatunek
import com.example.most.KartaStolu
import com.example.most.MostKlient
import com.example.most.NowaKarta
import com.example.most.ZdarzenieSzyny
import com.example.ui.katedra.PolaczenieStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Stół ratyfikacji na telefonie — prawdziwe karty z mostu (`/api/stol`), nie atrapa z AI Studio.
 * Zakładki: 0 Stół · 1 Izba Akceptacji · 2 Historia (szyna Katedry) · 3 Agenci (TeOgochi) · 4 Katedra.
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
)

class StolViewModel(application: Application) : AndroidViewModel(application) {
    private val store = PolaczenieStore(application)
    private val _ui = MutableStateFlow(StolUiState(sparowany = store.wczytaj() != null))
    val ui: StateFlow<StolUiState> = _ui.asStateFlow()

    fun wybierz(zakladka: Int) = _ui.update { it.copy(zakladka = zakladka, otwarta = null, formularz = false) }

    /** Karty, szyna i stado naraz. Stary widok zostaje przy błędzie — z ostrzeżeniem. */
    fun odswiez() {
        val p = store.wczytaj()
        if (p == null) { _ui.update { StolUiState(zakladka = it.zakladka) }; return }
        if (_ui.value.wczytuje) return
        _ui.update { it.copy(sparowany = true, wczytuje = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val k = MostKlient(p.adres, p.klucz)
            val karty = async { k.stol(p.token) }
            val zdarzenia = async { k.zdarzenia(p.token) }
            val stan = async { k.stan(p.token) }
            val wk = karty.await()
            val wz = zdarzenia.await()
            val ws = stan.await()
            _ui.update { u ->
                u.copy(
                    wczytuje = false,
                    wczytano = u.wczytano || wk is MostKlient.Wynik.Ok,
                    karty = (wk as? MostKlient.Wynik.Ok)?.wartosc ?: u.karty,
                    zdarzenia = (wz as? MostKlient.Wynik.Ok)?.wartosc ?: u.zdarzenia,
                    gatunki = (ws as? MostKlient.Wynik.Ok)?.wartosc?.gatunki ?: u.gatunki,
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
    fun decyzja(karta: KartaStolu, akcja: AkcjaStolu, uczestnicy: List<String> = emptyList()) {
        val p = store.wczytaj() ?: return
        _ui.update { it.copy(pracuje = true, bladAkcji = null) }
        viewModelScope.launch {
            val w = withContext(Dispatchers.IO) { MostKlient(p.adres, p.klucz).decyzja(p.token, karta.id, akcja, uczestnicy) }
            when (w) {
                is MostKlient.Wynik.Ok -> {
                    val zdanie = when (akcja) {
                        AkcjaStolu.PRZYJMIJ -> "„${karta.tytul}” przyjęte — stado zaczyna pracę."
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

    fun komunikatPokazany() = _ui.update { it.copy(komunikat = null) }

    /** Token odrzucony: parowanie jest w zakładce Katedra (tam też telefon zapomina połączenie). */
    private fun opisBledu(w: MostKlient.Wynik.Blad) =
        if (w.rozparowany) "${w.opis} Sparuj telefon w zakładce Katedra." else w.opis
}
