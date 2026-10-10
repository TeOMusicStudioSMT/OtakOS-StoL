package com.example.most

/**
 * 🌍 Tłumacz Katedry dla StoL-a — ta sama droga co w Hubie i apkach Pierścienia (TeO_Genesis lib/tlumaczRdzen.js):
 * StoL jest pisany po polsku (źródło prawdy), a brakujące napisy tłumaczy most lokalnym modelem
 * (`POST /api/tlumacz {jezyk, teksty}` → `{mapa, pominiete, blad?}`) i pamięta na dysku Katedry
 * (`GET /api/tlumacz/:jezyk` → `{slownik}`). Słownik jest WSPÓLNY z Hubem — co Hub już przetłumaczył, telefon ma od razu.
 *
 * Tu tylko czysta logika (testy JVM). Compose i pamięć telefonu: ui/tlumacz/TlumaczStoL.kt.
 * Tłumaczymy wyłącznie napisy interfejsu przekazane przez `tl("…")` — rozmowy z agentami, karty Stołu, Biblie
 * i wiadomości TOST to treść, nie interfejs (jak `[data-bez-tlumaczenia]` w Hubie).
 */
object FiltrTlumacza {
    private val litery = Regex("\\p{L}{2,}")
    private val adres = Regex("^(https?://|www\\.|/api/|[\\w.-]+@[\\w.-]+$)", RegexOption.IGNORE_CASE)
    private val plik = Regex("^[\\w.-]+\\.(js|ts|tsx|json|mjs|md|png|jpg|mp4|gguf|zip)$", RegexOption.IGNORE_CASE)
    private val hash = Regex("^[0-9a-f]{7,40}\\b")
    private val sciezka = Regex("^[\\w.-]+/[\\w./-]+$")
    private val skrot = Regex("^[A-Z0-9_]{2,}$")
    private val maleLitery = Regex("[a-ząćęłńóśźż]")
    private val samogloski = Regex("[AEIOUY]")
    private val kod = Regex("^[a-z]{2,3}(-[A-Za-z]{2,4})?$")

    /** Ten sam filtr co w moście: słowa tak, liczby/adresy/pliki nie. */
    fun doTlumaczenia(t: String): Boolean {
        val s = t.trim()
        if (s.length < 2 || s.length > 600) return false
        if (!litery.containsMatchIn(s)) return false
        if (adres.containsMatchIn(s)) return false
        if (plik.matches(s)) return false
        if (hash.containsMatchIn(s) || sciezka.matches(s)) return false
        if (skrot.matches(s) && !maleLitery.containsMatchIn(s)) return s.length > 3 && samogloski.containsMatchIn(s)
        return true
    }

    /** Poprawny kod języka (en, de, pt-BR) albo null. */
    fun kodJezyka(j: String?): String? = j?.trim()?.takeIf { kod.matches(it) }
}

/** Języki w wyborze — te same co w Hubie (TeO_Genesis lib/locale.ts → JEZYKI). */
val JEZYKI_KATEDRY: List<Pair<String, String>> = listOf(
    "pl" to "🇵🇱 Polski", "en" to "🇬🇧 English", "de" to "🇩🇪 Deutsch", "it" to "🇮🇹 Italiano", "es" to "🇪🇸 Español",
    "fr" to "🇫🇷 Français", "pt" to "🇵🇹 Português", "nl" to "🇳🇱 Nederlands", "cs" to "🇨🇿 Čeština", "sk" to "🇸🇰 Slovenčina",
    "uk" to "🇺🇦 Українська", "lt" to "🇱🇹 Lietuvių", "sv" to "🇸🇪 Svenska", "no" to "🇳🇴 Norsk", "da" to "🇩🇰 Dansk",
    "fi" to "🇫🇮 Suomi", "hu" to "🇭🇺 Magyar", "ro" to "🇷🇴 Română", "el" to "🇬🇷 Ελληνικά", "tr" to "🇹🇷 Türkçe",
    "ar" to "🇸🇦 العربية", "he" to "🇮🇱 עברית", "hi" to "🇮🇳 हिन्दी", "ja" to "🇯🇵 日本語", "ko" to "🇰🇷 한국어",
    "zh" to "🇨🇳 中文", "vi" to "🇻🇳 Tiếng Việt", "id" to "🇮🇩 Bahasa Indonesia",
)

