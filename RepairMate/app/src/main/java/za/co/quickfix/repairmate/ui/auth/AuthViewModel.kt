package za.co.quickfix.repairmate.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.co.quickfix.repairmate.data.auth.AppUser
import za.co.quickfix.repairmate.data.auth.AuthFailure
import za.co.quickfix.repairmate.data.auth.AuthReason
import za.co.quickfix.repairmate.data.auth.AuthRepository
import za.co.quickfix.repairmate.domain.FieldError
import za.co.quickfix.repairmate.domain.Validators

data class AuthUiState(
    val user: AppUser? = null,
    val loading: Boolean = false,
    val nameError: FieldError? = null,
    val emailError: FieldError? = null,
    val passwordError: FieldError? = null,
    val confirmError: FieldError? = null,
    val failure: AuthReason? = null
)

/**
 * Handles login, registration and logout. Input is validated locally first so bad input
 * never reaches the network (and never crashes the app).
 */
class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState(user = repo.currentUser))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        val emailError = Validators.email(email)
        val passwordError = if (password.isEmpty()) FieldError.REQUIRED else null
        if (emailError != null || passwordError != null) {
            Log.d(TAG, "Login blocked by validation")
            _state.update { it.copy(emailError = emailError, passwordError = passwordError, failure = null) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, emailError = null, passwordError = null, failure = null) }
            repo.login(email.trim(), password).fold(
                onSuccess = { user -> _state.update { AuthUiState(user = user) } },
                onFailure = { t -> onFailure(t) }
            )
        }
    }

    fun register(name: String, email: String, password: String, confirm: String) {
        val nameError = Validators.name(name)
        val emailError = Validators.email(email)
        val passwordError = Validators.password(password)
        val confirmError = Validators.confirmPassword(password, confirm)
        if (listOf(nameError, emailError, passwordError, confirmError).any { it != null }) {
            Log.d(TAG, "Registration blocked by validation")
            _state.update {
                it.copy(
                    nameError = nameError, emailError = emailError,
                    passwordError = passwordError, confirmError = confirmError, failure = null
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { AuthUiState(loading = true) }
            repo.register(name.trim(), email.trim(), password).fold(
                onSuccess = { user -> _state.update { AuthUiState(user = user) } },
                onFailure = { t -> onFailure(t) }
            )
        }
    }

    fun logout() {
        repo.logout()
        _state.value = AuthUiState()
    }

    fun clearErrors() {
        _state.update {
            it.copy(nameError = null, emailError = null, passwordError = null, confirmError = null, failure = null)
        }
    }

    private fun onFailure(t: Throwable) {
        val reason = (t as? AuthFailure)?.reason ?: AuthReason.UNKNOWN
        Log.w(TAG, "Auth failed: $reason")
        _state.update { it.copy(loading = false, failure = reason) }
    }

    private companion object {
        const val TAG = "AuthViewModel"
    }
}
