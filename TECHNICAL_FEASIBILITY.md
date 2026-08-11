# Technical Feasibility: Status-Bar Network Speed Indicator on Stock Samsung One UI

This document was written **before** implementation, as required. It answers one question
honestly: *can a third-party app, without root, make a view that lives inside Samsung
SystemUI's actual status-bar icon row and reflows automatically when neighboring icons
appear/disappear?*

**Short answer: No.** That specific behavior is only achievable with root + a SystemUI
modification (Xposed/LSPosed module) or by being a signature/system-signed app. Nothing
distributed as a normal, sideloaded/Play-Store APK can do it. There is one — and only one —
non-root mechanism that gets meaningfully close, and this project uses it as Tier 1.

## 1. How the status bar is actually built

Samsung's SystemUI (a fork of AOSP SystemUI) draws the status bar as several fixed
`LinearLayout`/`ViewGroup` regions inside `com.android.systemui`, e.g. a
`system_icons` container (wifi, signal, battery, alarm, VPN, mute, etc., driven by
`StatusBarIconController` + `StatusBarIconView`) and a separate `notification_icon_area`
(managed by `NotificationIconAreaController`/`NotificationIconContainer`) that renders one
small icon per active, icon-visible notification, ordered newest-first, reflowing
automatically as notifications post/cancel.

Only **`com.android.systemui`**, running as a privileged system process signed with the
platform certificate, can add/remove children of `system_icons`. There is no public
Android API, Intent, ContentProvider, Binder service, or SDK surface that lets a
third-party app register a view there. This has been true on every stock Android release
from 5.0 through 16, and Samsung's One UI fork does not add one back — if anything, One UI
is *more* locked down (Knox attestation, no OEM unlock on carrier-locked units, S Pen/DeX
restrictions unrelated but indicative of the general posture).

The `notification_icon_area`, in contrast, **is** reachable by third-party apps, because
posting a notification with a small icon is a fully public, documented API
(`NotificationManager`/`NotificationCompat`). That's the one legitimate door.

## 2. What's possible without root (Tier 1 — used as default)

- **TrafficStats / ConnectivityManager / NetworkCapabilities**: fully public, no special
  permission beyond `ACCESS_NETWORK_STATE`. Used for speed sampling and network-type
  detection (Wi‑Fi/mobile/VPN/Ethernet).
- **Foreground Service with an ongoing notification whose small icon is a bitmap we draw
  ourselves** (e.g. the glyph "2.4M"): Android renders small icons as a white/alpha
  silhouette in the status bar since Lollipop (`Icon`/`IconCompat` from a bitmap is
  permitted; the OS strips color and keeps only the alpha channel). By rendering the speed
  text into a bitmap and setting it as the notification's small icon, the **text itself
  appears as a status-bar icon** in the notification-icon region. This is exactly the
  technique used by every non-root "internet speed meter" app on the Play Store — it is
  the actual, achievable ceiling for this project without root.
  - It **does** reflow automatically: when another app posts/clears a notification with
    an icon, the notification-icon row re-lays-out and our icon moves with it, same as any
    other notification icon. That satisfies the "moves when other icons appear/disappear"
    requirement — but only with respect to *other notification icons*, not the
    system-icon row (wifi/battery/signal/VPN/clock), which is a separate, unreachable
    container. So the speed indicator will sit among notification icons, generally to the
    left of the system icon cluster and to the right of any other notification icons — it
    cannot be interleaved *inside* the system icon cluster itself (e.g. literally between
    Wi‑Fi and battery).
  - The notification itself is unavoidable (Android requires *a* notification to run a
    foreground service, full stop, since Android 8/O). We minimize it: `IMPORTANCE_LOW`
    channel (no sound, no heads-up, no vibration), no expandable big content, terse text,
    marked ongoing/non-dismissible-by-swipe, and (from Android 14, `FGS` types) declared as
    `dataSync`/`specialUse` as appropriate. It cannot be made to post zero shade entries —
    that is an Android platform rule, not a Samsung one, and no legitimate API bypasses it.
