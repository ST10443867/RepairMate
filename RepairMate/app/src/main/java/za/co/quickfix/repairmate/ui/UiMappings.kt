package za.co.quickfix.repairmate.ui

import androidx.annotation.StringRes
import za.co.quickfix.repairmate.R
import za.co.quickfix.repairmate.data.auth.AuthReason
import za.co.quickfix.repairmate.data.local.RepairStatus
import za.co.quickfix.repairmate.domain.FieldError
import za.co.quickfix.repairmate.ui.booking.UiMessage

/** Maps domain enums to string resources so every message is localised (English / isiZulu). */

@StringRes
fun FieldError.resId(): Int = when (this) {
    FieldError.REQUIRED -> R.string.err_required
    FieldError.INVALID_EMAIL -> R.string.err_invalid_email
    FieldError.PASSWORD_TOO_SHORT -> R.string.err_password_short
    FieldError.PASSWORD_WEAK -> R.string.err_password_weak
    FieldError.PASSWORDS_DONT_MATCH -> R.string.err_passwords_mismatch
    FieldError.TEXT_TOO_SHORT -> R.string.err_text_short
    FieldError.INVALID_DATE -> R.string.err_invalid_date
    FieldError.DATE_IN_PAST -> R.string.err_date_past
}

@StringRes
fun AuthReason.resId(): Int = when (this) {
    AuthReason.INVALID_CREDENTIALS -> R.string.auth_err_invalid_credentials
    AuthReason.EMAIL_IN_USE -> R.string.auth_err_email_in_use
    AuthReason.WEAK_PASSWORD -> R.string.auth_err_weak_password
    AuthReason.NETWORK -> R.string.auth_err_network
    AuthReason.UNKNOWN -> R.string.auth_err_unknown
}

@StringRes
fun RepairStatus.labelRes(): Int = when (this) {
    RepairStatus.RECEIVED -> R.string.status_received
    RepairStatus.DIAGNOSING -> R.string.status_diagnosing
    RepairStatus.IN_PROGRESS -> R.string.status_in_progress
    RepairStatus.READY -> R.string.status_ready
    RepairStatus.COLLECTED -> R.string.status_collected
}

@StringRes
fun UiMessage.textRes(): Int = when (this) {
    UiMessage.SAVED_ONLINE -> R.string.msg_saved_online
    UiMessage.SAVED_OFFLINE -> R.string.msg_saved_offline
    UiMessage.REFRESH_FAILED -> R.string.msg_refresh_failed
    UiMessage.STATUS_UPDATED -> R.string.msg_status_updated
    UiMessage.STATUS_FAILED -> R.string.msg_status_failed
}
