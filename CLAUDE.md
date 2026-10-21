# CLAUDE.md — Catchit

Android app (package `com.ilsecondodasinistra.catchit`) that shows live departures of ACTV/AVM bus and tram lines
in Venezia/Mestre. Bottom bar = destinations (`ImporterConfig.destinations`): Venezia and Centro from San Marco Musatti
(poles `6059`, `6060`), Stazione and Aeroporto from Forte Marghera (`172`/`1172` Hermada: a guess, no stop is called
"uffici giudiziari"/"tribunale"). Each has two tabs: *andata* = departures from the origin that reach the target without
changing, *ritorno* = departures from the target that reach the origin. The app never ships the whole network, only the
lines that directly link those places (derived from the feed by the importer).

## Working agreements
- Talk to the user in Italian; ask (AskUserQuestion, recommended option first) before significant architectural decisions.
- UI strings: Italian only, in `strings.xml` (`board_*` for the board screen).
- Commit on `master`, Conventional-ish messages ending with the Co-Authored-By line.

## Build & run
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
- **Tram realtime (checked 2026-10-21 21:40)**: `tripUpdates` has no T1/T2, but `vehiclePositions` does list the trams as
  vehicles `TRAM_01…TRAM_20` (ids 3361x–3363x) with lat/lon/bearing and **no trip, no route, no stop** (74 of 127 vehicles
  are trip-less; none are near the 15/43/12L stops, so those lines simply weren't running). So trams only show timetable
  data today; an ETA would have to be inferred from position + bearing + the T1 stop sequence. The official AVM app uses
  data that isn't in the open feed. `/tram/GTFSRT/*` and `/aut/GTFSRT/alerts` give 404/403.
- The feed only covers ~25 of the 97 lines at any time; absence from `tripUpdates` means "no live data", not "not running".
- The navigation (vaporetti) feeds live under `/nav/` (same schema); unused so far.

## Board behaviour
- Per leg: timetable + live merged over a window of 6 h ahead (crosses midnight into the next service day) and the last 15 min
  behind (`DepartureMerger.DEFAULT_*`); if that leaves fewer than 5 upcoming rows (a line stopped for the night) the window
  grows to 30 h (`BoardViewModel`), rows of other days get a "domani" tag. Live updates only count for the (stop, line) pairs
  of the leg (`Timetable.stopRoutes`). Past rows are dimmed; the list opens on the first non-past row (scroll up for the past).
- Notices (indicative timetable, feed offline) live outside the lazy list so they stay visible after the initial scroll.
