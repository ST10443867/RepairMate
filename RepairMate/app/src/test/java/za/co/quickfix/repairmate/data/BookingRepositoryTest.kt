package za.co.quickfix.repairmate.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import za.co.quickfix.repairmate.data.auth.AppUser
import za.co.quickfix.repairmate.data.local.Booking
import za.co.quickfix.repairmate.data.local.RepairStatus
import za.co.quickfix.repairmate.data.local.SyncState
import za.co.quickfix.repairmate.data.remote.FsQueryResult
import za.co.quickfix.repairmate.data.remote.FsValue
import za.co.quickfix.repairmate.data.repo.BookingRepository
import za.co.quickfix.repairmate.data.repo.CreateOutcome
import za.co.quickfix.repairmate.testutil.FakeAuthRepository
import za.co.quickfix.repairmate.testutil.FakeBookingDao
import za.co.quickfix.repairmate.testutil.FakeFirestoreApi
import java.io.IOException

class BookingRepositoryTest {

    private val dao = FakeBookingDao()
    private val api = FakeFirestoreApi()
    private val auth = FakeAuthRepository().apply { user = AppUser("u1", "Test", "t@example.com") }
    private val repo = BookingRepository(dao, api, auth) { "test-project" }

    private fun draft(model: String = "XPS 13") = Booking(
        userId = "u1", deviceBrand = "Dell", deviceModel = model,
        issue = "Screen flickers on boot", preferredDate = "2030-01-01"
    )

    @Test fun create_online_uploads_and_marks_synced() = runTest {
        assertEquals(CreateOutcome.SYNCED, repo.createBooking(draft()))
        val saved = dao.all().single()
        assertEquals(SyncState.SYNCED, saved.syncState)
        assertEquals("doc1", saved.remoteId)
        assertEquals(1, api.created.size)
    }

    @Test fun create_offline_keeps_booking_as_pending_draft() = runTest {
        api.failWith = IOException("no network")
        assertEquals(CreateOutcome.SAVED_OFFLINE, repo.createBooking(draft()))
        val saved = dao.all().single()
        assertEquals(SyncState.PENDING, saved.syncState)
        assertNull(saved.remoteId)
    }

    @Test fun signed_out_user_cannot_upload_so_booking_stays_pending() = runTest {
        auth.user = null // idToken() returns null
        assertEquals(CreateOutcome.SAVED_OFFLINE, repo.createBooking(draft()))
        assertEquals(0, api.created.size)
    }

    @Test fun sync_pending_uploads_all_queued_drafts_once_back_online() = runTest {
        api.failWith = IOException("offline")
        repo.createBooking(draft("A"))
        repo.createBooking(draft("B"))
        api.failWith = null

        assertEquals(2, repo.syncPending("u1"))
        assertTrue(dao.all().all { it.syncState == SyncState.SYNCED })
        assertEquals(2, api.created.size)
        // Running again uploads nothing (no duplicates)
        assertEquals(0, repo.syncPending("u1"))
    }

    @Test fun refresh_updates_existing_row_instead_of_duplicating() = runTest {
        repo.createBooking(draft())
        val uploaded = api.created.single()
        val remoteDoc = uploaded.copy(
            name = "projects/test-project/databases/(default)/documents/bookings/doc1",
            fields = uploaded.fields!! + ("status" to FsValue(stringValue = "IN_PROGRESS"))
        )
        api.queryResults = listOf(FsQueryResult(document = remoteDoc), FsQueryResult(document = null))

        assertTrue(repo.refresh("u1"))
        val rows = dao.all()
        assertEquals(1, rows.size)
        assertEquals(RepairStatus.IN_PROGRESS, rows.single().status)
    }

    @Test fun refresh_returns_false_when_offline_but_keeps_local_data() = runTest {
        repo.createBooking(draft())
        api.failWith = IOException("offline")
        assertFalse(repo.refresh("u1"))
        assertEquals(1, dao.all().size)
    }

    @Test fun advance_status_patches_remote_and_updates_local() = runTest {
        repo.createBooking(draft())
        val booking = dao.all().single()

        assertTrue(repo.advanceStatus(booking))
        assertEquals(RepairStatus.DIAGNOSING, dao.all().single().status)
        assertEquals("status", api.lastMasks?.single())
        assertEquals("DIAGNOSING", api.lastPatch!!.fields!!["status"]!!.stringValue)
    }

    @Test fun advance_status_does_nothing_for_unsynced_or_finished_bookings() = runTest {
        api.failWith = IOException("offline")
        repo.createBooking(draft())
        assertFalse(repo.advanceStatus(dao.all().single())) // no remoteId yet

        val finished = draft().copy(localId = 99, remoteId = "x", status = RepairStatus.COLLECTED)
        assertFalse(repo.advanceStatus(finished))
    }
}
