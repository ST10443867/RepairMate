package za.co.quickfix.repairmate.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The stages a repair moves through at QuickFix (from the Part 1 research:
 * intake -> diagnosis -> repair -> ready -> collected).
 */
enum class RepairStatus {
    RECEIVED, DIAGNOSING, IN_PROGRESS, READY, COLLECTED;

    /** The next stage, or null when the repair is already collected. */
    fun next(): RepairStatus? = entries.getOrNull(ordinal + 1)

    companion object {
        /** Unknown or missing values fall back to RECEIVED so a bad document never crashes the app. */
        fun fromName(name: String?): RepairStatus = entries.firstOrNull { it.name == name } ?: RECEIVED
    }
}

/** PENDING = saved on the phone only (offline draft). SYNCED = also stored in Firestore. */
enum class SyncState { PENDING, SYNCED }

/**
 * A repair booking. Doubles as the Room entity (offline cache + drafts) and the app's domain model.
 * [localId] is the Room key; [remoteId] is the Firestore document id once uploaded.
 */
@Entity(
    tableName = "bookings",
    indices = [Index("remoteId"), Index("userId")]
)
data class Booking(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val remoteId: String? = null,
    val userId: String,
    val deviceBrand: String,
    val deviceModel: String,
    val issue: String,
    /** ISO date, e.g. 2026-10-05 */
    val preferredDate: String,
    val status: RepairStatus = RepairStatus.RECEIVED,
    val priority: String = "Medium",
    val internalNotes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncState: SyncState = SyncState.PENDING
)
