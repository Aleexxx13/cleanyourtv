# Optional TCL volume handling in this fork

This is for the tested TCL Smart TV Pro G05 running Android 11. With Google TV disabled, HomeKeyService restores the app and Settings keys. On this firmware, passing volume through the accessibility filter caused doubled taps and sometimes runaway repeats. Consuming volume events restored one step per tap but removed held repeats.

HomeKeyService can now consume that second pass and provide held-key repeats. This is off unless the theme contains these entries, alongside the existing app mappings:

```json
{
  "key_filter": true,
  "app_keys": ["24:tcl-volume", "25:tcl-volume"]
}
```

Merge the entries into the existing `app_keys` list; do not replace the other shortcuts. Enable only HomeKeyService, since InputKeyService does not implement this target. Keep `gamepad_passthrough` false: that feature can suspend filtering and hide the release event. For that reason held-volume repeats are disabled whenever the gamepad helper is configured. Do not use it on firmware where consuming volume also removes the first step.

The firmware supplies the initial step. After 500 ms, the existing helper adjusts the relevant audio stream by one step per tick. The interval progressively decreases from about 138 ms at the first repeat to 50 ms after four seconds held. There is no artificial limit on the hold duration or number of steps; the TV’s normal volume range still applies. Releasing cancels repetition immediately. A stalled main thread never catches up with a burst. Another key press, standby, cancellation, interruption, unbinding, destruction and config loading also stop it. This mode relies on Android delivering the release or cancellation event; it has no separate duration timeout.

There is no additional service, thread, dependency or permission. No callbacks run while idle. Google TV does not need to run. Audio routes with fixed volume may ignore Android volume adjustments; check the actual output with the physical remote.

On the tested TV, the user reported temporary volume latency while an application starts, then confirmed normal operation once it is running. This remains a known limitation to investigate; the current version was accepted with that observation.

Before installing, save the existing APK, theme and accessibility settings privately. To back out safely, disable HomeKeyService first, then restore that APK and theme. Restoring an earlier theme with filtering enabled without this fix can restore the original volume problem. Do not clear launcher data or uninstall it. Keep Home and the screensaver selection unchanged.
