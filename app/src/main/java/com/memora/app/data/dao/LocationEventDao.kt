package com.memora.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.memora.app.data.entity.LocationEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationEventDao {

    @Insert
    suspend fun insert(event: LocationEventEntity)

    @Query("SELECT * FROM location_events WHERE object_id = :objectId ORDER BY event_time DESC, recorded_time DESC")
    suspend fun forObject(objectId: String): List<LocationEventEntity>

    @Query("SELECT * FROM location_events WHERE object_id = :objectId ORDER BY event_time DESC, recorded_time DESC")
    fun observeForObject(objectId: String): Flow<List<LocationEventEntity>>

    @Query(
        "SELECT * FROM location_events WHERE object_id = :objectId " +
            "ORDER BY recorded_time DESC LIMIT 1",
    )
    suspend fun latestFor(objectId: String): LocationEventEntity?

    @Query("SELECT * FROM location_events WHERE operation_id = :operationId LIMIT 1")
    suspend fun findByOperationId(operationId: String): LocationEventEntity?
}
