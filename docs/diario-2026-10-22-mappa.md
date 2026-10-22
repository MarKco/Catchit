# Diario tecnico — 22 ottobre 2026: mappa, scorciatoie, profilo di avvio

Cosa è stato fatto in questa sessione, perché, cosa è stato misurato e cosa resta aperto.
Le note operative brevi (comandi, regole) stanno in [`CLAUDE.md`](../CLAUDE.md); qui c'è il racconto completo.

## 1. Cosa è stato fatto

| Area | Cambiamento |
|---|---|
| Schede | Tolte le etichette «Andata»/«Ritorno»: resta solo il sottotitolo «verso <dove>», con la stessa formattazione (SemiBold, stile Tab). Stringhe e `Leg.title()` eliminate. |
| Mappa | Un FAB in ogni pagina della barra apre una mappa (MapLibre + tile OpenStreetMap) centrata sui pali di salita della tratta, con i mezzi in arrivo. |
| Marker | Badge col nome della linea, nel colore della lista; freccia di direzione (bearing) se il dato è noto; fumetto sopra con i minuti all'arrivo in fermata. Tram senza linea: puntino grigio, senza nome. |
| Percorso | Dalla scheda di un passaggio, la riga «È a X da Y» è cliccabile («Vedi sulla mappa»): stessa mappa, con il percorso intero del mezzo (polilinea + tutte le fermate, passate in grigio e future a colori, salita e discesa cerchiate). Lo stesso si ottiene toccando un marker nella mappa del FAB (secondo tap o tap sulla mappa: deseleziona). |
| Rifiniture mappa | Marker che scivolano (1,2 s) tra un fix e l'altro; pulsante «ricentra»; età della posizione nel sottotitolo («posizione di 7 s fa»); tile invertiti nel tema scuro. |
| Dati | Il builder produce anche `geo.json` (schema 3): percorsi semplificati e sequenza completa delle fermate di ogni corsa che collega i posti. File separato, letto solo all'apertura della mappa. |
| Realtime | `VehiclePositionsParser.parseAll` (id/etichetta, corsa, linea, fermata, bearing); bearing `0.0` = sconosciuto; tram senza linea esposti come `looseTrams`. |
| Scorciatoie | Scorciatoie dinamiche dell'icona, una per voce visibile della barra, nell'ordine della barra. |
| Avvio | Baseline profile rigenerato con i percorsi della mappa; il generatore e il benchmark non cercano più «Andata». |
| APK | In `release` solo `arm64-v8a`: l'APK passa da ~52 MB (tutte le ABI) a 15,6 MB. |

## 2. Decisioni e motivi

