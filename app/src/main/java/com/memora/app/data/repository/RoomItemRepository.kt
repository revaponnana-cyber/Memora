package com.memora.app.data.repository

import androidx.room.withTransaction
import com.memora.app.data.MemoraDatabase
import com.memora.app.data.entity.LocationEntity
import com.memora.app.data.entity.LocationEventEntity
import com.memora.app.data.entity.ObjectEntity
import com.memora.app.domain.model.Item
import com.memora.app.domain.model.ItemStatus
import com.memora.app.domain.model.ItemSummary
import com.memora.app.domain.model.ItemWithPlaces
import com.memora.app.domain.model.MovementRecord
import com.memora.app.domain.model.ObjectCandidate
import com.memora.app.domain.model.PastPlace
import com.memora.app.domain.model.PlacePath
import com.memora.app.domain.model.PlaceType
import com.memora.app.domain.repository.ItemRepository
import com.memora.app.domain.repository.MoveOutcome
import com.memora.app.intelligence.TextNormalizer
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Room-backed implementation of [ItemRepository].
 *
 * All writes that change an item's location run in ONE transaction
 * (assignment §4.6): validate → read previous location → write event → update
 * current location. Same-place moves and repeated submissions produce no
 * duplicate events. Events are append-only.
 */
class RoomItemRepository(
    private val database: MemoraDatabase,
) : ItemRepository {

    private val objects = database.objectDao()
    private val locations = database.locationDao()
    private val events = database.locationEventDao()

    override fun observeItems(): Flow<List<ItemSummary>> =
        combine(objects.observeActive(), locations.observeAll()) { objs, locs ->
            val byId = locs.associateBy { it.id }
            objs.map { toSummary(it, byId) }
        }

    override fun observeItem(id: String): Flow<ItemWithPlaces?> =
        combine(
            objects.observeById(id),
            locations.observeAll(),
            events.observeForObject(id),
        ) { obj, locs, evts ->
            obj?.let { toDetail(it, locs.associateBy { l -> l.id }, evts) }
        }

    override suspend fun getItem(id: String): Item? = objects.getById(id)?.toDomain()

    override suspend fun itemSummary(id: String): ItemSummary? {
        val obj = objects.getById(id) ?: return null
        val byId = locations.getByIds(listOfNotNull(obj.currentLocationId))
            .associateBy { it.id }
        return toSummary(obj, byId)
    }

    override suspend fun findCandidates(matchKey: String): List<ObjectCandidate> {
        val byId = allLocationsById()
        return objects.getByMatchKey(matchKey).map { obj ->
            val path = obj.currentLocationId?.let { pathOf(it, byId) }
            ObjectCandidate(
                id = obj.id,
                name = obj.canonicalName,
                description = obj.description,
                currentPathDisplay = path?.display(),
            )
        }
    }

    override suspend fun findPlace(chain: List<String>): PlacePath? {
        if (chain.isEmpty()) return null
        val byId = allLocationsById()
        val byParent = byId.values.groupBy { it.parentLocationId }
        var parentId: String? = null
        var leafId: String? = null
        for (name in chain.reversed()) {
            val match = byParent[parentId].orEmpty()
                .firstOrNull { it.name.equals(name, ignoreCase = true) && !isArchived(it) }
                ?: return null
            leafId = match.id
            parentId = match.id
        }
        return leafId?.let { pathOf(it, byId) }
    }

    override suspend fun ensurePlace(chain: List<String>): PlacePath {
        require(chain.isNotEmpty()) { "place chain must not be empty" }
        var parentId: String? = null
        var leafId: String = ""
        for ((index, name) in chain.reversed().withIndex()) {
            val existing = locations.findChild(name, parentId)
            val place = existing ?: LocationEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                locationType = guessType(index, chain.size),
                parentLocationId = parentId,
                description = null,
                createdAt = System.currentTimeMillis(),
                archivedAt = null,
            ).also { locations.insert(it) }
            parentId = place.id
            leafId = place.id
        }
        return pathOf(leafId, allLocationsById())
            ?: PlacePath(leafId, chain.reversed())
    }

    override suspend fun createItem(
        name: String,
        description: String?,
        placeChain: List<String>,
        eventTime: Long,
        recordedTime: Long,
        source: String,
        sentence: String?,
        confirmed: Boolean,
    ): Item = database.withTransaction {
        val place = if (placeChain.isEmpty()) null else ensurePlace(placeChain)
        val now = System.currentTimeMillis()
        val obj = ObjectEntity(
            id = UUID.randomUUID().toString(),
            canonicalName = TextNormalizer.stripDeterminer(name),
            matchKey = TextNormalizer.matchKey(name),
            description = description,
            quantity = 1,
            status = ItemStatus.ACTIVE.name,
            currentLocationId = place?.leafId,
            createdAt = now,
            updatedAt = now,
            archivedAt = null,
        )
        objects.insert(obj)
        events.insert(
            LocationEventEntity(
                id = UUID.randomUUID().toString(),
                objectId = obj.id,
                fromLocationId = null,
                toLocationId = place?.leafId,
                eventType = "PLACED",
                eventTime = eventTime,
                recordedTime = recordedTime,
                source = source,
                originalSentence = sentence,
                confidence = if (confirmed) 1.0 else 0.5,
                confirmed = confirmed,
                operationId = UUID.randomUUID().toString(),
            ),
        )
        obj.toDomain()
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
    ): MoveOutcome = database.withTransaction {
        val obj = objects.getById(itemId) ?: return@withTransaction MoveOutcome.NotFound
        if (placeChain.isEmpty()) {
            return@withTransaction MoveOutcome.Unchanged(obj.toDomain(), null)
        }
        val place = ensurePlace(placeChain)

        // Already there: do NOT create a movement event (assignment §4.6).
        if (obj.currentLocationId == place.leafId) {
            return@withTransaction MoveOutcome.Unchanged(obj.toDomain(), place)
        }
        // Repeated submission of the same action (double tap / re-recognition):
        // suppress when the LAST event already recorded this same target within
        // a short window. A genuine move that happens to revisit a place is
        // never suppressed (its last event points elsewhere) (assignment §4.6).
        val latest = events.latestFor(itemId)
        if (
            latest != null &&
            latest.toLocationId == place.leafId &&
            latest.recordedTime >= recordedTime - DUPLICATE_WINDOW_MS
        ) {
            return@withTransaction MoveOutcome.Unchanged(obj.toDomain(), place)
        }

        val byId = allLocationsById()
        val previousPath = obj.currentLocationId?.let { pathOf(it, byId) }
        events.insert(
            LocationEventEntity(
                id = UUID.randomUUID().toString(),
                objectId = itemId,
                fromLocationId = obj.currentLocationId,
                toLocationId = place.leafId,
                eventType = "MOVED",
                eventTime = eventTime,
                recordedTime = recordedTime,
                source = source,
                originalSentence = sentence,
                confidence = confidence,
                confirmed = confirmed,
                operationId = UUID.randomUUID().toString(),
            ),
        )
        objects.updateCurrentLocation(itemId, place.leafId, System.currentTimeMillis())
        MoveOutcome.Moved(
            item = obj.copy(currentLocationId = place.leafId, updatedAt = System.currentTimeMillis()).toDomain(),
            path = place,
            previousLocationId = obj.currentLocationId,
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
    ): MoveOutcome = database.withTransaction {
        val obj = objects.getById(itemId) ?: return@withTransaction MoveOutcome.NotFound
        val byId = allLocationsById()
        if (obj.currentLocationId == locationId) {
            return@withTransaction MoveOutcome.Unchanged(
                obj.toDomain(),
                locationId?.let { pathOf(it, byId) },
            )
        }
        val previousPath = obj.currentLocationId?.let { pathOf(it, byId) }
        events.insert(
            LocationEventEntity(
                id = UUID.randomUUID().toString(),
                objectId = itemId,
                fromLocationId = obj.currentLocationId,
                toLocationId = locationId,
                eventType = eventType,
                eventTime = recordedTime,
                recordedTime = recordedTime,
                source = source,
                originalSentence = sentence,
                confidence = 1.0,
                confirmed = true,
                operationId = UUID.randomUUID().toString(),
            ),
        )
        objects.updateCurrentLocation(itemId, locationId, System.currentTimeMillis())
        MoveOutcome.Moved(
            item = obj.copy(currentLocationId = locationId, updatedAt = System.currentTimeMillis()).toDomain(),
            path = locationId?.let { pathOf(it, byId) },
            previousLocationId = obj.currentLocationId,
            previousPathDisplay = previousPath?.display(),
        )
    }

    override suspend fun history(itemId: String): List<MovementRecord> =
        events.forObject(itemId).map { it.toDomain() }

    override suspend fun searchItems(query: String): List<ItemSummary> {
        val byId = allLocationsById()
        return objects.search(query.trim()).map { toSummary(it, byId) }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private suspend fun allLocationsById(): Map<String, LocationEntity> =
        locations.getAll().associateBy { it.id }

    private fun isArchived(location: LocationEntity): Boolean = location.archivedAt != null

    private fun guessType(indexFromOuter: Int, size: Int): String = when {
        size > 1 && indexFromOuter == 0 -> PlaceType.ROOM.name
        else -> PlaceType.CONTAINER.name
    }

    /** Walks parent links: Bedroom ▸ Drawer (outermost first). */
    private fun pathOf(leafId: String, byId: Map<String, LocationEntity>): PlacePath? {
        val names = ArrayDeque<String>()
        var current = byId[leafId] ?: return null
        var guard = 0
        while (guard++ < MAX_DEPTH) {
            names.addFirst(current.name)
            val parentId = current.parentLocationId ?: break
            current = byId[parentId] ?: break
        }
        return PlacePath(leafId, names.toList())
    }

    private fun toSummary(obj: ObjectEntity, byId: Map<String, LocationEntity>): ItemSummary =
        ItemSummary(
            id = obj.id,
            name = obj.canonicalName,
            description = obj.description,
            currentPath = obj.currentLocationId?.let { pathOf(it, byId) },
            updatedAt = obj.updatedAt,
        )

    private fun toDetail(
        obj: ObjectEntity,
        byId: Map<String, LocationEntity>,
        events: List<LocationEventEntity>,
    ): ItemWithPlaces {
        val summary = toSummary(obj, byId)
        val currentLeaf = obj.currentLocationId
        val currentSince = events
            .filter { it.toLocationId == currentLeaf }
            .maxOfOrNull { it.eventTime }

        // Distinct past addresses, newest first, one entry per place (no repeats).
        val past = LinkedHashMap<String, PastPlace>()
        for (event in events) {
            val to = event.toLocationId ?: continue
            if (to == currentLeaf) continue
            if (past.containsKey(to)) continue
            val path = pathOf(to, byId) ?: continue
            past[to] = PastPlace(path = path, lastSeenAt = event.eventTime)
        }
        return ItemWithPlaces(
            item = summary,
            currentSince = currentSince,
            pastPlaces = past.values.sortedByDescending { it.lastSeenAt },
        )
    }

    private fun ObjectEntity.toDomain(): Item = Item(
        id = id,
        name = canonicalName,
        description = description,
        status = runCatching { ItemStatus.valueOf(status) }.getOrDefault(ItemStatus.ACTIVE),
        currentLocationId = currentLocationId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun LocationEventEntity.toDomain(): MovementRecord = MovementRecord(
        id = id,
        itemId = objectId,
        fromLocationId = fromLocationId,
        toLocationId = toLocationId,
        eventType = eventType,
        eventTime = eventTime,
        recordedTime = recordedTime,
        source = source,
        originalSentence = originalSentence,
        confirmed = confirmed,
    )

    private companion object {
        const val DUPLICATE_WINDOW_MS = 10_000L
        const val MAX_DEPTH = 32
    }
}
