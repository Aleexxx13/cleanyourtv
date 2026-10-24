# Minimal weather on Home (fork only)

Show Aix-en-Provence, its current temperature in Celsius and a short French weather condition directly below the clock. Reuse the launcher's Outfit font, white/secondary-white colours and native Android views. No background panel, border, animation, extra navigation stop, forecast page or new dependency.

## Approaches

The owner chose direct integration into this fork instead of a separate companion application. This requires INTERNET permission in this fork; it is not proposed for the upstream reference launcher, whose offline design remains unchanged there. Reusing the existing full-screen weather add-on would not meet the requested always-visible Home display.

## Behaviour and implementation boundaries

Use Open-Meteo current temperature, weather code and day/night for the fixed city. No device location, account, API key, ads or telemetry. Reuse the existing native Java design and drawing approach. Display the city above a compact temperature/condition line below the header, without covering the app title or dock. Handle rain, cloud, sun, clear night, fog, snow and thunderstorms accurately rather than reducing all codes to rain.

Fetch over HTTPS on a bounded worker with connection/read timeouts and a bounded response, never on the UI thread. Refresh at most every 30 minutes while Home is visible. Stop scheduled refreshes when Home is hidden and cancel network work on destruction. Cache a validated report and its timestamp for quick Home returns. Old data must be marked unavailable instead of silently presented as current. Failure cannot block Home or steal D-pad focus.

Provide a default-off `weather` theme/config switch and opt in only in the owner's local TV theme. City coordinates/name are configuration, not location permission. Include source attribution in the feature documentation/settings without cluttering the Home line.

## Recovery

Before installation, save the currently installed APK, external launcher theme/files, package/disabled-package lists, getprop, settings, Home owner and accessibility state in ignored private backup storage. Record checksums and write a local recovery guide/script. Preserve the package identity, signing certificate and versionCode so `pm install -r` can restore the exact previous APK while retaining the launcher's private app data (including saved tile order). Do not uninstall or clear app data. Private files that Android does not allow shell to read remain in place across replacement; do not claim a full private-data export.

The rollback target is the interface immediately before weather, retaining reordering and remote shortcuts. Full Google TV/debloat restoration remains a separate existing procedure. Also allow hiding weather through its theme option without replacing the APK.

## Verification

Unit-test WMO mapping, malformed or missing values, stale/future timestamps and refresh timing. Build and lint the release, check the existing size gate, then verify the actual TV display, D-pad, app return and helper binding. Ask the owner to verify physical shortcuts. No binaries, private backups, IPs or screenshots with identifying details in Git.