- **MapLibre + tile OSM** (scelto fra MapLibre, osmdroid, Google Maps): nessuna chiave API, nessun Play Services, marker/linee/rotazione in Compose. Prezzo: i tile OSM hanno limiti d'uso, ok per uso personale; per una distribuzione pubblica serve un fornitore di tile. L'attribuzione «© OpenStreetMap» è disegnata da `MapScreen` (quella integrata è spenta). Lo user agent lo imposta MapLibre con nome e versione dell'app.
- **Wrapper `MapView` scritto a mano** invece di `maplibre-compose`: meno dipendenze, nessun rischio di incompatibilità con Kotlin 2.4 / AGP 9.4.
- **Marker come bitmap** (`MapMarkers`): lo stile OSM non ha un server di font (`glyphs`), quindi niente layer di testo. Una bitmap dà anche pieno controllo della grafica. Il badge sta al centro della bitmap (immagine trasparente nell'altra metà) così la coordinata del mezzo è il centro del badge. La freccia è un secondo layer sotto il badge, ruotato col bearing: del triangolo si vede solo la punta.
- **La mappa è un livello a schermo intero dentro `BoardContent`**, non una schermata nuova: così il polling ogni 20 s della scheda continua e la mappa è viva. Il tasto indietro la chiude.
- **Quali mezzi si vedono** (scelta dell'utente): le *corse live in arrivo della tratta* il cui veicolo è entro 2 km da un palo di salita (`MAP_RADIUS_METERS`). La direzione è già giusta perché si usano le coppie (fermata, linea) della tratta (`Timetable.stopRoutes`): lo stesso tram T1 compare solo dal lato giusto. Più il mezzo evidenziato, ovunque sia.
- **File `geo.json` separato e lazy**: non pesa sull'avvio (il decode di `timetable.json` costa 30–45 ms) e l'aggiornamento in background lo scrive prima del file dell'orario, così non esiste mai un orario senza i suoi dati mappa. `SCHEMA` portato a 3: i file scaricati dalle versioni precedenti vengono scartati e riscaricati.
- **Corse abbinate ai percorsi per linea + fermate, non per id corsa**: nel feed live solo 134 corse su 158 hanno un id presente nello zip statico. `GeoCatalog.findRun` sceglie la corsa della linea che sale e poi scende alle fermate giuste e che visita più fermate «viste» dal feed.
- **Fermate passate/future**: l'ultima fermata servita è il campo `stop_id` della posizione; in mancanza, la prima fermata che il feed si aspetta ancora.
- **Scorciatoie dinamiche, non statiche**: la barra è modificabile in Impostazioni, le statiche non seguirebbero ordine e visibilità. L'icona è la stessa `ImageVector` della barra, disegnata in una bitmap adattiva.
- **Solo arm64 in `release`**: le `.so` sono salvate non compresse nell'APK, quindi ogni ABI pesa per intero sia da inviare sia sul telefono. Il `debug` le tiene tutte, così qualunque emulatore funziona. Se servisse un telefono a 32 bit si rimette `armeabi-v7a` nella riga `abiFilters` di `app/build.gradle.kts`.
- **Tile scuri** invertendo luminosità (`raster-brightness-min=1`, `max=0`) e ruotando la tinta di 180°: nessun secondo fornitore. Il risultato è leggibile ma non è una mappa scura vera (le strade diventano nere).

## 3. Cosa si è scoperto sul feed AVM (verificato il 22/10/2026, di giorno)

La nota precedente («i tram non hanno corsa né linea») era stata presa di sera: di giorno è falsa.

- **Tram:** 11 tram su 13 hanno corsa, linea (`T1_UM`/`T2_UM`) e aggiornamenti per fermata, come gli autobus. Fanno eccezione ad esempio `TRAM_05` (nessuna corsa né linea) e `TRAM_20` (corsa ma niente linea e nessun aggiornamento).
- **`tripUpdates`:** 158 corse; ogni corsa elenca *tutte le fermate rimanenti* (da 0 a 24 e oltre), con `stop_sequence` uguale a quello dello statico.
- **`vehiclePositions`:** 327 veicoli, 150 con corsa (tutte presenti in `tripUpdates`), 145 con linea. Il `bearing` c'è sempre, ma `0.0` significa «non noto» (101 veicoli: parcheggiati o senza corsa). Non c'è la velocità. Il campo `stop_id` è l'*ultima fermata servita* (la prossima fermata dell'aggiornamento è quella dopo). Esiste anche `current_stop_sequence`. Le posizioni hanno di solito 47–60 s (fino a ~190 s). Niente occupazione dei mezzi, niente accessibilità.
- **Id corsa:** 134 su 158 sono presenti nello zip statico; per i veicoli 126 su 150.
- **Altri endpoint:** `/tram/GTFSRT/*` e `/aut/GTFSRT/alerts` rispondono 404/403.
- **Controllo per tratta:** per ogni tratta ci sono meno corse con aggiornamento alla fermata di salita che veicoli della stessa linea entro 2 km; i veicoli «extra» sono quelli già passati o nell'altro verso, e non vanno mostrati.
- **Dimensioni dei dati mappa:** stimati ~109 KB di percorsi (tutte le corse delle 11 linee, 63 mila punti grezzi), in realtà solo le corse che collegano i posti: 22 corse e 22 percorsi, 3671 punti dopo la semplificazione a 4 m, `geo.json` ≈ 55 KB (`timetable.json` ≈ 104–107 KB, invariato).

## 4. Misure

| Cosa | Valore |
|---|---|
| Avvio a freddo, telefono fisico (Android 17, release, 10 iterazioni) | **270 ms** mediana con profilo (254–319), 320 ms senza (316–335) |
| Generazione del baseline profile | 7 min 33 s su un telefono fisico; il profilo passa da 214 KB a 279 KB (nomi offuscati da R8) |
| `libmaplibre.so` per ABI (non compressa) | arm64 13,1 MB · armv7 9,5 MB · x86 13,4 MB · x86_64 13,35 MB |
| APK debug (4 ABI) | 71,9 MB |
| APK release | 52 MB circa con tutte le ABI · 25,1 MB con arm64+armv7 · **15,6 MB con solo arm64** |

MapLibre non rallenta l'avvio: `MapLibre.getInstance` e il caricamento della libreria nativa avvengono solo quando si apre la prima mappa, e nel manifest unito non c'è nessun provider/inizializzatore della libreria. Non c'è però una misura «prima di MapLibre» sullo stesso telefono con cui confrontare.

## 5. Come provare

```bash
# build, test, dati
./gradlew :tools:importer:run                       # scrive timetable.json e geo.json (comando separato, prima della build)
./gradlew :app:testDebugUnitTest :tools:importer:test :timetable-builder:test
./gradlew :app:installDebug

# scorciatoie
adb shell dumpsys shortcut | grep destination-      # le 4 scorciatoie pubblicate
adb shell am start -a com.ilsecondodasinistra.catchit.OPEN_DESTINATION \
  -n com.ilsecondodasinistra.catchit/.MainActivity --es destination stazione

# tema scuro
adb shell cmd uimode night yes   # e `no` per tornare

# profilo e benchmark (con più dispositivi collegati: ANDROID_SERIAL=<seriale>)
./gradlew :app:generateBaselineProfile
./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest   # numeri in baselineprofile/build/outputs/connected_android_test_additional_output/
```

Test aggiunti: `GeoCatalogTest` (decodifica del percorso, scelta della corsa), tre test in `TimetableBuilderTest` (corsa intera, percorso a delta, semplificazione). Sono stati rivisti anche i test del parser dei veicoli (una corsa vuota non è mai un abbinamento).

## 6. Limiti noti e cose non verificate

- Le **icone delle scorciatoie** non sono state viste sul launcher (verificato solo che le 4 scorciatoie risultano pubblicate e navigano).
- Lo stato della mappa non sopravvive a un cambio di configurazione (rotazione): è `remember`, non `rememberSaveable`, come la scheda del passaggio.
- Un veicolo vicino al bordo alto può avere il fumetto sotto la barra del titolo; la camera non lo segue.
- Il raggio di 2 km è una costante: una corsa lontana (es. un tram a 8 km, 25 minuti) non compare finché non si avvicina.
- Le posizioni hanno ~1 minuto di età e arrivano ogni 20 s: lo scorrimento dei marker è una linea retta verso il nuovo fix, non una stima lungo il percorso (il feed non ha la velocità).
- Il percorso del T1 disegna anche l'anello al capolinea (è la forma del GTFS).
- Se un orario scaricato non contiene il file mappa (versione vecchia), i percorsi mancano: la scheda disegna comunque le fermate tra salita e discesa unite da linee rette.
- I tile OSM vanno bene per uso personale; per distribuire l'app a molti serve un fornitore di tile con condizioni adatte.

## 7. Proposte discusse e non fatte

| Proposta | Costo | Beneficio | Note |
|---|---|---|---|
| Promemoria «parti tra N min» su un passaggio | medio (permesso notifiche, worker) | alto | Android non garantisce il polling esatto in background |
| Widget con i prossimi 3 passaggi | medio-alto (Glance) | alto | in background si aggiorna ogni ~15 min: orari programmati + ultimo dato live |
| Ritardo tipico per linea e fascia oraria | medio (storico locale) | medio | l'app raccoglie dati solo da aperta: campione scarso |
| Destinazioni modificabili dall'utente | alto | basso se l'app è personale | il builder gira già sul telefono |
| Tile scuri «veri» (secondo fornitore) | basso | cosmetico | richiede condizioni d'uso e attribuzione aggiuntive |
| Avvisi di servizio, affollamento dei mezzi | — | — | non fattibili: endpoint 403 / dato assente nel feed |

## 8. File principali

- Dati: `timetable-builder/.../TimetableBuilder.kt` (`buildFiles`, corse intere, percorsi semplificati), `TimetableConfig.kt` (`SCHEMA = 3`), `tools/importer/.../Main.kt`, `app/.../data/GeoData.kt` (`GeoCatalog`), `TimetableRepository.kt` (`geo()`, installazione e pulizia dei file `geo-<inizio>.json`), `update/TimetableUpdater.kt`.
- Realtime: `data/VehiclePositionsParser.kt`, `ui/board/BoardViewModel.kt` (`looseTrams`, `geo()`, scorciatoie, destinazione richiesta).
- Mappa: `ui/map/MapModel.kt` (funzione pura, testabile), `MapMarkers.kt`, `TransitMap.kt`, `MapScreen.kt`; `ui/board/BoardScreen.kt` (FAB, livello mappa), `DepartureSheet.kt` (riga «Vedi sulla mappa»).
- Scorciatoie: `shortcuts/ShortcutPublisher.kt`, `MainActivity.kt` (`ACTION_OPEN_DESTINATION`, `onNewIntent`).
- Avvio: `baselineprofile/.../BaselineProfileGenerator.kt`, `StartupBenchmarks.kt`, `app/src/release/generated/baselineProfiles/`.
