package za.co.quickfix.repairmate.data.remote

import za.co.quickfix.repairmate.data.local.Booking
import za.co.quickfix.repairmate.data.local.RepairStatus
import za.co.quickfix.repairmate.data.local.SyncState

/** Converts between the app's [Booking] and Firestore's typed-field JSON. */

fun Booking.toFsDocument(): FsDocument = FsDocument(
    fields = mapOf(
        "userId" to FsValue(stringValue = userId),
        "deviceBrand" to FsValue(stringValue = deviceBrand),
        "deviceModel" to FsValue(stringValue = deviceModel),
        "issue" to FsValue(stringValue = issue),
        "preferredDate" to FsValue(stringValue = preferredDate),
        "status" to FsValue(stringValue = status.name),
        "priority" to FsValue(stringValue = priority),
        "internalNotes" to FsValue(stringValue = internalNotes),
        "createdAt" to FsValue(integerValue = createdAt.toString())
    )
)

/** The document id is the last segment of the resource path in [FsDocument.name]. */
fun FsDocument.documentId(): String? = name?.substringAfterLast('/')?.takeIf { it.isNotBlank() }

/** Returns null for documents that do not look like a booking (e.g. missing userId). */
fun FsDocument.toBooking(): Booking? {
    val f = fields ?: return null
    val userId = f["userId"]?.stringValue ?: return null
    return Booking(
        remoteId = documentId(),
        userId = userId,
        deviceBrand = f["deviceBrand"]?.stringValue.orEmpty(),
        deviceModel = f["deviceModel"]?.stringValue.orEmpty(),
        issue = f["issue"]?.stringValue.orEmpty(),
        preferredDate = f["preferredDate"]?.stringValue.orEmpty(),
        status = RepairStatus.fromName(f["status"]?.stringValue),
        priority = f["priority"]?.stringValue ?: "Medium",
        internalNotes = f["internalNotes"]?.stringValue.orEmpty(),
        createdAt = f["createdAt"]?.integerValue?.toLongOrNull() ?: 0L,
        syncState = SyncState.SYNCED
    )
}

/** Body for a PATCH that changes status, priority, and internalNotes fields. */
fun fullAdminPatch(status: RepairStatus, priority: String, internalNotes: String): FsDocument =
    FsDocument(
        fields = mapOf(
            "status" to FsValue(stringValue = status.name),
            "priority" to FsValue(stringValue = priority),
            "internalNotes" to FsValue(stringValue = internalNotes)
        )
    )

/** Body for a PATCH that changes only the status field. */
fun statusPatch(status: RepairStatus): FsDocument =
    FsDocument(fields = mapOf("status" to FsValue(stringValue = status.name)))

/** Structured query: SELECT * FROM bookings. */
fun allBookingsQuery(): RunQueryRequest = RunQueryRequest(
    StructuredQuery(
        from = listOf(CollectionSelector("bookings"))
    )
)

/** Structured query: SELECT * FROM bookings WHERE userId == [userId]. */
fun userBookingsQuery(userId: String): RunQueryRequest = RunQueryRequest(
    StructuredQuery(
        from = listOf(CollectionSelector("bookings")),
        where = QueryFilter(
            FieldFilter(
                field = FieldReference("userId"),
                op = "EQUAL",
                value = FsValue(stringValue = userId)
            )
        )
    )
)
