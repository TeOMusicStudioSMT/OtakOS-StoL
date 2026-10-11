package com.example.ui.katedra

import android.content.Context
import com.example.most.TozsamoscKatedry
import com.example.most.ZrodloAdresu

/**
 * Zapamiętane połączenie z Katedrą: adres tunelu, klucz Straży i token urządzenia,
 * a od 2026-10-11 też tożsamość Katedry z wizytówki (nick + klucz ed25519) — po niej StoL
 * odnajduje Katedrę w rejestrze otakos.wtf, gdy tunel zmieni adres (most/Odnajdywanie.kt).
 * `zrodlo` — skąd jest obecny adres (parowanie albo rejestr).
 * Trzymane lokalnie w SharedPreferences telefonu — nigdzie dalej nie jedzie.
 */
data class Polaczenie(
    val adres: String,
    val klucz: String,
    val token: String,
    val tozsamosc: TozsamoscKatedry? = null,
    val zrodlo: ZrodloAdresu = ZrodloAdresu.PAROWANIE,
)

class PolaczenieStore(context: Context) {
    private val prefs = context.getSharedPreferences("stol_katedra", Context.MODE_PRIVATE)

    fun wczytaj(): Polaczenie? {
        val a = prefs.getString("adres", null) ?: return null
        val k = prefs.getString("klucz", null) ?: return null
        val t = prefs.getString("token", null) ?: return null
        val nick = prefs.getString("nick", null)
        val kw = prefs.getString("kluczWizytowki", null)
        val tozsamosc = if (nick != null && kw != null) TozsamoscKatedry(nick, kw) else null
        return Polaczenie(a, k, t, tozsamosc, ZrodloAdresu.zNazwy(prefs.getString("zrodlo", null)))
    }

    fun zapisz(p: Polaczenie) {
        prefs.edit()
            .putString("adres", p.adres).putString("klucz", p.klucz).putString("token", p.token)
            .putString("nick", p.tozsamosc?.nick).putString("kluczWizytowki", p.tozsamosc?.klucz)
            .putString("zrodlo", p.zrodlo.name)
            .apply()
    }

    fun zapomnij() { prefs.edit().clear().apply() }
}
