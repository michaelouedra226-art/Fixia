package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fixia_settings", Context.MODE_PRIVATE)

    private val _themeFlow = MutableStateFlow(getThemePreference())
    val themeFlow: StateFlow<String> = _themeFlow.asStateFlow()

    private val _qualityFlow = MutableStateFlow(getAiQualityMode())
    val qualityFlow: StateFlow<String> = _qualityFlow.asStateFlow()

    fun getGeminiApiKey(): String {
        val customKey = prefs.getString("custom_gemini_api_key", "") ?: ""
        if (customKey.isNotBlank()) {
            return customKey.trim()
        }
        // Fallback to BuildConfig key if defined
        return try {
            val buildKey = BuildConfig.GEMINI_API_KEY
            if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
        } catch (e: Throwable) {
            ""
        }
    }

    fun hasCustomKey(): Boolean {
        return prefs.getString("custom_gemini_api_key", "")?.isNotBlank() == true
    }

    fun saveGeminiApiKey(key: String) {
        prefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
    }

    fun clearGeminiApiKey() {
        prefs.edit().remove("custom_gemini_api_key").apply()
    }

    fun getAiQualityMode(): String {
        return prefs.getString("ai_quality_mode", "flash") ?: "flash"
    }

    fun setAiQualityMode(mode: String) {
        prefs.edit().putString("ai_quality_mode", mode).apply()
        _qualityFlow.value = mode
    }

    fun getThemePreference(): String {
        return prefs.getString("theme_preference", "system") ?: "system"
    }

    fun setThemePreference(theme: String) {
        prefs.edit().putString("theme_preference", theme).apply()
        _themeFlow.value = theme
    }

    fun getEmergencyContactPhone(): String {
        return prefs.getString("emergency_contact_phone", "112") ?: "112"
    }

    fun saveEmergencyContactPhone(phone: String) {
        prefs.edit().putString("emergency_contact_phone", phone.trim()).apply()
    }

    fun getEmergencyContactName(): String {
        return prefs.getString("emergency_contact_name", "Contact d'urgence") ?: "Contact d'urgence"
    }

    fun saveEmergencyContactName(name: String) {
        prefs.edit().putString("emergency_contact_name", name.trim()).apply()
    }
}
