package za.co.quickfix.repairmate.ui.settings

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import za.co.quickfix.repairmate.R
import za.co.quickfix.repairmate.data.auth.AppUser
import za.co.quickfix.repairmate.data.settings.LanguageManager
import za.co.quickfix.repairmate.data.settings.ThemeMode
import za.co.quickfix.repairmate.ui.ChoiceRow
import za.co.quickfix.repairmate.ui.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsVm: SettingsViewModel,
    authVm: AuthViewModel,
    user: AppUser,
    onBack: () -> Unit
) {
    val settings by settingsVm.settings.collectAsStateWithLifecycle()
    val token by settingsVm.fcmToken.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var language by remember { mutableStateOf(LanguageManager.current()) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) { settingsVm.loadToken() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionTitle(R.string.settings_account)
            Text(user.name, style = MaterialTheme.typography.titleMedium)
            Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { authVm.logout() }) { Text(stringResource(R.string.action_logout)) }

            Divider()

            SectionTitle(R.string.settings_language)
            ChoiceRow(language == LanguageManager.ENGLISH, stringResource(R.string.language_english)) {
                language = LanguageManager.ENGLISH
                LanguageManager.set(LanguageManager.ENGLISH) // recreates the activity with new strings
            }
            ChoiceRow(language == LanguageManager.ZULU, stringResource(R.string.language_zulu)) {
                language = LanguageManager.ZULU
                LanguageManager.set(LanguageManager.ZULU)
            }

            Divider()

            SectionTitle(R.string.settings_theme)
            ChoiceRow(settings.themeMode == ThemeMode.SYSTEM, stringResource(R.string.theme_system)) { settingsVm.setThemeMode(ThemeMode.SYSTEM) }
            ChoiceRow(settings.themeMode == ThemeMode.LIGHT, stringResource(R.string.theme_light)) { settingsVm.setThemeMode(ThemeMode.LIGHT) }
            ChoiceRow(settings.themeMode == ThemeMode.DARK, stringResource(R.string.theme_dark)) { settingsVm.setThemeMode(ThemeMode.DARK) }

            Divider()

            SectionTitle(R.string.settings_notifications)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.settings_notifications_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                )
                Switch(
                    checked = settings.notifications,
                    onCheckedChange = { on ->
                        settingsVm.setNotifications(on)
                        if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.settings_fcm_token), style = MaterialTheme.typography.labelLarge)
            val t = token
            if (t == null) {
                Text(stringResource(R.string.token_unavailable), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(t.take(28) + "…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { copyToClipboard(context, t) }) { Text(stringResource(R.string.action_copy_token)) }
            }
        }
    }
}

@Composable
private fun SectionTitle(res: Int) {
    Text(stringResource(res), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Divider() {
    HorizontalDivider(Modifier.padding(vertical = 8.dp))
}

private fun copyToClipboard(context: Context, token: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("FCM token", token))
    Toast.makeText(context, R.string.token_copied, Toast.LENGTH_SHORT).show()
}
