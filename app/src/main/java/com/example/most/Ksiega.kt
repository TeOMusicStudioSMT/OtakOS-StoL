package com.example.most

/**
 * 📖 Żywa Księga na telefonie (Suweren 2026-10-29: „rób Księgę w StoL… dogrywanie z telefonu”).
 * Wpisy żyją w Katedrze (services/ZywaKsiega.js w teo-app-hub), telefon czyta i dokłada:
 *  · lista    — `GET /api/zywa-ksiega`,
 *  · nowa     — `POST /api/zywa-ksiega {tekst, publiczny}` (stado zaczyna samo: słowo Kronikarza, obraz Palety),
 *  · zdjęcie  — `POST /api/zywa-ksiega/:id/zdjecie` (surowy obraz; most zdejmuje metadane, w tym GPS),
 *  · nagranie — `POST /api/zywa-ksiega/:id/nagranie` (surowe wideo z galerii; most → mp4),
 *  · 🌍/🔒   — `PUT /api/zywa-ksiega/:id {publiczny}`, ↻ stado — `POST /:id/stado`.
 * Usuwanie wpisu zostaje przy maszynie (most odmawia telefonowi).
 */
data class DodatekStada(val stan: String, val tekst: String?, val plik: String?, val powod: String?) {
    val gotowy: Boolean get() = stan == "gotowe"
    val opisStanu: String
        get() = when (stan) {
            "czeka" -> "czeka w kolejce…"
            "robi" -> "tworzy…"
            "blad" -> "⚠ ${powod ?: "błąd"}"
            else -> ""
        }

    companion object {
        fun zJson(m: Map<String, Any?>) = DodatekStada(
            stan = m.napis("stan").orEmpty(),
            tekst = m.napis("tekst")?.takeIf { it.isNotBlank() },
            plik = m.napis("plik")?.takeIf { it.isNotBlank() },
            powod = m.napis("powod")?.takeIf { it.isNotBlank() },
        )
    }
}

data class WpisKsiegi(
    val id: String,
    val kiedy: String,
    val tekst: String,
    val publiczny: Boolean,
    /** Nazwa pliku zdjęcia we wpisie (`zdjecie.jpg`) i jego wersja (czas wgrania — do odświeżenia podglądu). */
    val zdjecie: String?,
    val zdjecieKiedy: String?,
    val nagranieSekundy: Double?,
    val maNagranie: Boolean,
    val rysunkow: Int,
    val slowo: DodatekStada?,
    val ilustracja: DodatekStada?,
) {
    /** Stado jeszcze pracuje nad tym wpisem — warto odświeżać. */
    val stadoPracuje: Boolean get() = listOfNotNull(slowo, ilustracja).any { it.stan == "czeka" || it.stan == "robi" }
    val stadoBlad: Boolean get() = listOfNotNull(slowo, ilustracja).any { it.stan == "blad" }

    companion object {
        /** Ścieżki plików wpisu w moście — tylko te nazwy most w ogóle podaje (Straż: WZOR_KSIEGI_DLA_SPAROWANYCH). */
        val PLIKI = setOf("zdjecie.jpg", "ilustracja.png", "nagranie.mp4")
        private val ID = Regex("^k-\\d{8}-[0-9a-f]{6}$")

        @Suppress("UNCHECKED_CAST")
        fun zJson(m: Map<String, Any?>): WpisKsiegi? {
            val id = m.napis("id")?.takeIf { ID.matches(it) } ?: return null
            val stado = m.lista("stado").mapNotNull { it as? Map<String, Any?> }
            fun dodatek(rodzaj: String) = stado.firstOrNull { it.napis("rodzaj") == rodzaj }?.let(DodatekStada::zJson)
            val zdj = m.obiekt("zdjecie")
            val nag = m.obiekt("nagranie")
            return WpisKsiegi(
                id = id,
                kiedy = m.napis("kiedy").orEmpty(),
                tekst = m.napis("tekst").orEmpty(),
                publiczny = m.logika("publiczny") ?: false,
                zdjecie = zdj?.napis("plik")?.takeIf { it in PLIKI },
                zdjecieKiedy = zdj?.napis("kiedy"),
                nagranieSekundy = nag?.liczba("sekundy"),
                maNagranie = nag?.napis("plik")?.let { it in PLIKI } == true,
                rysunkow = m.lista("rysunki").size,
                slowo = dodatek("slowo"),
                ilustracja = dodatek("ilustracja"),
            )
        }

        @Suppress("UNCHECKED_CAST")
        fun lista(m: Map<String, Any?>): List<WpisKsiegi> =
            m.lista("wpisy").mapNotNull { (it as? Map<String, Any?>)?.let(::zJson) }

        /** Ciało nowego wpisu; pusta Iskra wraca jako błąd bez pytania mostu. */
        fun cialoNowego(tekst: String, publiczny: Boolean): String? {
            val t = tekst.trim()
            if (t.isEmpty() || t.length > MAX_TEKSTU) return null
            return "{\"tekst\":${jsonNapis(t)},\"publiczny\":$publiczny}"
        }

        const val MAX_TEKSTU = 4000
        /** Cloudflare (tunel) przyjmuje do 100 MB w jednym żądaniu — większy film nie dojdzie do mostu. */
        const val MAX_NAGRANIA_BAJTOW = 95L * 1024 * 1024
        const val MAX_ZDJECIA_BAJTOW = 25L * 1024 * 1024
    }
}
