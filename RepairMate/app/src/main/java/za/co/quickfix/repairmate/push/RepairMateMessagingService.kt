package za.co.quickfix.repairmate.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import za.co.quickfix.repairmate.R

/**
 * Receives Firebase Cloud Messaging pushes.
 * - App in background: FCM shows "notification" messages itself (uses the default channel/icon from the manifest).
 * - App in foreground: this method runs, so we show the notification ourselves.
 */
class RepairMateMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Never log the full token; it identifies this install.
        Log.i(TAG, "FCM token refreshed (length=${token.length})")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"]
        Log.d(TAG, "Message received, hasBody=${body != null}")
        if (body != null) Notifications.show(this, title, body)
    }

    private companion object {
        const val TAG = "RepairMateFcm"
    }
}
