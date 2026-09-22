package za.co.quickfix.repairmate.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import za.co.quickfix.repairmate.AppContainer
import za.co.quickfix.repairmate.data.auth.AppUser
import za.co.quickfix.repairmate.ui.auth.AuthGraph
import za.co.quickfix.repairmate.ui.auth.AuthViewModel
import za.co.quickfix.repairmate.ui.booking.BookingDetailScreen
import za.co.quickfix.repairmate.ui.booking.BookingViewModel
import za.co.quickfix.repairmate.ui.booking.HomeScreen
import za.co.quickfix.repairmate.ui.booking.NewBookingScreen
import za.co.quickfix.repairmate.ui.settings.SettingsScreen
import za.co.quickfix.repairmate.ui.settings.SettingsViewModel
import za.co.quickfix.repairmate.ui.theme.RepairMateTheme

/** Small helper so ViewModels can be created with constructor arguments. */
inline fun <reified VM : ViewModel> vmFactory(crossinline create: () -> VM): ViewModelProvider.Factory =
    viewModelFactory { initializer { create() } }

private object Routes {
    const val HOME = "home"
    const val NEW = "new"
    const val DETAIL = "detail/{id}"
    const val SETTINGS = "settings"
    fun detail(id: Long) = "detail/$id"
}

/** Top of the UI: picks the theme, then shows either the auth screens or the signed-in app. */
@Composable
fun RepairMateRoot(container: AppContainer) {
    val settingsVm: SettingsViewModel = viewModel(factory = vmFactory { SettingsViewModel(container.settingsRepository) })
    val authVm: AuthViewModel = viewModel(factory = vmFactory { AuthViewModel(container.authRepository) })
    val settings by settingsVm.settings.collectAsStateWithLifecycle()
    val auth by authVm.state.collectAsStateWithLifecycle()

    RepairMateTheme(themeMode = settings.themeMode) {
        Surface(Modifier.fillMaxSize()) {
            val user = auth.user
            if (user == null) {
                AuthGraph(authVm)
            } else {
                MainGraph(container, authVm, settingsVm, user)
            }
        }
    }
}

@Composable
private fun MainGraph(container: AppContainer, authVm: AuthViewModel, settingsVm: SettingsViewModel, user: AppUser) {
    // Keyed by uid so a different user logging in on the same phone gets a fresh ViewModel.
    val bookingVm: BookingViewModel = viewModel(
        key = "bookings-${user.uid}",
        factory = vmFactory { BookingViewModel(container.bookingRepository, user.uid, user.isAdmin) }
    )
    val nav = rememberNavController()

    // Ask for the Android 13+ notification permission once the user is signed in.
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            if (user.isAdmin) {
                za.co.quickfix.repairmate.ui.booking.AdminDashboardScreen(
                    vm = bookingVm,
                    onOpenBooking = { nav.navigate(Routes.detail(it)) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) }
                )
            } else {
                HomeScreen(
                    userName = user.name,
                    vm = bookingVm,
                    onNewBooking = { nav.navigate(Routes.NEW) },
                    onOpenBooking = { nav.navigate(Routes.detail(it)) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) }
                )
            }
        }
        composable(Routes.NEW) {
            NewBookingScreen(bookingVm, onBack = { nav.popBackStack() })
        }
        composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            BookingDetailScreen(bookingVm, entry.arguments?.getLong("id") ?: -1L, isAdmin = user.isAdmin, onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(settingsVm, authVm, user, onBack = { nav.popBackStack() })
        }
    }
}
