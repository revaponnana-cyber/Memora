package com.memora.app.domain.usecase

import com.memora.app.domain.model.Item
import com.memora.app.domain.model.PlacePath
import com.memora.app.domain.repository.ItemRepository
import com.memora.app.domain.repository.MoveOutcome

/** Manual "Move" action (e.g. from the item screen). Reuses the place-chain rules. */
class MoveItemUseCase(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke(
        itemId: String,
        placeChain: List<String>,
        sentence: String? = null,
    ): MoveOutcome = repository.moveItem(
        itemId = itemId,
        placeChain = placeChain,
        eventTime = System.currentTimeMillis(),
        recordedTime = System.currentTimeMillis(),
        source = "MANUAL",
        sentence = sentence,
        confirmed = true,
        confidence = 1.0,
    )
}

/** Manual item creation (Add tab). Duplicate display names are welcome. */
class AddItemUseCase(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke(
        name: String,
        description: String?,
        placeChain: List<String>,
    ): Item = repository.createItem(
        name = name.trim(),
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        placeChain = placeChain,
        eventTime = System.currentTimeMillis(),
        recordedTime = System.currentTimeMillis(),
        source = "MANUAL",
        sentence = null,
        confirmed = true,
    )
}

/**
 * Undo a move by restoring the previous location. History is preserved: the
 * undo itself appends a new event (type UNDO) — nothing is deleted.
 */
class UndoMoveUseCase(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke(token: UndoToken): MoveOutcome = repository.moveToLocation(
        itemId = token.itemId,
        locationId = token.previousLocationId,
        eventType = "UNDO",
        recordedTime = System.currentTimeMillis(),
        source = "UNDO",
        sentence = "Undo → ${token.previousPathDisplay ?: "previous location"}",
    )
}
