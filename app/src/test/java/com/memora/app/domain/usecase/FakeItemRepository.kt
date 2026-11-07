package com.memora.app.domain.usecase

import com.memora.app.domain.model.Item
import com.memora.app.domain.model.ItemStatus
import com.memora.app.domain.model.ItemSummary
import com.memora.app.domain.model.ItemWithPlaces
import com.memora.app.domain.model.MovementRecord
import com.memora.app.domain.model.ObjectCandidate
import com.memora.app.domain.model.PlacePath
import com.memora.app.domain.repository.ItemRepository
import com.memora.app.domain.repository.MoveOutcome
import com.memora.app.intelligence.TextNormalizer
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * In-memory [ItemRepository] for pure JVM tests of the domain layer.
 * The Room implementation's transactional guarantees are covered by
 * instrumented tests (MemoraDatabaseTest); this fake keeps only enough
 * semantics to exercise the use cases.
 */
class FakeItemRepository : ItemRepository {

    class FakeItem(
        val item: Item,
        val matchKey: String,
        /** Place chain innermost-first, mirroring the real repository. */
        val chain: MutableList<String> = mutableListOf(),
    )

    val items = linkedMapOf<String, FakeItem>()
    val moves = mutableListOf<String>()

    fun addExisting(id: String, name: String, chain: List<String> = emptyList()) {
        val now = System.currentTimeMillis()
        items[id] = FakeItem(
            item = Item(id, name, null, ItemStatus.ACTIVE, chain.firstOrNull(), now, now),
            matchKey = TextNormalizer.matchKey(name),
            chain = chain.toMutableList(),
        )
    }

    override fun observeItems(): Flow<List<ItemSummary>> = flow {
        emit(items.values.map { it.summary() })
    }

    override fun observeItem(id: String): Flow<ItemWithPlaces?> = flow {
        emit(items[id]?.let { ItemWithPlaces(it.summary(), null, emptyList()) })
    }

    override suspend fun getItem(id: String): Item? = items[id]?.item

    override suspend fun itemSummary(id: String): ItemSummary? = items[id]?.summary()

    override suspend fun findCandidates(matchKey: String): List<ObjectCandidate> =
        items.values
            .filter { it.matchKey == matchKey }
            .map {
                ObjectCandidate(
                    id = it.item.id,
                    name = it.item.name,
                    description = null,
                    currentPathDisplay = it.pathOrNull()?.display(),
                )
            }

    override suspend fun findPlace(chain: List<String>): PlacePath? =
        chain.takeIf { it.isNotEmpty() }?.let { PlacePath(it.first(), it.reversed()) }

    override suspend fun ensurePlace(chain: List<String>): PlacePath =
        PlacePath(chain.first(), chain.reversed())

    override suspend fun createItem(
        name: String,
        description: String?,
        placeChain: List<String>,
        eventTime: Long,
        recordedTime: Long,
        source: String,
        sentence: String?,
        confirmed: Boolean,
    ): Item {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val item = Item(id, name, description, ItemStatus.ACTIVE, placeChain.firstOrNull(), now, now)
        items[id] = FakeItem(item, TextNormalizer.matchKey(name), placeChain.toMutableList())
        moves.add("CREATE:$name:${placeChain.joinToString(">")}")
        return item
    }

    override suspend fun moveItem(
        itemId: String,
        placeChain: List<String>,
        eventTime: Long,
        recordedTime: Long,
        source: String,
        sentence: String?,
        confirmed: Boolean,
        confidence: Double,
    ): MoveOutcome {
        val fake = items[itemId] ?: return MoveOutcome.NotFound
        val target = placeChain.firstOrNull()
        if (fake.item.currentLocationId == target) {
            return MoveOutcome.Unchanged(fake.item, fake.pathOrNull())
        }
        val previous = fake.item.currentLocationId
        val previousPath = fake.pathOrNull()
        val updated = fake.item.copy(
            currentLocationId = target,
            updatedAt = System.currentTimeMillis(),
        )
        fake.chain.clear()
        fake.chain.addAll(placeChain)
        items[itemId] = FakeItem(updated, fake.matchKey, fake.chain)
        moves.add("MOVE:$itemId:${placeChain.joinToString(">")}")
        return MoveOutcome.Moved(
            item = updated,
            path = fake.pathOrNull(),
            previousLocationId = previous,
            previousPathDisplay = previousPath?.display(),
        )
    }

    override suspend fun moveToLocation(
        itemId: String,
        locationId: String?,
        eventType: String,
        recordedTime: Long,
        source: String,
        sentence: String?,
    ): MoveOutcome {
        val fake = items[itemId] ?: return MoveOutcome.NotFound
        val previous = fake.item.currentLocationId
        val previousPath = fake.pathOrNull()
        val updated = fake.item.copy(
            currentLocationId = locationId,
            updatedAt = System.currentTimeMillis(),
        )
        items[itemId] = FakeItem(updated, fake.matchKey, fake.chain)
        moves.add("MOVE_TO:$itemId:$locationId")
        return MoveOutcome.Moved(updated, fake.pathOrNull(), previous, previousPath?.display())
    }

    override suspend fun history(itemId: String): List<MovementRecord> = emptyList()

    override suspend fun searchItems(query: String): List<ItemSummary> =
        items.values
            .filter { it.item.name.contains(query, ignoreCase = true) }
            .map { it.summary() }

    private fun FakeItem.pathOrNull(): PlacePath? =
        chain.takeIf { it.isNotEmpty() }?.let { PlacePath(it.first(), it.reversed()) }

    private fun FakeItem.summary(): ItemSummary =
        ItemSummary(item.id, item.name, item.description, pathOrNull(), item.updatedAt)
}
