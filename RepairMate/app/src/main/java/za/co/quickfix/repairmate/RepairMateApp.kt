package za.co.quickfix.repairmate

import android.app.Application
import android.util.Log
import za.co.quickfix.repairmate.push.Notifications

class RepairMateApp : Application() {
    /** Hand-rolled dependency container (keeps the prototype simple; Hilt could replace it later). */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Log.i("RepairMateApp", "Starting RepairMate ${BuildConfig.VERSION_NAME}")
        container = AppContainer(this)
        Notifications.ensureChannel(this)
    }
}
