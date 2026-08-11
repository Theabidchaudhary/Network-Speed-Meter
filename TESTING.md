# Testing Plan

This app could not be physically tested on a Galaxy Note 10+ or Galaxy S25 Ultra during
development (no device access, no Android SDK/emulator reachable from this sandboxed
environment — see README §6). Everything below is what a human needs to run manually on both
target devices before shipping.

## Basic

- [ ] Connect to Wi‑Fi only; download/upload a large file; icon and home-screen numbers track it.
- [ ] Switch to mobile data only; repeat.
- [ ] Airplane mode / no connection; meter shows `0 B/s` (or hides, per "show when zero" setting).
- [ ] Download-only traffic, upload-only traffic, and simultaneous up/down all show correctly.

## Status bar

- [ ] Post another app's notification with an icon; confirm ours reflows rather than being
      overlapped.
- [ ] Dismiss that notification; confirm ours reflows back.
- [ ] Enable VPN; confirm network label switches to "VPN" and throughput still shows (see
      README known limitations re: possible overcounting).
- [ ] Enable Bluetooth, hotspot, an alarm, location, and any Samsung privacy indicators
      simultaneously; confirm our icon never overlaps another icon and doesn't get silently
      dropped for space in a way the user can't recover from.
- [ ] Trigger multiple simultaneous notifications (messaging apps, calls, media playback);
      confirm behavior matches One UI's documented icon-count cap (extras collapse to a count,
      not a crash or corrupted icon).
- [ ] Change clock format (12h/24h) and check battery percentage display; neither should affect
      our icon's rendering.

## Lifecycle

- [ ] Open app, background it (home button); meter keeps running.
- [ ] Force-close from Recents (swipe away); meter keeps running (foreground service).
- [ ] Lock the screen; confirm updates continue (check notification/icon after unlocking).
- [ ] Reboot the device with "Start after boot" ON; meter comes back without opening the app.
- [ ] Reboot with "Start after boot" OFF; meter stays off until manually started.
- [ ] Leave running for several hours (ideally overnight) with screen mostly off; confirm it's
      still updating in the morning — this is the main real-world reliability test.
- [ ] Enable Battery Saver / Power saving mode; confirm behavior (may throttle sampling
      interval effects, note any observed change).
- [ ] Force Doze via `adb shell dumpsys deviceidle force-idle` (or wait for natural Doze);
      confirm the service survives and resumes normal sampling on exiting Doze.
- [ ] Toggle Wi‑Fi ↔ mobile data repeatedly; confirm no crash, no stuck values, correct network
      label each time.

## Samsung-specific

- [ ] **Galaxy Note 10+**: verify icon legibility/scale at its DPI and One UI build; verify
      Device Care "sleeping apps" behavior per README §8; verify status-bar icon count/cap
      behavior for its One UI version.
- [ ] **Galaxy S25 Ultra**: same checks on its (newer) One UI build; specifically check whether
      notification icons default to a dot instead of an icon glyph, and whether the user needs
      to change a setting to see our rendered text.

## Settings correctness

- [ ] Every display mode (download only / upload only / both) renders sensibly at every unit
      and decimal-place combination, including very large (GB/s) and very small (0 B/s) values.
- [ ] Update interval change takes effect without restarting the service.
- [ ] Wi‑Fi-only / mobile-only filters correctly zero the displayed value on the non-matching
      network (with the documented caveat that this hides rather than truly isolates traffic).
- [ ] Theme (system/light/dark) and text size changes apply immediately to both the app UI and
      the rendered status-bar icon.
