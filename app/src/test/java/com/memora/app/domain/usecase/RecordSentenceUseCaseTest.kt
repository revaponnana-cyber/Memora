package com.memora.app.domain.usecase

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests of the sentence agent end to end (no Android, no database):
 * sentence → pipeline → identity resolution → outcome.
 */
class RecordSentenceUseCaseTest {

    private val repository = FakeItemRepository()
    private val useCase = RecordSentenceUseCase(repository)

    @Test
    fun `main example sentence creates a new item with nested place`() = runBlocking {
        val outcome = useCase.execute("i put my car keys in my drawer in bedroom")

        assertTrue(outcome is RecordSentenceUseCase.Outcome.Saved)
        outcome as RecordSentenceUseCase.Outcome.Saved
        assertEquals("car keys", outcome.item.name)
        assertEquals("Bedroom ▸ Drawer", outcome.path?.display())
        // Creation has no undo token — undo is for moves.
        assertNull(outcome.undo)
    }

    @Test
    fun `second sentence moves the SAME object and keeps its past location`() = runBlocking {
        useCase.execute("i put my car keys in my drawer in bedroom")
        val outcome = useCase.execute("i put my car keys at the window")

        assertTrue(outcome is RecordSentenceUseCase.Outcome.Saved)
        outcome as RecordSentenceUseCase.Outcome.Saved
        assertEquals("Window", outcome.path?.display())
        // Undo must restore the previous place (history is never rewritten).
        assertEquals("drawer", outcome.undo?.previousLocationId)
    }

    @Test
    fun `same-named objects are ambiguous and never resolved by recency`() = runBlocking {
        repository.addExisting("1", "Car key")
        repository.addExisting("2", "Car key", listOf("table", "hall"))
        repository.addExisting("3", "Car key")

        val outcome = useCase.execute("i put my car key at the window")

        assertTrue(outcome is RecordSentenceUseCase.Outcome.NeedsChoice)
        outcome as RecordSentenceUseCase.Outcome.NeedsChoice
        assertEquals(3, outcome.candidates.size)
    }

    @Test
    fun `explicit choice moves exactly that object`() = runBlocking {
        repository.addExisting("1", "Car key")
        repository.addExisting("2", "Car key")

        val first = useCase.execute("i put my car key at the window")
        assertTrue(first is RecordSentenceUseCase.Outcome.NeedsChoice)
        first as RecordSentenceUseCase.Outcome.NeedsChoice

        val outcome = useCase.executeChosen(first.statement, "2")
        assertTrue(outcome is RecordSentenceUseCase.Outcome.Saved)
        assertEquals("2", (outcome as RecordSentenceUseCase.Outcome.Saved).item.id)
        assertEquals("Window", outcome.path?.display())
    }

    @Test
    fun `none of these adds a NEW physical object with the same name`() = runBlocking {
        repository.addExisting("1", "Car key")
        repository.addExisting("2", "Car key")

        val first = useCase.execute("i put my car key at the window")
        assertTrue(first is RecordSentenceUseCase.Outcome.NeedsChoice)
        first as RecordSentenceUseCase.Outcome.NeedsChoice

        val outcome = useCase.executeChosen(first.statement, null)
        assertTrue(outcome is RecordSentenceUseCase.Outcome.Saved)
        outcome as RecordSentenceUseCase.Outcome.Saved
        assertEquals(3, repository.items.size)
        assertTrue(outcome.item.id != "1" && outcome.item.id != "2")
        assertEquals("car key", outcome.item.name)
    }

    @Test
    fun `uncertain statements ask for confirmation instead of recording`() = runBlocking {
        val outcome = useCase.execute("i think i left my charger in the kitchen")
        assertTrue(outcome is RecordSentenceUseCase.Outcome.NeedsConfirmation)
        outcome as RecordSentenceUseCase.Outcome.NeedsConfirmation

        val confirmed = useCase.executeConfirmed(outcome.statement, confirmed = true)
        assertTrue(confirmed is RecordSentenceUseCase.Outcome.Saved)
        assertEquals("charger", (confirmed as RecordSentenceUseCase.Outcome.Saved).item.name)
    }

    @Test
    fun `uncertain statements can be recorded as uncertain rather than dropped`() = runBlocking {
        val outcome = useCase.execute("i think i left my charger in the kitchen")
        outcome as RecordSentenceUseCase.Outcome.NeedsConfirmation

        val saved = useCase.executeConfirmed(outcome.statement, confirmed = false)
        assertTrue(saved is RecordSentenceUseCase.Outcome.Saved)
        assertEquals("charger", (saved as RecordSentenceUseCase.Outcome.Saved).item.name)
        // UI-level Cancel never calls the use case at all — nothing is written.
    }

    @Test
    fun `future plans and hypotheticals are never recorded as facts`() = runBlocking {
        val plan = useCase.execute("i will move my key to the drawer")
        assertTrue(plan is RecordSentenceUseCase.Outcome.NotRecorded)

        val hypothetical = useCase.execute("what if i put my key in the drawer")
        assertTrue(hypothetical is RecordSentenceUseCase.Outcome.NotRecorded)

        assertTrue(repository.items.isEmpty())
        assertTrue(repository.moves.isEmpty())
    }

    @Test
    fun `find questions answer with the current location`() = runBlocking {
        useCase.execute("i put my car keys in my drawer in bedroom")
        val outcome = useCase.execute("where is my car keys")

        assertTrue(outcome is RecordSentenceUseCase.Outcome.Answer)
        outcome as RecordSentenceUseCase.Outcome.Answer
        assertTrue(outcome.text.contains("car keys"))
        assertTrue(outcome.text.contains("Bedroom ▸ Drawer"))
    }

    @Test
    fun `unrecognized sentences say so honestly`() = runBlocking {
        val outcome = useCase.execute("sing me a song")
        assertTrue(outcome is RecordSentenceUseCase.Outcome.NotRecorded)
        assertTrue(repository.items.isEmpty())
    }

    @Test
    fun `same place move reports Unchanged - idempotent`() = runBlocking {
        useCase.execute("i put my car keys in my drawer in bedroom")
        val outcome = useCase.execute("i put my car keys in my drawer in bedroom")

        assertTrue(outcome is RecordSentenceUseCase.Outcome.Unchanged)
        assertNotNull((outcome as RecordSentenceUseCase.Outcome.Unchanged).path)
    }
}