- **BOOT_COMPLETED receiver** to relaunch the foreground service after reboot: public,
  normal permission.
- **Battery optimization exemption** (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`): public API,
  requires explicit user approval via a system dialog; we surface a settings-screen button
  that opens it, never auto-grant.
- **WorkManager/AlarmManager watchdog** that pings and restarts the foreground service if
  the process was killed and restart is otherwise permitted: public API.

## 3. What's possible with Shizuku

Shizuku grants the app the privileges of the `adb`/shell UID (via ADB pairing or root) at
runtime, without flashing anything. We evaluated it specifically for `android.permission
.STATUS_BAR` and for `NotificationManager`/`StatusBarIconController` hidden APIs.

- `android.permission.STATUS_BAR` (and `STATUS_BAR_SERVICE`) are `signature`-level
  permissions restricted to apps signed with the **platform certificate** (i.e., built
  into the ROM). The shell UID does **not** hold these, and `pm grant` cannot grant a
  signature permission to an arbitrary app regardless of Shizuku. So Shizuku **cannot**
  get a third-party app into the real system-icon container, and it cannot register a
  SystemUI plugin either (SystemUI plugins are a separate, deprecated AOSP mechanism that
  also requires a system/signature-level allow-listing, removed as an external extension
  point on Samsung firmware).
- Where Shizuku *would* help, marginally: silently granting itself
  `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`-equivalent app-ops without the system dialog, or
  calling a couple of hidden `NotificationManager` methods. None of that changes the
  status-bar placement ceiling described above, and it adds a real-world usability cost
  (pairing over ADB WiFi debugging, keeping the Shizuku service alive). Given the marginal
  gain, **this project ships without a Shizuku dependency**; Tier 1 already reaches the
  same practical result for the parts Shizuku could touch.

## 4. What requires root

- Actually inserting a view into `system_icons`/`NotificationIconContainer` as a sibling
  of Wi‑Fi/battery/signal requires modifying or hooking `com.android.systemui` itself —
  e.g. an LSPosed/Xposed module hooking `StatusBarIconController` to inject a custom
  `StatusBarIconView`, or a custom SystemUI APK repack flashed as a system app (Magisk
  module). Both require root.
- With root this is genuinely achievable and would satisfy the literal requirement
  (interleaved with system icons, moving exactly as they move), which is why it's kept as
  an optional **Tier 3**, not implemented by default, and clearly labeled advanced/
  unsupported in-app.

## 5. Samsung-specific restrictions observed/documented

- One UI's notification-icon area historically caps the number of simultaneously shown
  notification icons (commonly 4, configurable by the user under
  *Settings → Notifications → Status bar → Show icons as* → "Show icon" vs "Show
  notification count"); when more notifications are active than fit, extras collapse into
  a small count indicator instead of individual icons. Our icon can be pushed out of view
  by other apps' notifications, and the user may need to set our channel/app as
  high-priority-for-icon-display, or reduce other notification clutter, to keep it
  visible at all times. This is documented for the user in the README, not hidden.
- Samsung's aggressive background-process management ("Put unused apps to sleep",
  Device Care battery optimization, Sleeping/Deep sleeping apps list) can kill background
  services more eagerly than stock AOSP Doze. We request the standard battery-optimization
  exemption and document the extra Samsung-specific opt-outs (excluding the app from
  "Sleeping apps"/"Never sleeping apps" list) the user must set manually — there is no
  public API to do this programmatically.
- Knox: on Knox-tripped or carrier-locked (e.g. many US carrier) units, OEM unlock/root is
  unavailable, so Tier 3 simply isn't an option on those units; Tier 1 is unaffected by
  Knox since it uses only public APIs.

## 6. Feasibility ranking of the approaches investigated

| # | Approach | Gets into real system-icon row? | Root/Shizuku needed? | Verdict |
|---|---|---|---|---|
| 1 | Notification small-icon rendered as text (notification-icon area) | No (adjacent area, reflows independently) | No | **Used — best available without root** |
| 2 | Ongoing foreground-service notification, default icon | No | No | Inferior to #1 (no live text) |
| 3 | `TYPE_APPLICATION_OVERLAY` fixed-position window | No — literally the anti-pattern this project must avoid | No | Rejected per requirements |
| 4 | Accessibility service drawing over status bar | No, same problem as #3 plus worse privacy footprint | No | Rejected |
| 5 | SystemUI plugin API | Would, if allow-listed | Root (allow-listing is system-signature only) | Tier 3 only |
| 6 | LSPosed/Xposed SystemUI hook | Yes | Root | Tier 3 only |
| 7 | Shizuku for STATUS_BAR permission | No — permission is signature-level, shell UID doesn't hold it | Shizuku | Not viable, not used |

## 7. Recommendation

Ship **Tier 1 only** by default: foreground service + `TrafficStats` sampling +
notification whose small icon is a live-rendered bitmap of the current speed, minimal
`IMPORTANCE_LOW` silent channel, boot receiver, battery-exemption prompt, and a watchdog
restart path. Document Tier 3 (root/LSPosed SystemUI module) as a theoretical, unsupported,
not-implemented option for advanced users who explicitly want true system-icon
interleaving and are willing to root. Do not implement Tier 3 in this project, and never
represent Tier 1 as equivalent to real SystemUI integration anywhere in the UI or docs.

## 8. Direct answers to the required questions

1. **True SystemUI element or overlay?** Neither in the pejorative sense — it's a
   notification-driven status-bar icon (a real status-bar icon, not a fixed-coordinate
   overlay), which is the closest legitimate non-root mechanism.
2. **Same layout container as Samsung's existing system icons?** No — it lives in the
   adjacent notification-icon container, not `system_icons`.
3/4. **Moves automatically when another icon appears/disappears?** Yes, but only relative
   to *other notification icons*; it does not react to system icons (Wi‑Fi/battery/signal)
   appearing or disappearing, because those live in a container we cannot reach.
5. **Run for days?** Yes, engineered for it (foreground service, restart watchdog, boot
   receiver), subject to the user granting battery-optimization exemption and Samsung not
   force-stopping the app manually.
6. **Continue after reboot?** Yes, via `BOOT_COMPLETED`, if the user leaves auto-start
   enabled.
7. **Continue after screen lock?** Yes — foreground services are exempt from
   screen-off/Doze CPU restrictions in the way they matter here (network access, periodic
   sampling).
8. **Continue after removal from Recents?** Yes, for a foreground service with a
   notification (Android does not kill these on swipe-away by design); Samsung's separate
   "sleeping apps" feature is the real risk, documented for the user to disable.
9. **Creates a notification-shade entry?** Yes — unavoidable, this is an Android platform
   rule for foreground services, not a choice this app makes.
10. **Can that entry be minimized?** Yes, to the practical minimum: silent `LOW`
    importance channel, no sound/vibration/heads-up, compact non-expanding content,
    non-swipe-dismissible only because it's ongoing (required for a live service
    indicator).
11. **Requires root?** No, for the shipped Tier 1 implementation.
12. **Requires Shizuku?** No — evaluated and found not to help with the core requirement.
13. **Note10+-specific limitations?** Older One UI/Android build on this device may have a
    smaller notification-icon slot count and different DPI scaling; text bitmap rendering
    is DPI-aware to compensate. No functional difference otherwise.
14. **S25 Ultra-specific limitations?** Newer One UI may hide notification icons by
    default in favor of a notification dot depending on user preference under status bar
    settings; the user may need to enable "Show icon" mode for our channel to be visible
    as an icon rather than a dot.
15. **Any Samsung-specific restriction that blocks the exact requested behavior?** Yes —
    the fundamental blocker (no non-root access to the system-icon container) is an
    AOSP/Android-wide restriction, not unique to Samsung; Samsung layers additional
    aggressive background-app management on top, which is separately documented and
    mitigated via user-facing settings links, not code-level workarounds.
