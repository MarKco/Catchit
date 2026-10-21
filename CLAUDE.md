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
  straight-line distance from the vehicle to the boarding stop. The feed has **no accessibility data** (`wheelchair_accessible` /
  `bikes_allowed` are empty in trips.txt), so the sheet doesn't show any.
- A live passage that matches no timetable row borrows headsign, destination, duration and stops from the busiest timetable row of that
  line (`Timetable.typical`).
- Since 2026-10-22 00:00 the live feed uses the 793 trip ids, equal to the ids in the static zip: an exact join by trip id would now be
  possible (not needed so far). Many entities carry only the `trip_id` and no stop updates: they are ignored.
- AVM's zip server sometimes answers 200 with a ~2 KB page (or drops the connection) instead of the 14 MB file. The updater checks the length
  declared by HEAD and the zip magic; a bad download is a *network* error (retried), never a build error (which blocks that zip). A manual
  check retries a zip that failed before.
