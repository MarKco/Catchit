# CLAUDE.md — Catchit

Android app (package `com.ilsecondodasinistra.catchit`) that shows live departures of a few ACTV/AVM bus and tram
lines in Venezia/Mestre. POC: stop *San Marco Musatti* (poles `6059` tram→Venezia, `6060` tram→Favaro), lines
T1, 31H, 32H, 12L, N1, N2. The app must never contain the whole network, only a small configured subset.

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
  and writes `app/src/main/assets/timetable.json` for the lines/stops in `ImporterConfig.kt`. Run it **in a separate Gradle
  invocation before** `installDebug` (the app doesn't depend on it, the stale asset gets packaged otherwise).
- Realtime: `https://tpl.actv.it/aut/GTFSRT/tripUpdates` (protobuf, no auth, ~50 KB), polled every 20 s only while the screen
  is STARTED. Parsed by the hand-written `ProtoReader`/`TripUpdatesParser` (no protobuf dependency).
- **RT `trip_id`s can't be joined to the static feed**: they embed the feed version (`AUT_ACTV_792_*` live vs `793` in the zip) and AVM
  publishes a new version often. `DepartureMerger` matches by line + `time - delay` (±150 s) instead. RT carries absolute predicted
  time, delay, `route_id`, `direction_id` but no headsign (taken from the timetable).
- The static feed has a validity window (`feed_start_date`): before it, only live passages exist (the UI says so).
- Seen on 2026-10-21: the tram (T1/T2) is absent from both `tripUpdates` and `vehiclePositions`: trams only show timetable data.
- The navigation (vaporetti) feeds live under `/nav/` (same schema); unused so far.
