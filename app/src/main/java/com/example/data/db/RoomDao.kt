package com.example.data.db

import androidx.room.*
import com.example.data.models.DiagnosticEntity
import com.example.data.models.RoomEntity
import com.example.data.models.ZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms ORDER BY floor ASC, name ASC")
    fun getAllRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE id = :id")
    fun getRoomById(id: Long): Flow<RoomEntity?>

    @Query("SELECT * FROM rooms WHERE id = :id")
    suspend fun getRoomByIdSync(id: Long): RoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity): Long

    @Update
    suspend fun updateRoom(room: RoomEntity)

    @Query("DELETE FROM rooms WHERE id = :id")
    suspend fun deleteRoom(id: Long)

    @Query("SELECT * FROM zones WHERE roomId = :roomId ORDER BY name ASC")
    fun getZonesForRoom(roomId: Long): Flow<List<ZoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZone(zone: ZoneEntity): Long

    @Query("DELETE FROM zones WHERE id = :id")
    suspend fun deleteZone(id: Long)

    @Query("SELECT * FROM diagnostics WHERE roomId = :roomId ORDER BY timestamp DESC")
    fun getDiagnosticsForRoom(roomId: Long): Flow<List<DiagnosticEntity>>

    @Query("SELECT COUNT(*) FROM diagnostics WHERE roomId = :roomId AND isResolved = 0")
    fun getActiveProblemCountForRoom(roomId: Long): Flow<Int>
}
