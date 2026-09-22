package za.co.quickfix.repairmate.data.repo

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import za.co.quickfix.repairmate.data.auth.AuthRepository
import za.co.quickfix.repairmate.data.local.Booking
import za.co.quickfix.repairmate.data.local.BookingDao
import za.co.quickfix.repairmate.data.local.SyncState
import za.co.quickfix.repairmate.data.remote.FirestoreApi
import za.co.quickfix.repairmate.data.remote.documentId
import za.co.quickfix.repairmate.data.remote.statusPatch
import za.co.quickfix.repairmate.data.remote.fullAdminPatch
import za.co.quickfix.repairmate.data.remote.allBookingsQuery
import za.co.quickfix.repairmate.data.remote.toBooking
import za.co.quickfix.repairmate.data.remote.toFsDocument
import za.co.quickfix.repairmate.data.remote.userBookingsQuery

enum class CreateOutcome { SYNCED, SAVED_OFFLINE }

/**
 * Single source of truth for bookings.
 *
 * Offline-first: every booking is written to Room first (as PENDING), then uploaded to Firestore
 * through the REST API. If the upload fails (no network) the booking stays PENDING and is retried
 * by [syncPending], which [refresh] calls first. The UI only ever observes Room.
 */
class BookingRepository(
    private val dao: BookingDao,
    private val api: FirestoreApi,
    private val auth: AuthRepository,
    private val projectId: () -> String
) {
    fun observe(userId: String): Flow<List<Booking>> = dao.observeForUser(userId)

    fun observeAll(): Flow<List<Booking>> = dao.observeAllBookings()

    fun observeOne(localId: Long): Flow<Booking?> = dao.observeById(localId)

    /** Save locally, then try to upload. Never throws for network problems. */
    suspend fun createBooking(draft: Booking): CreateOutcome {
        val localId = dao.upsert(draft.copy(remoteId = null, syncState = SyncState.PENDING))
        Log.d(TAG, "Saved booking locally (localId=$localId), trying upload")
        return if (push(localId)) CreateOutcome.SYNCED else CreateOutcome.SAVED_OFFLINE
    }

    /** Upload every booking still marked PENDING. Returns how many were uploaded. */
    suspend fun syncPending(userId: String): Int {
        val pending = dao.pending(userId)
        Log.d(TAG, "syncPending: ${pending.size} booking(s) waiting")
        var uploaded = 0
        pending.forEach { if (push(it.localId)) uploaded++ }
        return uploaded
    }

    /** Upload pending drafts, then pull bookings from Firestore into Room. */
    suspend fun refresh(userId: String, isAdmin: Boolean = false): Boolean {
        if (!isAdmin) {
            syncPending(userId)
        }
        return safeCall("refresh") {
            val token = bearer() ?: return@safeCall false
            val query = if (isAdmin) allBookingsQuery() else userBookingsQuery(userId)
            val results = api.queryBookings(projectId(), token, query)
            val remote = results.mapNotNull { it.document?.toBooking() }
            remote.forEach { incoming ->
                // Reuse the local row (if any) so the same remote booking is never duplicated.
                val existing = incoming.remoteId?.let { dao.findByRemoteId(it) }
                dao.upsert(incoming.copy(localId = existing?.localId ?: 0L))
            }
            Log.i(TAG, "Refresh complete: ${remote.size} booking(s) from Firestore (isAdmin=$isAdmin)")
            true
        }
    }

    /** Update booking fields on Firestore and locally. */
    suspend fun adminUpdateBooking(booking: Booking, nextStatus: za.co.quickfix.repairmate.data.local.RepairStatus, priority: String, notes: String): Boolean {
        val remoteId = booking.remoteId ?: return false
        return safeCall("adminUpdateBooking") {
            val token = bearer() ?: return@safeCall false
            val patchDoc = fullAdminPatch(nextStatus, priority, notes)
            api.updateBooking(projectId(), remoteId, token, listOf("status", "priority", "internalNotes"), patchDoc)
            dao.update(booking.copy(status = nextStatus, priority = priority, internalNotes = notes))
            Log.i(TAG, "Booking $remoteId updated by admin: status=$nextStatus, priority=$priority")
            true
        }
    }

    /** Demo helper: PATCH the booking to its next stage. Returns false if it could not be updated. */
    suspend fun advanceStatus(booking: Booking): Boolean {
        val next = booking.status.next() ?: return false
        val remoteId = booking.remoteId ?: return false
        return safeCall("advanceStatus") {
            val token = bearer() ?: return@safeCall false
            api.updateBooking(projectId(), remoteId, token, listOf("status"), statusPatch(next))
            dao.update(booking.copy(status = next))
            Log.i(TAG, "Booking $remoteId moved ${booking.status} -> $next")
            true
        }
    }

    private suspend fun push(localId: Long): Boolean {
        val booking = dao.getById(localId) ?: return false
        if (booking.syncState == SyncState.SYNCED) return true
        return safeCall("push booking $localId") {
            val token = bearer() ?: return@safeCall false
            val created = api.createBooking(projectId(), token, booking.toFsDocument())
            dao.update(booking.copy(remoteId = created.documentId(), syncState = SyncState.SYNCED))
            Log.i(TAG, "Uploaded booking $localId as ${created.documentId()}")
            true
        }
    }

    private suspend fun bearer(): String? = auth.idToken()?.let { "Bearer $it" }

    /** Runs a network step; any failure (offline, 403, timeout...) is logged and reported as false. */
    private suspend fun safeCall(what: String, block: suspend () -> Boolean): Boolean =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "$what failed: ${e.javaClass.simpleName}: ${e.message}")
            false
        }

    private companion object {
        const val TAG = "BookingRepository"
    }
}
