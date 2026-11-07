package com.memora.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.memora.app.data.entity.ObjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjectDao {

    @Query("SELECT * FROM objects WHERE archived_at IS NULL AND status = 'ACTIVE' ORDER BY updated_at DESC")
    fun observeActive(): Flow<List<ObjectEntity>>

    @Query("SELECT * FROM objects WHERE id = :id")
    fun observeById(id: String): Flow<ObjectEntity?>

    @Query("SELECT * FROM objects WHERE id = :id")
    suspend fun getById(id: String): ObjectEntity?

    @Query(
        "SELECT * FROM objects WHERE match_key = :matchKey AND archived_at IS NULL " +
            "ORDER BY created_at ASC",
    )
    suspend fun getByMatchKey(matchKey: String): List<ObjectEntity>

    @Query(
        "SELECT * FROM objects WHERE archived_at IS NULL AND status = 'ACTIVE' AND " +
            "(canonical_name LIKE '%' || :query || '%' OR IFNULL(description, '') LIKE '%' || :query || '%') " +
            "ORDER BY updated_at DESC",
    )
    suspend fun search(query: String): List<ObjectEntity>

    @Insert
    suspend fun insert(obj: ObjectEntity)

    @Query("UPDATE objects SET current_location_id = :locationId, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateCurrentLocation(id: String, locationId: String?, updatedAt: Long)

    @Query("UPDATE objects SET status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)
}
