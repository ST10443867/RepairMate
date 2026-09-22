package za.co.quickfix.repairmate.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import za.co.quickfix.repairmate.data.local.Booking
import za.co.quickfix.repairmate.data.local.RepairStatus
import za.co.quickfix.repairmate.data.local.SyncState
import za.co.quickfix.repairmate.data.remote.FsDocument
import za.co.quickfix.repairmate.data.remote.FsValue
import za.co.quickfix.repairmate.data.remote.documentId
import za.co.quickfix.repairmate.data.remote.statusPatch
import za.co.quickfix.repairmate.data.remote.toBooking
import za.co.quickfix.repairmate.data.remote.toFsDocument
import za.co.quickfix.repairmate.data.remote.userBookingsQuery

class FirestoreMapperTest {

    private val booking = Booking(
        userId = "user-1", deviceBrand = "Dell", deviceModel = "XPS 13",
        issue = "Screen flickers", preferredDate = "2026-11-02",
        status = RepairStatus.DIAGNOSING, createdAt = 1_700_000_000_000L
    )

    @Test fun booking_round_trips_through_firestore_document() {
        val doc = booking.toFsDocument().copy(name = "projects/p/databases/(default)/documents/bookings/abc123")
        val back = doc.toBooking()!!
        assertEquals("abc123", back.remoteId)
        assertEquals("user-1", back.userId)
        assertEquals("Dell", back.deviceBrand)
        assertEquals("XPS 13", back.deviceModel)
        assertEquals(RepairStatus.DIAGNOSING, back.status)
        assertEquals(1_700_000_000_000L, back.createdAt)
        assertEquals(SyncState.SYNCED, back.syncState)
    }

    @Test fun integers_are_sent_as_strings_as_firestore_requires() {
        assertEquals("1700000000000", booking.toFsDocument().fields!!["createdAt"]!!.integerValue)
    }

    @Test fun document_without_user_id_is_ignored() {
        assertNull(FsDocument(fields = mapOf("deviceBrand" to FsValue(stringValue = "HP"))).toBooking())
        assertNull(FsDocument(fields = null).toBooking())
    }

    @Test fun document_id_is_last_path_segment() {
        assertEquals("xyz", FsDocument(name = "a/b/c/xyz").documentId())
        assertNull(FsDocument(name = null).documentId())
    }

    @Test fun status_patch_only_contains_status() {
        val patch = statusPatch(RepairStatus.READY)
        assertEquals(setOf("status"), patch.fields!!.keys)
        assertEquals("READY", patch.fields!!["status"]!!.stringValue)
    }

    @Test fun query_filters_on_user_id() {
        val filter = userBookingsQuery("user-9").structuredQuery.where!!.fieldFilter
        assertEquals("userId", filter.field.fieldPath)
        assertEquals("EQUAL", filter.op)
        assertEquals("user-9", filter.value.stringValue)
    }
}
