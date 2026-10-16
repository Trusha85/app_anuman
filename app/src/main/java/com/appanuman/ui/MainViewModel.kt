package com.appanuman.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.appanuman.data.collector.AppInventoryCollector
import com.appanuman.data.engine.RiskScoringEngine
import com.appanuman.data.model.AppRiskProfile
import com.appanuman.data.model.Language
import com.appanuman.data.model.UserMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val collector = AppInventoryCollector(application)

    private val prefs by lazy {
        try {
            val masterKey = MasterKey.Builder(application)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                application,
                "appanuman_user_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            application.getSharedPreferences("appanuman_user_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    private val _appList = MutableStateFlow<List<AppRiskProfile>>(emptyList())
    val appList: StateFlow<List<AppRiskProfile>> = _appList.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanDuration = MutableStateFlow(0.0f)
    val scanDuration: StateFlow<Float> = _scanDuration.asStateFlow()

    private val _includeSystemApps = MutableStateFlow(false)
    val includeSystemApps: StateFlow<Boolean> = _includeSystemApps.asStateFlow()

    private val _userMode = MutableStateFlow(UserMode.SIMPLE)
    val userMode: StateFlow<UserMode> = _userMode.asStateFlow()

    private val _language = MutableStateFlow(Language.EN)
    val language: StateFlow<Language> = _language.asStateFlow()

    private val _deviceRiskScore = MutableStateFlow(0)
    val deviceRiskScore: StateFlow<Int> = _deviceRiskScore.asStateFlow()

    private val _deviceRiskGrade = MutableStateFlow("A")
    val deviceRiskGrade: StateFlow<String> = _deviceRiskGrade.asStateFlow()

    init {
        // Load stored language & mode
        val savedLang = prefs.getString("pref_language", Language.EN.name)
        _language.value = try { Language.valueOf(savedLang ?: "EN") } catch (e: Exception) { Language.EN }

        val savedMode = prefs.getString("pref_user_mode", UserMode.SIMPLE.name)
        _userMode.value = try { UserMode.valueOf(savedMode ?: "SIMPLE") } catch (e: Exception) { UserMode.SIMPLE }

        scanApps()
    }

    fun scanApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            val startTime = System.currentTimeMillis()
            val apps = collector.collectAllApps(_includeSystemApps.value)
            val duration = (System.currentTimeMillis() - startTime) / 1000f

            val (score, grade) = RiskScoringEngine.calculateDeviceRisk(apps)

            _appList.value = apps.sortedByDescending { it.score }
            _deviceRiskScore.value = score
            _deviceRiskGrade.value = grade
            _scanDuration.value = duration
            _isScanning.value = false
        }
    }

    fun setLanguage(lang: Language) {
        _language.value = lang
        prefs.edit().putString("pref_language", lang.name).apply()
    }

    fun setUserMode(mode: UserMode) {
        _userMode.value = mode
        prefs.edit().putString("pref_user_mode", mode.name).apply()
    }

    fun setIncludeSystemApps(include: Boolean) {
        _includeSystemApps.value = include
        scanApps()
    }

    fun getAppByPackageName(packageName: String): AppRiskProfile? {
        return _appList.value.find { it.packageName == packageName }
    }
}
