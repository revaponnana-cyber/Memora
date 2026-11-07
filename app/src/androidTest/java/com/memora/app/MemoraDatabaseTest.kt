package com.memora.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.MemoraDatabase
import com.memora.app.data.repository.RoomItemRepository
import com.memora.app.domain.repository.MoveOutcome
import com.memora.app.intelligence.TextNormalizer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Database invariants of the item slice (assignment §4.6):
 * UUID identity with duplicate names, nested places without duplicate
 * addresses, append-only movement history, idempotent moves.
 */
@RunWith(AndroidJUnit4::class)
class MemoraDatabaseTest {

    private lateinit var database: MemoraDatabase
    private lateinit var repository: RoomItemRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MemoraDatabase::class.java).build()
        repository = RoomItemRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sameNamesGetDistinctIdsAndBothAreFound() = runBlocking {
        val a = repository.createItem(
            name = "Car key", description = null, placeChain = listOf("table"),
            eventTime = 1_000L, recordedTime = 1_000L, source = "MANUAL", sentence = null, confirmed = true,
        )
        val b = repository.createItem(
            name = "Car key", description = null, placeChain = listOf("table"),
            eventTime = 2_000L, recordedTime = 2_000L, source = "MANUAL", sentence = null, confirmed = true,
        )

        assertNotEquals(a.id, b.id)
        val candidates = repository.findCandidates(TextNormalizer.matchKey("Car key"))
        assertEquals(2, candidates.size)
    }

    @Test
    fun nestedChainCreatesOnePlacePerAddressAndReusesThem() = runBlocking {
        repository.createItem(
            name = "car keys", description = null, placeChain = listOf("drawer", "bedroom"),
            eventTime = 1L, recordedTime = 1L, source = "SENTENCE", sentence = null, confirmed = true,
        )
        repository.createItem(
            name = "book", description = null, placeChain = listOf("drawer", "bedroom"),
            eventTime = 2L, recordedTime = 2L, source = "SENTENCE", sentence = null, confirmed = true,
        )

        // Only two addresses exist: Bedroom and its child Drawer — created once.
        assertEquals(2, database.locationDao().getAll().size)
        val path = repository.findPlace(listOf("drawer", "bedroom"))
        assertEquals("Bedroom ▸ Drawer", path?.display())
    }

    @Test
    fun moveRecordsHistoryEventAndUpdatesCurrentPlace() = runBlocking {
        val item = repository.createItem(
            name = "charger", description = null, placeChain = listOf("kitchen"),
            eventTime = 1L, recordedTime = 1L, source = "SENTENCE", sentence = null, confirmed = true,
        )
        val outcome = repository.moveItem(
            itemId = item.id, placeChain = listOf("window"),
            eventTime = 2L, recordedTime = 2L, source = "SENTENCE", sentence = "s2", confirmed = true, confidence = 1.0,
        )

        assertTrue(outcome is MoveOutcome.Moved)
        val history = repository.history(item.id)
        assertEquals(2, history.size) // PLACED + MOVED

        val detail = repository.observeItem(item.id).first()!!
        assertEquals("Window", detail.item.currentPath?.display())
        assertEquals(1, detail.pastPlaces.size)
        assertEquals("Kitchen", detail.pastPlaces.first().path.display())
    }

    @Test
    fun samePlaceMoveCreatesNoEvent() = runBlocking {
        val item = repository.createItem(
            name = "charger", description = null, placeChain = listOf("kitchen"),
            eventTime = 1L, recordedTime = 1L, source = "SENTENCE", sentence = null, confirmed = true,
        )
        val outcome = repository.moveItem(
            itemId = item.id, placeChain = listOf("kitchen"),
            eventTime = 2L, recordedTime = 2L, source = "SENTENCE", sentence = "s", confirmed = true, confidence = 1.0,
        )

        assertTrue(outcome is MoveOutcome.Unchanged)
        assertEquals(1, repository.history(item.id).size) // still only PLACED
    }

    @Test
    fun repeatedSubmissionWithinWindowIsSuppressed() = runBlocking {
        val item = repository.createItem(
            name = "charger", description = null, placeChain = listOf("kitchen"),
            eventTime = 1L, recordedTime = 1_000L, source = "SENTENCE", sentence = null, confirmed = true,
        )
        repository.moveItem(
            itemId = item.id, placeChain = listOf("window"),
            eventTime = 2L, recordedTime = 2_000L, source = "SENTENCE", sentence = "s", confirmed = true, confidence = 1.0,
        )
        val repeat = repository.moveItem(
            itemId = item.id, placeChain = listOf("window"),
            eventTime = 3L, recordedTime = 3_000L, source = "SENTENCE", sentence = "s", confirmed = true, confidence = 1.0,
        )

        assertTrue(repeat is MoveOutcome.Unchanged)
        assertEquals(2, repository.history(item.id).size) // PLACED + one MOVED
    }

    @Test
    fun genuineRevisitIsNotSuppressedAsDuplicate() = runBlocking {
        val item = repository.createItem(
            name = "charger", description = null, placeChain = listOf("kitchen"),
            eventTime = 1L, recordedTime = 1_000L, source = "SENTENCE", sentence = null, confirmed = true,
        )
        repository.moveItem(
            itemId = item.id, placeChain = listOf("window"),
            eventTime = 2L, recordedTime = 2_000L, source = "SENTENCE", sentence = "s", confirmed = true, confidence = 1.0,
        )
        // Back to the kitchen within the window — a real move, so it records.
        val revisit = repository.moveItem(
            itemId = item.id, placeChain = listOf("kitchen"),
            eventTime = 3L, recordedTime = 3_000L, source = "SENTENCE", sentence = "s2", confirmed = true, confidence = 1.0,
        )

        assertTrue(revisit is MoveOutcome.Moved)
        assertEquals(3, repository.history(item.id).size)
    }

    @Test
    fun pastPlacesAreDeduplicated() = runBlocking {
        val item = repository.createItem(
            name = "car keys", description = null, placeChain = listOf("drawer", "bedroom"),
            eventTime = 1L, recordedTime = 1_000L, source = "SENTENCE", sentence = null, confirmed = true,
        )
        repository.moveItem(
            itemId = item.id, placeChain = listOf("window"),
            eventTime = 2L, recordedTime = 2_000L, source = "SENTENCE", sentence = null, confirmed = true, confidence = 1.0,
        )
        repository.moveItem(
            itemId = item.id, placeChain = listOf("drawer", "bedroom"),
            eventTime = 3L, recordedTime = 3_000L, source = "SENTENCE", sentence = null, confirmed = true, confidence = 1.0,
        )
        repository.moveItem(
            itemId = item.id, placeChain = listOf("window"),
            eventTime = 4L, recordedTime = 4_000L, source = "SENTENCE", sentence = null, confirmed = true, confidence = 1.0,
        )

        val detail = repository.observeItem(item.id).first()!!
        assertEquals("Window", detail.item.currentPath?.display())
        // The drawer was visited twice — it appears as ONE past address.
        assertEquals(1, detail.pastPlaces.size)
        assertEquals("Bedroom ▸ Drawer", detail.pastPlaces.first().path.display())
    }

    @Test
    fun undoRestoresPreviousPlaceAndKeepsHistory() = runBlocking {
        val item = repository.createItem(
            name = "charger", description = null, placeChain = listOf("kitchen"),
            eventTime = 1L, recordedTime = 1L, source = "SENTENCE", sentence = null, confirmed = true,
        )
        val move = repository.moveItem(
            itemId = item.id, placeChain = listOf("window"),
            eventTime = 2L, recordedTime = 2L, source = "SENTENCE", sentence = null, confirmed = true, confidence = 1.0,
        )
        move as MoveOutcome.Moved

        val undo = repository.moveToLocation(
            itemId = item.id,
            locationId = move.previousLocationId,
            eventType = "UNDO",
            recordedTime = 3L,
            source = "UNDO",
            sentence = "undo",
        )

        assertTrue(undo is MoveOutcome.Moved)
        val detail = repository.observeItem(item.id).first()!!
        assertEquals("Kitchen", detail.item.currentPath?.display())
        // Nothing was rewritten: PLACED + MOVED + UNDO.
        assertEquals(3, repository.history(item.id).size)
        // The undone window stays in past places.
        assertTrue(detail.pastPlaces.any { it.path.display() == "Window" })
    }

    @Test
    fun locationOfUnknownPlaceIsNotRecreatedTwice() = runBlocking {
        val first = repository.ensurePlace(listOf("drawer", "bedroom"))
        val second = repository.ensurePlace(listOf("drawer", "bedroom"))
        assertEquals(first.leafId, second.leafId)
        assertEquals(2, database.locationDao().getAll().size)
    }
}
