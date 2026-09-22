package za.co.quickfix.repairmate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY createdAt DESC")
    fun observeAllBookings(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeForUser(userId: String): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE localId = :localId")
    fun observeById(localId: Long): Flow<Booking?>

    @Query("SELECT * FROM bookings WHERE localId = :localId")
    suspend fun getById(localId: Long): Booking?

    @Query("SELECT * FROM bookings WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): Booking?

    @Query("SELECT * FROM bookings WHERE userId = :userId AND syncState = 'PENDING'")
    suspend fun pending(userId: String): List<Booking>

    /** Insert or replace (a non-zero localId replaces the existing row). Returns the row id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(booking: Booking): Long

    @Update
    suspend fun update(booking: Booking)
}
