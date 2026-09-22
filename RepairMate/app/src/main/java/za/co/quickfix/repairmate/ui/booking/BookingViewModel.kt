package za.co.quickfix.repairmate.ui.booking

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.co.quickfix.repairmate.data.local.Booking
import za.co.quickfix.repairmate.data.repo.BookingRepository
import za.co.quickfix.repairmate.data.repo.CreateOutcome
import za.co.quickfix.repairmate.domain.FieldError
import za.co.quickfix.repairmate.domain.Validators

data class BookingFormErrors(
    val brand: FieldError? = null,
    val model: FieldError? = null,
    val issue: FieldError? = null,
    val date: FieldError? = null
) {
    val hasAny: Boolean get() = listOf(brand, model, issue, date).any { it != null }
}

data class BookingFormState(
    val errors: BookingFormErrors = BookingFormErrors(),
    val saving: Boolean = false,
    val outcome: CreateOutcome? = null
)

/** One-off messages shown as snackbars. */
enum class UiMessage { SAVED_ONLINE, SAVED_OFFLINE, REFRESH_FAILED, STATUS_UPDATED, STATUS_FAILED }

/** Scoped to one signed-in user (see RepairMateRoot, which keys the ViewModel by uid). */
class BookingViewModel(
    private val repo: BookingRepository,
    private val userId: String,
    private val isAdmin: Boolean = false
) : ViewModel() {

    /** Room is the source of truth, so the list works offline. */
    val bookings: StateFlow<List<Booking>> = (if (isAdmin) repo.observeAll() else repo.observe(userId))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _form = MutableStateFlow(BookingFormState())
    val form: StateFlow<BookingFormState> = _form.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    init {
        refresh(silent = true)
    }

    fun booking(localId: Long): Flow<Booking?> = repo.observeOne(localId)

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            _refreshing.value = true
            val ok = repo.refresh(userId, isAdmin)
            _refreshing.value = false
            if (!ok && !silent) _messages.send(UiMessage.REFRESH_FAILED)
        }
    }

    /** [dateIso] is yyyy-MM-dd from the date picker (null if the user has not picked one). */
    fun submit(brand: String, model: String, issue: String, dateIso: String?) {
        if (isAdmin) {
            Log.e(TAG, "Admins cannot book repairs")
            return
        }
        val errors = BookingFormErrors(
            brand = Validators.required(brand),
            model = Validators.required(model),
            issue = Validators.issueDescription(issue),
            date = Validators.bookingDate(dateIso)
        )
        if (errors.hasAny) {
            Log.d(TAG, "Booking blocked by validation")
            _form.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            _form.update { it.copy(errors = BookingFormErrors(), saving = true) }
            val outcome = repo.createBooking(
                Booking(
                    userId = userId,
                    deviceBrand = brand.trim(),
                    deviceModel = model.trim(),
                    issue = issue.trim(),
                    preferredDate = dateIso.orEmpty()
                )
            )
            Log.i(TAG, "Booking created, outcome=$outcome")
            _messages.send(if (outcome == CreateOutcome.SYNCED) UiMessage.SAVED_ONLINE else UiMessage.SAVED_OFFLINE)
            _form.update { it.copy(saving = false, outcome = outcome) }
        }
    }

    fun consumeOutcome() = _form.update { it.copy(outcome = null) }

    fun clearFormErrors() = _form.update { BookingFormState() }

    /** Simulates a technician moving the repair to its next stage (client/demo). */
    fun advance(booking: Booking) {
        viewModelScope.launch {
            val ok = repo.advanceStatus(booking)
            _messages.send(if (ok) UiMessage.STATUS_UPDATED else UiMessage.STATUS_FAILED)
        }
    }

    /** Admin-only: Updates full repair details. */
    fun updateBookingAdmin(booking: Booking, status: za.co.quickfix.repairmate.data.local.RepairStatus, priority: String, notes: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            _form.update { it.copy(saving = true) }
            val ok = repo.adminUpdateBooking(booking, status, priority, notes)
            _messages.send(if (ok) UiMessage.STATUS_UPDATED else UiMessage.STATUS_FAILED)
            _form.update { it.copy(saving = false) }
        }
    }

    private companion object {
        const val TAG = "BookingViewModel"
    }
}