/** Odpowiedź `POST /api/tlumacz`. `pominiete` = model ich nie oddał — zostają po polsku. */
data class OdpowiedzTlumacza(val mapa: Map<String, String>, val pominiete: List<String>, val blad: String?) {
    companion object {
        fun zJson(m: Map<String, Any?>) = OdpowiedzTlumacza(
            mapa = napisy(m.obiekt("mapa")),
            pominiete = m.lista("pominiete").filterIsInstance<String>(),
            blad = m.napis("blad"),
        )

        /** Obiekt JSON → mapa napisów (inne wartości odpadają). */
        fun napisy(m: Map<String, Any?>?): Map<String, String> =
            m.orEmpty().mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap()
    }
}

/**
 * Słownik jednego języka + kolejka brakujących (bez Androida). Bezpieczny wątkowo: UI pyta z wątku głównego,
 * paczki wracają z wątku w tle.
 */
class SlownikTlumacza(val jezyk: String) {
    private val slownik = HashMap<String, String>()
    private val brakujace = LinkedHashSet<String>()
    private val wDrodze = HashSet<String>()
    private val nieudane = HashSet<String>()

    /**
     * Napis w języku słownika albo null, gdy tłumaczenia jeszcze nie ma (wtedy trafia do kolejki).
     * Spacje wokół zostają (tłumaczymy treść, nie białe znaki). `pl` i teksty spoza filtra = bez zmian.
     */
    @Synchronized
    fun przetlumacz(tekst: String): String? {
        if (jezyk == "pl" || jezyk.startsWith("pl-")) return tekst
        val klucz = tekst.trim()
        if (!FiltrTlumacza.doTlumaczenia(klucz)) return tekst
        slownik[klucz]?.let { return tekst.replaceFirst(klucz, it) }
        if (klucz !in nieudane && klucz !in wDrodze) brakujace.add(klucz)
        return null
    }

    /** Następna paczka do mostu (≤ 30, jak w Hubie); zabrane teksty są „w drodze”, aż wróci odpowiedź. */
    @Synchronized
    fun paczka(max: Int = 30): List<String> {
        val p = brakujace.take(max)
        p.forEach { brakujace.remove(it); wDrodze.add(it) }
        return p
    }

    @Synchronized
    fun czeka(): Int = brakujace.size + wDrodze.size

    /** Odpowiedź mostu: tłumaczenia do słownika, pominięte — po polsku na stałe (do zmiany języka). */
    @Synchronized
    fun przyjmij(paczka: List<String>, odp: OdpowiedzTlumacza) {
        slownik.putAll(odp.mapa)
        nieudane.addAll(odp.pominiete)
        wDrodze.removeAll(paczka.toSet())
    }

    /**
     * Paczka nie doszła. `naStale` (most odpowiedział błędem) = nie mielimy w kółko, polski zostaje;
     * bez połączenia (`naStale = false`) — wraca do kolejki, spróbujemy przy następnym napisie.
     */
    @Synchronized
    fun nieDoszla(paczka: List<String>, naStale: Boolean) {
        wDrodze.removeAll(paczka.toSet())
        if (naStale) nieudane.addAll(paczka) else paczka.forEach { if (it !in slownik) brakujace.add(it) }
    }

    /** Słownik z dysku Katedry albo z pamięci telefonu. */
    @Synchronized
    fun dolacz(m: Map<String, String>) { slownik.putAll(m) }

    @Synchronized
    fun kopia(): Map<String, String> = HashMap(slownik)

    /** Do pamięci telefonu: obiekt JSON (ten sam kształt co `_OtakOs_Wymiar/tlumaczenia/<język>.json`). */
    @Synchronized
    fun doJson(): String = slownik.entries.joinToString(",", "{", "}") { (k, v) -> "${jsonNapis(k)}:${jsonNapis(v)}" }

    companion object {
        /** Ciało `POST /api/tlumacz`. */
        fun cialoZapytania(jezyk: String, teksty: List<String>): String =
            "{\"jezyk\":${jsonNapis(jezyk)},\"teksty\":[${teksty.joinToString(",") { jsonNapis(it) }}]}"

        /** Słownik zapisany przez `doJson` (zepsuty zapis = pusto). */
        @Suppress("UNCHECKED_CAST")
        fun zJson(tekst: String?): Map<String, String> =
            runCatching { OdpowiedzTlumacza.napisy(Json.parsuj(tekst.orEmpty()) as? Map<String, Any?>) }.getOrDefault(emptyMap())
    }
}
