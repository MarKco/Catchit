# CLAUDE.md — Catchit

Android app (package `com.ilsecondodasinistra.catchit`) that shows live departures of ACTV/AVM bus and tram lines
in Venezia/Mestre. Bottom bar = destinations (`ImporterConfig.destinations`): Venezia and Centro from San Marco Musatti
(poles `6059`, `6060`), Stazione and Aeroporto from Forte Marghera (`172`/`1172` Hermada: a guess, no stop is called
"uffici giudiziari"/"tribunale"). Each has two tabs: *andata* = departures from the origin that reach the target without
changing, *ritorno* = departures from the target that reach the origin. The app never ships the whole network, only the
lines that directly link those places (derived from the feed by the importer).

Long-form notes of the map/shortcuts/startup work (decisions, feed findings, measurements, limits, ideas not done): `docs/diario-2026-10-22-mappa.md`.

## Working agreements
- Talk to the user in Italian; ask (AskUserQuestion, recommended option first) before significant architectural decisions.
- UI strings: Italian only, in `strings.xml` (`board_*` for the board screen).
- Commit on `master`, Conventional-ish messages ending with the Co-Authored-By line.

## Build & run
- If a phone has the app installed with another signing key, `installDebug`/`adb install -r` fail with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: don't uninstall a phone in use (its data goes), test on the emulator instead.
- `./gradlew :app:installDebug` (emulator `emulator-5554`; adb in `~/Library/Android/sdk/platform-tools`).
  `installDebug` on a running app does not kill it: `adb shell am force-stop` before relaunching.
- Tests: `./gradlew :app:testDebugUnitTest :tools:importer:test`.
- Single module `:app` + JVM tool `:tools:importer`. Versions in `gradle/libs.versions.toml` (same stack as ~/projects/discography:
  AGP 9.4 built-in Kotlin, Compose, Hilt, KSP). Material3 is pinned to `1.5.0-beta02` because the BOM's 1.4.0 hides the
  Expressive APIs (`MaterialExpressiveTheme`, `LargeFlexibleTopAppBar`).

