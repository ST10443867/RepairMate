package za.co.quickfix.repairmate.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import za.co.quickfix.repairmate.data.settings.AppSettings
import za.co.quickfix.repairmate.data.settings.SettingsRepository
import za.co.quickfix.repairmate.data.settings.ThemeMode
import za.co.quickfix.repairmate.push.Push

class SettingsViewModel(private val repo: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    init {
        // Keep the FCM topic subscription in sync with the notifications setting.
        viewModelScope.launch {
            repo.settings.map { it.notifications }.distinctUntilChanged().collect { Push.setSubscribed(it) }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repo.setThemeMode(mode) }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { repo.setNotifications(enabled) }
    }

    fun loadToken() {
        viewModelScope.launch { _fcmToken.value = Push.token() }
    }
}
