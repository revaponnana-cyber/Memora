package com.memora.app.intelligence

import com.memora.app.domain.model.Intent
import com.memora.app.domain.model.Modality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * End-to-end cases of the local pipeline against the sentences the app must
 * understand first (the items slice).
 */
class SentenceInterpreterTest {

    @Test
    fun `the main example sentence parses into object and nested place`() {
        val statement = SentenceInterpreter.parse("i put my car keys in my drawer in bedroom")
        assertNotNull(statement)
        statement!!
        assertEquals(Intent.SET_LOCATION, statement.intent)
        assertEquals(Modality.STATEMENT, statement.modality)
        assertEquals("car keys", statement.itemPhrase)
        assertEquals(listOf("drawer", "bedroom"), statement.placeChain)
        assertNull(statement.eventTimeOffsetMs)
    }

    @Test
    fun `moving the same object to a new place keeps the item phrase`() {
        val statement = SentenceInterpreter.parse("i put my car keys at the window")
        assertEquals("car keys", statement?.itemPhrase)
        assertEquals(listOf("window"), statement?.placeChain)
    }

    @Test
    fun `yesterday shifts the event time but not the meaning`() {
        val statement = SentenceInterpreter.parse("I moved my charger to the kitchen yesterday")
        assertEquals("charger", statement?.itemPhrase)
        assertEquals(listOf("kitchen"), statement?.placeChain)
        assertEquals(-24L * 3_600_000L, statement?.eventTimeOffsetMs)
    }

    @Test
    fun `uncertain statements keep their modality for the confirmation step`() {
        val statement = SentenceInterpreter.parse("I think I left my charger in the kitchen")
        assertEquals(Modality.UNCERTAIN, statement?.modality)
        assertEquals(Intent.SET_LOCATION, statement?.intent)
        assertEquals("charger", statement?.itemPhrase)
    }

    @Test
    fun `future plans are parsed but flagged as INTENTION`() {
        val statement = SentenceInterpreter.parse("I will put my key in the drawer")
        assertEquals(Modality.INTENTION, statement?.modality)
    }

    @Test
    fun `blank input yields null`() {
        assertNull(SentenceInterpreter.parse("   "))
    }

    @Test
    fun `find questions parse without a place`() {
        val statement = SentenceInterpreter.parse("where is my passport")
        assertEquals(Intent.FIND_ITEM, statement?.intent)
        assertEquals("passport", statement?.itemPhrase)
        assertEquals(emptyList<String>(), statement?.placeChain)
    }
}
