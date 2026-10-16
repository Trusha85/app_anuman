# App-Anuman (ऐप-अनुमान)

App-Anuman is an on-device Android app risk auditor built with Kotlin and Jetpack Compose.
It targets Android API 29+ (Android 10+) and requires zero root or ADB privileges from the end user.
All scanning and analysis are performed 100% on-device and offline — zero data leaves the phone.

---

## Architecture & Security Highlights

1. **Manifest Hardening**:
   - Only 2 permissions declared: `QUERY_ALL_PACKAGES` and `PACKAGE_USAGE_STATS`.
   - **Zero network access**: `android.permission.INTERNET` is strictly omitted.
   - Cleartext traffic is completely blocked via `res/xml/network_security_config.xml`.
   - `android:allowBackup="false"` and `android:debuggable="false"` configured for release builds.
   - Screen protection enabled in `MainActivity.onCreate()` via `window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)`.
2. **Encrypted Storage**:
   - `PermissionSnapshotManager` diffs declared permissions across app updates using `EncryptedSharedPreferences` to detect permission creep.
3. **Deterministic Scoring Engine**:
   - Explainable risk score (0–100) mapped to four bands (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
   - Every single score point produces an exact `ScoringFinding` waterfall entry.
   - Five distinct mobile attack chains detected: `OTP_STEALER`, `OVERLAY_TRAP`, `SILENT_SPY`, `PERSIST_ADMIN`, and `DORMANT_THREAT`.

---

## Data Layer API for UI team

*This section provides full API documentation for Trusha and Mahek to build UI screens and features on top of the data layer.*

### 1. `MainViewModel` StateFlow Properties

The UI should observe these StateFlows from `com.appanuman.ui.MainViewModel`:

| StateFlow Property | Type | Description |
|---|---|---|
| `scanState` | `StateFlow<ScanState>` | Current scan status: `IDLE`, `SCANNING`, `DONE`, `ERROR` |
| `appList` | `StateFlow<List<AppRiskProfile>>` | Scanned inventory of apps with risk profiles |
| `deviceSummary` | `StateFlow<DeviceRiskSummary?>` | Overall device risk score, letter grade (A-F), and counts |
| `panicResult` | `StateFlow<PanicCheckResult?>` | Result of the "Am I Being Scammed?" panic check |
| `includeSystemApps` | `StateFlow<Boolean>` | Whether system apps are included in the scan (default `false`) |
| `userMode` | `StateFlow<UserMode>` | Current UI mode: `SIMPLE`, `DETAILED`, or `GRANDPARENT` |
| `language` | `StateFlow<Language>` | Selected display language: `EN` (English) or `HI` (Hindi) |

### 2. `MainViewModel` Functions

```kotlin
// Trigger on-device scan (runs on Dispatchers.IO)
fun startScan(includeSystem: Boolean = includeSystemApps.value)

// Toggle inclusion of system apps and trigger rescan
fun toggleSystemApps()

// Update user mode preference (persisted)
fun setUserMode(mode: UserMode)

// Update language preference (persisted)
fun setLanguage(lang: Language)

// Immediate panic check for active scams / remote access
fun runPanicCheck(): PanicCheckResult

// Filter app list by chip / category
fun getFilteredApps(filter: AppFilter): List<AppRiskProfile>

// Generate plain-language summary for WhatsApp/SMS family sharing
fun getShareText(lang: Language): String
```

---

### 3. Data Models (`com.appanuman.data.model`)

#### `AppRiskProfile`
Represents an individual scanned application on the device.
```kotlin
data class AppRiskProfile(
  val label: String,
  val packageName: String,
  val versionName: String,
  val firstInstallTime: Long,
  val lastUpdateTime: Long,
  val installerPackageName: String?,
  val targetSdkVersion: Int,
  val installerTrust: InstallerTrust,
  val declaredPermissions: List<PermissionInfo>,
  val grantedPermissions: List<PermissionInfo>,
  val categoryMap: Map<PermissionCategory, CategoryResult>,
  val specialAccess: SpecialAccessResult,
  val riskScore: Int,
  val riskBand: RiskBand,
  val scoringFindings: List<ScoringFinding>,
  val triggeredChains: List<AttackChain>,
  val threatNarratives: List<ThreatNarrative>,
  val contextMismatch: Boolean,
  val permissionCreepFlag: Boolean,
  val addedPermissions: List<String>,
  val daysUnused: Int?,
  val isDormant: Boolean,
  val isRemoteAccessApp: Boolean,
  val whyFlagged: List<String>,
  val icon: android.graphics.Bitmap?
)
```

#### `PermissionInfo`
```kotlin
data class PermissionInfo(
  val permissionName: String,
  val category: PermissionCategory,
  val state: PermissionState,
  val protectionLevel: Int
)
```

#### `CategoryResult`
```kotlin
data class CategoryResult(
  val category: PermissionCategory,
  val state: PermissionState,
  val permissions: List<PermissionInfo>
)
```

#### `SpecialAccessResult`
```kotlin
data class SpecialAccessResult(
  val accessibilityEnabled: PermissionState,
  val deviceAdminActive: PermissionState,
  val notificationListenerEnabled: PermissionState,
  val overlayState: PermissionState,
  val installPackagesState: PermissionState,
  val enabledAccessibilityServices: List<String>,
  val activeDeviceAdmins: List<String>
)
```

#### `ScoringFinding`
```kotlin
data class ScoringFinding(
  val ruleId: String,
  val label_en: String,
  val label_hi: String,
  val points: Int,
  val evidence: String
)
```

#### `AttackChain`
```kotlin
data class AttackChain(
  val chainId: AttackChainId,
  val matched: Boolean,
  val evidence: List<String>
)
```

#### `ThreatNarrative`
```kotlin
data class ThreatNarrative(
  val chainId: AttackChainId,
  val title_en: String,
  val title_hi: String,
  val body_en: String,
  val body_hi: String,
  val analogy_en: String,
  val analogy_hi: String,
  val mitre_ref: String,
  val severity: RiskBand
)
```

#### `DeviceRiskSummary`
```kotlin
data class DeviceRiskSummary(
  val deviceScore: Int,
  val grade: String,               // "A", "B", "C", "D", "F"
  val criticalCount: Int,
  val highCount: Int,
  val mediumCount: Int,
  val lowCount: Int,
  val dormantCount: Int,
  val sideloadedCount: Int,
  val scanDurationMs: Long,
  val scannedAppCount: Int,
  val lastScanTime: Long
)
```

#### `PanicCheckResult`
```kotlin
data class PanicCheckResult(
  val remoteAccessAppsFound: List<String>,
  val recentSpecialAccessApps: List<String>,
  val recentSideloads: List<String>,
  val activeOverlayRisk: Boolean,
  val upiRiskApps: List<String>,
  val overallVerdict: PanicVerdict
)
```

---

### 4. Enums

- **`ScanState`**: `IDLE`, `SCANNING`, `DONE`, `ERROR`
- **`UserMode`**: `SIMPLE`, `DETAILED`, `GRANDPARENT`
- **`Language`**: `EN`, `HI`
- **`RiskBand`**: `LOW` (0-24), `MEDIUM` (25-49), `HIGH` (50-74), `CRITICAL` (75-100)
- **`InstallerTrust`**: `VERIFIED`, `SYSTEM_INSTALL`, `UNKNOWN`, `SIDELOADED`, `PREINSTALLED`
- **`PermissionCategory`**:
  `SPECIAL_ACCESS`, `SMS`, `CONTACTS_CALLS`, `CAMERA_MIC`, `LOCATION`, `PHONE_IDENTITY`, `STORAGE`, `BACKGROUND`, `NETWORK`
- **`PermissionState`**: `GRANTED`, `DECLARED_ONLY`, `NOT_REQUESTED`, `UNABLE_TO_CHECK`
- **`AttackChainId`**:
  - `OTP_STEALER` (MITRE T1412)
  - `OVERLAY_TRAP` (MITRE T1417)
  - `SILENT_SPY` (MITRE T1418)
  - `PERSIST_ADMIN` (MITRE T1401)
  - `DORMANT_THREAT` (MITRE T1444)
- **`PanicVerdict`**: `SAFE`, `WARNING`, `DANGER`
- **`AppFilter`**: `ALL`, `CRITICAL`, `HIGH`, `DORMANT`, `SIDELOADED`, `MISMATCH`

---

## Building and Testing

Set Java Home to JDK 17+ and run:

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```
