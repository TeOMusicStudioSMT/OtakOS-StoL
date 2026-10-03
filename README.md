# StoL — Katedra w telefonie

**StoL** to apka na Androida (Kotlin, Jetpack Compose) dla Suwerena Katedry OtakOS:
okno na **własną, lokalną Katedrę** — co w tej chwili robią jej TeOgochi.
Telefon patrzy na stado, zleca mu **nowy wspólny projekt** i decyduje na **Stole ratyfikacji**.
Silniki, ponawianie zleceń i odłączanie urządzeń zostają przy maszynie.

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
`MostKlient` (java.net), `LinkParowania`, `StanStada`, `ProjektStada` (+ `NowyProjekt`), `KartaStolu` (+ `NowaKarta`), mały parser JSON.
Ekrany Stołu (`ui/stol/StolEkrany.kt`) to sam Compose bez importów z Androida. Testy JVM:
`app/src/test/java/com/example/most/MostTest.kt`.

## Stół ratyfikacji (zakładki Stół / Izba / Historia / Agenci)

Karty propozycji z mostu (`/api/stol`, services/Stol.js w Katedrze), wygląd z AI Studio (marmurowy stół):

- **Stół**: karty pogrupowane po drodze **na stole → opracowuje stado → do akceptacji → zratyfikowane**
  (plus „utknęła” i „odłożone”). Karty kładzie się z Katedry (Podcast Twin → „Na Stół” / „📄 Plik”)
  albo z telefonu („Połóż na stół”).
- **Karta**: „Przyjmij → Projekt Stada” (uczestnicy: proponowani w karcie, do odznaczenia),
  po Biblii projektu „Ratyfikuj → moduły Katedry” (dopiero wtedy Marketplace, muzyka, 3D, wideo
  dostają zlecenia), „Odłóż ze stołu”, zapis decyzji.
- **Rundy doskonalenia i pętla kreatywna** (przy przyjęciu i w „Doskonal” zamiast ratyfikacji): stado
  robi 1–5 rund; po każdej Sędzia ocenia Biblię względem wizji (0–10) i wypisuje braki, na których
  następna runda buduje; ≥ 9/10 kończy wcześniej. Pętla (0–3) — każdy punkt planu szlifowany krytycznie.
- **Warsztat karty** (do akceptacji, po ratyfikacji, w pracy): „▶ Teraz: N rund” — stado doskonali od razu,
  karta pokazuje postęp na żywo; „🌙 Na Nocną Zmianę: P × N rund” — Katedra zrobi to, gdy śpisz. Karta
  pokazuje, co czeka na noc i czy Nocna Zmiana jest włączona (włącza się ją przy Katedrze).
- **Głos Stołu** 🔊: syntezator mowy telefonu (lokalnie) mówi, gdy projekt jest gotowy do ratyfikacji
  (z rundami i oceną), gdy utknie i gdy moduły oddadzą zlecenia. Tylko gdy StoL jest otwarty; przełącznik w nagłówku.
- **Izba Akceptacji**: tylko to, co czeka na Suwerena; liczba na plakietce.
- **Historia**: szyna zdarzeń Katedry (`/api/szyna/zdarzenia`), domyślnie Stół i projekty.
- **Agenci**: stado TeOgochi z mostu (forma, dziedzina, XP, co ostatnio zrobił).

Odświeżanie co 10 s, tylko gdy zakładka stołu jest na ekranie. Dawne atrapy z AI Studio
(agenci wpisani w kod, symulowana praca, baza Room) są usunięte.

## 💬 TOST — rozmowy z innymi Katedrami (zakładka TOST)

- **Kontakty**: Katedry online z rejestru otakos.wtf (tylko nicki, zatwierdzone przez Suwerena strony)
  i te, z którymi już pisałeś; 🟢 online / ⚪ offline, liczba nowych, ⏳ czekające.
- **Wątek**: telefon pisze przez **swoją** Katedrę (`/api/tost/siec/…`, tunel + klucz); Katedra szyfruje
  kopertę end-to-end (X25519 + AES-GCM, podpis ed25519) i niesie ją do tunelu drugiej Katedry.
  Gdy ta jest offline — wiadomość **czeka w Twojej Katedrze** i wychodzi sama (stan: ⏳ czeka / ✓ dostarczona / ✕ niedostarczona).
- **🪪 Wizytówki Katedr**: przycisk otwiera `otakos.wtf/#katedry?moja=<nick>` — przeglądarka telefonu
  zapamiętuje „moją Katedrę” i panel „Twoja” pokazuje ją z sieci (telefon nie widzi mostu 127.0.0.1).
- Wymaga, by Katedra miała wizytówkę (nick), Kwantowy Tunel i włączony meldunek.
- **🏛️ Zatwierdzanie Katedr** (Izba Akceptacji): gdy Twoja Katedra jest zarządcą rejestru otakos.wtf, nowe Katedry, które
  się zameldowały, czekają tu na „✓ Zatwierdź” / „Odrzuć” — Katedra wysyła podpisaną listę do rejestru; plakietka Izby je liczy.

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
