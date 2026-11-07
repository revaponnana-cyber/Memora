package com.memora.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One movement/placement event. Events are append-only: corrections and undo
 * add new events, they never rewrite old ones.
 *
 * [eventTime] = when the movement actually happened ("yesterday").
 * [recordedTime] = when Memora learned about it ("today").
 * [operationId] = idempotency key; duplicates of the same recorded action are
 * suppressed (assignment §4.6).
 */
@Entity(
    tableName = "location_events",
    indices = [
        Index("object_id"),
        Index("to_location_id"),
        Index(value = ["operation_id"], unique = true),
    ],
    foreignKeys = [
        // History must survive: an object with events cannot be deleted, only archived.
        ForeignKey(
            entity = ObjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["object_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["from_location_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_location_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class LocationEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "object_id") val objectId: String,
    @ColumnInfo(name = "from_location_id") val fromLocationId: String?,
    @ColumnInfo(name = "to_location_id") val toLocationId: String?,
    /** MOVED / PLACED / UNDO / CORRECTED */
    @ColumnInfo(name = "event_type") val eventType: String,
    @ColumnInfo(name = "event_time") val eventTime: Long,
    @ColumnInfo(name = "recorded_time") val recordedTime: Long,
    /** SENTENCE / MANUAL / UNDO */
    val source: String,
    @ColumnInfo(name = "original_sentence") val originalSentence: String?,
    val confidence: Double?,
    val confirmed: Boolean,
    @ColumnInfo(name = "operation_id") val operationId: String,
)
