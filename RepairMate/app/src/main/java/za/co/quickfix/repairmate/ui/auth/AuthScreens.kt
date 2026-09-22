package za.co.quickfix.repairmate.ui.auth

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import za.co.quickfix.repairmate.R
import za.co.quickfix.repairmate.ui.AuthScaffold
import za.co.quickfix.repairmate.ui.FailureText
import za.co.quickfix.repairmate.ui.FormField
import za.co.quickfix.repairmate.ui.PasswordField
import za.co.quickfix.repairmate.ui.PrimaryButton

/** Switches between the login and registration screens. */
@Composable
fun AuthGraph(vm: AuthViewModel) {
    var showRegister by rememberSaveable { mutableStateOf(false) }
    if (showRegister) {
        RegisterScreen(vm, onSwitch = { vm.clearErrors(); showRegister = false })
    } else {
        LoginScreen(vm, onSwitch = { vm.clearErrors(); showRegister = true })
    }
}

@Composable
fun LoginScreen(vm: AuthViewModel, onSwitch: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    // Passwords use plain remember (not rememberSaveable) so they are never written to saved state.
    var password by remember { mutableStateOf("") }

    AuthScaffold(R.string.login_title, R.string.login_subtitle) {
        FormField(email, { email = it }, R.string.label_email, state.emailError, keyboardType = KeyboardType.Email)
        PasswordField(
            password, { password = it }, R.string.label_password, state.passwordError,
            imeAction = ImeAction.Done, onDone = { vm.login(email, password) }
        )
        FailureText(state.failure)
        PrimaryButton(R.string.action_login, state.loading) { vm.login(email, password) }
        TextButton(onClick = onSwitch) { Text(stringResource(R.string.link_to_register)) }
    }
}

@Composable
fun RegisterScreen(vm: AuthViewModel, onSwitch: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    AuthScaffold(R.string.register_title, R.string.register_subtitle) {
        FormField(name, { name = it }, R.string.label_name, state.nameError)
        FormField(email, { email = it }, R.string.label_email, state.emailError, keyboardType = KeyboardType.Email)
        PasswordField(password, { password = it }, R.string.label_password, state.passwordError)
        PasswordField(
            confirm, { confirm = it }, R.string.label_confirm_password, state.confirmError,
            imeAction = ImeAction.Done, onDone = { vm.register(name, email, password, confirm) }
        )
        FailureText(state.failure)
        PrimaryButton(R.string.action_register, state.loading) { vm.register(name, email, password, confirm) }
        TextButton(onClick = onSwitch) { Text(stringResource(R.string.link_to_login)) }
    }
}
