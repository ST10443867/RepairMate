package za.co.quickfix.repairmate.data.auth

data class AppUser(val uid: String, val name: String, val email: String) {
    val isAdmin: Boolean get() = email.endsWith("@quickfix.co.za")
}

/** Why an auth call failed. The UI maps these to localised messages. */
enum class AuthReason { INVALID_CREDENTIALS, EMAIL_IN_USE, WEAK_PASSWORD, NETWORK, UNKNOWN }

class AuthFailure(val reason: AuthReason, cause: Throwable? = null) : Exception(reason.name, cause)

/** Abstraction over the auth provider so ViewModels can be unit-tested with a fake. */
interface AuthRepository {
    val currentUser: AppUser?
    suspend fun register(name: String, email: String, password: String): Result<AppUser>
    suspend fun login(email: String, password: String): Result<AppUser>
    fun logout()

    /** A fresh-enough Firebase ID token to send as a Bearer token to the Firestore REST API. */
    suspend fun idToken(): String?
}
