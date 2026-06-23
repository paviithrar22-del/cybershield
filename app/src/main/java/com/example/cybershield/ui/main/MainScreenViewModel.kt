package com.example.cybershield.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cybershield.data.AppSettings
import com.example.cybershield.data.DataRepository
import com.example.cybershield.data.DefaultDataRepository
import com.example.cybershield.data.Incident
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainScreenViewModel(
    private val dataRepository: DataRepository,
    private val appSettings: AppSettings
) : ViewModel() {

    val incidents: StateFlow<List<Incident>> = dataRepository.incidents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalScanned: StateFlow<Int> = dataRepository.totalScanned
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalFlagged: StateFlow<Int> = dataRepository.totalFlagged
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Sensitivity State
    val sensitivity: String
        get() = appSettings.sensitivity

    // Guardian Contact Details
    val guardianName: String
        get() = appSettings.guardianName
    
    val guardianPhone: String
        get() = appSettings.guardianPhone

    val guardianEmail: String
        get() = appSettings.guardianEmail

    val monitoredApps: Set<String>
        get() = appSettings.monitoredApps

    val customKeywords: Set<String>
        get() = appSettings.customKeywords

    private val _themeModeState = mutableStateOf(appSettings.themeMode)
    var themeMode: String
        get() = _themeModeState.value
        set(value) {
            appSettings.themeMode = value
            _themeModeState.value = value
        }

    fun addCustomKeyword(keyword: String) {
        val current = appSettings.customKeywords.toMutableSet()
        if (keyword.isNotBlank() && current.add(keyword.trim().lowercase(java.util.Locale.ROOT))) {
            appSettings.customKeywords = current
        }
    }

    fun removeCustomKeyword(keyword: String) {
        val current = appSettings.customKeywords.toMutableSet()
        if (current.remove(keyword)) {
            appSettings.customKeywords = current
        }
    }

    val useGemini: Boolean
        get() = appSettings.useGemini

    val geminiApiKey: String
        get() = appSettings.geminiApiKey

    fun saveSettings(
        name: String,
        phone: String,
        email: String,
        sensitivityLevel: String,
        enabledPackages: Set<String>,
        useGeminiVal: Boolean,
        geminiApiKeyVal: String
    ) {
        appSettings.guardianName = name
        appSettings.guardianPhone = phone
        appSettings.guardianEmail = email
        appSettings.sensitivity = sensitivityLevel
        appSettings.monitoredApps = enabledPackages
        appSettings.useGemini = useGeminiVal
        appSettings.geminiApiKey = geminiApiKeyVal
    }

    fun clearLogs() {
        viewModelScope.launch {
            dataRepository.clearIncidents()
        }
    }

    fun resetStatistics() {
        viewModelScope.launch {
            dataRepository.resetStats()
        }
    }
}
