package za.co.quickfix.repairmate.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import za.co.quickfix.repairmate.data.auth.AppUser
import za.co.quickfix.repairmate.data.auth.AuthFailure
import za.co.quickfix.repairmate.data.auth.AuthReason
import za.co.quickfix.repairmate.domain.FieldError
import za.co.quickfix.repairmate.testutil.FakeAuthRepository
import za.co.quickfix.repairmate.testutil.MainDispatcherRule
import za.co.quickfix.repairmate.ui.auth.AuthViewModel

class AuthViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repo = FakeAuthRepository()
    private val vm = AuthViewModel(repo)

    @Test fun starts_signed_out_when_no_session_exists() = assertNull(vm.state.value.user)

    @Test fun restores_existing_session() {
        repo.user = AppUser("u1", "Lesego", "l@example.com")
        assertEquals("u1", AuthViewModel(repo).state.value.user?.uid)
    }

    @Test fun login_with_bad_email_is_blocked_before_hitting_the_network() {
        vm.login("not-an-email", "secret123")
        assertEquals(FieldError.INVALID_EMAIL, vm.state.value.emailError)
        assertEquals(0, repo.loginCalls)
    }

    @Test fun login_with_empty_password_shows_required() {
        vm.login("a@b.com", "")
        assertEquals(FieldError.REQUIRED, vm.state.value.passwordError)
        assertEquals(0, repo.loginCalls)
    }

    @Test fun login_success_sets_user() {
        vm.login("a@b.com", "secret123")
        assertNotNull(vm.state.value.user)
        assertFalse(vm.state.value.loading)
        assertEquals(1, repo.loginCalls)
    }

    @Test fun login_failure_exposes_reason_and_clears_loading() {
        repo.nextResult = Result.failure(AuthFailure(AuthReason.INVALID_CREDENTIALS))
        vm.login("a@b.com", "wrongpass1")
        assertEquals(AuthReason.INVALID_CREDENTIALS, vm.state.value.failure)
        assertNull(vm.state.value.user)
        assertFalse(vm.state.value.loading)
    }

    @Test fun unexpected_exception_maps_to_unknown_failure() {
        repo.nextResult = Result.failure(IllegalStateException("boom"))
        vm.login("a@b.com", "secret123")
        assertEquals(AuthReason.UNKNOWN, vm.state.value.failure)
    }

    @Test fun register_reports_every_invalid_field_at_once() {
        vm.register(name = "", email = "bad", password = "short", confirm = "different")
        val s = vm.state.value
        assertEquals(FieldError.REQUIRED, s.nameError)
        assertEquals(FieldError.INVALID_EMAIL, s.emailError)
        assertEquals(FieldError.PASSWORD_TOO_SHORT, s.passwordError)
        assertEquals(FieldError.PASSWORDS_DONT_MATCH, s.confirmError)
        assertEquals(0, repo.registerCalls)
    }

    @Test fun register_success_signs_the_user_in() {
        vm.register("Lesego", "l@example.com", "repair2026", "repair2026")
        assertEquals("Lesego", vm.state.value.user?.name)
        assertEquals(1, repo.registerCalls)
    }

    @Test fun logout_clears_user() {
        vm.login("a@b.com", "secret123")
        vm.logout()
        assertNull(vm.state.value.user)
        assertEquals(true, repo.loggedOut)
    }

    @Test fun clear_errors_resets_field_and_failure_state() {
        vm.login("bad", "")
        vm.clearErrors()
        assertNull(vm.state.value.emailError)
        assertNull(vm.state.value.passwordError)
        assertNull(vm.state.value.failure)
    }
}
