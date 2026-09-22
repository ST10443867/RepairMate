package za.co.quickfix.repairmate.data.auth

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Firebase Authentication (email + password).
 *
 * Password security: the password is sent over TLS and Firebase hashes it server-side with a
 * salted, memory-hard scrypt variant. The app never stores or logs the password.
 */
class FirebaseAuthRepository(private val auth: FirebaseAuth) : AuthRepository {

    override val currentUser: AppUser?
        get() = auth.currentUser?.toAppUser()

    override suspend fun register(name: String, email: String, password: String): Result<AppUser> =
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw AuthFailure(AuthReason.UNKNOWN)
            // Store the display name on the Firebase profile
            val profile = UserProfileChangeRequest.Builder().setDisplayName(name).build()
            user.updateProfile(profile).await()
            Log.i(TAG, "Registered new user uid=${user.uid}")
            Result.success(AppUser(user.uid, name, email))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Registration failed", e)
            Result.failure(e.toAuthFailure())
        }

    override suspend fun login(email: String, password: String): Result<AppUser> =
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw AuthFailure(AuthReason.UNKNOWN)
            Log.i(TAG, "Login OK uid=${user.uid}")
            Result.success(user.toAppUser())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Login failed", e)
            Result.failure(e.toAuthFailure())
        }

    override fun logout() {
        Log.i(TAG, "Logging out")
        auth.signOut()
    }

    override suspend fun idToken(): String? = try {
        // forceRefresh = false: Firebase refreshes automatically when the token is close to expiry.
        auth.currentUser?.getIdToken(false)?.await()?.token
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Could not get ID token", e)
        null
    }

    private fun FirebaseUser.toAppUser() = AppUser(
        uid = uid,
        name = displayName?.takeIf { it.isNotBlank() } ?: email?.substringBefore('@').orEmpty(),
        email = email.orEmpty()
    )

    private fun Exception.toAuthFailure(): AuthFailure {
        val reason = when (this) {
            is AuthFailure -> return this
            is FirebaseAuthUserCollisionException -> AuthReason.EMAIL_IN_USE
            is FirebaseAuthWeakPasswordException -> AuthReason.WEAK_PASSWORD
            is FirebaseAuthInvalidUserException,
            is FirebaseAuthInvalidCredentialsException -> AuthReason.INVALID_CREDENTIALS
            is FirebaseNetworkException -> AuthReason.NETWORK
            else -> AuthReason.UNKNOWN
        }
        return AuthFailure(reason, this)
    }

    private companion object {
        const val TAG = "FirebaseAuthRepo"
    }
}
