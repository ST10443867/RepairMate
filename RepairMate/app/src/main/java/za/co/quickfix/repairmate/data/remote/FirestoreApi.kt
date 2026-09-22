package za.co.quickfix.repairmate.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the Cloud Firestore REST API.
 * Every call sends the signed-in user's Firebase ID token as a Bearer token, so the
 * Firestore security rules (firestore.rules) can check request.auth.uid.
 *
 * Reference: https://firebase.google.com/docs/firestore/reference/rest
 */
interface FirestoreApi {

    /** Create a booking document with an auto-generated id. */
    @POST("v1/projects/{project}/databases/(default)/documents/bookings")
    suspend fun createBooking(
        @Path("project") project: String,
        @Header("Authorization") bearer: String,
        @Body body: FsDocument
    ): FsDocument

    /** Query the bookings collection (used to fetch only the current user's bookings). */
    @POST("v1/projects/{project}/databases/(default)/documents:runQuery")
    suspend fun queryBookings(
        @Path("project") project: String,
        @Header("Authorization") bearer: String,
        @Body body: RunQueryRequest
    ): List<FsQueryResult>

    /** Update only the fields named in [masks] (e.g. "status"). */
    @PATCH("v1/projects/{project}/databases/(default)/documents/bookings/{id}")
    suspend fun updateBooking(
        @Path("project") project: String,
        @Path("id") id: String,
        @Header("Authorization") bearer: String,
        @Query("updateMask.fieldPaths") masks: List<String>,
        @Body body: FsDocument
    ): FsDocument
}
