# App-Anuman (app_anuman)

**App-Anuman** is an on-device Android risk auditor built with Kotlin and Jetpack Compose (Target API 29+) designed for non-technical users, including elderly family members. It scans all installed apps completely offline with zero network permissions, calculates deterministic risk scores, and explains threats in plain language and analogies (in both English and Hindi).

---

## Features (mahek/features)

### 1. Panic Screen ("Am I Being Scammed?")
- Route: `panicScreen`
- Evaluates installed apps using `PanicCheckEngine`:
  - Remote control packages detection (TeamViewer, AnyDesk, RustDesk, Zoho, etc.)
  - Recent special access on new installs (< 48 hours)
  - Apps sideloaded in last 24 hours
  - Active screen control risk (chains: `OTP_STEALER`, `OVERLAY_TRAP`, `SILENT_SPY`)
  - UPI fraud pattern (SMS + Notification listener on finance apps)
- Animated 5-step checklist with plain explanations.
- Verdict card: `SAFE` (Green), `WARNING` (Amber), `DANGER` (Pulsing Red).
- Pre-written warning report shareable via WhatsApp/SMS (`ShareCompat`).

### 2. Dormant Danger Screen
- Route: `dormantList`
- Detects unused apps (30+ days) holding sensitive permissions (SMS, Camera, Location, Storage).
- `PACKAGE_USAGE_STATS` educational permission request flow.
- Visual exposure score bar (`daysUnused * dangerWeight / 100`).
- One-tap "Clean Up" button directing to system app details.

### 3. Share with Family Screen
- Route: `shareScreen`
- Generates plain-language security reports with zero technical jargon.
- Live bilingual toggle (English & Hindi) for previewing before sending.
- Native Android `ShareCompat` integration.
- Privacy guarantee notice: only the summary is shared, all raw scan data remains on-device.

### 4. Live App Install Monitor
- `AppInstallReceiver` listening for `PACKAGE_ADDED` and `PACKAGE_REPLACED`.
- Evaluates newly installed or updated packages immediately in background.
- High-priority notification alert for `HIGH` and `CRITICAL` risk apps or sideloaded apps with sensitive access.
- Tapping notification directly opens App Detail.

### 5. Full Localization (English & Hindi)
- `res/values/strings.xml` and `res/values-hi/strings.xml`
- 100% paired localization keys with zero hardcoded strings in Compose code.
- Plain analogies for all 9 permission categories in both languages.

### 6. Window Security & Privacy Hardening
- `window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)` in `MainActivity.onCreate()` prevents screenshots and screen recording of results.
- `android:networkSecurityConfig` with `<base-config cleartextTrafficPermitted="false"/>`.
- `android:debuggable="false"` and `android:allowBackup="false"`.
- App-Anuman self-audits itself and scores <= 10 (LOW).

---

## Building the Release APK

```bash
# Build signed release APK
./gradlew assembleRelease
```
The signed release APK will be located at:
`app/build/outputs/apk/release/app-release.apk`
