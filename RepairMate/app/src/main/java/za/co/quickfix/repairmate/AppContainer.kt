package za.co.quickfix.repairmate

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import za.co.quickfix.repairmate.data.auth.AuthRepository
import za.co.quickfix.repairmate.data.auth.FirebaseAuthRepository
import za.co.quickfix.repairmate.data.local.AppDatabase
import za.co.quickfix.repairmate.data.remote.FirestoreApi
import za.co.quickfix.repairmate.data.repo.BookingRepository
import za.co.quickfix.repairmate.data.settings.SettingsRepository

/** Creates and holds the app's long-lived objects. */
class AppContainer(app: Application) {

    val settingsRepository = SettingsRepository(app)

    val authRepository: AuthRepository = FirebaseAuthRepository(FirebaseAuth.getInstance())

    private val database = AppDatabase.create(app)

    private val api: FirestoreApi by lazy {
        val logging = HttpLoggingInterceptor { Log.d("HTTP", it) }.apply {
            // Full request/response bodies are only logged in debug builds; the token header is always hidden.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }
        Retrofit.Builder()
            .baseUrl("https://firestore.googleapis.com/")
            .client(OkHttpClient.Builder().addInterceptor(logging).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FirestoreApi::class.java)
    }

    val bookingRepository: BookingRepository by lazy {
        BookingRepository(
            dao = database.bookingDao(),
            api = api,
            auth = authRepository,
            // Read from google-services.json at runtime, so no project id is hard-coded.
            projectId = { FirebaseApp.getInstance().options.projectId.orEmpty() }
        )
    }
}
