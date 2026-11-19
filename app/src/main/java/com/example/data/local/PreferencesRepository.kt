package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "farsi_joon_prefs")

enum class ThemeMode {
  SYSTEM, LIGHT, DARK
}

data class UserPreferences(
  val onboardingCompleted: Boolean = false,
  val hideFinglishAfter12: Boolean = false,
  val calmFocusMode: Boolean = false,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val dailyReminderEnabled: Boolean = false,
  val reminderHour: Int = 19,
  val reminderMinute: Int = 0
)

class PreferencesRepository(private val context: Context) {

  private object Keys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val HIDE_FINGLISH_AFTER_12 = booleanPreferencesKey("hide_finglish_after_12")
    val CALM_FOCUS_MODE = booleanPreferencesKey("calm_focus_mode")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
    val REMINDER_HOUR = intPreferencesKey("reminder_hour")
    val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
  }

  val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
    UserPreferences(
      onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
      hideFinglishAfter12 = prefs[Keys.HIDE_FINGLISH_AFTER_12] ?: false,
      calmFocusMode = prefs[Keys.CALM_FOCUS_MODE] ?: false,
      themeMode = runCatching {
        ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
      }.getOrDefault(ThemeMode.SYSTEM),
      dailyReminderEnabled = prefs[Keys.DAILY_REMINDER_ENABLED] ?: false,
      reminderHour = prefs[Keys.REMINDER_HOUR] ?: 19,
      reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0
    )
  }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
  }

  suspend fun setHideFinglishAfter12(hide: Boolean) {
    context.dataStore.edit { it[Keys.HIDE_FINGLISH_AFTER_12] = hide }
  }

  suspend fun setCalmFocusMode(enabled: Boolean) {
    context.dataStore.edit { it[Keys.CALM_FOCUS_MODE] = enabled }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  suspend fun setDailyReminder(enabled: Boolean, hour: Int = 19, minute: Int = 0) {
    context.dataStore.edit {
      it[Keys.DAILY_REMINDER_ENABLED] = enabled
      it[Keys.REMINDER_HOUR] = hour
      it[Keys.REMINDER_MINUTE] = minute
    }
  }

  suspend fun resetPreferences() {
    context.dataStore.edit { it.clear() }
  }
}
