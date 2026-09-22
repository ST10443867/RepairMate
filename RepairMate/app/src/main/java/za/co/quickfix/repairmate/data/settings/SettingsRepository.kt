package za.co.quickfix.repairmate.data.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notifications: Boolean = true
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Persists user settings with Jetpack DataStore. */
class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val notificationsKey = booleanPreferencesKey("notifications")

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                themeMode = runCatching { ThemeMode.valueOf(prefs[themeKey] ?: "SYSTEM") }
                    .getOrDefault(ThemeMode.SYSTEM),
                notifications = prefs[notificationsKey] ?: true
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.dataStore.edit { it[notificationsKey] = enabled }
    }
}

/**
 * App language via AppCompatDelegate per-app locales. Android stores the choice itself
 * (see AppLocalesMetadataHolderService in the manifest) and recreates the activity on change.
 */
object LanguageManager {
    const val ENGLISH = "en"
    const val ZULU = "zu"

    fun current(): String =
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags().startsWith(ZULU)) ZULU else ENGLISH

    fun set(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }
}
