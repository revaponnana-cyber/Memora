package com.memora.app.domain.model

/** A physical item. Identity is [id] (UUID) — the display [name] is NOT unique. */
data class Item(
    val id: String,
    val name: String,
    val description: String?,
    val status: ItemStatus,
    val currentLocationId: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class ItemStatus { ACTIVE, MISSING, DISCARDED, ARCHIVED }

/** A place in the storage hierarchy (room, cupboard, box, surface…). */
data class Place(
    val id: String,
    val name: String,
    val placeType: PlaceType,
    val parentPlaceId: String?,
)

enum class PlaceType { ROOM, CONTAINER, SURFACE, AREA }

/** A resolved place chain, outermost first: Bedroom ▸ Drawer. */
data class PlacePath(
    val leafId: String,
    /** Names outermost → innermost, e.g. ["Bedroom", "Drawer"]. */
    val namesOutsideIn: List<String>,
) {
    /** Human-readable address: "Bedroom ▸ Drawer" (each name capitalized). */
    fun display(): String = namesOutsideIn.joinToString(" ▸ ") {
        it.replaceFirstChar { ch -> ch.uppercaseChar() }
    }
}

/** An item row for lists: the item plus its current place path. */
data class ItemSummary(
    val id: String,
    val name: String,
    val description: String?,
    val currentPath: PlacePath?,
    val updatedAt: Long,
)

/** Detail view: item, current place, and each distinct past place (deduplicated). */
data class ItemWithPlaces(
    val item: ItemSummary,
    val currentSince: Long?,
    /** Distinct previous places, most recent first. One entry per address. */
    val pastPlaces: List<PastPlace>,
)

data class PastPlace(
    val path: PlacePath,
    val lastSeenAt: Long,
)

/** One movement event. [eventTime] is when it happened; [recordedTime] when learned. */
data class MovementRecord(
    val id: String,
    val itemId: String,
    val fromLocationId: String?,
    val toLocationId: String?,
    val eventType: String,
    val eventTime: Long,
    val recordedTime: Long,
    val source: String,
    val originalSentence: String?,
    val confirmed: Boolean,
)
