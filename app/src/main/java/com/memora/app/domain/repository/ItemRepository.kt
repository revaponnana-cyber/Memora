package com.memora.app.domain.repository

import com.memora.app.domain.model.Item
import com.memora.app.domain.model.ItemSummary
import com.memora.app.domain.model.ItemWithPlaces
import com.memora.app.domain.model.MovementRecord
import com.memora.app.domain.model.ObjectCandidate
import com.memora.app.domain.model.PlacePath
import kotlinx.coroutines.flow.Flow

/**
 * Storage contract for items and places. The Room implementation lives in
 * `data/`; domain logic and tests depend only on this interface.
 *
 * Invariants the implementation must guarantee:
 * - Item display names are NOT unique; ids are UUIDs.
 * - Moving an item to the place it is already in creates NO event (§4.6).
 * - Repeating the same recorded action shortly after does not duplicate events.
 * - Events are never deleted or rewritten; corrections/undo append new events.
 * - Place chains are created/reused deterministically, without duplicate addresses.
 */
interface ItemRepository {

    fun observeItems(): Flow<List<ItemSummary>>

    fun observeItem(id: String): Flow<ItemWithPlaces?>

    suspend fun getItem(id: String): Item?

    /** One-shot list row for [id], including its current place path. */
    suspend fun itemSummary(id: String): ItemSummary?

    /** All items whose match key equals [matchKey] (name-based candidate lookup). */
    suspend fun findCandidates(matchKey: String): List<ObjectCandidate>

    suspend fun findPlace(chain: List<String>): PlacePath?

    /** Creates (or reuses) each place in the chain and returns the resolved path. */
    suspend fun ensurePlace(chain: List<String>): PlacePath

    /**
     * Creates a new physical item (fresh UUID — duplicate names are allowed)
     * and records its first placement in one transaction.
     */
    suspend fun createItem(
        name: String,
        description: String?,
        placeChain: List<String>,
        eventTime: Long,
        recordedTime: Long,
        source: String,
        sentence: String?,
        confirmed: Boolean,
    ): Item

    /**
     * Moves an item to the given place chain. Handles: same-place no-op,
     * recent-duplicate suppression, and the update+event in ONE transaction.
     */
    suspend fun moveItem(
        itemId: String,
        placeChain: List<String>,
        eventTime: Long,
        recordedTime: Long,
        source: String,
        sentence: String?,
        confirmed: Boolean,
        confidence: Double,
    ): MoveOutcome

    /** Restores a previous location (undo/correction) by appending a new event. */
    suspend fun moveToLocation(
        itemId: String,
        locationId: String?,
        eventType: String,
        recordedTime: Long,
        source: String,
        sentence: String?,
    ): MoveOutcome

    suspend fun history(itemId: String): List<MovementRecord>

    suspend fun searchItems(query: String): List<ItemSummary>
}

sealed interface MoveOutcome {
    data class Moved(
        val item: Item,
        val path: PlacePath?,
        val previousLocationId: String?,
        val previousPathDisplay: String?,
    ) : MoveOutcome

    data class Unchanged(val item: Item, val path: PlacePath?) : MoveOutcome
    data object NotFound : MoveOutcome
}
