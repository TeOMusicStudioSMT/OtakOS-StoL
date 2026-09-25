# StoL — Katedra w telefonie

**StoL** to apka na Androida (Kotlin, Jetpack Compose) dla Suwerena Katedry OtakOS:
okno na **własną, lokalną Katedrę** — co w tej chwili robią jej TeOgochi.
Głównie obserwacja: telefon patrzy na stado i może mu zlecić **nowy wspólny projekt** — nic więcej
w Katedrze nie zmienia (silniki, ponawianie zleceń, odłączanie urządzeń zostają przy maszynie).

## Co działa naprawdę (zakładka „Katedra”)

- **Parowanie z mostem** (Wiesio-Bridge) przez Kwantowy Tunel:
  Katedra → Dashboard → karta **„StoL — Katedra w telefonie”** → „Paruj telefon” → QR.
  Skan aparatem otwiera StoL (`otakos-stol://paruj?adres=…&k=…&kod=…`), albo link wkleja się ręcznie.
  Kod: 6 cyfr, 5 minut, jednorazowy → wymieniany na **token urządzenia**.
- **Stado na żywo** — strumień SSE z mostu (`/api/stado/strumien`), gdy ekran jest otwarty;
  zdarzenie z Katedry dochodzi w milisekundach, zerwane połączenie wznawia się samo (2 s → 30 s).
  Wyklute TeOgochi z etapem, XP i kolorem
  z Katedry, **co ostatnio zrobiły** (fakty z szyny zdarzeń mostu), jaja, świeżość migawki.
  Gdy agent milczy, ekran mówi „cisza” — nic nie jest zmyślane.
- **🧱 Świat klocków** (przycisk w zakładce Katedra): scena z mostu (`/swiat/`) w WebView —
  każdy TeOgochi ma swoją płytkę LEGO, a na niej klocek za każde **prawdziwe dzieło** z dysku
  Katedry (utwory Joanny, filmy Klatki, odcinki Reżysera, apki i gry Kodeksa, modele 3D…).
  Zdarzenie z szyny → figurka podskakuje, dymek mówi, co zrobiła. Stuknięcie w płytkę →
  katalog: dzieła z podglądem (odsłuch, film, obraz, „otwórz apkę”) i ślady.
- **🧩 Wspólne projekty stada** — lista projektów z postępem (kto już oddał wkład, co wkłady
  zleciły modułom: 🛒 Marketplace, 🎵 muzyka, 🧊 Assety3D, 🎬 wideo — z błędami modułów, jeśli
  padły) i **natywny formularz „＋ Nowy projekt”**: nazwa, wizja, kto bierze udział, czy wkłady
  same zlecają moduły. Most przyjmuje go tylko z kluczem Straży **i** tokenem sparowanego
  telefonu (`POST /api/stado/projekt/nowy`); projekt pamięta, z którego urządzenia przyszedł.
  Lista odświeża się sama na zdarzenia „projekt” ze strumienia.
- Odłączenie telefonu: w apce albo w karcie StoL w Katedrze.

Rdzeń połączenia (`app/src/main/java/com/example/most/`) to czysty Kotlin bez Androida:
`MostKlient` (java.net), `LinkParowania`, `StanStada`, `ProjektStada` (+ `NowyProjekt`), mały parser JSON. Testy JVM:
`app/src/test/java/com/example/most/MostTest.kt`.

## Co jest szkicem

Zakładki **Stół / Akceptacje / Historia / Agenci** to pierwotny projekt z AI Studio:
sześciu agentów wpisanych w kod (Otak-Alpha, Vektor-9…) i „praca” symulowana
(`StolViewModel.simulateAgentIteration`). Zostały jako szkic wizji — nie łączą się z Katedrą.

## Wizja (Suweren, 2026-09-24)

Kiedyś był to stół z nazwami. Dziś: **sama pisząca się opowieść, wizualnie graficzna,
jak klocki LEGO — w wykonaniu naszych TeOgochi.** Każdy agent wnosi coś ze swojej profesji,
ma swój sandbox, swój mały świat, i buduje go narzędziami Katedry. Świat skórek i agentów,
który sami tworzą. Na smartfonie — moduł obserwacji, z którego można też rzucić stadu nowy projekt.

Droga od tego, co jest, do wizji: migawka stada + szyna zdarzeń (jest) → strumień zdarzeń
zamiast odpytywania (jest) → klocki jako prawdziwe dzieła agentów (jest) → scena, na której
się układają (jest, 2D izometrycznie) → dalej: bryły z Assety3D (GLB) jako klocki 3D,
film klockowy (odtwarzanie dnia z szyny), Delegat w świecie (rozmowa z TeOgochi z płytki).

## Budowanie

Wymaga Android SDK (compileSdk 36). `./gradlew assembleDebug`.
