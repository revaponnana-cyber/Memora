package com.memora.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A physical object. The primary key is a UUID; [canonicalName] is explicitly
 * NOT unique — three different car keys are three rows named "Car key".
 */
@Entity(
    tableName = "objects",
    indices = [
        Index("match_key"),
        Index("current_location_id"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["current_location_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class ObjectEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "canonical_name") val canonicalName: String,
    /** Normalized lookup key for identity resolution (see TextNormalizer.matchKey). */
    @ColumnInfo(name = "match_key") val matchKey: String,
    val description: String?,
    val quantity: Int,
    /** ACTIVE / MISSING / DISCARDED / ARCHIVED */
    val status: String,
    @ColumnInfo(name = "current_location_id") val currentLocationId: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "archived_at") val archivedAt: Long?,
)
