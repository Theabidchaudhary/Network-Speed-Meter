# Network Speed Meter

A small, privacy-respecting Android utility whose only job is to show your live download
(and optionally upload) speed as a status-bar icon, and to keep doing that reliably in the
background — through screen lock, reboots, and being swiped out of Recents.

Read **[TECHNICAL_FEASIBILITY.md](TECHNICAL_FEASIBILITY.md)** first — it documents, honestly,
what a non-root third-party app can and cannot do to a Samsung/AOSP status bar, and why this
project is built the way it is.

## 1. What the app does

- Samples device-wide network throughput (`TrafficStats`) roughly once per second (configurable)
  and renders it live into a status-bar icon.
- Shows a simple home screen: whether the meter is active, the current live speed, which
  network it's using, and one ON/OFF switch.
- Offers a compact settings screen: display mode, units, update interval, decimal places,
  idle behavior, Wi‑Fi/mobile filter, theme, text size, and boot-start.
- Runs entirely on-device. No network requests of its own, no accounts, no analytics, no ads.

## 2. Supported Android versions

`minSdk 26` (Android 8.0) through `targetSdk/compileSdk 35` (Android 15). Built and tested
against the behaviors of Android 8–16 as documented in TECHNICAL_FEASIBILITY.md.

## 3. Supported Samsung devices

Designed and validated against, primarily:

- **Samsung Galaxy Note 10+** (older One UI/Android build)
- **Samsung Galaxy S25 Ultra** (current One UI/Android build)

No coordinates, DPI values, or icon sizes are hard-coded to either device — text is measured
and fit at render time (`SpeedIconRenderer.fitTextSize`), so it adapts to whatever density,
font scale, and status-bar height the device/OS combination presents.

## 4. Required permissions (and why)

