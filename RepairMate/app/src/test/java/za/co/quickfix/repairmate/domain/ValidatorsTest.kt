package za.co.quickfix.repairmate.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ValidatorsTest {

    // --- email ---
    @Test fun email_valid_addresses_pass() {
        assertNull(Validators.email("lesego@example.com"))
        assertNull(Validators.email("  first.last+tag@sub.domain.co.za  "))
    }

    @Test fun email_blank_is_required() = assertEquals(FieldError.REQUIRED, Validators.email("   "))

    @Test fun email_malformed_is_rejected() {
        assertEquals(FieldError.INVALID_EMAIL, Validators.email("no-at-sign.com"))
        assertEquals(FieldError.INVALID_EMAIL, Validators.email("a@b"))
        assertEquals(FieldError.INVALID_EMAIL, Validators.email("a@.com"))
    }

    // --- password ---
    @Test fun password_empty_is_required() = assertEquals(FieldError.REQUIRED, Validators.password(""))

    @Test fun password_too_short() = assertEquals(FieldError.PASSWORD_TOO_SHORT, Validators.password("abc123"))

    @Test fun password_needs_letter_and_digit() {
        assertEquals(FieldError.PASSWORD_WEAK, Validators.password("abcdefgh"))
        assertEquals(FieldError.PASSWORD_WEAK, Validators.password("12345678"))
    }

    @Test fun password_strong_passes() = assertNull(Validators.password("repair2026"))

    @Test fun confirm_password_must_match() {
        assertEquals(FieldError.PASSWORDS_DONT_MATCH, Validators.confirmPassword("abc12345", "abc12346"))
        assertNull(Validators.confirmPassword("abc12345", "abc12345"))
        assertEquals(FieldError.REQUIRED, Validators.confirmPassword("abc12345", ""))
    }

    // --- name / required / issue ---
    @Test fun name_rules() {
        assertEquals(FieldError.REQUIRED, Validators.name(" "))
        assertEquals(FieldError.TEXT_TOO_SHORT, Validators.name("A"))
        assertNull(Validators.name("Lesego"))
    }

    @Test fun issue_needs_enough_detail() {
        assertEquals(FieldError.REQUIRED, Validators.issueDescription(""))
        assertEquals(FieldError.TEXT_TOO_SHORT, Validators.issueDescription("broken"))
        assertNull(Validators.issueDescription("Screen flickers when charging"))
    }

    // --- booking date ---
    private val today = LocalDate.of(2026, 10, 5)

    @Test fun date_missing_is_required() {
        assertEquals(FieldError.REQUIRED, Validators.bookingDate(null, today))
        assertEquals(FieldError.REQUIRED, Validators.bookingDate("", today))
    }

    @Test fun date_garbage_is_invalid() {
        assertEquals(FieldError.INVALID_DATE, Validators.bookingDate("05/10/2026", today))
        assertEquals(FieldError.INVALID_DATE, Validators.bookingDate("2026-13-40", today))
    }

    @Test fun date_in_past_is_rejected_but_today_and_future_pass() {
        assertEquals(FieldError.DATE_IN_PAST, Validators.bookingDate("2026-10-04", today))
        assertNull(Validators.bookingDate("2026-10-05", today))
        assertNull(Validators.bookingDate("2026-12-01", today))
    }
}
