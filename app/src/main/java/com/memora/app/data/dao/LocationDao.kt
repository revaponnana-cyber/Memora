package com.memora.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.memora.app.data.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query("SELECT * FROM locations WHERE archived_at IS NULL")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE archived_at IS NULL")
    suspend fun getAll(): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun getById(id: String): LocationEntity?

    @Query("SELECT * FROM locations WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<LocationEntity>

    /** Child of a specific parent (parent NULL = top level). */
    @Query(
        "SELECT * FROM locations WHERE name = :name AND parent_location_id IS :parentId " +
            "AND archived_at IS NULL LIMIT 1",
    )
    suspend fun findChild(name: String, parentId: String?): LocationEntity?

    /** Any place with this name, whatever its parent. */
    @Query("SELECT * FROM locations WHERE name = :name AND archived_at IS NULL")
    suspend fun findByName(name: String): List<LocationEntity>

    @Insert
    suspend fun insert(location: LocationEntity)
}
