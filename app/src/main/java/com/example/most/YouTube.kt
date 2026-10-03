package com.example.most

/**
 * 📺 Publikacje YouTube w Izbie Akceptacji (Suweren 2026-10-03: „TeOgochi cały proces zrobią w postprodukcji”,
 * ostatnie „tak” — w Izbie). Kronikarz w Katedrze pisze tytuł, opis i tagi (services/PublikacjeYouTube.js
 * w teo-app-hub), telefon tylko pokazuje i decyduje:
 *  · lista   — `GET /api/youtube/publikacje`,
 *  · decyzja — `POST /api/youtube/publikacje/:id/zatwierdz|odrzuc` (✓ = Impresariat wysyła jako NIEPUBLICZNY).
 * Zmiana konta YouTube i kluczy — tylko przy maszynie (Straż), telefon tego nie dotyka.
 */
data class PublikacjaYouTube(
    val id: String,
    val etap: String,
    val nazwa: String,
    val tytul: String,
    val opis: String,
    val tagi: List<String>,
    val url: String?,
    val blad: String?,
    val uwaga: String?,
    /** Kanał, na który pójdzie film (wiele kanałów; zmiana kanału — w Hubie). */
    val kanalNazwa: String? = null,
) {
    val czekaNaSuwerena: Boolean get() = etap == "do_akceptacji"

    val opisEtapu: String
        get() = when (etap) {
            "przygotowuje" -> "✍️ Kronikarz pisze…"
            "do_akceptacji" -> "⏳ czeka na Twoje ✓"
            "wysylanie" -> "⬆️ Impresariat wysyła"
            "prywatna" -> "🔒 na YouTube, ale prywatny"
            "opublikowana" -> "✓ na YouTube"
            "odrzucona" -> "✕ odrzucona"
            "blad" -> "⚠ błąd"
            else -> etap
        }

    companion object {
        fun zJson(m: Map<String, Any?>): PublikacjaYouTube? {
            val id = m.napis("id")?.takeIf { it.isNotBlank() } ?: return null
            return PublikacjaYouTube(
                id = id,
                etap = m.napis("etap").orEmpty(),
                nazwa = m.napis("nazwa").orEmpty(),
                tytul = m.napis("tytul").orEmpty(),
                opis = m.napis("opis").orEmpty(),
                tagi = m.lista("tagi").mapNotNull { it as? String },
                url = m.napis("url"),
                blad = m.napis("blad"),
                uwaga = m.napis("uwaga"),
                kanalNazwa = m.napis("kanalNazwa")?.takeIf { it.isNotBlank() },
            )
        }

        @Suppress("UNCHECKED_CAST")
        fun lista(m: Map<String, Any?>): List<PublikacjaYouTube> =
            m.lista("publikacje").mapNotNull { (it as? Map<String, Any?>)?.let(::zJson) }
    }
}
