package za.co.quickfix.repairmate.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** Thin wrapper around the Firebase Cloud Messaging SDK. */
object Push {
    private const val TAG = "Push"

    /** All app users subscribe to this topic to receive repair-status notifications. */
    const val TOPIC = "status_updates"

    /** Subscribe or unsubscribe this device. Failures (e.g. offline) are logged, not thrown. */
    suspend fun setSubscribed(enabled: Boolean) {
        try {
            val messaging = FirebaseMessaging.getInstance()
            if (enabled) messaging.subscribeToTopic(TOPIC).await()
            else messaging.unsubscribeFromTopic(TOPIC).await()
            Log.i(TAG, "Topic '$TOPIC' subscribed=$enabled")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not update topic subscription: ${e.message}")
        }
    }

    /** The device's FCM registration token (paste it into Firebase Console > Send test message). */
    suspend fun token(): String? = try {
        FirebaseMessaging.getInstance().token.await()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Could not fetch FCM token: ${e.message}")
        null
    }
}
