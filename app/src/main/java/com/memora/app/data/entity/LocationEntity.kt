package com.memora.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A place in the storage hierarchy (room, cupboard, box, surface…).
 * Nesting is via [parentLocationId]: "Drawer" inside "Bedroom".
 * (name, parent) is unique — no duplicate addresses for the same context.
 */
@Entity(
    tableName = "locations",
    indices = [
        Index("parent_location_id"),
        Index(value = ["name", "parent_location_id"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_location_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class LocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** ROOM / CONTAINER / SURFACE / AREA */
    @ColumnInfo(name = "location_type") val locationType: String,
    @ColumnInfo(name = "parent_location_id") val parentLocationId: String?,
    val description: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "archived_at") val archivedAt: Long?,
)