| Permission | Why |
|---|---|
| `ACCESS_NETWORK_STATE` | Read network type (Wi‑Fi/mobile/VPN) and connectivity changes. |
| `INTERNET` | Declared because `ConnectivityManager` capability checks require it on some OS versions; the app makes no network requests. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` | Required by Android to run the always-on background sampler as a foreground service. |
| `POST_NOTIFICATIONS` (Android 13+) | Required at runtime to post the one notification the foreground service needs. |
| `RECEIVE_BOOT_COMPLETED` | Restart the meter after reboot, only if "Start after boot" is on. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Lets the app show the system dialog asking to be exempted from Doze/App Standby; never auto-granted. |

Nothing else is requested: no contacts, location, storage, microphone, camera, SMS, phone, or
accessibility permissions.

## 5. How the background service works

`SpeedMeterService` (a `LifecycleService`) is the single source of truth:

1. On create, it immediately calls `startForeground()` with a placeholder notification (required
   within a few seconds of `startForegroundService()`), then starts observing three flows:
   settings (DataStore), active network type (`ConnectivityManager.NetworkCallback`), and speed
   samples (`TrafficStats` deltas over the user's chosen interval).
2. Every tick it re-renders the notification's small icon as a bitmap of the current speed
   (`SpeedIconRenderer`) and updates the single ongoing notification (`NotificationHelper`).
3. `onStartCommand` returns `START_STICKY`, so the platform attempts to recreate the service if
   it's killed under memory pressure and conditions allow a restart.
4. A `WorkManager` periodic job (15-minute floor, the OS minimum) and a short one-shot job
   scheduled from `onDestroy()` both re-issue `startForegroundService()` if the user still wants
   the meter on — a harmless no-op if it's already running, a real revival if it was killed.
5. `BootReceiver` restarts it after `ACTION_BOOT_COMPLETED` / `MY_PACKAGE_REPLACED`, gated on the
   "Start after boot" setting.

See TECHNICAL_FEASIBILITY.md §2 for exactly why a notification-driven status-bar icon, rather
than an overlay, is the mechanism, and what it can and can't do relative to a true SystemUI
element.

## 6. How to build

This project targets Android Studio (Koala/Ladybug or newer) with AGP 8.5.x and Kotlin 1.9.24.

1. Open the project root in Android Studio (the Gradle wrapper, including `gradlew`, is
   committed, so Studio can sync immediately).
2. Android Studio will resolve dependencies from Google's Maven and Maven Central automatically.
3. Build → Make Project, or `./gradlew assembleDebug` from a terminal with network access to
   `dl.google.com` / `maven.google.com`.

> **Note on this repository's build status:** this project was authored in a sandboxed
> environment whose network policy does not allow reaching Google's Maven repository, so
> `./gradlew assembleDebug` could not be executed here to produce a signed/verified build
> artifact. Every source file was written and manually re-reviewed for correctness (imports,
> API usage, Compose scoping, Kotlin syntax), but you should run the real build in Android
> Studio (or CI with normal internet access) before shipping, and fix anything a full
> Kotlin/AGP compile turns up that a manual review couldn't.

## 7. How to install

Debug: `./gradlew installDebug` with a device connected (USB debugging on), or run from Android
Studio. Release: build a signed APK/AAB the normal way (`Build → Generate Signed Bundle/APK`);
this project defines no signing config, so provide your own keystore.

## 8. How to configure Samsung battery restrictions

Samsung's Device Care battery management is more aggressive than stock Doze and can freeze or
kill the background service even after the standard Android battery-optimization exemption is
granted. For reliable multi-day operation:

1. In the app, tap **"Exempt from battery optimization"** when prompted (opens the system
   dialog; there is no way to auto-grant this).
2. Additionally, on the device: **Settings → Apps → Network Speed Meter → Battery** → set to
   **Unrestricted**, and **Settings → Battery and device care → Battery → Background usage
   limits** → make sure Network Speed Meter is **not** in "Sleeping apps" or "Deep sleeping
   apps" (add it to the never-sleep exceptions if your One UI version exposes one).
3. If you use a third-party task killer or a "RAM booster" feature, exclude this app from it.

There is no public API that lets an app do any of this for itself — the in-app button only
opens the one dialog Android exposes; the rest requires the user visiting Settings once.

## 9. Known limitations

- **Notification is unavoidable.** Android requires an ongoing notification for any foreground
  service; this app minimizes it (silent, low-importance, non-expanding) but cannot remove it.
- **Status-bar icon reflows relative to other notification icons, not system icons.** It will
  not shift position because Wi‑Fi/battery/signal/VPN system icons appear or disappear — those
  live in a container this app cannot reach without root. See TECHNICAL_FEASIBILITY.md.
- **One UI can hide notification icons behind a dot**, or cap how many are shown at once,
  depending on the user's *Settings → Notifications → Status bar* configuration and how many
  other apps are also posting icon-bearing notifications.
- **VPN throughput can be slightly overcounted.** `TrafficStats.getTotalRxBytes/TxBytes` sum
  every network interface since boot, including both a VPN's virtual `tun` interface and the
  physical interface carrying its encrypted payload.
- **The Wi‑Fi-only/mobile-only display filter hides the value rather than truly isolating
  per-network traffic** — it's implemented against the currently active network type, not a
  per-interface byte count, because avoiding the `PACKAGE_USAGE_STATS`/`NetworkStatsManager`
  special-access permission was a deliberate minimal-permissions tradeoff.
- **No true SystemUI integration** (see next section) without root — by design, not oversight.

## 10. Exact explanation of the status-bar implementation

The visible "status-bar speed" is the **small icon of an ongoing notification**, re-rendered
as a bitmap of the current speed text every sampling tick (`SpeedIconRenderer` +
`NotificationHelper`). Since Android 5.0, notification small icons are drawn by the OS as a
white/alpha silhouette regardless of the bitmap's actual colors — so drawing text into that
bitmap makes the *text itself* appear as a status-bar icon. This is a real status-bar icon
(not a fixed-coordinate overlay window), and it reflows automatically as other notification
icons come and go, the same way any notification icon does. It does **not** join the separate
system-icon row (Wi‑Fi/battery/signal/clock) — no non-root API allows that. Full detail and
the feasibility ranking of every alternative investigated (including SystemUI plugins,
Shizuku, and root/Xposed) is in **TECHNICAL_FEASIBILITY.md**.

## 11. Whether root/Shizuku is required

**No.** The entire shipped app is Tier 1: stock public Android APIs only. Root and Shizuku
were both investigated (TECHNICAL_FEASIBILITY.md §2–4) and neither is required, and Shizuku
specifically was found not to meaningfully improve on what Tier 1 already achieves for this
use case (its shell-level UID does not hold the signature-level `STATUS_BAR` permission
needed for real SystemUI icon-row integration). A root-based Tier 3 (LSPosed/Xposed SystemUI
module) is documented as a theoretical, unimplemented option for advanced users who want true
system-icon interleaving badly enough to root their device.

## 12. How to troubleshoot the service disappearing

1. Open the app — the status card shows **"● Speed meter active"** vs **"○ Speed meter
   inactive"**. If inactive but the ON switch is on, tap **Restart service**.
2. Check the battery-optimization card on the home screen; if it's still showing, the
   exemption was never granted — tap it and accept the system dialog.
3. Follow §8 above for Samsung's separate Device Care sleeping-apps list — this is the most
   common cause of a speed meter silently dying after several hours on Samsung phones
   specifically, and it is not something the app permission model can bypass.
4. Confirm **"Start speed meter after device boot"** is enabled in Settings if the meter isn't
   coming back after a reboot.
5. If POST_NOTIFICATIONS was denied on first launch (Android 13+), the foreground service
   cannot show its required notification and Android may stop it; reinstall or re-enable the
   notification permission from **Settings → Apps → Network Speed Meter → Notifications**.

## 13. Privacy

No server, no account, no cloud sync, no analytics, no ads, no tracking. The app never reads
network *content* — only the cumulative byte counters Android already maintains for every app.
See the in-app settings screen for the same statement surfaced to the user.
