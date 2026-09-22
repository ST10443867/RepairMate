package za.co.quickfix.repairmate.testutil

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import za.co.quickfix.repairmate.data.auth.AppUser
import za.co.quickfix.repairmate.data.auth.AuthRepository
import za.co.quickfix.repairmate.data.local.Booking
import za.co.quickfix.repairmate.data.local.BookingDao
import za.co.quickfix.repairmate.data.local.SyncState
import za.co.quickfix.repairmate.data.remote.FirestoreApi
import za.co.quickfix.repairmate.data.remote.FsDocument
import za.co.quickfix.repairmate.data.remote.FsQueryResult
import za.co.quickfix.repairmate.data.remote.FsValue
import za.co.quickfix.repairmate.data.remote.RunQueryRequest

/** Replaces Dispatchers.Main so ViewModels using viewModelScope can run in plain JVM tests. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** In-memory stand-in for the Room DAO. */
class FakeBookingDao : BookingDao {
    private val items = MutableStateFlow<List<Booking>>(emptyList())
    private var nextId = 1L

    fun all(): List<Booking> = items.value

    override fun observeAllBookings(): Flow<List<Booking>> =
        items.map { list -> list.sortedByDescending { it.createdAt } }

    override fun observeForUser(userId: String): Flow<List<Booking>> =
        items.map { list -> list.filter { it.userId == userId }.sortedByDescending { it.createdAt } }

    override fun observeById(localId: Long): Flow<Booking?> =
        items.map { list -> list.firstOrNull { it.localId == localId } }

    override suspend fun getById(localId: Long): Booking? = items.value.firstOrNull { it.localId == localId }

    override suspend fun findByRemoteId(remoteId: String): Booking? =
        items.value.firstOrNull { it.remoteId == remoteId }

    override suspend fun pending(userId: String): List<Booking> =
        items.value.filter { it.userId == userId && it.syncState == SyncState.PENDING }

    override suspend fun upsert(booking: Booking): Long {
        val id = if (booking.localId == 0L) nextId++ else booking.localId
        items.value = items.value.filterNot { it.localId == id } + booking.copy(localId = id)
        return id
    }

    override suspend fun update(booking: Booking) {
        items.value = items.value.map { if (it.localId == booking.localId) booking else it }
    }
}

/** Fake Firestore REST API. Set [failWith] to simulate being offline. */
class FakeFirestoreApi : FirestoreApi {
    var failWith: Exception? = null
    val created = mutableListOf<FsDocument>()
    var queryResults: List<FsQueryResult> = emptyList()
    var lastMasks: List<String>? = null
    var lastPatch: FsDocument? = null

    override suspend fun createBooking(project: String, bearer: String, body: FsDocument): FsDocument {
        failWith?.let { throw it }
        created += body
        return body.copy(name = "projects/$project/databases/(default)/documents/bookings/doc${created.size}")
    }

    override suspend fun queryBookings(project: String, bearer: String, body: RunQueryRequest): List<FsQueryResult> {
        failWith?.let { throw it }
        return queryResults
    }

    override suspend fun updateBooking(
        project: String, id: String, bearer: String, masks: List<String>, body: FsDocument
    ): FsDocument {
        failWith?.let { throw it }
        lastMasks = masks
        lastPatch = body
        return body
    }
}

/** Fake auth provider with controllable results. */
class FakeAuthRepository : AuthRepository {
    var user: AppUser? = null
    var nextResult: Result<AppUser>? = null
    var loginCalls = 0
    var registerCalls = 0
    var loggedOut = false

    override val currentUser: AppUser? get() = user

    override suspend fun register(name: String, email: String, password: String): Result<AppUser> {
        registerCalls++
        return nextResult ?: Result.success(AppUser("uid-1", name, email))
    }

    override suspend fun login(email: String, password: String): Result<AppUser> {
        loginCalls++
        return nextResult ?: Result.success(AppUser("uid-1", "Test User", email))
    }

    override fun logout() {
        loggedOut = true
        user = null
    }

    override suspend fun idToken(): String? = if (user != null) "token" else null
}

/** Handy helper for building Firestore string values in tests. */
fun fsString(value: String) = FsValue(stringValue = value)
