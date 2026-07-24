package com.example.cybershield.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettings private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "cybershield_settings"
        private const val KEY_SENSITIVITY = "sensitivity"
        private const val KEY_GUARDIAN_NAME = "guardian_name"
        private const val KEY_GUARDIAN_PHONE = "guardian_phone"
        private const val KEY_GUARDIAN_EMAIL = "guardian_email"
        private const val KEY_MONITORED_APPS = "monitored_apps"
        private const val KEY_USE_GEMINI = "use_gemini"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_CUSTOM_KEYWORDS = "custom_keywords"
        private const val KEY_THEME_MODE = "theme_mode"
        
        private const val KEY_TOTAL_SCANNED = "total_scanned"
        private const val KEY_TOTAL_FLAGGED = "total_flagged"
        private const val KEY_SETUP_COMPLETED = "setup_completed"

        // Default set of package names we monitor
        val DEFAULT_MONITORED_APPS = setOf(
            "com.whatsapp",                   // WhatsApp
            "com.instagram.android",          // Instagram
            "com.snapchat.android",           // Snapchat
            "com.facebook.orca",              // Messenger
            "com.android.mms",                // SMS (varies by OEM but commonly SMS)
            "com.google.android.apps.messaging" // Google Messages
        )

        @Volatile
        private var instance: AppSettings? = null

        fun getInstance(context: Context): AppSettings {
            return instance ?: synchronized(this) {
                instance ?: AppSettings(context.applicationContext).also { instance = it }
            }
        }
    }

    // Guardian contact info and the user's Gemini API key are sensitive - stored via
    // EncryptedSharedPreferences (AES256-GCM values, AES256-SIV keys) backed by a
    // hardware-attested master key, instead of plain, human-readable SharedPreferences XML.
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Flow for real-time stats updates
    private val _totalScannedFlow = MutableStateFlow(totalScanned)
    val totalScannedFlow: StateFlow<Int> = _totalScannedFlow.asStateFlow()

    private val _totalFlaggedFlow = MutableStateFlow(totalFlagged)
    val totalFlaggedFlow: StateFlow<Int> = _totalFlaggedFlow.asStateFlow()

    var sensitivity: String
        get() = sharedPreferences.getString(KEY_SENSITIVITY, "Medium") ?: "Medium"
        set(value) {
            sharedPreferences.edit().putString(KEY_SENSITIVITY, value).apply()
        }

    var guardianName: String
        get() = sharedPreferences.getString(KEY_GUARDIAN_NAME, "") ?: ""
        set(value) {
            sharedPreferences.edit().putString(KEY_GUARDIAN_NAME, value).apply()
        }

    var guardianPhone: String
        get() = sharedPreferences.getString(KEY_GUARDIAN_PHONE, "") ?: ""
        set(value) {
            sharedPreferences.edit().putString(KEY_GUARDIAN_PHONE, value).apply()
        }

    var guardianEmail: String
        get() = sharedPreferences.getString(KEY_GUARDIAN_EMAIL, "") ?: ""
        set(value) {
            sharedPreferences.edit().putString(KEY_GUARDIAN_EMAIL, value).apply()
        }

    var monitoredApps: Set<String>
        get() = sharedPreferences.getStringSet(KEY_MONITORED_APPS, DEFAULT_MONITORED_APPS) ?: DEFAULT_MONITORED_APPS
        set(value) {
            sharedPreferences.edit().putStringSet(KEY_MONITORED_APPS, value).apply()
        }

    var useGemini: Boolean
        get() = sharedPreferences.getBoolean(KEY_USE_GEMINI, false)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_USE_GEMINI, value).apply()
        }

    var geminiApiKey: String
        get() = sharedPreferences.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) {
            sharedPreferences.edit().putString(KEY_GEMINI_API_KEY, value).apply()
        }

    var isSetupCompleted: Boolean
        get() = sharedPreferences.getBoolean(KEY_SETUP_COMPLETED, false)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_SETUP_COMPLETED, value).apply()
        }

    var customKeywords: Set<String>
        get() = sharedPreferences.getStringSet(KEY_CUSTOM_KEYWORDS, emptySet()) ?: emptySet()
        set(value) {
            sharedPreferences.edit().putStringSet(KEY_CUSTOM_KEYWORDS, value).apply()
        }

    var themeMode: String
        get() = sharedPreferences.getString(KEY_THEME_MODE, "Space Slate") ?: "Space Slate"
        set(value) {
            sharedPreferences.edit().putString(KEY_THEME_MODE, value).apply()
        }

    var totalScanned: Int
        get() = sharedPreferences.getInt(KEY_TOTAL_SCANNED, 0)
        set(value) {
            sharedPreferences.edit().putInt(KEY_TOTAL_SCANNED, value).apply()
            _totalScannedFlow.value = value
        }

    var totalFlagged: Int
        get() = sharedPreferences.getInt(KEY_TOTAL_FLAGGED, 0)
        set(value) {
            sharedPreferences.edit().putInt(KEY_TOTAL_FLAGGED, value).apply()
            _totalFlaggedFlow.value = value
        }

    fun incrementScanned() {
        totalScanned = totalScanned + 1
    }

    fun incrementFlagged() {
        totalFlagged = totalFlagged + 1
    }

    fun resetStats() {
        totalScanned = 0
        totalFlagged = 0
    }

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
        _totalScannedFlow.value = 0
        _totalFlaggedFlow.value = 0
    }
}
