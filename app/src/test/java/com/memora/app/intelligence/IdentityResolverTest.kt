package com.memora.app.intelligence

import com.memora.app.domain.model.ObjectCandidate
import com.memora.app.domain.model.ObjectResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityResolverTest {

    private fun candidate(id: String, name: String) = ObjectCandidate(
        id = id,
        name = name,
        description = null,
        currentPathDisplay = null,
    )

    @Test
    fun `no candidates means a NEW physical object`() {
        val resolution = IdentityResolver.resolve("car keys", emptyList())
        assertEquals(ObjectResolution.New, resolution)
    }

    @Test
    fun `one matching candidate is selected`() {
        val resolution = IdentityResolver.resolve(
            "car keys",
            listOf(candidate("1", "car key")),
        )
        assertTrue(resolution is ObjectResolution.Existing)
        assertEquals("1", (resolution as ObjectResolution.Existing).candidate.id)
    }

    @Test
    fun `plural and singular match the same object`() {
        val resolution = IdentityResolver.resolve(
            "my car keys",
            listOf(candidate("7", "Car key")),
        )
        assertTrue(resolution is ObjectResolution.Existing)
    }

    @Test
    fun `multiple candidates with the same name are AMBIGUOUS - never picked by recency`() {
        val resolution = IdentityResolver.resolve(
            "car key",
            listOf(
                candidate("a", "Car key"),
                candidate("b", "car key"),
                candidate("c", "Car key"),
            ),
        )
        assertTrue(resolution is ObjectResolution.Ambiguous)
        val ambiguous = resolution as ObjectResolution.Ambiguous
        assertEquals(3, ambiguous.candidates.size)
    }

    @Test
    fun `similar names are NOT merged into one candidate set`() {
        // "keys" matches only "keys", not "key" phrases with other words
        val resolution = IdentityResolver.resolve(
            "wallet",
            listOf(candidate("1", "car key"), candidate("2", "keys")),
        )
        assertEquals(ObjectResolution.New, resolution)
    }
}
