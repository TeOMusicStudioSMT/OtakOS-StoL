package com.example.most

/**
 * Parser ramek Server-Sent Events (text/event-stream) — linia po linii.
 * Komentarze (`: puls`) są pomijane; pusta linia kończy ramkę.
 */
class SseParser(private val naRamke: (zdarzenie: String, dane: String) -> Unit) {
    private var zdarzenie = "message"
    private val dane = StringBuilder()

    fun linia(l: String) {
        when {
            l.isEmpty() -> {
                if (dane.isNotEmpty()) naRamke(zdarzenie, dane.toString())
                zdarzenie = "message"; dane.setLength(0)
            }
            l.startsWith(":") -> Unit
            l.startsWith("event:") -> zdarzenie = l.removePrefix("event:").trim()
            l.startsWith("data:") -> {
                if (dane.isNotEmpty()) dane.append('\n')
                dane.append(l.removePrefix("data:").removePrefix(" "))
            }
        }
    }
}

/** Zdarzenie z szyny mostu, tak jak wychodzi strumieniem do telefonu (bez pola `dane`). */
data class ZdarzenieSzyny(val id: Long?, val kiedy: String?, val agent: String, val rodzaj: String, val tresc: String) {
    companion object {
        fun zJson(m: Map<String, Any?>) = ZdarzenieSzyny(
            id = m.liczba("id")?.toLong(),
            kiedy = m.napis("kiedy"),
            agent = m.napis("agent").orEmpty(),
            rodzaj = m.napis("rodzaj").orEmpty(),
            tresc = m.napis("tresc").orEmpty(),
        )
    }
}
