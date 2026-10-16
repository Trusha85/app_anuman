package com.appanuman.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appanuman.data.model.AppFilter
import com.appanuman.data.model.AppRiskProfile
import com.appanuman.data.model.DeviceRiskSummary
import com.appanuman.data.model.PanicCheckResult
import com.appanuman.data.repository.AppRiskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ScanState {
    IDLE, SCANNING, DONE, ERROR
}

enum class UserMode {
    SIMPLE, DETAILED, GRANDPARENT
}

enum class Language {
    EN, HI
}

class MainViewModel(
    application: Application,
    private val repository: AppRiskRepository = AppRiskRepository(application.applicationContext)
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("appanuman_ui_prefs", Context.MODE_PRIVATE)

    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _appList = MutableStateFlow<List<AppRiskProfile>>(emptyList())
    val appList: StateFlow<List<AppRiskProfile>> = _appList.asStateFlow()

    private val _deviceSummary = MutableStateFlow<DeviceRiskSummary?>(null)
    val deviceSummary: StateFlow<DeviceRiskSummary?> = _deviceSummary.asStateFlow()

    private val _panicResult = MutableStateFlow<PanicCheckResult?>(null)
    val panicResult: StateFlow<PanicCheckResult?> = _panicResult.asStateFlow()

    private val _includeSystemApps = MutableStateFlow(false)
    val includeSystemApps: StateFlow<Boolean> = _includeSystemApps.asStateFlow()

    private val initialUserMode = try {
        UserMode.valueOf(prefs.getString("user_mode", UserMode.SIMPLE.name) ?: UserMode.SIMPLE.name)
    } catch (e: Exception) {
        UserMode.SIMPLE
    }
    private val _userMode = MutableStateFlow(initialUserMode)
    val userMode: StateFlow<UserMode> = _userMode.asStateFlow()

    private val initialLanguage = try {
        Language.valueOf(prefs.getString("language", Language.EN.name) ?: Language.EN.name)
    } catch (e: Exception) {
        Language.EN
    }
    private val _language = MutableStateFlow(initialLanguage)
    val language: StateFlow<Language> = _language.asStateFlow()

    fun startScan(includeSystem: Boolean = _includeSystemApps.value) {
        _includeSystemApps.value = includeSystem
        _scanState.value = ScanState.SCANNING

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (apps, summary) = repository.scanApps(includeSystem)
                _appList.value = apps
                _deviceSummary.value = summary
                _scanState.value = ScanState.DONE
            } catch (e: Exception) {
                _scanState.value = ScanState.ERROR
            }
        }
    }

    fun toggleSystemApps() {
        val next = !_includeSystemApps.value
        _includeSystemApps.value = next
        startScan(next)
    }

    fun setUserMode(mode: UserMode) {
        _userMode.value = mode
        prefs.edit().putString("user_mode", mode.name).apply()
    }

    fun setLanguage(lang: Language) {
        _language.value = lang
        prefs.edit().putString("language", lang.name).apply()
    }

    fun runPanicCheck(): PanicCheckResult {
        val result = repository.evaluatePanic(_appList.value)
        _panicResult.value = result
        return result
    }

    fun getFilteredApps(filter: AppFilter): List<AppRiskProfile> {
        return repository.filterApps(_appList.value, filter)
    }

    fun getShareText(lang: Language): String {
        return repository.buildShareReport(
            summary = _deviceSummary.value,
            apps = _appList.value,
            isHindi = (lang == Language.HI)
        )
    }
}
