# App-Anuman (ऐप-अनुमान) — On-Device App Risk Auditor

**App-Anuman** is a privacy-first, 100% on-device Android application risk auditor built with Kotlin and Jetpack Compose.
It targets Android API 29+ (Android 10+) and requires zero root or ADB privileges from the end user.
All scanning and analysis are performed completely offline — zero network permissions are declared, and zero data leaves the phone.

---

## Architecture & Security Highlights

1. **Manifest Hardening & Zero Network Access**:
   - Only 2 permissions declared: `QUERY_ALL_PACKAGES` and `PACKAGE_USAGE_STATS`.
   - **Zero network access**: `android.permission.INTERNET` is strictly omitted.
   - Cleartext traffic is blocked via `res/xml/network_security_config.xml`.
   - `android:allowBackup="false"` and `android:debuggable="false"` configured for release builds.
   - Screen protection enabled in `MainActivity.onCreate()` via `window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)`.
2. **Encrypted Storage**:
   - `PermissionSnapshotManager` diffs declared permissions across app updates using `EncryptedSharedPreferences` to detect permission creep.
3. **Deterministic Scoring Engine**:
   - Explainable risk score (0–100) mapped to four bands (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
   - Every single score point produces an exact `ScoringFinding` waterfall entry.
   - Five distinct mobile attack chains detected: `OTP_STEALER`, `OVERLAY_TRAP`, `SILENT_SPY`, `PERSIST_ADMIN`, and `DORMANT_THREAT`.
4. **Self-Auditing Guarantee**:
   - "An app that audits security must pass its own audit."
   - App-Anuman appears in its own audit results and scores $\le 10$ (`LOW` band) with 0 dangerous permissions.

---

## 7 Core Screens & Presentation Modes

- **HomeScreen**: Device health grade (A–F), circular gauge score (0–100), quick panic CTA, and 3 plain-language security questions.
- **AppListScreen**: Search and filter by Critical, High, Dormant, Sideloaded, and Permission Creep.
- **AppDetailScreen**: 9-category interactive capability grid, plain-language threat narrative, permission creep banner, score waterfall, and direct actions (Open Settings, Uninstall).
- **PanicScreen ("Am I Being Scammed?")**: Real-time evaluation of remote access tools (AnyDesk, TeamViewer, etc.), recent accessibility/overlay grants (<48h), 24h sideloads, and active traps.
- **DormantDangerScreen**: Unused applications holding sensitive permissions (>30, >60, >90 days) with exposure scoring.
- **ShareFamilyScreen**: Plain-language family safety reports with bilingual English/Hindi preview and native Android share chooser.
- **AboutScreen**: Transparency pledge, offline guarantees, MIT license, and self-audit certificate.

### Three Persona Presentation Modes
- **Simple Mode**: Color chips, action badges, and simplified non-technical explanations.
- **Detailed Mode**: Full waterfall breakdown, MITRE ATT&CK IDs, individual permission counts, and technical evidence.
- **Grandparent Mode**: Extra-large typography ($\ge 20$sp body), high-contrast cards, and simple real-world metaphors (e.g., *"Like letting someone open all your bank letters"*).

---

## Building and Testing

Set `JAVA_HOME` to JDK 17+ and run:

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Build signed debug APK
./gradlew assembleDebug

# Build signed release APK
./gradlew assembleRelease
```

Generated APKs:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`