## Data
- Static: `./gradlew :tools:importer:run [--args=--refresh]` downloads AVM's `actv_aut.zip` (cached in `tools/cache/`, git-ignored)
  and writes `app/src/main/assets/timetable.json` from the places in `ImporterConfig.kt` (a `Place` matches stops by id,
  exact name or name prefix). Notes: the last stop of a trip always allows alighting (the night loops' terminus is flagged
  "no drop-off"); holiday services exist only in `calendar_dates` and are emitted with no weekly days. Run it **in a separate Gradle
  invocation before** `installDebug` (the app doesn't depend on it, the stale asset gets packaged otherwise).
- Realtime: `https://tpl.actv.it/aut/GTFSRT/tripUpdates` (protobuf, no auth, ~50 KB), polled every 20 s only while the screen
  is STARTED. Parsed by the hand-written `ProtoReader`/`TripUpdatesParser` (no protobuf dependency).
- **RT `trip_id`s can't be joined to the static feed**: they embed the feed version (`AUT_ACTV_792_*` live vs `793` in the zip) and AVM
  publishes a new version often. `DepartureMerger` matches by line + `time - delay` (±150 s) instead. RT carries absolute predicted
  time, delay, `route_id`, `direction_id` but no headsign (taken from the timetable).
- The static feed has a validity window (`feed_start_date`): before it, `Timetable.runsOn` reuses the first timetable (same weekday) as an *indicative* stand-in and the UI says so.
- **Tram realtime**: checked at night (2026-10-21 21:40) the trams were trip-less (just `TRAM_01…TRAM_20` with lat/lon/bearing), but
  **in daytime (2026-10-22 09:10) 11 of 13 trams carry trip + route (`T1_UM`/`T2_UM`) + stop updates**, like the buses; only a couple
  (e.g. `TRAM_05`) have no trip/route. So trams are live like buses when running. `/tram/GTFSRT/*` and `/aut/GTFSRT/alerts` give 404/403.
- `vehiclePositions` (checked 2026-10-22 09:12): every vehicle has a `bearing`, but `0.0` means "unknown" (101 of 327: parked/trip-less) and is
  read as null. `stop_id` (field 7) is the *last stop served* (the next stop update is the one after it), fixes are ~50-60 s old. A trip's
  `stop_time_update`s cover every remaining stop (`stop_sequence` equals the static one). Not every RT trip id is in the static zip (134/158),
  so runs are matched by route + stops, never by trip id.
- The feed only covers ~25 of the 97 lines at any time; absence from `tripUpdates` means "no live data", not "not running".
- The navigation (vaporetti) feeds live under `/nav/` (same schema); unused so far.

## Board behaviour
- Per leg: timetable + live merged over a window of 6 h ahead (crosses midnight into the next service day) and the last 15 min
  behind (`DepartureMerger.DEFAULT_*`); if that leaves fewer than 5 upcoming rows (a line stopped for the night) the window
  grows to 30 h (`BoardViewModel`), rows of other days get a "domani" tag. Live updates only count for the (stop, line) pairs
  of the leg (`Timetable.stopRoutes`). Past rows are dimmed; the list opens on the first non-past row (scroll up for the past).
- Glass UI (Haze, like Discography): header (title, live status, tabs, notices) and bottom bar are `Modifier.glass(haze)` overlays;
  the pages are `hazeSource`s and pad their lists by the measured header/bar heights. No collapsing app bar (the list can't
  scroll behind a bar whose height changes). Notices live in the header so they stay visible after the initial scroll.
- Each line has its own badge color (`ui/theme/LineColors.kt`; ACTV paints all of them red). Add new lines there.

## Timetable updates (on the phone)
- `TimetableUpdater` (checked 5 s after start, and daily by `TimetableUpdateWorker`: unmetered network, battery not low):
  HEAD on the AVM zip → if ETag/Last-Modified/size changed (stamp includes `TimetableConfig.fingerprint`) → download + `TimetableBuilder`
  at background priority → `TimetableRepository.install` (`files/timetables/timetable-<feedStart>.json`). Bundled asset is the fallback.
- `Timetable` holds several versions and answers each day with the one in force that day (a timetable published early doesn't replace the
  current one before its start). `SnapshotPolicy.obsolete` prunes superseded files. Changing `ImporterConfig`/`TimetableConfig`
  destinations changes the fingerprint: older downloaded files are dropped; regenerate the bundled asset with the importer.

## Settings, calendar, log
- Overflow menu (three dots, header): pick day+time (Material date picker from today to the end of the timetable, then time picker), back to
  today (calendar icon with today's number), settings. Another day = whole-day timetable only (`BoardViewModel.wholeDay`), no live data and
  no polling, countdowns replaced by times; the list opens at the picked time. The app forces `Locale.ITALIAN` (`MainActivity.attachBaseContext`)
  so Material pickers are Italian.
- Settings (`ui/settings`): timetable versions/lines/last check, "Controlla ora" and "Forza aggiornamento" (asks before a download on mobile
  data), bottom-bar editor (drag handle + switch; saved in `BarPreferences`; the first visible button is the start screen; at least one stays
  visible), and the log (`UpdateLog`: last 50 events, kept in `files/update-log.jsonl`, copy/share text starts with app/Android/device and
  timetable versions so it can be pasted in a bug report).

## Passage sheet (tap on a card)
- Asset schema 2 (`TimetableConfig.SCHEMA` is part of the fingerprint: bump it when the generated JSON changes, older downloaded
  files are then dropped): each pattern has `alight` (stop where the run reaches the other end of the leg), `durations` (seconds, aligned
  with `times`) and a `shape` (stops from boarding to getting off with the *median* offset of the trips of that pattern, so intermediate
  stop times are typical, shown with "~"). Stops carry lat/lon. The builder reads `stop_times.txt` twice (pass 1: which trips link the ends;
  pass 2: the stops between).
- Live details: `TripUpdatesParser.tripStopTimes` (predicted time at every stop of the shown trips, so the arrival and the stops in between
  are exact for live trips) and `VehiclePositionsParser` (fix of the vehicle by trip id; trams have none). `DepartureSheet` shows the
  straight-line distance from the vehicle to the boarding stop. The *static* feed has no accessibility data (`wheelchair_accessible` /
  `bikes_allowed` are empty in trips.txt). The live `VehicleDescriptor` has a `wheelchair_accessible` field (2 = accessible on 174 vehicles, 3 = not on 75), but every tram reads 3,
  which is implausible for low-floor trams: unreliable, so the sheet doesn't show any.
- A live passage that matches no timetable row borrows headsign, destination, duration and stops from the busiest timetable row of that
  line (`Timetable.typical`).
- Since 2026-10-22 00:00 the live feed uses the 793 trip ids, equal to the ids in the static zip: an exact join by trip id would now be
  possible (not needed so far). Many entities carry only the `trip_id` and no stop updates: they are ignored.
- AVM's zip server sometimes answers 200 with a ~2 KB page (or drops the connection) instead of the 14 MB file. The updater checks the length
  declared by HEAD and the zip magic; a bad download is a *network* error (retried), never a build error (which blocks that zip). A manual
  check retries a zip that failed before.

## Start-up performance
- Measure on the **release** build (R8 on): `./gradlew :app:assembleRelease`, `adb install -r app/build/outputs/apk/release/app-release.apk`,
  then `adb shell cmd package compile -f -m speed-profile com.ilsecondodasinistra.catchit` (adb installs don't apply the profile by themselves).
  Cold start = `adb shell am force-stop …; adb shell am start -W -n …/.MainActivity` (TotalTime); `ReportDrawnWhen` logs "Fully drawn".
  Phases: `adb shell setprop log.tag.CatchitStartup DEBUG` and `adb logcat -s CatchitStartup` (`StartupTrace`, also Perfetto sections).
- What was found (emulator, Android 17 arm64, release): data ready at +67 ms after the process start (it was +640 ms) because the timetables
  are read from `Application.onCreate` (`TimetableRepository.preload`) and `BoardViewModel` is created in `MainActivity.onCreate`; the list
  query (`buildState`) costs 1–5 ms, JSON decode ~30–45 ms. Then ~240 ms of the main thread is *asleep* waiting for the launch animation
  (launcher + SurfaceFlinger, software GPU), and the frames after the content are bound by the RenderThread, not by app code (main-thread
  CPU ~40 ms). A stock simple app (Settings) cold-starts in ~230–320 ms on the same emulator: that is the floor. Blur (Haze) is not the cost
  (median 483 ms without it vs 508 with it). Wall times here are noisy (±100 ms): trust the phases, and re-measure on a real phone.
- Baseline profile: module `:baselineprofile` (generator = the main journeys of the app). Regenerate when the UI changes a lot:
  `./gradlew :app:generateBaselineProfile` (needs an emulator/device, ~12 min; result in `app/src/release/generated/baselineProfiles/`, committed).
  `StartupBenchmarks` compares cold start with and without it: `./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest`. It needs a **physical
  device**: Macrobenchmark refuses emulators ("not representative"; it fails with `EMULATOR`). Measured on a physical phone (Android 17, release, 10 cold starts, 2026-10-22, after the map was added): **270 ms median with the profile (254–319), 320 ms without
  (316–335)**. The profile was regenerated on a physical phone with the map journeys (FAB, "Vedi sulla mappa"); the generator and the benchmark wait for the tab
  text "verso Venezia" (tabs are no longer called Andata/Ritorno). MapLibre doesn't touch the start: it's initialised only when a map opens, and has no provider/initializer in the manifest.
  Release APK: see the ABI note in the Map section (`.so` are stored uncompressed, so each ABI adds its full size).
- The splash is held until the first list exists (max 800 ms) so there is no spinner flash.

## Map (FAB on each page, MapLibre + OSM raster tiles)
- `ui/map`: `MapModel.buildMapModel` (pure; testable) works out what the map shows from `BoardUiState`: boarding stops of the leg (`Timetable.stopRoutes`),
  the **upcoming live passages of the leg** whose vehicle is within `MAP_RADIUS_METERS` (2 km) of a boarding stop (so direction is already right:
  `(stop, route)` pairs), yellow "Tram" pills for `looseTrams` (trams with no route), and, for one selected/highlighted passage, its whole run
  (`GeoCatalog.findRun`: route + boarding/alight stop + stops seen live; polyline + stops, passed ones grey, boarding/alight ringed).
  `MapScreen` is a full-screen layer *inside* `BoardContent` (not another screen) so the 20 s polling keeps running; back closes it.
  The FAB is hidden when browsing another day. Entry points: the FAB (leg of the current tab) and the "Vedi sulla mappa" row of the passage sheet
  (that passage highlighted). Tapping a vehicle on the map selects it (draws its run); tapping it again or the map deselects.
- Yellow "Tram" pills (trams the feed places with no trip/route, `TRAM_05` and the like): the subtitle counts them apart ("1 mezzo nei dintorni · 1 tram senza linea") and tapping one shows
  a card saying why it has no name/minutes. Nothing in the feed ties them to a trip (no trip id, and no trip in `tripUpdates` names them as its vehicle), so they are *not* matched to
  scheduled passages. A real case showed why: a scheduled T1 absent from the feed turned out to be running 31 min late on another tram (the feed adds a trip when the tram starts it), while the
  anonymous tram nearby was something else.
- `TransitMap` wraps `MapView` by hand (no maplibre-compose). Markers are bitmaps from `MapMarkers` (the style has no glyphs, so no text layers):
  arrow layer (rotated by bearing, peeking out of the badge) under a badge+ETA-bubble layer. OSM attribution is drawn by `MapScreen` (built-in one is off).
  MapLibre's `libmaplibre.so` is 9–13 MB uncompressed per ABI; `release` keeps only `arm64-v8a` (the phones in use are 64-bit; add `armeabi-v7a` back in `app/build.gradle.kts` for a 32-bit phone), `debug` keeps all so any emulator runs; `MapLibre.getInstance` runs only when a map is first opened.
- Map polish: vehicles glide (1.2 s, straight line) from the previous fix to the new one (`Glide` in `TransitMap`; the feed has no speed, so no dead reckoning);
  recenter button re-frames `MapModel.focus`; the subtitle of a selected vehicle says how old its fix is; in the dark theme the OSM tiles are inverted with
  raster brightness min/max + hue-rotate (no second tile provider).
- Data: `TimetableBuilder.buildFiles` returns `timetable.json` + `geo.json` (schema 3: `runs` = whole stop sequence of each qualifying trip with its
  `path` index, `paths` = shapes.txt simplified to 4 m and delta-packed in 1e-5°; ~55 KB). `geo.json` is read lazily (`TimetableRepository.geo()`) only when a
  map opens; downloaded versions live in `files/timetables/geo-<feedStart>.json`, written before the timetable and pruned with it.

## Launcher shortcuts
- `ShortcutPublisher` publishes one **dynamic** shortcut per visible destination of the bar, in the bar's order (5 s after start, whenever the bar or the
  timetable changes: `BoardViewModel`). Icon = the bar's `ImageVector` drawn into an adaptive bitmap. They launch `MainActivity` with
  `ACTION_OPEN_DESTINATION` + `EXTRA_DESTINATION`; `BoardViewModel.requestedDestination` carries it to `BoardContent`, which selects it and closes any sheet/map
  (`singleTop`: `onNewIntent` handles a shortcut used while the app is open). Test: `adb shell am start -a com.ilsecondodasinistra.catchit.OPEN_DESTINATION -n …/.MainActivity --es destination stazione`.

## Back and bar
- Map and Settings are `PredictiveLayer`s (`ui/PredictiveLayer.kt`) over the board, which stays composed (state, tab, scroll and polling survive; Settings used to replace the board and
  every return reset it to the first destination). The layer fades/scales in, `PredictiveBackHandler` drives it while the back gesture moves, the button plays the same exit.
  Its content gets `entered`: the `MapView` is built only after the enter animation (creating it blocks the main thread).
- `TransitMap` uses MapLibre's **TextureView** mode (`textureMode(true)`): the default SurfaceView flashed beige then black on open and can't be faded/scaled. It stays transparent
  until the camera is framed and a frame is fully rendered (else the first frame is the default camera: a blurry zoomed tile), with a 1.5 s timeout.
- The tabs have no "Andata"/"Ritorno" labels: each is titled "verso <where the leg goes>" (SemiBold). The baseline profile generator and the startup benchmark
  look for that text ("verso Venezia" on the first destination), not for the old labels.
- Back from a bar button other than the first goes to the first (`BackHandler` in `BoardContent`); from the first it leaves the app.
- Default bar order is fixed (`BarPreferences.DEFAULT_ORDER`: Venezia, Centro, Stazione, Aeroporto), whatever order the timetable lists;
  Settings has "Ripristina ordine predefinito".
- A card's title is "verso <where the passage takes you in this leg>" (the stop where it gets off), with the headsign as a small "capolinea X" line
  when different: the headsign is where the vehicle ends its run (the night loops end in Venezia but pass through Mestre Centro).
- A card shows "departure → arrival" under the minutes (`arrivalOf`, shared with the sheet: live arrival at the stop where it gets off when
  the feed has one for that trip, else departure + the usual duration; always the timetable when browsing another day). Live arrivals are bold.
