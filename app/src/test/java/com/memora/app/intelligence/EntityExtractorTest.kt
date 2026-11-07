package com.memora.app.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntityExtractorTest {

    private fun extract(sentence: String): EntityExtractor.Extraction? =
        EntityExtractor.extract(TextNormalizer.normalize(sentence))

    @Test
    fun `nested place chain is innermost first`() {
        val result = extract("I put my car keys in my drawer in bedroom")
        assertEquals("car keys", result?.itemPhrase)
        assertEquals(listOf("drawer", "bedroom"), result?.placeChain)
    }

    @Test
    fun `single place sentences`() {
        val result = extract("I put my car keys at the window")
        assertEquals("car keys", result?.itemPhrase)
        assertEquals(listOf("window"), result?.placeChain)
    }

    @Test
    fun `moved and took use the destination after to`() {
        val result = extract("I moved my charger to the kitchen")
        assertEquals("charger", result?.itemPhrase)
        assertEquals(listOf("kitchen"), result?.placeChain)
    }

    @Test
    fun `state sentences is in work`() {
        val result = extract("my passport is in the bedroom cupboard")
        assertEquals("passport", result?.itemPhrase)
        assertEquals(listOf("bedroom cupboard"), result?.placeChain)
    }

    @Test
    fun `surface placement with nested kitchen`() {
        val result = extract("I left my glasses on the table in the kitchen")
        assertEquals("glasses", result?.itemPhrase)
        assertEquals(listOf("table", "kitchen"), result?.placeChain)
    }

    @Test
    fun `unrelated text yields no extraction`() {
        assertNull(extract("hello there"))
        assertNull(extract("sing me a song"))
    }

    @Test
    fun `place chain parser splits on in and strips determiners`() {
        assertEquals(
            listOf("drawer", "bedroom"),
            EntityExtractor.parsePlaceChain("my drawer in the bedroom"),
        )
        assertEquals(listOf("window"), EntityExtractor.parsePlaceChain("the window"))
    }
}
