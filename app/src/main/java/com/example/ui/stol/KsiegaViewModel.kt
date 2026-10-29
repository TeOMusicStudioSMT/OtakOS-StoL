package com.example.ui.stol

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.most.MostKlient
import com.example.most.WpisKsiegi
import com.example.ui.katedra.PolaczenieStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 📖 Żywa Księga na telefonie: wpisy z Katedry, nowa Iskra, zdjęcie i film z galerii telefonu dołożone do wpisu.
 * Połączenie (adres, klucz, token) to samo co Stół — parowanie w zakładce Katedra.
 */
data class KsiegaUiState(
    val wpisy: List<WpisKsiegi> = emptyList(),
    val wczytano: Boolean = false,
    val wczytuje: Boolean = false,
    val blad: String? = null,
    /** Co teraz idzie do mostu („zapisuję Iskrę…”, „wysyłam film 42 MB…”) albo null. */
    val wysyla: String? = null,
    val bladAkcji: String? = null,
    val komunikat: String? = null,
    /** Podglądy obrazków: klucz `id/plik/wersja`. */
    val obrazy: Map<String, Bitmap> = emptyMap(),
)

class KsiegaViewModel(application: Application) : AndroidViewModel(application) {
    private val store = PolaczenieStore(application)
    private val _ui = MutableStateFlow(KsiegaUiState())
    val ui: StateFlow<KsiegaUiState> = _ui.asStateFlow()
    private val wczytywaneObrazy = mutableSetOf<String>()

    fun odswiez() {
        val p = store.wczytaj() ?: return
        if (_ui.value.wczytuje) return
        _ui.update { it.copy(wczytuje = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val w = MostKlient(p.adres, p.klucz).ksiega(p.token)
            _ui.update {
                it.copy(
                    wczytuje = false,
                    wczytano = it.wczytano || w is MostKlient.Wynik.Ok,
                    wpisy = (w as? MostKlient.Wynik.Ok)?.wartosc ?: it.wpisy,
                    blad = (w as? MostKlient.Wynik.Blad)?.opis,
                )
            }
        }
    }

    /** Nowa Iskra, potem (jeśli wybrane) zdjęcie i film — każdy krok mówi, co robi; błąd pliku nie kasuje wpisu. */
    fun dodaj(tekst: String, publiczny: Boolean, zdjecie: Uri?, film: Uri?, poSukcesie: () -> Unit) {
        val p = store.wczytaj() ?: return
        if (_ui.value.wysyla != null) return
        viewModelScope.launch {
            _ui.update { it.copy(wysyla = "zapisuję Iskrę…", bladAkcji = null) }
            val k = MostKlient(p.adres, p.klucz)
            val w = withContext(Dispatchers.IO) { k.nowyWpis(p.token, tekst, publiczny) }
            if (w is MostKlient.Wynik.Blad) { _ui.update { it.copy(wysyla = null, bladAkcji = w.opis) }; return@launch }
            val wpis = (w as MostKlient.Wynik.Ok).wartosc
            poSukcesie()
            val bledy = listOfNotNull(
                zdjecie?.let { wyslij(k, p.token, wpis.id, "zdjecie", it) },
                film?.let { wyslij(k, p.token, wpis.id, "nagranie", it) },
            )
            _ui.update {
                it.copy(
                    wysyla = null,
                    bladAkcji = bledy.joinToString("\n").ifBlank { null },
                    komunikat = if (bledy.isEmpty()) "📖 Iskra w Księdze — stado już tworzy" else "📖 Iskra zapisana, ale nie wszystko doszło",
                )
            }
            odswiez()
        }
    }

    /** Dołóż zdjęcie albo film do istniejącego wpisu. */
    fun doloz(id: String, rodzaj: String, uri: Uri) {
        val p = store.wczytaj() ?: return
        if (_ui.value.wysyla != null) return
        viewModelScope.launch {
            val blad = wyslij(MostKlient(p.adres, p.klucz), p.token, id, rodzaj, uri)
            _ui.update { it.copy(wysyla = null, bladAkcji = blad, komunikat = if (blad == null) (if (rodzaj == "zdjecie") "📷 Zdjęcie we wpisie" else "🎥 Film we wpisie") else null) }
            odswiez()
        }
    }

    /** Zwraca opis błędu albo null. Plik czytany strumieniem z galerii — bez kopiowania całego filmu do pamięci. */
    private suspend fun wyslij(k: MostKlient, token: String, id: String, rodzaj: String, uri: Uri): String? = withContext(Dispatchers.IO) {
        val cr = getApplication<Application>().contentResolver
        val typ = cr.getType(uri) ?: if (rodzaj == "zdjecie") "image/jpeg" else "video/mp4"
        val dlugosc = rozmiar(uri)
            ?: return@withContext "Nie umiem odczytać rozmiaru pliku — wybierz go jeszcze raz z galerii."
        val mb = dlugosc / 1_048_576.0
        _ui.update { it.copy(wysyla = if (rodzaj == "zdjecie") "wysyłam zdjęcie…" else "wysyłam film (%.1f MB) — most go przelicza, to potrwa…".format(mb)) }
        val strumien = runCatching { cr.openInputStream(uri) }.getOrNull()
            ?: return@withContext "Nie mogę otworzyć pliku z galerii."
        when (val w = k.wyslijDoWpisu(token, id, rodzaj, typ, dlugosc, strumien)) {
            is MostKlient.Wynik.Ok -> null
            is MostKlient.Wynik.Blad -> w.opis
        }
    }

    private fun rozmiar(uri: Uri): Long? {
        val cr = getApplication<Application>().contentResolver
        runCatching {
            cr.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) return c.getLong(0).takeIf { it > 0 }
            }
        }
        return runCatching { cr.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull()?.takeIf { it > 0 }
    }

