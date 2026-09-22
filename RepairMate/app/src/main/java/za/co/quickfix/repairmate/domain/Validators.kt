package za.co.quickfix.repairmate.domain

import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Validation problems. The UI maps each to a localised message, so the logic stays testable. */
enum class FieldError {
    REQUIRED,
    INVALID_EMAIL,
    PASSWORD_TOO_SHORT,
    PASSWORD_WEAK,
    PASSWORDS_DONT_MATCH,
    TEXT_TOO_SHORT,
    INVALID_DATE,
    DATE_IN_PAST
}

/** Pure input validation. Every function returns null when the value is valid. */
object Validators {
    // Own regex (android.util.Patterns is not available in plain JVM unit tests).
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    const val MIN_PASSWORD_LENGTH = 8
    const val MIN_ISSUE_LENGTH = 10

    fun name(value: String): FieldError? = when {
        value.isBlank() -> FieldError.REQUIRED
        value.trim().length < 2 -> FieldError.TEXT_TOO_SHORT
        else -> null
    }

    fun email(value: String): FieldError? = when {
        value.isBlank() -> FieldError.REQUIRED
        !EMAIL.matches(value.trim()) -> FieldError.INVALID_EMAIL
        else -> null
    }

    fun password(value: String): FieldError? = when {
        value.isEmpty() -> FieldError.REQUIRED
        value.length < MIN_PASSWORD_LENGTH -> FieldError.PASSWORD_TOO_SHORT
        !(value.any { it.isLetter() } && value.any { it.isDigit() }) -> FieldError.PASSWORD_WEAK
        else -> null
    }

    fun confirmPassword(password: String, confirm: String): FieldError? = when {
        confirm.isEmpty() -> FieldError.REQUIRED
        password != confirm -> FieldError.PASSWORDS_DONT_MATCH
        else -> null
    }

    fun required(value: String): FieldError? =
        if (value.isBlank()) FieldError.REQUIRED else null

    fun issueDescription(value: String): FieldError? = when {
        value.isBlank() -> FieldError.REQUIRED
        value.trim().length < MIN_ISSUE_LENGTH -> FieldError.TEXT_TOO_SHORT
        else -> null
    }

    /** [iso] must be yyyy-MM-dd and not before [today]. */
    fun bookingDate(iso: String?, today: LocalDate = LocalDate.now()): FieldError? {
        if (iso.isNullOrBlank()) return FieldError.REQUIRED
        val date = try {
            LocalDate.parse(iso)
        } catch (e: DateTimeParseException) {
            return FieldError.INVALID_DATE
        }
        return if (date.isBefore(today)) FieldError.DATE_IN_PAST else null
    }
}
