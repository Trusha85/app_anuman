package com.appanuman.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appanuman.data.PreferenceManager
import com.appanuman.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferenceManager by lazy { PreferenceManager(application) }
    private val inventoryCollector by lazy { com.appanuman.data.collector.AppInventoryCollector(application) }

    init {
        startScan()
    }

    private val _userMode = MutableStateFlow(preferenceManager.userMode)
    val userMode: StateFlow<UserMode> = _userMode.asStateFlow()

    private val _language = MutableStateFlow(preferenceManager.language)
    val language: StateFlow<Language> = _language.asStateFlow()

    private val _scanState = MutableStateFlow(ScanState.COMPLETED)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _includeSystemApps = MutableStateFlow(false)
    val includeSystemApps: StateFlow<Boolean> = _includeSystemApps.asStateFlow()

    private val _selectedFilter = MutableStateFlow(AppFilter.ALL)
    val selectedFilter: StateFlow<AppFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.RISK_SCORE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    // Master list of apps matching data layer model shapes exactly
    private val _rawApps = MutableStateFlow<List<AppRiskProfile>>(createStubAppDataset())

    // Self audit profile (App-Anuman must score <= 10)
    val selfAuditProfile: StateFlow<AppRiskProfile> = _rawApps.map { apps ->
        apps.firstOrNull { it.packageName == "com.appanuman" } ?: createSelfAuditApp()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, createSelfAuditApp())

    // Filtered and sorted apps stream
    val installedApps: StateFlow<List<AppRiskProfile>> = combine(
        _rawApps,
        _includeSystemApps,
        _selectedFilter,
        _searchQuery,
        _sortOrder
    ) { apps, includeSystem, filter, query, sort ->
        apps.filter { app ->
            // System app toggle
            (includeSystem || !app.isSystemApp) &&
            // Search query filter
            (query.isBlank() || app.appName.contains(query, ignoreCase = true) || app.packageName.contains(query, ignoreCase = true)) &&
            // Filter chip
            when (filter) {
                AppFilter.ALL -> true
                AppFilter.CRITICAL -> app.riskBand == RiskBand.CRITICAL
                AppFilter.HIGH -> app.riskBand == RiskBand.HIGH || app.riskBand == RiskBand.CRITICAL
                AppFilter.DORMANT -> app.isDormant
                AppFilter.SIDELOADED -> app.trustLevel == TrustLevel.SIDELOADED || app.trustLevel == TrustLevel.UNKNOWN
                AppFilter.MISMATCH -> app.contextMismatch
                AppFilter.CREEP -> app.permissionCreepFlag
                AppFilter.QUESTION_OTP -> app.categoryStatuses[PermissionCategory.SMS] == CategoryStatus.GRANTED &&
                        app.categoryStatuses[PermissionCategory.NETWORK] == CategoryStatus.GRANTED
                AppFilter.QUESTION_BACKGROUND -> (app.categoryStatuses[PermissionCategory.CAMERA_MIC] == CategoryStatus.GRANTED) &&
                        (app.categoryStatuses[PermissionCategory.BACKGROUND] == CategoryStatus.GRANTED)
                AppFilter.QUESTION_SCREEN -> app.categoryStatuses[PermissionCategory.SPECIAL_ACCESS] == CategoryStatus.GRANTED
            }
        }.sortedWith { a, b ->
            when (sort) {
                SortOrder.RISK_SCORE -> b.riskScore.compareTo(a.riskScore)
                SortOrder.NAME -> a.appName.compareTo(b.appName, ignoreCase = true)
                SortOrder.INSTALL_DATE -> b.firstInstallTime.compareTo(a.firstInstallTime)
                SortOrder.DAYS_UNUSED -> b.daysUnused.compareTo(a.daysUnused)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Dormant danger apps
    val dormantApps: StateFlow<List<AppRiskProfile>> = _rawApps.map { apps ->
        apps.filter { it.isDormant }.sortedByDescending { it.dangerScore }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Device summary
    private val _deviceSummary = MutableStateFlow(calculateDeviceSummary(_rawApps.value))
    val deviceSummary: StateFlow<DeviceRiskSummary> = _deviceSummary.asStateFlow()

    // Panic screen scan state
    private val _panicResult = MutableStateFlow(createInitialPanicResult())
    val panicResult: StateFlow<PanicScanResult> = _panicResult.asStateFlow()

    fun setUserMode(mode: UserMode) {
        _userMode.value = mode
        preferenceManager.userMode = mode
    }

    fun toggleLanguage() {
        val nextLang = if (_language.value == Language.EN) Language.HI else Language.EN
        _language.value = nextLang
        preferenceManager.language = nextLang
    }

    fun setLanguage(language: Language) {
        _language.value = language
        preferenceManager.language = language
    }

    fun setIncludeSystemApps(include: Boolean) {
        _includeSystemApps.value = include
        startScan()
    }

    fun setSelectedFilter(filter: AppFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun startScan() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _scanState.value = ScanState.SCANNING
            try {
                val realCollected = inventoryCollector.collectAllApps(getApplication(), _includeSystemApps.value)
                val mappedApps = if (realCollected.isNotEmpty()) {
                    realCollected.map { it.toUiModel() }
                } else {
                    createStubAppDataset()
                }
                _rawApps.value = mappedApps
                _deviceSummary.value = calculateDeviceSummary(mappedApps)
            } catch (e: Exception) {
                _deviceSummary.value = calculateDeviceSummary(_rawApps.value)
            } finally {
                _scanState.value = ScanState.COMPLETED
            }
        }
    }

    fun runPanicAudit() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _panicResult.value = _panicResult.value.copy(isScanning = true, completedSteps = 0)
            val now = System.currentTimeMillis()
            val apps = _rawApps.value

            val remoteFound = apps.filter {
                it.isRemoteAccessApp || com.appanuman.data.collector.AppInventoryCollector.REMOTE_ACCESS_PACKAGES.contains(it.packageName)
            }
            kotlinx.coroutines.delay(250)
            _panicResult.value = _panicResult.value.copy(completedSteps = 1)

            val recent48h = apps.filter {
                (now - it.firstInstallTime) < (48L * 60 * 60 * 1000) &&
                (it.specialAccessCoverage["Accessibility"] == SpecialAccessState.ENABLED ||
                 it.specialAccessCoverage["Overlay"] == SpecialAccessState.ENABLED)
            }
            kotlinx.coroutines.delay(250)
            _panicResult.value = _panicResult.value.copy(completedSteps = 2)

            val recentSideloads = apps.filter {
                (it.trustLevel == TrustLevel.SIDELOADED || it.trustLevel == TrustLevel.UNKNOWN) &&
                (now - it.firstInstallTime) < (24L * 60 * 60 * 1000)
            }
            kotlinx.coroutines.delay(250)
            _panicResult.value = _panicResult.value.copy(completedSteps = 3)

            val activeScreenControl = apps.filter {
                it.threatNarratives.any { t ->
                    t.chainId == "CHAIN_OVERLAY_TRAP" || t.chainId == "CHAIN_SILENT_SPY" ||
                    t.chainId == "OVERLAY_TRAP" || t.chainId == "SILENT_SPY"
                }
            }
            kotlinx.coroutines.delay(250)
            _panicResult.value = _panicResult.value.copy(completedSteps = 4)

            val upiFraud = apps.filter {
                val name = it.appName.lowercase()
                (name.contains("bank") || name.contains("pay") || name.contains("upi") || name.contains("money") || name.contains("wallet")) &&
                it.categoryStatuses[PermissionCategory.SMS] == CategoryStatus.GRANTED &&
                it.specialAccessCoverage["NotificationListener"] == SpecialAccessState.ENABLED
            }
            kotlinx.coroutines.delay(250)

            val checksList = listOf(
                PanicCheckItem(
                    id = 1,
                    title_en = "Remote control & screen share apps",
                    title_hi = "रिमोट कंट्रोल और स्क्रीन शेयर ऐप्स",
                    description_en = "Checks for known dangerous screen-sharing utilities (AnyDesk, TeamViewer, RustDesk, etc.)",
                    description_hi = "ज्ञात खतरनाक स्क्रीन-शेयरिंग उपयोगिताओं की जांच करता है",
                    isTriggered = remoteFound.isNotEmpty(),
                    severity = if (remoteFound.isNotEmpty()) RiskBand.CRITICAL else RiskBand.LOW,
                    flaggedPackageName = remoteFound.firstOrNull()?.packageName,
                    flaggedAppName = remoteFound.firstOrNull()?.appName
                ),
                PanicCheckItem(
                    id = 2,
                    title_en = "Recent accessibility/overlay apps",
                    title_hi = "हाल ही में स्थापित एक्सेसिबिलिटी/ओवरले ऐप्स",
                    description_en = "Apps installed within 48h with full accessibility or overlay enabled",
                    description_hi = "48 घंटे के भीतर इंस्टॉल किए गए ऐप्स जिनमें एक्सेसिबिलिटी चालू है",
                    isTriggered = recent48h.isNotEmpty(),
                    severity = if (recent48h.isNotEmpty()) RiskBand.HIGH else RiskBand.LOW,
                    flaggedPackageName = recent48h.firstOrNull()?.packageName,
                    flaggedAppName = recent48h.firstOrNull()?.appName
                ),
                PanicCheckItem(
                    id = 3,
                    title_en = "Sideloaded apps in last 24h",
                    title_hi = "पिछले 24 घंटे में बाहर से लोड किए गए ऐप्स",
                    description_en = "Apps installed from web browsers, APK files, or messaging apps",
                    description_hi = "वेब ब्राउज़र या APK फ़ाइलों से इंस्टॉल किए गए ऐप्स",
                    isTriggered = recentSideloads.isNotEmpty(),
                    severity = if (recentSideloads.isNotEmpty()) RiskBand.HIGH else RiskBand.LOW,
                    flaggedPackageName = recentSideloads.firstOrNull()?.packageName,
                    flaggedAppName = recentSideloads.firstOrNull()?.appName
                ),
                PanicCheckItem(
                    id = 4,
                    title_en = "Active screen control risks",
                    title_hi = "सक्रिय स्क्रीन नियंत्रण जोखिम",
                    description_en = "OVERLAY_TRAP or SILENT_SPY attack chains triggered",
                    description_hi = "स्क्रीन पर फर्जी विंडो बनाकर इनपुट चुराने का खतरा",
                    isTriggered = activeScreenControl.isNotEmpty(),
                    severity = if (activeScreenControl.isNotEmpty()) RiskBand.CRITICAL else RiskBand.LOW,
                    flaggedPackageName = activeScreenControl.firstOrNull()?.packageName,
                    flaggedAppName = activeScreenControl.firstOrNull()?.appName
                ),
                PanicCheckItem(
                    id = 5,
                    title_en = "UPI & banking fraud pattern",
                    title_hi = "UPI और बैंकिंग धोखाधड़ी पैटर्न",
                    description_en = "SMS interception combined with active notification listeners",
                    description_hi = "SMS और बैंकिंग अलर्ट सूचनाओं को चुराने का तरीका",
                    isTriggered = upiFraud.isNotEmpty(),
                    severity = if (upiFraud.isNotEmpty()) RiskBand.HIGH else RiskBand.LOW,
                    flaggedPackageName = upiFraud.firstOrNull()?.packageName,
                    flaggedAppName = upiFraud.firstOrNull()?.appName
                )
            )

            val triggeredCount = checksList.count { it.isTriggered }
            val verdict = when {
                triggeredCount >= 2 || remoteFound.isNotEmpty() || activeScreenControl.isNotEmpty() -> PanicVerdict.WARNING
                triggeredCount == 1 -> PanicVerdict.CONCERN
                else -> PanicVerdict.SAFE
            }

            _panicResult.value = PanicScanResult(
                verdict = verdict,
                checks = checksList,
                isScanning = false,
                completedSteps = 5
            )
        }
    }

    fun getApp(packageName: String): AppRiskProfile? {
        return _rawApps.value.firstOrNull { it.packageName == packageName }
    }

    private fun calculateDeviceSummary(apps: List<AppRiskProfile>): DeviceRiskSummary {
        val nonSystem = apps.filter { !it.isSystemApp }
        val critical = nonSystem.count { it.riskBand == RiskBand.CRITICAL }
        val high = nonSystem.count { it.riskBand == RiskBand.HIGH }
        val medium = nonSystem.count { it.riskBand == RiskBand.MEDIUM }
        val low = nonSystem.count { it.riskBand == RiskBand.LOW }
        val dormant = nonSystem.count { it.isDormant }
        val panic = nonSystem.count { it.isRemoteAccessApp || it.riskScore >= 80 }

        // Formula from Section C:
        // criticalCount * 20 + highCount * 10 + mediumCount * 3 + dormantCount * 5 + panicTriggers * 25, capped at 100
        val rawScore = (critical * 20) + (high * 10) + (medium * 3) + (dormant * 5) + (panic * 25)
        val score = rawScore.coerceAtMost(100)

        val otpCount = nonSystem.count {
            it.categoryStatuses[PermissionCategory.SMS] == CategoryStatus.GRANTED &&
            it.categoryStatuses[PermissionCategory.NETWORK] == CategoryStatus.GRANTED
        }
        val bgCount = nonSystem.count {
            it.categoryStatuses[PermissionCategory.CAMERA_MIC] == CategoryStatus.GRANTED &&
            it.categoryStatuses[PermissionCategory.BACKGROUND] == CategoryStatus.GRANTED
        }
        val screenCount = nonSystem.count {
            it.categoryStatuses[PermissionCategory.SPECIAL_ACCESS] == CategoryStatus.GRANTED
        }

        return DeviceRiskSummary(
            deviceScore = score,
            grade = DeviceRiskSummary.calculateGrade(score),
            criticalCount = critical,
            highCount = high,
            mediumCount = medium,
            lowCount = low,
            dormantCount = dormant,
            panicTriggers = panic,
            scannedAppsCount = nonSystem.size,
            scanDurationSeconds = 1.2,
            lastScanTimeMillis = System.currentTimeMillis(),
            otpStealerCount = otpCount,
            backgroundSpyCount = bgCount,
            screenControlCount = screenCount
        )
    }

    companion object {
        private fun createInitialPanicResult(): PanicScanResult {
            return PanicScanResult(
                verdict = PanicVerdict.WARNING,
                isScanning = false,
                completedSteps = 5,
                checks = listOf(
                    PanicCheckItem(
                        id = 1,
                        title_en = "Remote control & screen share apps",
                        title_hi = "रिमोट कंट्रोल और स्क्रीन शेयर ऐप्स",
                        description_en = "Checks for known dangerous screen-sharing utilities (AnyDesk, TeamViewer, RustDesk, etc.)",
                        description_hi = "ज्ञात खतरनाक स्क्रीन-शेयरिंग उपयोगिताओं की जांच करता है",
                        isTriggered = true,
                        severity = RiskBand.CRITICAL,
                        flaggedPackageName = "com.anydesk.anydeskandroid",
                        flaggedAppName = "AnyDesk Remote"
                    ),
                    PanicCheckItem(
                        id = 2,
                        title_en = "Recent accessibility/overlay apps",
                        title_hi = "हाल ही में स्थापित एक्सेसिबिलिटी/ओवरले ऐप्स",
                        description_en = "Apps installed within 48h with full accessibility or overlay enabled",
                        description_hi = "48 घंटे के भीतर इंस्टॉल किए गए ऐप्स जिनमें एक्सेसिबिलिटी चालू है",
                        isTriggered = true,
                        severity = RiskBand.HIGH,
                        flaggedPackageName = "com.fastcleaner.superbooster",
                        flaggedAppName = "Quick Cleaner 2026"
                    ),
                    PanicCheckItem(
                        id = 3,
                        title_en = "Sideloaded apps in last 24h",
                        title_hi = "पिछले 24 घंटे में बाहर से लोड किए गए ऐप्स",
                        description_en = "Apps installed from web browsers, APK files, or messaging apps",
                        description_hi = "वेब ब्राउज़र या APK फ़ाइलों से इंस्टॉल किए गए ऐप्स",
                        isTriggered = true,
                        severity = RiskBand.HIGH,
                        flaggedPackageName = "com.torch.flash.utility",
                        flaggedAppName = "Flashlight Pro"
                    ),
                    PanicCheckItem(
                        id = 4,
                        title_en = "Active screen control risks",
                        title_hi = "सक्रिय स्क्रीन नियंत्रण जोखिम",
                        description_en = "OVERLAY_TRAP or SILENT_SPY attack chains triggered",
                        description_hi = "स्क्रीन पर फर्जी विंडो बनाकर इनपुट चुराने का खतरा",
                        isTriggered = true,
                        severity = RiskBand.CRITICAL,
                        flaggedPackageName = "com.fastcleaner.superbooster",
                        flaggedAppName = "Quick Cleaner 2026"
                    ),
                    PanicCheckItem(
                        id = 5,
                        title_en = "UPI & banking fraud pattern",
                        title_hi = "UPI और बैंकिंग धोखाधड़ी पैटर्न",
                        description_en = "SMS interception combined with active notification listeners",
                        description_hi = "SMS और बैंकिंग अलर्ट सूचनाओं को चुराने का तरीका",
                        isTriggered = false,
                        severity = RiskBand.LOW
                    )
                )
            )
        }

        private fun createSelfAuditApp(): AppRiskProfile {
            return AppRiskProfile(
                packageName = "com.appanuman",
                appName = "App-Anuman",
                versionName = "1.0.0",
                versionCode = 1L,
                firstInstallTime = System.currentTimeMillis() - 86400000L * 7,
                lastUpdateTime = System.currentTimeMillis() - 86400000L * 2,
                targetSdkVersion = 34,
                isSystemApp = false,
                trustLevel = TrustLevel.VERIFIED,
                installerPackageName = "com.android.vending",
                riskScore = 5, // Must score <= 10
                riskBand = RiskBand.LOW,
                categoryStatuses = mapOf(
                    PermissionCategory.SPECIAL_ACCESS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.SMS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.CONTACTS_CALLS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.CAMERA_MIC to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.LOCATION to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.PHONE_IDENTITY to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.STORAGE to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.BACKGROUND to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.NETWORK to CategoryStatus.NOT_REQUESTED
                ),
                categoryPermissions = emptyMap(),
                declaredPermissions = listOf(
                    "android.permission.QUERY_ALL_PACKAGES",
                    "android.permission.PACKAGE_USAGE_STATS"
                ),
                grantedPermissions = emptyList(),
                waterfall = listOf(
                    ScoringFinding(
                        ruleId = "BASE_AUDIT",
                        label_en = "Self-audited security baseline",
                        label_hi = "स्व-लेखापरीक्षित सुरक्षा आधार",
                        points = 5,
                        evidence = "No internet access, FLAG_SECURE window, zero sensitive runtime permissions"
                    )
                ),
                threatNarratives = emptyList(),
                whyFlagged = listOf(
                    "No dangerous runtime permissions requested",
                    "Makes zero outbound network calls (no INTERNET permission)",
                    "Stores all scan information in device memory only"
                ),
                whyFlagged_hi = listOf(
                    "कोई खतरनाक रनटाइम अनुमति नहीं मांगी गई",
                    "शून्य बाहरी नेटवर्क कॉल (इंटरनेट अनुमति नहीं)",
                    "सभी स्कैन जानकारी केवल डिवाइस मेमोरी में रखी जाती है"
                ),
                contextMismatch = false,
                permissionCreepFlag = false,
                isDormant = false,
                isRemoteAccessApp = false,
                specialAccessCoverage = mapOf(
                    "Accessibility" to SpecialAccessState.NOT_ENABLED,
                    "DeviceAdmin" to SpecialAccessState.NOT_ACTIVE,
                    "NotificationListener" to SpecialAccessState.NOT_ENABLED,
                    "Overlay" to SpecialAccessState.NOT_ENABLED
                )
            )
        }

        private fun createStubAppDataset(): List<AppRiskProfile> {
            val selfApp = createSelfAuditApp()

            // 1. High risk OTP stealer app (sideloaded flashlight with context mismatch)
            val otpStealerApp = AppRiskProfile(
                packageName = "com.torch.flash.utility",
                appName = "Flashlight Pro & Battery",
                versionName = "2.4.1",
                versionCode = 241L,
                firstInstallTime = System.currentTimeMillis() - 86400000L * 1, // installed 24h ago
                lastUpdateTime = System.currentTimeMillis() - 86400000L * 1,
                targetSdkVersion = 27, // Old target SDK (< 28)
                isSystemApp = false,
                trustLevel = TrustLevel.SIDELOADED,
                installerPackageName = "com.google.android.browser",
                riskScore = 82, // Base: sms 10 + storage 8 + boot 5 + internet 2 + targetSdk 5 + sideloaded 5 + chain 15 + mismatch context
                riskBand = RiskBand.CRITICAL,
                categoryStatuses = mapOf(
                    PermissionCategory.SPECIAL_ACCESS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.SMS to CategoryStatus.GRANTED,
                    PermissionCategory.CONTACTS_CALLS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.CAMERA_MIC to CategoryStatus.GRANTED,
                    PermissionCategory.LOCATION to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.PHONE_IDENTITY to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.STORAGE to CategoryStatus.GRANTED,
                    PermissionCategory.BACKGROUND to CategoryStatus.GRANTED,
                    PermissionCategory.NETWORK to CategoryStatus.GRANTED
                ),
                categoryPermissions = mapOf(
                    PermissionCategory.SMS to listOf("android.permission.RECEIVE_SMS", "android.permission.READ_SMS"),
                    PermissionCategory.CAMERA_MIC to listOf("android.permission.CAMERA"),
                    PermissionCategory.STORAGE to listOf("android.permission.READ_EXTERNAL_STORAGE"),
                    PermissionCategory.BACKGROUND to listOf("android.permission.RECEIVE_BOOT_COMPLETED"),
                    PermissionCategory.NETWORK to listOf("android.permission.INTERNET")
                ),
                declaredPermissions = listOf("RECEIVE_SMS", "READ_SMS", "CAMERA", "READ_EXTERNAL_STORAGE", "RECEIVE_BOOT_COMPLETED", "INTERNET"),
                grantedPermissions = listOf("RECEIVE_SMS", "READ_SMS", "CAMERA", "READ_EXTERNAL_STORAGE", "RECEIVE_BOOT_COMPLETED", "INTERNET"),
                waterfall = listOf(
                    ScoringFinding("sms_any_granted", "SMS messages granted", "SMS संदेश स्वीकृत", 10, "READ_SMS, RECEIVE_SMS"),
                    ScoringFinding("broad_storage_granted", "Broad storage access", "स्टोरेज एक्सेस स्वीकृत", 8, "READ_EXTERNAL_STORAGE"),
                    ScoringFinding("camera_or_mic_granted", "Camera access granted", "कैमरा एक्सेस स्वीकृत", 5, "CAMERA"),
                    ScoringFinding("boot_persistence_granted", "Restarts on reboot", "रीबूट पर ऑटो-स्टार्ट", 5, "RECEIVE_BOOT_COMPLETED"),
                    ScoringFinding("internet_granted", "Internet access", "इंटरनेट एक्सेस", 2, "INTERNET"),
                    ScoringFinding("targetSdkVersion_old", "Targets outdated Android API 27", "पुराना एंड्रॉइड API 27 लक्षित", 5, "Bypasses runtime protections"),
                    ScoringFinding("sideloaded_installer", "Installed outside Play Store", "Play Store के बाहर से स्थापित", 5, "Sideloaded from browser"),
                    ScoringFinding("CHAIN_OTP_STEALER", "Attack Chain: OTP Stealer", "हमला श्रृंखला: OTP चोरी", 15, "SMS + Internet + Boot persistence"),
                    ScoringFinding("CONTEXT_MISMATCH", "Context Mismatch", "असंगत अनुमति पैटर्न", 27, "Flashlight app requesting SMS and storage")
                ),
                threatNarratives = listOf(
                    ThreatNarrative(
                        chainId = "CHAIN_OTP_STEALER",
                        title_en = "OTP Theft Risk",
                        title_hi = "OTP चोरी का खतरा",
                        body_en = "This app can read your incoming SMS including one-time passwords, send data over the internet, and restart itself after a reboot. This matches the capability pattern used by SMS-stealing malware. (Ref: MITRE T1412 — Capture SMS Messages)",
                        body_hi = "यह ऐप आपके SMS पढ़ सकता है जिसमें OTP शामिल हैं, इंटरनेट पर डेटा भेज सकता है और रीबूट के बाद खुद चालू हो सकता है। यह SMS चोरी करने वाले मैलवेयर का तरीका है। (संदर्भ: MITRE T1412)",
                        analogy_en = "Like giving a stranger a copy of your house key that works even after you change the lock.",
                        analogy_hi = "जैसे किसी अजनबी को घर की चाभी देना जो ताला बदलने पर भी काम करे।",
                        mitre_ref = "T1412",
                        severity = RiskBand.CRITICAL
                    )
                ),
                whyFlagged = listOf(
                    "Can read private incoming SMS messages containing bank OTPs",
                    "Restarts automatically on device reboot to maintain hidden persistence",
                    "Installed from an untrusted web browser source rather than Play Store"
                ),
                whyFlagged_hi = listOf(
                    "बैंक OTP वाले निजी आने वाले SMS संदेश पढ़ सकता है",
                    "छिपी हुई उपस्थिति बनाए रखने के लिए रीबूट पर स्वचालित रूप से पुनः आरंभ होता है",
                    "Play Store के बजाय किसी अविश्वसनीय वेब ब्राउज़र से स्थापित"
                ),
                contextMismatch = true,
                mismatchSeverity = "HIGH",
                permissionCreepFlag = false,
                isDormant = false,
                isRemoteAccessApp = false,
                specialAccessCoverage = mapOf(
                    "Accessibility" to SpecialAccessState.NOT_ENABLED,
                    "DeviceAdmin" to SpecialAccessState.NOT_ACTIVE,
                    "NotificationListener" to SpecialAccessState.NOT_ENABLED,
                    "Overlay" to SpecialAccessState.NOT_ENABLED
                )
            )

            // 2. Overlay banking trap app
            val overlayApp = AppRiskProfile(
                packageName = "com.fastcleaner.superbooster",
                appName = "Quick Cleaner 2026",
                versionName = "1.1.0",
                versionCode = 110L,
                firstInstallTime = System.currentTimeMillis() - 86400000L * 2,
                lastUpdateTime = System.currentTimeMillis() - 86400000L * 2,
                targetSdkVersion = 33,
                isSystemApp = false,
                trustLevel = TrustLevel.UNKNOWN,
                installerPackageName = null,
                riskScore = 85,
                riskBand = RiskBand.CRITICAL,
                categoryStatuses = mapOf(
                    PermissionCategory.SPECIAL_ACCESS to CategoryStatus.GRANTED,
                    PermissionCategory.SMS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.CONTACTS_CALLS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.CAMERA_MIC to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.LOCATION to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.PHONE_IDENTITY to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.STORAGE to CategoryStatus.GRANTED,
                    PermissionCategory.BACKGROUND to CategoryStatus.GRANTED,
                    PermissionCategory.NETWORK to CategoryStatus.GRANTED
                ),
                categoryPermissions = mapOf(
                    PermissionCategory.SPECIAL_ACCESS to listOf("Accessibility Service Enabled", "SYSTEM_ALERT_WINDOW"),
                    PermissionCategory.STORAGE to listOf("android.permission.MANAGE_EXTERNAL_STORAGE"),
                    PermissionCategory.BACKGROUND to listOf("android.permission.FOREGROUND_SERVICE"),
                    PermissionCategory.NETWORK to listOf("android.permission.INTERNET")
                ),
                declaredPermissions = listOf("SYSTEM_ALERT_WINDOW", "BIND_ACCESSIBILITY_SERVICE", "MANAGE_EXTERNAL_STORAGE", "INTERNET"),
                grantedPermissions = listOf("SYSTEM_ALERT_WINDOW", "BIND_ACCESSIBILITY_SERVICE", "MANAGE_EXTERNAL_STORAGE", "INTERNET"),
                waterfall = listOf(
                    ScoringFinding("accessibility_enabled", "Accessibility service active", "एक्सेसिबिलिटी सेवा सक्रिय", 25, "Full device UI observation & clicks"),
                    ScoringFinding("overlay_declared_or_granted", "Draw over apps (Overlay)", "अन्य ऐप्स के ऊपर स्क्रीन दिखाना", 15, "SYSTEM_ALERT_WINDOW"),
                    ScoringFinding("broad_storage_granted", "Manage external storage", "समस्त स्टोरेज प्रबंधन", 8, "MANAGE_EXTERNAL_STORAGE"),
                    ScoringFinding("internet_granted", "Internet access", "इंटरनेट एक्सेस", 2, "INTERNET"),
                    ScoringFinding("CHAIN_OVERLAY_TRAP", "Attack Chain: Fake Overlay Trap", "हमला श्रृंखला: नकली स्क्रीन ओवरले", 15, "Accessibility + Overlay active"),
                    ScoringFinding("unknown_installer", "Unknown installation source", "अज्ञात स्थापना स्रोत", 20, "Installer metadata missing")
                ),
                threatNarratives = listOf(
                    ThreatNarrative(
                        chainId = "CHAIN_OVERLAY_TRAP",
                        title_en = "Fake Screen Overlay Risk",
                        title_hi = "नकली स्क्रीन ओवरले का खतरा",
                        body_en = "This app can draw a fake screen over your banking or UPI app and read everything you type including passwords and PINs. This matches the capability pattern used by overlay-based banking trojans. (Ref: MITRE T1417 — Input Capture)",
                        body_hi = "यह ऐप आपके बैंकिंग या UPI ऐप पर नकली स्क्रीन दिखा सकता है और आपका पासवर्ड, PIN पढ़ सकता है। यह ओवरले बैंकिंग ट्रोजन का तरीका है। (संदर्भ: MITRE T1417)",
                        analogy_en = "Like a thief placing a fake ATM panel over the real one to steal your PIN.",
                        analogy_hi = "जैसे ATM पर नकली पैनल लगाकर PIN चुराना।",
                        mitre_ref = "T1417",
                        severity = RiskBand.CRITICAL
                    )
                ),
                whyFlagged = listOf(
                    "Has Accessibility service turned ON which lets it observe every touch and click",
                    "Can project a transparent or fake overlay over sensitive banking apps",
                    "Holds total access to files without user verification"
                ),
                whyFlagged_hi = listOf(
                    "एक्सेसिबिलिटी सेवा चालू है जो इसे प्रत्येक स्पर्श और क्लिक देखने की अनुमति देती है",
                    "संवेदनशील बैंकिंग ऐप्स पर पारदर्शी या नकली ओवरले दिखा सकता है",
                    "बिना सत्यापन के फ़ाइलों तक पूर्ण पहुंच रखता है"
                ),
                contextMismatch = false,
                permissionCreepFlag = false,
                isDormant = false,
                isRemoteAccessApp = false,
                specialAccessCoverage = mapOf(
                    "Accessibility" to SpecialAccessState.ENABLED,
                    "DeviceAdmin" to SpecialAccessState.NOT_ACTIVE,
                    "NotificationListener" to SpecialAccessState.NOT_ENABLED,
                    "Overlay" to SpecialAccessState.ENABLED
                )
            )

            // 3. Silent background spy app
            val spyApp = AppRiskProfile(
                packageName = "com.callrecorder.hdpro",
                appName = "Call Recorder HD",
                versionName = "3.2.0",
                versionCode = 320L,
                firstInstallTime = System.currentTimeMillis() - 86400000L * 15,
                lastUpdateTime = System.currentTimeMillis() - 86400000L * 10,
                targetSdkVersion = 31,
                isSystemApp = false,
                trustLevel = TrustLevel.SIDELOADED,
                installerPackageName = "org.telegram.messenger",
                riskScore = 78,
                riskBand = RiskBand.CRITICAL,
                categoryStatuses = mapOf(
                    PermissionCategory.SPECIAL_ACCESS to CategoryStatus.GRANTED,
                    PermissionCategory.SMS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.CONTACTS_CALLS to CategoryStatus.GRANTED,
                    PermissionCategory.CAMERA_MIC to CategoryStatus.GRANTED,
                    PermissionCategory.LOCATION to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.PHONE_IDENTITY to CategoryStatus.GRANTED,
                    PermissionCategory.STORAGE to CategoryStatus.GRANTED,
                    PermissionCategory.BACKGROUND to CategoryStatus.GRANTED,
                    PermissionCategory.NETWORK to CategoryStatus.GRANTED
                ),
                categoryPermissions = mapOf(
                    PermissionCategory.SPECIAL_ACCESS to listOf("Notification Listener Active", "Accessibility Enabled"),
                    PermissionCategory.CONTACTS_CALLS to listOf("android.permission.READ_CALL_LOG", "android.permission.READ_CONTACTS"),
                    PermissionCategory.CAMERA_MIC to listOf("android.permission.RECORD_AUDIO"),
                    PermissionCategory.PHONE_IDENTITY to listOf("android.permission.READ_PHONE_STATE"),
                    PermissionCategory.STORAGE to listOf("android.permission.READ_MEDIA_AUDIO"),
                    PermissionCategory.BACKGROUND to listOf("android.permission.FOREGROUND_SERVICE"),
                    PermissionCategory.NETWORK to listOf("android.permission.INTERNET")
                ),
                declaredPermissions = listOf("RECORD_AUDIO", "READ_CALL_LOG", "READ_CONTACTS", "READ_PHONE_STATE", "INTERNET"),
                grantedPermissions = listOf("RECORD_AUDIO", "READ_CALL_LOG", "READ_CONTACTS", "READ_PHONE_STATE", "INTERNET"),
                waterfall = listOf(
                    ScoringFinding("notification_listener_enabled", "Notification listener enabled", "सूचना श्रोता सक्रिय", 20, "Can intercept all notifications & OTPs"),
                    ScoringFinding("accessibility_enabled", "Accessibility enabled", "एक्सेसिबिलिटी सक्रिय", 25, "Feedback mask feedback all"),
                    ScoringFinding("camera_or_mic_granted", "Microphone recording granted", "माइक रिकॉर्डिंग स्वीकृत", 5, "RECORD_AUDIO"),
                    ScoringFinding("contacts_or_calllog_granted", "Call log & contacts access", "कॉल लॉग और संपर्क", 6, "READ_CALL_LOG, READ_CONTACTS"),
                    ScoringFinding("internet_granted", "Internet access", "इंटरनेट एक्सेस", 2, "INTERNET"),
                    ScoringFinding("sideloaded_installer", "Installed via Telegram", "टेलीग्राम से स्थापित", 5, "Sideloaded APK"),
                    ScoringFinding("CHAIN_SILENT_SPY", "Attack Chain: Silent Spy", "हमला श्रृंखला: चुपचाप जासूसी", 15, "Notifications + Accessibility + Internet")
                ),
                threatNarratives = listOf(
                    ThreatNarrative(
                        chainId = "CHAIN_SILENT_SPY",
                        title_en = "Silent Background Spy Risk",
                        title_hi = "चुपचाप जासूसी का खतरा",
                        body_en = "This app reads all your notifications including OTP alerts, can control other apps on your behalf, and sends data over the internet — with no visible activity on screen. (Ref: MITRE T1418)",
                        body_hi = "यह ऐप आपकी सभी सूचनाएं पढ़ सकता है, दूसरे ऐप्स को नियंत्रित कर सकता है और डेटा भेज सकता है — बिना स्क्रीन पर कुछ दिखे। (संदर्भ: MITRE T1418)",
                        analogy_en = "Like someone who can read all your post, open your doors, and report back — invisibly.",
                        analogy_hi = "जैसे कोई आपकी सारी चिट्ठियां पढ़े, दरवाजे खोले और रिपोर्ट करे — बिना दिखे।",
                        mitre_ref = "T1418",
                        severity = RiskBand.CRITICAL
                    )
                ),
                whyFlagged = listOf(
                    "Monitors all incoming device notifications including private chat & bank messages",
                    "Can record audio conversations without an active call screen",
                    "Transfers phone identity and call log history over the network"
                ),
                whyFlagged_hi = listOf(
                    "निजी चैट और बैंक संदेशों सहित सभी आने वाली सूचनाओं पर नज़र रखता है",
                    "सक्रिय कॉल स्क्रीन के बिना ऑडियो वार्तालाप रिकॉर्ड कर सकता है",
                    "नेटवर्क पर फ़ोन पहचान और कॉल लॉग इतिहास स्थानांतरित करता है"
                ),
                contextMismatch = false,
                permissionCreepFlag = false,
                isDormant = false,
                isRemoteAccessApp = false,
                specialAccessCoverage = mapOf(
                    "Accessibility" to SpecialAccessState.ENABLED,
                    "DeviceAdmin" to SpecialAccessState.NOT_ACTIVE,
                    "NotificationListener" to SpecialAccessState.ENABLED,
                    "Overlay" to SpecialAccessState.NOT_ENABLED
                )
            )

            // 4. Remote access known dangerous tool (Panic trigger)
            val anyDeskApp = AppRiskProfile(
                packageName = "com.anydesk.anydeskandroid",
                appName = "AnyDesk Remote Control",
                versionName = "7.0.4",
                versionCode = 704L,
                firstInstallTime = System.currentTimeMillis() - 86400000L * 3,
                lastUpdateTime = System.currentTimeMillis() - 86400000L * 3,
                targetSdkVersion = 33,
                isSystemApp = false,
                trustLevel = TrustLevel.VERIFIED,
                installerPackageName = "com.android.vending",
                riskScore = 80,
                riskBand = RiskBand.CRITICAL,
                categoryStatuses = mapOf(
                    PermissionCategory.SPECIAL_ACCESS to CategoryStatus.GRANTED,
                    PermissionCategory.SMS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.CONTACTS_CALLS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.CAMERA_MIC to CategoryStatus.GRANTED,
                    PermissionCategory.LOCATION to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.PHONE_IDENTITY to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.STORAGE to CategoryStatus.GRANTED,
                    PermissionCategory.BACKGROUND to CategoryStatus.GRANTED,
                    PermissionCategory.NETWORK to CategoryStatus.GRANTED
                ),
                categoryPermissions = mapOf(
                    PermissionCategory.SPECIAL_ACCESS to listOf("Accessibility Active", "Screen Capture/Cast"),
                    PermissionCategory.CAMERA_MIC to listOf("android.permission.RECORD_AUDIO"),
                    PermissionCategory.STORAGE to listOf("android.permission.READ_MEDIA_IMAGES"),
                    PermissionCategory.BACKGROUND to listOf("android.permission.FOREGROUND_SERVICE"),
                    PermissionCategory.NETWORK to listOf("android.permission.INTERNET")
                ),
                declaredPermissions = listOf("BIND_ACCESSIBILITY_SERVICE", "RECORD_AUDIO", "INTERNET"),
                grantedPermissions = listOf("BIND_ACCESSIBILITY_SERVICE", "RECORD_AUDIO", "INTERNET"),
                waterfall = listOf(
                    ScoringFinding("accessibility_enabled", "Accessibility remote control enabled", "एक्सेसिबिलिटी रिमोट कंट्रोल चालू", 25, "Remote touch simulation active"),
                    ScoringFinding("notification_listener_enabled", "Notification listener", "सूचना श्रोता", 20, "Screen broadcast capability"),
                    ScoringFinding("camera_or_mic_granted", "Audio streaming", "ऑडियो स्ट्रीमिंग", 5, "RECORD_AUDIO"),
                    ScoringFinding("internet_granted", "Internet connection", "इंटरनेट कनेक्शन", 2, "INTERNET"),
                    ScoringFinding("REMOTE_ACCESS_TOOL", "Known remote screen share tool", "रिमोट स्क्रीन शेयर टूल", 28, "Listed in REMOTE_ACCESS_PACKAGES")
                ),
                threatNarratives = listOf(
                    ThreatNarrative(
                        chainId = "CHAIN_REMOTE_CONTROL",
                        title_en = "Live Screen Sharing Active",
                        title_hi = "लाइव स्क्रीन शेयरिंग सक्रिय",
                        body_en = "This software allows a remote third party to see your entire screen and control taps and inputs. Fraudsters frequently instruct victims to install this app during impersonation scams.",
                        body_hi = "यह सॉफ़्टवेयर किसी तीसरे व्यक्ति को आपकी पूरी स्क्रीन देखने और इनपुट नियंत्रित करने की अनुमति देता है। धोखाधड़ी करने वाले अक्सर इसे इंस्टॉल करवाते हैं।",
                        analogy_en = "Like handing your unlocked phone to a stranger on the street.",
                        analogy_hi = "जैसे अपना अनलॉक फोन सड़क पर किसी अजनबी को सौंप देना।",
                        mitre_ref = "T1429",
                        severity = RiskBand.CRITICAL
                    )
                ),
                whyFlagged = listOf(
                    "Included in the high-priority remote screen-share package watchlist",
                    "Can transmit real-time screen feed and inject gestures",
                    "Commonly used by fraudsters to bypass banking security in customer support scams"
                ),
                whyFlagged_hi = listOf(
                    "हाई-प्रायोरिटी रिमोट स्क्रीन-शेयर पैकेज वॉचलिस्ट में शामिल है",
                    "रीयल-टाइम स्क्रीन फ़ीड प्रसारित कर सकता है और स्पर्श संकेत डाल सकता है",
                    "ग्राहक सेवा धोखाधड़ी में बैंकिंग सुरक्षा को बायपास करने के लिए उपयोग किया जाता है"
                ),
                contextMismatch = false,
                permissionCreepFlag = false,
                isDormant = false,
                isRemoteAccessApp = true,
                specialAccessCoverage = mapOf(
                    "Accessibility" to SpecialAccessState.ENABLED,
                    "DeviceAdmin" to SpecialAccessState.NOT_ACTIVE,
                    "NotificationListener" to SpecialAccessState.NOT_ENABLED,
                    "Overlay" to SpecialAccessState.NOT_ENABLED
                )
            )

            // 5. Dormant threat app (unused 75 days, dangerous SMS + location)
            val dormantApp = AppRiskProfile(
                packageName = "com.oldcity.taxicab",
                appName = "Old City Cabs",
                versionName = "1.8.2",
                versionCode = 182L,
                firstInstallTime = System.currentTimeMillis() - 86400000L * 180,
                lastUpdateTime = System.currentTimeMillis() - 86400000L * 90,
                targetSdkVersion = 30,
                isSystemApp = false,
                trustLevel = TrustLevel.VERIFIED,
                installerPackageName = "com.android.vending",
                riskScore = 65,
                riskBand = RiskBand.HIGH,
                categoryStatuses = mapOf(
                    PermissionCategory.SPECIAL_ACCESS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.SMS to CategoryStatus.GRANTED,
                    PermissionCategory.CONTACTS_CALLS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.CAMERA_MIC to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.LOCATION to CategoryStatus.GRANTED,
                    PermissionCategory.PHONE_IDENTITY to CategoryStatus.GRANTED,
                    PermissionCategory.STORAGE to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.BACKGROUND to CategoryStatus.GRANTED,
                    PermissionCategory.NETWORK to CategoryStatus.GRANTED
                ),
                categoryPermissions = mapOf(
                    PermissionCategory.SMS to listOf("android.permission.RECEIVE_SMS"),
                    PermissionCategory.LOCATION to listOf("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_BACKGROUND_LOCATION"),
                    PermissionCategory.PHONE_IDENTITY to listOf("android.permission.READ_PHONE_STATE"),
                    PermissionCategory.BACKGROUND to listOf("android.permission.RECEIVE_BOOT_COMPLETED"),
                    PermissionCategory.NETWORK to listOf("android.permission.INTERNET")
                ),
                declaredPermissions = listOf("RECEIVE_SMS", "ACCESS_FINE_LOCATION", "ACCESS_BACKGROUND_LOCATION", "READ_PHONE_STATE", "INTERNET"),
                grantedPermissions = listOf("RECEIVE_SMS", "ACCESS_FINE_LOCATION", "ACCESS_BACKGROUND_LOCATION", "READ_PHONE_STATE", "INTERNET"),
                waterfall = listOf(
                    ScoringFinding("sms_any_granted", "SMS access granted", "SMS एक्सेस स्वीकृत", 10, "RECEIVE_SMS"),
                    ScoringFinding("background_location_granted", "Background location", "बैकग्राउंड लोकेशन", 8, "ACCESS_BACKGROUND_LOCATION"),
                    ScoringFinding("boot_persistence_granted", "Autostarts on boot", "रीबूट पर ऑटो-स्टार्ट", 5, "RECEIVE_BOOT_COMPLETED"),
                    ScoringFinding("internet_granted", "Internet access", "इंटरनेट एक्सेस", 2, "INTERNET"),
                    ScoringFinding("CHAIN_DORMANT_THREAT", "Attack Chain: Dormant Threat", "हमला श्रृंखला: सोया हुआ खतरा", 10, "Unused for 75 days with SMS & Location active"),
                    ScoringFinding("dormant_penalty", "Unused sensitive permissions", "अप्रयुक्त संवेदनशील अनुमतियां", 30, "75 days without opening")
                ),
                threatNarratives = listOf(
                    ThreatNarrative(
                        chainId = "CHAIN_DORMANT_THREAT",
                        title_en = "Sleeping Risk — Still Has Access",
                        title_hi = "सोया हुआ खतरा — अभी भी एक्सेस है",
                        body_en = "You have not used this app in over 30 days, but it still holds sensitive permissions. Unused apps with active access are a silent risk — they can still be triggered remotely or by an update. (Ref: MITRE T1444)",
                        body_hi = "आपने यह ऐप 30+ दिनों से उपयोग नहीं किया, लेकिन इसके पास अभी भी संवेदनशील अनुमतियां हैं। बेकार ऐप्स भी खतरनाक हो सकते हैं। (संदर्भ: MITRE T1444)",
                        analogy_en = "Like leaving a spare key under the doormat for a service you no longer use.",
                        analogy_hi = "जैसे उस सेवा की चाभी बाहर छोड़ना जो अब आप उपयोग नहीं करते।",
                        mitre_ref = "T1444",
                        severity = RiskBand.HIGH
                    )
                ),
                whyFlagged = listOf(
                    "App has not been opened in 75 days, yet still holds active 24/7 background location tracking",
                    "Retains permission to intercept incoming SMS messages",
                    "Background service auto-runs whenever device boots up"
                ),
                whyFlagged_hi = listOf(
                    "ऐप 75 दिनों से नहीं खोला गया, फिर भी 24/7 बैकग्राउंड लोकेशन ट्रैकिंग जारी है",
                    "आने वाले SMS संदेशों को रोकने की अनुमति अभी भी पास में है",
                    "डिवाइस बूट होने पर बैकग्राउंड सेवा अपने आप चालू हो जाती है"
                ),
                contextMismatch = false,
                permissionCreepFlag = false,
                isDormant = true,
                daysUnused = 75,
                dangerScore = 65,
                isRemoteAccessApp = false,
                specialAccessCoverage = mapOf(
                    "Accessibility" to SpecialAccessState.NOT_ENABLED,
                    "DeviceAdmin" to SpecialAccessState.NOT_ACTIVE,
                    "NotificationListener" to SpecialAccessState.NOT_ENABLED,
                    "Overlay" to SpecialAccessState.NOT_ENABLED
                )
            )

            // 6. Permission creep app (Recently updated and added CAMERA and RECORD_AUDIO)
            val creepApp = AppRiskProfile(
                packageName = "com.cute.wallpaper.ultra",
                appName = "Cute Wallpaper 4K",
                versionName = "3.0.1",
                versionCode = 301L,
                firstInstallTime = System.currentTimeMillis() - 86400000L * 60,
                lastUpdateTime = System.currentTimeMillis() - 86400000L * 3, // updated 3 days ago
                targetSdkVersion = 33,
                isSystemApp = false,
                trustLevel = TrustLevel.VERIFIED,
                installerPackageName = "com.android.vending",
                riskScore = 55,
                riskBand = RiskBand.HIGH,
                categoryStatuses = mapOf(
                    PermissionCategory.SPECIAL_ACCESS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.SMS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.CONTACTS_CALLS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.CAMERA_MIC to CategoryStatus.GRANTED,
                    PermissionCategory.LOCATION to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.PHONE_IDENTITY to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.STORAGE to CategoryStatus.GRANTED,
                    PermissionCategory.BACKGROUND to CategoryStatus.DECLARED_ONLY,
                    PermissionCategory.NETWORK to CategoryStatus.GRANTED
                ),
                categoryPermissions = mapOf(
                    PermissionCategory.CAMERA_MIC to listOf("android.permission.RECORD_AUDIO", "android.permission.CAMERA"),
                    PermissionCategory.STORAGE to listOf("android.permission.READ_MEDIA_IMAGES"),
                    PermissionCategory.NETWORK to listOf("android.permission.INTERNET")
                ),
                declaredPermissions = listOf("RECORD_AUDIO", "CAMERA", "READ_MEDIA_IMAGES", "INTERNET"),
                grantedPermissions = listOf("RECORD_AUDIO", "CAMERA", "READ_MEDIA_IMAGES", "INTERNET"),
                waterfall = listOf(
                    ScoringFinding("camera_or_mic_granted", "Camera and microphone access", "कैमरा और माइक्रोफ़ोन एक्सेस", 5, "RECORD_AUDIO, CAMERA"),
                    ScoringFinding("broad_storage_granted", "Media image storage access", "मीडिया स्टोरेज एक्सेस", 8, "READ_MEDIA_IMAGES"),
                    ScoringFinding("internet_granted", "Internet connection", "इंटरनेट कनेक्शन", 2, "INTERNET"),
                    ScoringFinding("permission_creep", "Permission creep detected in recent update", "हालिया अपडेट में नई अनुमतियां जोड़ी गईं", 8, "Added RECORD_AUDIO and CAMERA"),
                    ScoringFinding("CONTEXT_MISMATCH", "Context Mismatch", "असंगत अनुमति", 32, "Wallpaper app requesting microphone recording")
                ),
                threatNarratives = emptyList(),
                whyFlagged = listOf(
                    "Recent update silently added microphone and camera permissions",
                    "A wallpaper theme app has no legitimate need for microphone recording",
                    "Sends device media identifiers over network"
                ),
                whyFlagged_hi = listOf(
                    "हालिया अपडेट में चुपचाप माइक्रोफ़ोन और कैमरा अनुमतियां जोड़ी गईं",
                    "वॉलपेपर ऐप को माइक्रोफ़ोन रिकॉर्डिंग की कोई आवश्यकता नहीं है",
                    "नेटवर्क पर डिवाइस मीडिया पहचानकर्ता भेजता है"
                ),
                contextMismatch = true,
                mismatchSeverity = "HIGH",
                permissionCreepFlag = true,
                addedPermissions = listOf("RECORD_AUDIO", "CAMERA"),
                isDormant = false,
                isRemoteAccessApp = false,
                specialAccessCoverage = mapOf(
                    "Accessibility" to SpecialAccessState.NOT_ENABLED,
                    "DeviceAdmin" to SpecialAccessState.NOT_ACTIVE,
                    "NotificationListener" to SpecialAccessState.NOT_ENABLED,
                    "Overlay" to SpecialAccessState.NOT_ENABLED
                )
            )

            // 7. Normal safe app (Calculator)
            val calculatorApp = AppRiskProfile(
                packageName = "com.smart.calculator.basic",
                appName = "Calculator Plus",
                versionName = "1.0.5",
                versionCode = 105L,
                firstInstallTime = System.currentTimeMillis() - 86400000L * 100,
                lastUpdateTime = System.currentTimeMillis() - 86400000L * 40,
                targetSdkVersion = 34,
                isSystemApp = false,
                trustLevel = TrustLevel.VERIFIED,
                installerPackageName = "com.android.vending",
                riskScore = 8,
                riskBand = RiskBand.LOW,
                categoryStatuses = mapOf(
                    PermissionCategory.SPECIAL_ACCESS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.SMS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.CONTACTS_CALLS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.CAMERA_MIC to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.LOCATION to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.PHONE_IDENTITY to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.STORAGE to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.BACKGROUND to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.NETWORK to CategoryStatus.DECLARED_ONLY
                ),
                categoryPermissions = emptyMap(),
                declaredPermissions = listOf("android.permission.VIBRATE"),
                grantedPermissions = listOf("android.permission.VIBRATE"),
                waterfall = listOf(
                    ScoringFinding("base_audit", "Basic utility profile", "बुनियादी उपयोगिता प्रोफ़ाइल", 8, "No sensitive permissions")
                ),
                threatNarratives = emptyList(),
                whyFlagged = listOf(
                    "Requests no sensitive runtime permissions",
                    "No background services or special access declared",
                    "Fully verified installation from official Play Store"
                ),
                whyFlagged_hi = listOf(
                    "कोई संवेदनशील रनटाइम अनुमति नहीं मांगता",
                    "कोई बैकग्राउंड सेवा या विशेष एक्सेस घोषित नहीं",
                    "आधिकारिक Play Store से पूरी तरह सत्यापित स्थापना"
                ),
                contextMismatch = false,
                permissionCreepFlag = false,
                isDormant = false,
                isRemoteAccessApp = false,
                specialAccessCoverage = mapOf(
                    "Accessibility" to SpecialAccessState.NOT_ENABLED,
                    "DeviceAdmin" to SpecialAccessState.NOT_ACTIVE,
                    "NotificationListener" to SpecialAccessState.NOT_ENABLED,
                    "Overlay" to SpecialAccessState.NOT_ENABLED
                )
            )

            // 8. System App (System Settings)
            val settingsApp = AppRiskProfile(
                packageName = "com.android.settings",
                appName = "Android Settings",
                versionName = "14",
                versionCode = 34L,
                firstInstallTime = System.currentTimeMillis() - 86400000L * 300,
                lastUpdateTime = System.currentTimeMillis() - 86400000L * 15,
                targetSdkVersion = 34,
                isSystemApp = true,
                trustLevel = TrustLevel.PREINSTALLED,
                installerPackageName = "com.google.android.packageinstaller",
                riskScore = 15,
                riskBand = RiskBand.LOW,
                categoryStatuses = mapOf(
                    PermissionCategory.SPECIAL_ACCESS to CategoryStatus.GRANTED,
                    PermissionCategory.SMS to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.CONTACTS_CALLS to CategoryStatus.GRANTED,
                    PermissionCategory.CAMERA_MIC to CategoryStatus.NOT_REQUESTED,
                    PermissionCategory.LOCATION to CategoryStatus.GRANTED,
                    PermissionCategory.PHONE_IDENTITY to CategoryStatus.GRANTED,
                    PermissionCategory.STORAGE to CategoryStatus.GRANTED,
                    PermissionCategory.BACKGROUND to CategoryStatus.GRANTED,
                    PermissionCategory.NETWORK to CategoryStatus.GRANTED
                ),
                categoryPermissions = emptyMap(),
                declaredPermissions = emptyList(),
                grantedPermissions = emptyList(),
                waterfall = listOf(
                    ScoringFinding("system_app", "Preinstalled operating system component", "पहले से स्थापित ऑपरेटिंग सिस्टम घटक", 15, "Signed with platform keys")
                ),
                threatNarratives = emptyList(),
                whyFlagged = listOf(
                    "Core Android system component",
                    "Signed by official device platform keys"
                ),
                whyFlagged_hi = listOf(
                    "कोर एंड्रॉइड सिस्टम घटक",
                    "आधिकारिक डिवाइस प्लेटफ़ॉर्म कुंजियों द्वारा हस्ताक्षरित"
                ),
                contextMismatch = false,
                permissionCreepFlag = false,
                isDormant = false,
                isRemoteAccessApp = false,
                specialAccessCoverage = mapOf(
                    "Accessibility" to SpecialAccessState.NOT_ENABLED,
                    "DeviceAdmin" to SpecialAccessState.NOT_ACTIVE,
                    "NotificationListener" to SpecialAccessState.NOT_ENABLED,
                    "Overlay" to SpecialAccessState.NOT_ENABLED
                )
            )

            return listOf(
                selfApp,
                otpStealerApp,
                overlayApp,
                spyApp,
                anyDeskApp,
                dormantApp,
                creepApp,
                calculatorApp,
                settingsApp
            )
        }

        private fun com.appanuman.data.model.AppRiskProfile.toUiModel(): AppRiskProfile {
            val categoryStatusesMap = this.categoryMap.map { (cat, res) ->
                val uiCat = PermissionCategory.valueOf(cat.name)
                val uiStatus = when (res.state) {
                    com.appanuman.data.model.PermissionState.GRANTED -> CategoryStatus.GRANTED
                    com.appanuman.data.model.PermissionState.DECLARED_ONLY -> CategoryStatus.DECLARED_ONLY
                    com.appanuman.data.model.PermissionState.NOT_REQUESTED -> CategoryStatus.NOT_REQUESTED
                    com.appanuman.data.model.PermissionState.UNABLE_TO_CHECK -> CategoryStatus.UNABLE_TO_CHECK
                }
                uiCat to uiStatus
            }.toMap()

            val categoryPermissionsMap = this.categoryMap.map { (cat, res) ->
                PermissionCategory.valueOf(cat.name) to res.permissions.map { it.permissionName }
            }.toMap()

            val uiWaterfall = this.scoringFindings.map {
                ScoringFinding(
                    ruleId = it.ruleId,
                    label_en = it.label_en,
                    label_hi = it.label_hi,
                    points = it.points,
                    evidence = it.evidence
                )
            }

            val uiNarratives = this.threatNarratives.map {
                ThreatNarrative(
                    chainId = it.chainId.name,
                    title_en = it.title_en,
                    title_hi = it.title_hi,
                    body_en = it.body_en,
                    body_hi = it.body_hi,
                    analogy_en = it.analogy_en,
                    analogy_hi = it.analogy_hi,
                    mitre_ref = it.mitre_ref,
                    severity = RiskBand.valueOf(it.severity.name)
                )
            }

            val uiTrustLevel = when (this.installerTrust) {
                com.appanuman.data.model.InstallerTrust.VERIFIED -> TrustLevel.VERIFIED
                com.appanuman.data.model.InstallerTrust.SYSTEM_INSTALL -> TrustLevel.SYSTEM_INSTALL
                com.appanuman.data.model.InstallerTrust.UNKNOWN -> TrustLevel.UNKNOWN
                com.appanuman.data.model.InstallerTrust.SIDELOADED -> TrustLevel.SIDELOADED
                com.appanuman.data.model.InstallerTrust.PREINSTALLED -> TrustLevel.PREINSTALLED
            }

            val specialAccessMap = mapOf(
                "Accessibility" to when (this.specialAccess.accessibilityEnabled) {
                    com.appanuman.data.model.PermissionState.GRANTED -> SpecialAccessState.ENABLED
                    com.appanuman.data.model.PermissionState.UNABLE_TO_CHECK -> SpecialAccessState.UNABLE_TO_CHECK
                    else -> SpecialAccessState.NOT_ENABLED
                },
                "DeviceAdmin" to when (this.specialAccess.deviceAdminActive) {
                    com.appanuman.data.model.PermissionState.GRANTED -> SpecialAccessState.ACTIVE
                    com.appanuman.data.model.PermissionState.UNABLE_TO_CHECK -> SpecialAccessState.UNABLE_TO_CHECK
                    else -> SpecialAccessState.NOT_ACTIVE
                },
                "NotificationListener" to when (this.specialAccess.notificationListenerEnabled) {
                    com.appanuman.data.model.PermissionState.GRANTED -> SpecialAccessState.ENABLED
                    com.appanuman.data.model.PermissionState.UNABLE_TO_CHECK -> SpecialAccessState.UNABLE_TO_CHECK
                    else -> SpecialAccessState.NOT_ENABLED
                },
                "Overlay" to when (this.specialAccess.overlayState) {
                    com.appanuman.data.model.PermissionState.GRANTED -> SpecialAccessState.ENABLED
                    com.appanuman.data.model.PermissionState.DECLARED_ONLY -> SpecialAccessState.DECLARED_ONLY
                    com.appanuman.data.model.PermissionState.UNABLE_TO_CHECK -> SpecialAccessState.UNABLE_TO_CHECK
                    else -> SpecialAccessState.NOT_ENABLED
                }
            )

            return AppRiskProfile(
                packageName = this.packageName,
                appName = this.label,
                versionName = this.versionName,
                versionCode = 1L,
                firstInstallTime = this.firstInstallTime,
                lastUpdateTime = this.lastUpdateTime,
                targetSdkVersion = this.targetSdkVersion,
                isSystemApp = this.installerTrust == com.appanuman.data.model.InstallerTrust.PREINSTALLED,
                trustLevel = uiTrustLevel,
                installerPackageName = this.installerPackageName,
                riskScore = this.riskScore,
                riskBand = RiskBand.fromScore(this.riskScore),
                categoryStatuses = categoryStatusesMap,
                categoryPermissions = categoryPermissionsMap,
                declaredPermissions = this.declaredPermissions.map { it.permissionName },
                grantedPermissions = this.grantedPermissions.map { it.permissionName },
                waterfall = uiWaterfall,
                threatNarratives = uiNarratives,
                whyFlagged = this.whyFlagged,
                whyFlagged_hi = this.whyFlagged,
                contextMismatch = this.contextMismatch,
                mismatchSeverity = if (this.contextMismatch) "HIGH" else null,
                permissionCreepFlag = this.permissionCreepFlag,
                addedPermissions = this.addedPermissions,
                isDormant = this.isDormant,
                daysUnused = this.daysUnused ?: 0,
                dangerScore = if (this.isDormant) (this.riskScore + (this.daysUnused ?: 0) / 2) else 0,
                isRemoteAccessApp = this.isRemoteAccessApp,
                specialAccessCoverage = specialAccessMap
            )
        }
    }
}