    fun przelaczPubliczny(w: WpisKsiegi) = akcja { k, t -> k.ustawPubliczny(t, w.id, !w.publiczny) }
    fun ponowStado(w: WpisKsiegi) = akcja { k, t -> k.ponowStadoKsiegi(t, w.id) }

    private fun akcja(f: (MostKlient, String) -> MostKlient.Wynik<*>) {
        val p = store.wczytaj() ?: return
        viewModelScope.launch {
            val w = withContext(Dispatchers.IO) { f(MostKlient(p.adres, p.klucz), p.token) }
            if (w is MostKlient.Wynik.Blad) _ui.update { it.copy(bladAkcji = w.opis) }
            odswiez()
        }
    }

    /** Podgląd obrazka wpisu — pobrany raz na wersję, zmniejszony do ~1080 px (zdjęcia z mostu mają do 2048). */
    fun wczytajObraz(w: WpisKsiegi, plik: String) {
        val klucz = kluczObrazu(w, plik)
        if (klucz in _ui.value.obrazy) return
        synchronized(wczytywaneObrazy) { if (!wczytywaneObrazy.add(klucz)) return }
        val p = store.wczytaj() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val bajty = (MostKlient(p.adres, p.klucz).plikWpisu(p.token, w.id, plik) as? MostKlient.Wynik.Ok)?.wartosc
            val bmp = bajty?.let { zmniejszony(it, 1080) }
            if (bmp != null) _ui.update { it.copy(obrazy = it.obrazy + (klucz to bmp)) }
            else synchronized(wczytywaneObrazy) { wczytywaneObrazy.remove(klucz) }   // następne odświeżenie spróbuje znowu
        }
    }

    private fun zmniejszony(b: ByteArray, bok: Int): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(b, 0, b.size, o)
        var skala = 1
        while (maxOf(o.outWidth, o.outHeight) / (skala * 2) >= bok) skala *= 2
        return BitmapFactory.decodeByteArray(b, 0, b.size, BitmapFactory.Options().apply { inSampleSize = skala })
    }

    fun komunikatPokazany() = _ui.update { it.copy(komunikat = null) }
    fun zamknijBlad() = _ui.update { it.copy(bladAkcji = null) }

    companion object {
        fun kluczObrazu(w: WpisKsiegi, plik: String) =
            "${w.id}/$plik/${if (plik == w.zdjecie) w.zdjecieKiedy else w.ilustracja?.stan}"
    }
}
