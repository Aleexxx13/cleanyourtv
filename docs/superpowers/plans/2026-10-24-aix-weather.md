# Minimal Home Weather Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans for native execution in this session. Steps use checkbox syntax.

**Goal:** Transparent current weather for Aix-en-Provence below the Home clock, with previous-APK recovery.
**Architecture:** A pure Java model validates reports/freshness. A lifecycle-bound HTTPS controller updates native Outfit text. A theme switch opts in, with no new dependency or service.
**Tech Stack:** Java 17, Android API 26–34, HttpsURLConnection, platform JSON, JUnit 4.
**Spec:** ../specs/2026-10-24-aix-weather-design.md

## Global Constraints

- Fixed city, Celsius and French conditions; no device location, ads or telemetry.
- Default-off weather switch; no upstream PR, new dependency, service or change to safety scripts.
- At most one fetch per 30 minutes, only while Home is visible; bounded network work off UI thread.
- Preserve package, certificate and versionCode; no uninstall or data clear. Keep private backups out of Git.

## Review Focus

- Disabled/paused Home must not fetch or apply late responses.
- Missing/non-finite temperature and unknown codes must never become plausible fake weather.
- Stale/future cache and wrong TV clock must not present old data as current.
- City changes must invalidate cached data.
- Long French text and race panel must not overlap header, dock or title.

### Task 1: Baseline and recovery

**Files:** ignored `backup/20261024-before-weather/`, `work/aix-weather/restore.sh`, `work/aix-weather/README.md`.
**Interfaces:** Installed APK and checksum, external files archive/theme, package/settings and Home/helper snapshots; reinstall using existing ADB tools, restart with official theme script to rebind helper.

- [ ] Save installed APK, external theme files, package/disabled lists, getprop, settings and Home/accessibility snapshots; compare APK digest with TV.
- [ ] Write syntax-checked restoration script using `pm install -r` without clearing private app data. Explain private order/preferences stay in place, not exported.

### Task 2: Data and foreground fetch

**Files:** create `Weather.java`, `WeatherController.java`, `WeatherTest.java`; modify `Config.java`, `config.xml`, `AndroidManifest.xml`.
**Interfaces:** `Weather.Place` (enabled, name, lat, lon), `Weather.Report` (Celsius, WMO code, day, observation time); `Weather.fresh(report, now)`; `Weather.condition(code, day)`; `WeatherController.start()`, `.stop()`, `.close()`; `Config.weather`.

- [ ] Write failing tests for WMO groups, clear night, negative/rounded temperatures, missing/non-finite data, 90-minute expiry, future time, invalid coordinates, city identity and refresh timing.
- [ ] Run focused tests to see absent-feature failure; implement pure model and rerun green.
- [ ] Implement validated optional config and fork-only INTERNET permission.
- [ ] Implement 16 KiB bounded HTTPS JSON, 8-second connect/read timeouts, one worker, foreground-only 30-minute scheduling, keyed cache, failure throttling and lifecycle generation/cancellation guards.

### Task 3: Native view

**Files:** create native weather text views inside `WeatherController.java`; modify `MainActivity.java`; add linked feature documentation.
**Interfaces:** Non-focusable city and condition views owned by controller; Home lifecycle calls controller.

- [ ] Add transparent Outfit text under clock. Keep optional race panel beneath it.
- [ ] Start/stop/close with Home lifecycle. Document attribution, network permission, options and recovery.
- [ ] Run `testReleaseUnitTest lintRelease assembleRelease`; require zero errors and APK under 491,520 B.

### Task 4: Delivery

- [ ] Compare signing certificates; install via unchanged official script, with HOME_KEY_SERVICE=1 and STOCK_LAUNCHER=none.
- [ ] Apply a theme copied from actual baseline with weather opt-in. Check real data, layout, D-pad edges, app return, helper binding and order.
- [ ] Obtain physical remote/visual confirmation; keep accepted volume behaviour and user's choice to leave ADB open.
- [ ] Review branch, run relevant repo checks, commit and publish only to owner's fork. Report exact rollback command.
